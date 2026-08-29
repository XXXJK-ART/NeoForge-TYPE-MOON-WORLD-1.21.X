package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.data.ImaginarySpaceData.MagicMode;
import io.github.typemoonaddon.network.EffectPayload;
import io.github.typemoonaddon.registry.ModAttachments;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.CommonHooks;

public final class ImaginaryStorageService {
    public static final TagKey<Item> ITEM_BLACKLIST = TagKey.create(
        Registries.ITEM,
        TypeMoonAddon.id("imaginary_absorption_blacklist")
    );
    public static final TagKey<Block> BLOCK_BLACKLIST = TagKey.create(
        Registries.BLOCK,
        TypeMoonAddon.id("imaginary_absorption_blacklist")
    );
    public static final TagKey<EntityType<?>> PROTECTION_DISPELLABLE = TagKey.create(
        Registries.ENTITY_TYPE,
        TypeMoonAddon.id("imaginary_protection_dispellable")
    );

    private static final Map<UUID, PendingItem> PENDING_ITEMS = new HashMap<>();
    private static final Map<UUID, PendingBlock> PENDING_BLOCKS = new HashMap<>();
    private static final Map<UUID, Long> BLOCK_CAST_HELD_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE = new HashMap<>();

    /** Applies a client selection only after validating it on the server. */
    public static boolean selectMode(ServerPlayer player, MagicMode requestedMode) {
        if (!validCaster(player)) {
            return false;
        }

        MagicMode mode = requestedMode == null ? MagicMode.STORAGE : requestedMode;
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (mode == MagicMode.STORAGE && data.protectionActive() && !data.crestWormAssimilated()) {
            notify(player, "message.typemoonaddon.mode.storage_locked");
            return false;
        }
        if (data.setMagicMode(mode)) {
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        }
        return true;
    }

    public static boolean castFromTypeMoonWorld(ServerPlayer player) {
        if (!validCaster(player)) {
            return false;
        }

        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (data.crestWormAssimilated()) {
            return false;
        }
        MagicMode mode = data.magicMode();
        if (mode == MagicMode.STORAGE && data.protectionActive()) {
            notify(player, "message.typemoonaddon.mode.storage_locked");
            return false;
        }

        if (mode == MagicMode.PROTECTION) {
            return castProtection(player, data);
        }

        return acquireStorageTarget(player, player.serverLevel().getGameTime(), false);
    }

    public static boolean castFromAbsorption(ServerPlayer player) {
        if (!validCaster(player) || !TypeMoonIntegration.isAbsorptionLearned(player)) {
            return false;
        }

        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (data.magicMode() == MagicMode.PROTECTION) {
            return castProtection(player, data);
        }

        return acquireStorageTarget(player, player.serverLevel().getGameTime(), true);
    }

    public static void tick(MinecraftServer server) {
        tickPendingItems(server);
        tickPendingBlocks(server);
        tickProtectionZones(server);
        tickProtectionDurations(server);
    }

    public static boolean isProtectionActive(Entity entity) {
        return entity instanceof ServerPlayer player
            && player.isAlive()
            && !player.isSpectator()
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).protectionActive()
            && TypeMoonIntegration.isLearned(player);
    }

    /** Accepts only key-state timing; target selection remains entirely server-side. */
    public static void updateBlockCastHeld(ServerPlayer player, boolean held) {
        if (!held) {
            BLOCK_CAST_HELD_UNTIL.remove(player.getUUID());
            return;
        }
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (validCaster(player) && data.magicMode() == MagicMode.STORAGE
            && (!data.protectionActive() || data.crestWormAssimilated())) {
            BLOCK_CAST_HELD_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + 4L);
        }
    }

    /**
     * Protection rejects ordinary combat, magic, environmental and status-tick
     * damage. Damage explicitly tagged to bypass invulnerability remains as an
     * administrative/world safety escape (for example /kill and the void).
     */
    public static boolean nullifiesDamage(ServerPlayer player, DamageSource source) {
        if (RuleBreakerDispelService.dispelFromDamage(player, source)) {
            return false;
        }
        return isProtectionActive(player) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** Returns the damage left after Imaginary Protection absorbs this hit. */
    public static float absorbDamage(ServerPlayer player, DamageSource source, float amount) {
        if (amount <= 0.0F || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        if (RuleBreakerDispelService.dispelFromDamage(player, source) || !isProtectionActive(player)) {
            return amount;
        }
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        boolean wasActive = data.protectionActive();
        float remainder = data.absorbProtectionDamage(amount);
        player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        if (wasActive && !data.protectionActive()) {
            notify(player, "message.typemoonaddon.mode.protection_broken");
            player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS,
                0.9F,
                0.55F
            );
        }
        return remainder;
    }

    public static boolean nullifiesTargetedMagic(Entity caster, Entity target) {
        return caster != target && isProtectionActive(target);
    }

    public static void playerLoggedOut(ServerPlayer player) {
        LAST_MESSAGE.remove(player.getUUID());

        Iterator<PendingItem> iterator = PENDING_ITEMS.values().iterator();
        while (iterator.hasNext()) {
            PendingItem pending = iterator.next();
            if (pending.casterId().equals(player.getUUID())) {
                restorePendingItem(serverLevel(player.server, pending.dimension()), pending);
                iterator.remove();
            }
        }
        PENDING_BLOCKS.remove(player.getUUID());
        BLOCK_CAST_HELD_UNTIL.remove(player.getUUID());
    }

    public static void cancelChannelsForUpgrade(ServerPlayer player) {
        Iterator<PendingItem> iterator = PENDING_ITEMS.values().iterator();
        while (iterator.hasNext()) {
            PendingItem pending = iterator.next();
            if (pending.casterId().equals(player.getUUID())) {
                restorePendingItem(serverLevel(player.server, pending.dimension()), pending);
                iterator.remove();
            }
        }
        PENDING_BLOCKS.remove(player.getUUID());
        BLOCK_CAST_HELD_UNTIL.remove(player.getUUID());
    }

    private static boolean acquireStorageTarget(ServerPlayer player, long now, boolean includeLivingTargets) {
        if (PENDING_BLOCKS.containsKey(player.getUUID())) {
            return true;
        }
        if (includeLivingTargets && ImaginaryShadowService.hasLivingTarget(player)) {
            return ImaginaryShadowService.castFromAbsorption(player);
        }
        InteractionHand hand = absorbableHand(player);
        if (hand != null) {
            return absorbHand(player, hand);
        }

        ItemEntity target = rayTraceItem(player);
        if (target != null) {
            return beginItemAbsorption(player, target, now);
        }

        BlockHitResult blockHit = rayTraceBlock(player);
        if (blockHit != null) {
            return beginBlockAbsorption(player, blockHit.getBlockPos(), now);
        }

        notify(player, "message.typemoonaddon.no_storage_target");
        return false;
    }

    private static InteractionHand absorbableHand(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty()) {
            return InteractionHand.MAIN_HAND;
        }
        return player.getOffhandItem().isEmpty() ? null : InteractionHand.OFF_HAND;
    }

    private static boolean absorbHand(ServerPlayer player, InteractionHand hand) {
        ItemStack source = player.getItemInHand(hand);
        if (source.isEmpty() || source.is(ITEM_BLACKLIST)) {
            notify(player, "message.typemoonaddon.cannot_absorb");
            return false;
        }

        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.canFit(source)) {
            notify(player, "message.typemoonaddon.space_full");
            return false;
        }

        double manaCost = ProjectionManaCostService.cost(source);
        if (!TypeMoonIntegration.tryConsumeMana(player, manaCost)) {
            notify(player, "message.typemoonaddon.not_enough_mana");
            return false;
        }

        ItemStack original = source.copy();
        player.setItemInHand(hand, ItemStack.EMPTY);
        try {
            ItemStack remainder = data.insert(original);
            if (!remainder.isEmpty()) {
                throw new IllegalStateException("Imaginary Space capacity changed during hand transaction");
            }
        } catch (RuntimeException exception) {
            player.setItemInHand(hand, original);
            TypeMoonIntegration.refundMana(player, manaCost);
            TypeMoonAddon.LOGGER.error("Rolled back failed hand storage for {}", player.getGameProfile().getName(), exception);
            return false;
        }

        TypeMoonIntegration.addProficiency(player, GameplayConfig.PROFICIENCY_GAIN_PER_PRACTICE);
        try {
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new EffectPayload(
                    player.getId(),
                    EffectPayload.HAND_ABSORPTION,
                    GameplayConfig.ITEM_ABSORPTION_TICKS,
                    GrailParticleService.palette(player)
                )
            );
            player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.8F,
                0.55F
            );
        } catch (RuntimeException exception) {
            TypeMoonAddon.LOGGER.error(
                "Hand storage committed for {}, but client feedback failed",
                player.getGameProfile().getName(),
                exception
            );
        }
        return true;
    }

    private static boolean beginItemAbsorption(ServerPlayer player, ItemEntity itemEntity, long now) {
        if (!validItemTarget(player, itemEntity) || PENDING_ITEMS.containsKey(itemEntity.getUUID())) {
            notify(player, "message.typemoonaddon.cannot_absorb");
            return false;
        }

        ItemStack source = itemEntity.getItem();
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.canFit(source)) {
            notify(player, "message.typemoonaddon.space_full");
            return false;
        }

        PendingItem pending = new PendingItem(
            player.getUUID(),
            itemEntity.getUUID(),
            player.level().dimension(),
            now + GameplayConfig.ITEM_ABSORPTION_TICKS,
            source.copy(),
            itemEntity.isNoGravity(),
            itemEntity.getDeltaMovement()
        );
        PENDING_ITEMS.put(itemEntity.getUUID(), pending);
        itemEntity.setNoGravity(true);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        itemEntity.setNeverPickUp();
        PacketDistributor.sendToPlayersTrackingEntity(
            itemEntity,
            new EffectPayload(
                itemEntity.getId(),
                EffectPayload.ITEM_ABSORPTION,
                GameplayConfig.ITEM_ABSORPTION_TICKS,
                GrailParticleService.palette(player)
            )
        );
        player.serverLevel().playSound(
            null,
            itemEntity.blockPosition(),
            SoundEvents.AMETHYST_BLOCK_RESONATE,
            SoundSource.PLAYERS,
            0.8F,
            0.6F
        );
        return true;
    }

    private static boolean beginBlockAbsorption(ServerPlayer player, BlockPos pos, long now) {
        ServerLevel level = player.serverLevel();
        if (hasPendingItemForCaster(player.getUUID()) || isBlockPending(level.dimension(), pos)) {
            notify(player, "message.typemoonaddon.cannot_absorb_block");
            return false;
        }

        BlockState state = level.getBlockState(pos);
        ItemStack result = blockItem(state);
        if (!validBlockTarget(player, pos, state, result)) {
            notify(player, level.getBlockEntity(pos) == null
                ? "message.typemoonaddon.cannot_absorb_block"
                : "message.typemoonaddon.block_entity_unsupported");
            return false;
        }
        if (!player.getData(ModAttachments.IMAGINARY_SPACE.get()).canFit(result)) {
            notify(player, "message.typemoonaddon.space_full");
            return false;
        }

        PENDING_BLOCKS.put(player.getUUID(), new PendingBlock(
            player.getUUID(),
            level.dimension(),
            pos.immutable(),
            state,
            result.copy(),
            now + GameplayConfig.BLOCK_ABSORPTION_TICKS
        ));
        BLOCK_CAST_HELD_UNTIL.put(player.getUUID(), now + 4L);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 0.45F);
        notify(player, "message.typemoonaddon.block_absorption_started");
        return true;
    }

    private static void tickPendingBlocks(MinecraftServer server) {
        Iterator<PendingBlock> iterator = PENDING_BLOCKS.values().iterator();
        while (iterator.hasNext()) {
            PendingBlock pending = iterator.next();
            ServerLevel level = server.getLevel(pending.dimension());
            ServerPlayer player = server.getPlayerList().getPlayer(pending.casterId());
            if (level == null || player == null || !validCaster(player) || !blockCastHeld(player)
                || player.level() != level || !stillAimingAt(player, pending.pos())) {
                iterator.remove();
                if (player != null) {
                    notify(player, "message.typemoonaddon.block_absorption_canceled");
                }
                continue;
            }

            BlockState state = level.getBlockState(pending.pos());
            ItemStack result = blockItem(state);
            if (!state.equals(pending.snapshot()) || !ItemStack.matches(result, pending.result())
                || !validBlockTarget(player, pending.pos(), state, result)) {
                iterator.remove();
                notify(player, "message.typemoonaddon.block_absorption_canceled");
                continue;
            }

            if (level.getGameTime() < pending.finishTick()) {
                if (level.getGameTime() % 4L == 0L) {
                    GrailParticleService.send(
                        level,
                        player,
                        ParticleTypes.REVERSE_PORTAL,
                        pending.pos().getX() + 0.5D,
                        pending.pos().getY() + 0.5D,
                        pending.pos().getZ() + 0.5D,
                        3,
                        0.35D,
                        0.35D,
                        0.35D,
                        0.01D
                    );
                }
                continue;
            }

            var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
            if (!data.canFit(result)) {
                iterator.remove();
                notify(player, "message.typemoonaddon.space_full");
                continue;
            }
            double manaCost = ProjectionManaCostService.cost(result);
            if (!TypeMoonIntegration.tryConsumeMana(player, manaCost)) {
                iterator.remove();
                notify(player, "message.typemoonaddon.not_enough_mana");
                continue;
            }
            if (CommonHooks.fireBlockBreak(
                level,
                player.gameMode.getGameModeForPlayer(),
                player,
                pending.pos(),
                state
            ).isCanceled()) {
                iterator.remove();
                TypeMoonIntegration.refundMana(player, manaCost);
                notify(player, "message.typemoonaddon.cannot_absorb_block");
                continue;
            }
            if (!level.removeBlock(pending.pos(), false)) {
                iterator.remove();
                TypeMoonIntegration.refundMana(player, manaCost);
                notify(player, "message.typemoonaddon.block_absorption_canceled");
                continue;
            }

            try {
                ItemStack remainder = data.insert(result);
                if (!remainder.isEmpty()) {
                    throw new IllegalStateException("Imaginary Space capacity changed during block transaction");
                }
            } catch (RuntimeException exception) {
                level.setBlock(pending.pos(), pending.snapshot(), 3);
                TypeMoonIntegration.refundMana(player, manaCost);
                TypeMoonAddon.LOGGER.error(
                    "Rolled back failed block storage for {} at {}",
                    player.getGameProfile().getName(),
                    pending.pos(),
                    exception
                );
                iterator.remove();
                continue;
            }

            iterator.remove();
            TypeMoonIntegration.addProficiency(player, GameplayConfig.PROFICIENCY_GAIN_PER_PRACTICE);
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            GrailParticleService.send(
                level,
                player,
                ParticleTypes.SQUID_INK,
                pending.pos().getX() + 0.5D,
                pending.pos().getY() + 0.5D,
                pending.pos().getZ() + 0.5D,
                18,
                0.4D,
                0.4D,
                0.4D,
                0.02D
            );
            level.playSound(null, pending.pos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.75F, 0.5F);
            notify(player, "message.typemoonaddon.block_absorbed");
        }
    }

    private static void tickPendingItems(MinecraftServer server) {
        Iterator<PendingItem> iterator = PENDING_ITEMS.values().iterator();
        while (iterator.hasNext()) {
            PendingItem pending = iterator.next();
            ServerLevel level = server.getLevel(pending.dimension());
            ServerPlayer player = server.getPlayerList().getPlayer(pending.casterId());
            Entity rawEntity = level == null ? null : level.getEntity(pending.itemId());
            if (!(rawEntity instanceof ItemEntity itemEntity) || player == null || !validCaster(player)) {
                restorePendingItem(level, pending);
                iterator.remove();
                continue;
            }

            itemEntity.setDeltaMovement(Vec3.ZERO);
            if (level.getGameTime() < pending.finishTick()) {
                continue;
            }

            if (!validItemTarget(player, itemEntity) || !ItemStack.matches(pending.snapshot(), itemEntity.getItem())) {
                restorePendingItem(level, pending);
                iterator.remove();
                continue;
            }

            ItemStack source = itemEntity.getItem().copy();
            var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
            double manaCost = ProjectionManaCostService.cost(source);
            if (!data.canFit(source) || !TypeMoonIntegration.tryConsumeMana(player, manaCost)) {
                restorePendingItem(level, pending);
                notify(player, data.canFit(source) ? "message.typemoonaddon.not_enough_mana" : "message.typemoonaddon.space_full");
                iterator.remove();
                continue;
            }

            itemEntity.setItem(ItemStack.EMPTY);
            try {
                ItemStack remainder = data.insert(source);
                if (!remainder.isEmpty()) {
                    throw new IllegalStateException("Imaginary Space capacity changed during item transaction");
                }
            } catch (RuntimeException exception) {
                itemEntity.setItem(source);
                TypeMoonIntegration.refundMana(player, manaCost);
                restorePendingItem(level, pending);
                TypeMoonAddon.LOGGER.error(
                    "Rolled back failed dropped-item storage for {}",
                    player.getGameProfile().getName(),
                    exception
                );
                iterator.remove();
                continue;
            }

            // Remove the pending record before discard emits EntityLeaveLevelEvent.
            iterator.remove();
            itemEntity.discard();
            TypeMoonIntegration.addProficiency(player, GameplayConfig.PROFICIENCY_GAIN_PER_PRACTICE);
            try {
                player.syncData(ModAttachments.IMAGINARY_SPACE.get());
                level.playSound(
                    null,
                    itemEntity.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    0.7F,
                    0.55F
                );
            } catch (RuntimeException exception) {
                TypeMoonAddon.LOGGER.error(
                    "Dropped-item storage committed for {}, but client feedback failed",
                    player.getGameProfile().getName(),
                    exception
                );
            }
        }
    }

    private static void tickProtectionZones(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isProtectionActive(player)) {
                continue;
            }

            ServerLevel level = player.serverLevel();
            AABB zone = AABB.ofSize(
                player.getBoundingBox().getCenter(),
                GameplayConfig.PROTECTION_ZONE_SIZE,
                GameplayConfig.PROTECTION_ZONE_SIZE,
                GameplayConfig.PROTECTION_ZONE_SIZE
            );
            for (Entity entity : level.getEntities(player, zone, ImaginaryStorageService::isDispellableEntity)) {
                if (isOwnedBy(entity, player)) {
                    continue;
                }
                GrailParticleService.send(
                    level,
                    player,
                    ParticleTypes.REVERSE_PORTAL,
                    entity.getX(),
                    entity.getY(0.5D),
                    entity.getZ(),
                    6,
                    0.1D,
                    0.1D,
                    0.1D,
                    0.0D
                );
                entity.discard();
            }
        }
    }

    private static void tickProtectionDurations(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
            boolean wasActive = data.protectionActive();
            boolean changed = data.tickProtection();
            if (wasActive && !data.protectionActive()) {
                player.syncData(ModAttachments.IMAGINARY_SPACE.get());
                notify(player, "message.typemoonaddon.mode.protection_expired");
            } else if (changed && player.tickCount % 10 == 0) {
                player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            }
        }
    }

    private static boolean castProtection(
        ServerPlayer player,
        io.github.typemoonaddon.data.ImaginarySpaceData data
    ) {
        if (data.grailWormAscended()) {
            if (!data.togglePermanentProtection()) {
                notify(
                    player,
                    data.protectionCooldownTicks() > 0
                        ? "message.typemoonaddon.mode.protection_cooldown"
                        : "message.typemoonaddon.mode.protection_broken"
                );
                return false;
            }
        } else {
            if (data.protectionCooldownTicks() > 0 || data.protectionShield() <= 0.0F) {
                notify(player, "message.typemoonaddon.mode.protection_cooldown");
                return false;
            }
            data.activateProtection();
        }
        player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        notify(
            player,
            data.protectionActive()
                ? "message.typemoonaddon.mode.protection"
                : "message.typemoonaddon.mode.protection_disabled"
        );
        TypeMoonIntegration.addProficiency(player, GameplayConfig.PROFICIENCY_GAIN_PER_PRACTICE);
        player.serverLevel().playSound(
            null,
            player.blockPosition(),
            SoundEvents.END_PORTAL_FRAME_FILL,
            SoundSource.PLAYERS,
            0.7F,
            data.protectionActive() ? 0.7F : 0.45F
        );
        return true;
    }

    private static boolean isDispellableEntity(Entity entity) {
        return entity.isAlive()
            && (entity instanceof Projectile
                || entity instanceof AreaEffectCloud
                || entity.getType().is(PROTECTION_DISPELLABLE));
    }

    private static boolean isOwnedBy(Entity entity, ServerPlayer player) {
        if (entity instanceof Projectile projectile) {
            return projectile.getOwner() == player;
        }
        if (entity instanceof AreaEffectCloud cloud) {
            return cloud.getOwner() == player;
        }
        return false;
    }

    private static ItemEntity rayTraceItem(ServerPlayer player) {
        double maxRange = GameplayConfig.MAX_RANGE.get();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 intendedEnd = start.add(look.scale(maxRange));
        BlockHitResult blockHit = player.level().clip(
            new ClipContext(start, intendedEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
        );
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? intendedEnd : blockHit.getLocation();
        double maximumDistanceSquared = start.distanceToSqr(end);

        AABB search = player.getBoundingBox()
            .expandTowards(look.scale(Math.sqrt(maximumDistanceSquared)))
            .inflate(1.0D);
        ItemEntity closest = null;
        double closestDistanceSquared = maximumDistanceSquared;
        for (ItemEntity candidate : player.level().getEntitiesOfClass(ItemEntity.class, search, Entity::isAlive)) {
            AABB bounds = candidate.getBoundingBox().inflate(Math.max(0.3F, candidate.getPickRadius()));
            Optional<Vec3> intersection = bounds.clip(start, end);
            if (intersection.isPresent()) {
                double distanceSquared = start.distanceToSqr(intersection.get());
                if (distanceSquared <= closestDistanceSquared) {
                    closest = candidate;
                    closestDistanceSquared = distanceSquared;
                }
            }
        }
        return closest;
    }

    private static BlockHitResult rayTraceBlock(ServerPlayer player) {
        double maxRange = GameplayConfig.MAX_RANGE.get();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(maxRange));
        BlockHitResult hit = player.level().clip(
            new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
        );
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static boolean stillAimingAt(ServerPlayer player, BlockPos expected) {
        BlockHitResult hit = rayTraceBlock(player);
        return hit != null && hit.getBlockPos().equals(expected);
    }

    private static ItemStack blockItem(BlockState state) {
        return state.isAir() ? ItemStack.EMPTY : state.getBlock().asItem().getDefaultInstance();
    }

    private static boolean validBlockTarget(ServerPlayer player, BlockPos pos, BlockState state, ItemStack result) {
        ServerLevel level = player.serverLevel();
        if (state.isAir() || state.is(BLOCK_BLACKLIST) || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null || result.isEmpty() || result.is(ITEM_BLACKLIST)) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(pos) || !player.mayInteract(level, pos)) {
            return false;
        }
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= squaredRange();
    }

    private static boolean isBlockPending(ResourceKey<Level> dimension, BlockPos pos) {
        return PENDING_BLOCKS.values().stream()
            .anyMatch(pending -> pending.dimension().equals(dimension) && pending.pos().equals(pos));
    }

    private static boolean hasPendingItemForCaster(UUID casterId) {
        return PENDING_ITEMS.values().stream().anyMatch(pending -> pending.casterId().equals(casterId));
    }

    private static boolean blockCastHeld(ServerPlayer player) {
        return player.serverLevel().getGameTime() <= BLOCK_CAST_HELD_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE);
    }

    private static boolean validCaster(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && TypeMoonIntegration.isLearned(player);
    }

    private static boolean validItemTarget(ServerPlayer player, ItemEntity itemEntity) {
        if (!itemEntity.isAlive() || itemEntity.getItem().isEmpty() || itemEntity.getItem().is(ITEM_BLACKLIST)) {
            return false;
        }
        if (itemEntity.level() != player.level() || player.distanceToSqr(itemEntity) > squaredRange()) {
            return false;
        }
        Entity owner = itemEntity.getOwner();
        return owner == null || owner == player;
    }

    private static double squaredRange() {
        double range = GameplayConfig.MAX_RANGE.get();
        return range * range;
    }

    public static void entityLeavingLevel(Entity entity) {
        PendingItem pending = PENDING_ITEMS.remove(entity.getUUID());
        if (pending != null && entity instanceof ItemEntity itemEntity) {
            restorePendingItem(itemEntity, pending);
        }
    }

    public static void serverStopping(MinecraftServer server) {
        for (PendingItem pending : PENDING_ITEMS.values()) {
            restorePendingItem(server.getLevel(pending.dimension()), pending);
        }
        PENDING_ITEMS.clear();
        PENDING_BLOCKS.clear();
        BLOCK_CAST_HELD_UNTIL.clear();
        LAST_MESSAGE.clear();
    }

    private static void restorePendingItem(ServerLevel level, PendingItem pending) {
        if (level == null) {
            return;
        }
        Entity rawEntity = level.getEntity(pending.itemId());
        if (rawEntity instanceof ItemEntity itemEntity) {
            restorePendingItem(itemEntity, pending);
        }
    }

    private static void restorePendingItem(ItemEntity itemEntity, PendingItem pending) {
        if (itemEntity.isAlive()) {
            itemEntity.setNoGravity(pending.wasNoGravity());
            itemEntity.setDeltaMovement(pending.oldVelocity());
            itemEntity.setDefaultPickUpDelay();
        }
    }

    private static ServerLevel serverLevel(MinecraftServer server, ResourceKey<Level> dimension) {
        return server.getLevel(dimension);
    }

    private static void notify(ServerPlayer player, String translationKey) {
        long now = player.serverLevel().getGameTime();
        long previous = LAST_MESSAGE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous >= 10L) {
            LAST_MESSAGE.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    private record PendingItem(
        UUID casterId,
        UUID itemId,
        ResourceKey<Level> dimension,
        long finishTick,
        ItemStack snapshot,
        boolean wasNoGravity,
        Vec3 oldVelocity
    ) {
    }

    private record PendingBlock(
        UUID casterId,
        ResourceKey<Level> dimension,
        BlockPos pos,
        BlockState snapshot,
        ItemStack result,
        long finishTick
    ) {
    }

    private ImaginaryStorageService() {
    }
}

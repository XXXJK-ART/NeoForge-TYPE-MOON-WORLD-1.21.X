package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.registry.AddonAttachments;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SakuraImaginaryStorageService {
    private static final Map<UUID, PendingBlock> PENDING_BLOCKS = new HashMap<>();
    private static final Map<UUID, Long> BLOCK_CAST_HELD_UNTIL = new HashMap<>();

    private SakuraImaginaryStorageService() {
    }

    public static boolean selectMode(ServerPlayer player, ImaginarySpaceData.MagicMode requestedMode) {
        if (!validCaster(player)) {
            return false;
        }
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        ImaginarySpaceData.MagicMode mode = requestedMode == null ? ImaginarySpaceData.MagicMode.STORAGE : requestedMode;
        if (mode == ImaginarySpaceData.MagicMode.STORAGE && data.protectionActive() && !data.crestWormAssimilated()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.mode.storage_locked"), true);
            return false;
        }
        if (data.setMagicMode(mode)) {
            AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        }
        return true;
    }

    public static boolean cast(ServerPlayer player, boolean absorption) {
        if (!validCaster(player) || !SakuraTypeMoonIntegration.isLearned(player)) {
            return false;
        }

        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.magicMode() == ImaginarySpaceData.MagicMode.PROTECTION) {
            return toggleProtection(player, data);
        }

        if (absorption && data.crestWormAssimilated()) {
            return absorbFromTarget(player, data);
        }
        return storeFromTarget(player, data);
    }

    public static boolean learnAndGrantAttribute(ServerPlayer player) {
        if (!validCaster(player)) {
            return false;
        }
        boolean learned = SakuraTypeMoonIntegration.registry().knowledge(player).learn(SakuraTypeMoonIntegration.IMAGINARY_STORAGE);
        if (learned) {
            SakuraTypeMoonIntegration.registry().knowledge(player).setProficiency(SakuraTypeMoonIntegration.IMAGINARY_STORAGE, 0.0D);
        }
        return ensureImaginaryAttribute(player) && learned;
    }

    public static boolean ensureImaginaryAttribute(ServerPlayer player) {
        return validCaster(player) && SakuraTypeMoonIntegration.ensureImaginaryAttribute(player);
    }

    public static boolean isLearned(ServerPlayer player) {
        return validCaster(player) && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(SakuraTypeMoonIntegration.IMAGINARY_STORAGE);
    }

    public static boolean isAbsorptionLearned(ServerPlayer player) {
        return validCaster(player) && SakuraTypeMoonIntegration.registry().knowledge(player).isLearned(SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION);
    }

    public static boolean tryConsumeMana(ServerPlayer player, double amount) {
        return amount <= 0.0D || SakuraTypeMoonIntegration.registry().mana(player).tryConsume(amount);
    }

    public static void refundMana(ServerPlayer player, double amount) {
        if (amount > 0.0D) {
            SakuraTypeMoonIntegration.registry().mana(player).add(amount);
        }
    }

    public static void addProficiency(ServerPlayer player, double amount) {
        if (amount > 0.0D) {
            SakuraTypeMoonIntegration.registry().knowledge(player).addProficiency(activeMagic(player), amount);
        }
    }

    public static void updateBlockCastHeld(ServerPlayer player, boolean held) {
        if (!held) {
            BLOCK_CAST_HELD_UNTIL.remove(player.getUUID());
            return;
        }
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (validCaster(player)
                && data.magicMode() == ImaginarySpaceData.MagicMode.STORAGE
                && (!data.protectionActive() || data.crestWormAssimilated())) {
            BLOCK_CAST_HELD_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + 4L);
        }
    }

    public static void tick(MinecraftServer server) {
        tickPendingBlocks(server);
    }

    public static void playerUnavailable(ServerPlayer player) {
        PENDING_BLOCKS.remove(player.getUUID());
        BLOCK_CAST_HELD_UNTIL.remove(player.getUUID());
    }

    public static void serverStopping() {
        PENDING_BLOCKS.clear();
        BLOCK_CAST_HELD_UNTIL.clear();
    }

    private static boolean storeFromTarget(ServerPlayer player, ImaginarySpaceData data) {
        if (PENDING_BLOCKS.containsKey(player.getUUID())) {
            return true;
        }
        InteractionHand hand = absorbableHand(player);
        if (hand != null) {
            return absorbStack(player, data, player.getItemInHand(hand), hand);
        }

        ItemEntity itemEntity = rayTraceItem(player);
        if (itemEntity != null) {
            return absorbStack(player, data, itemEntity.getItem(), null, itemEntity);
        }

        BlockHitResult hit = rayTraceBlock(player);
        if (hit != null) {
            return beginBlockAbsorption(player, data, hit.getBlockPos());
        }

        player.displayClientMessage(Component.translatable("message.typemoonworld.no_storage_target"), true);
        return false;
    }

    private static boolean absorbFromTarget(ServerPlayer player, ImaginarySpaceData data) {
        if (SakuraImaginaryShadowService.hasLivingTarget(player)) {
            return SakuraImaginaryShadowService.castFromAbsorption(player);
        }
        return storeFromTarget(player, data);
    }

    private static boolean absorbStack(ServerPlayer player, ImaginarySpaceData data, ItemStack stack, InteractionHand hand) {
        return absorbStack(player, data, stack, hand, null);
    }

    private static boolean absorbStack(ServerPlayer player, ImaginarySpaceData data, ItemStack stack, InteractionHand hand, ItemEntity entity) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!data.canFit(stack)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.space_full"), true);
            return false;
        }
        double cost = ProjectionManaCostService.cost(stack);
        if (!tryConsumeMana(player, cost)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return false;
        }
        ItemStack source = stack.copy();
        ItemStack remainder = data.insert(source);
        if (!remainder.isEmpty()) {
            refundMana(player, cost);
            player.displayClientMessage(Component.translatable("message.typemoonworld.space_full"), true);
            return false;
        }
        if (hand != null) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        } else if (entity != null) {
            entity.discard();
        }
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        addProficiency(player, 0.2D);
        player.displayClientMessage(Component.translatable("message.typemoonworld.storage.stored"), true);
        return true;
    }

    private static boolean beginBlockAbsorption(ServerPlayer player, ImaginarySpaceData data, BlockPos pos) {
        if (isBlockPending(player.serverLevel().dimension(), pos)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.cannot_absorb_block"), true);
            return false;
        }
        BlockState state = player.serverLevel().getBlockState(pos);
        ItemStack result = state.isAir() ? ItemStack.EMPTY : state.getBlock().asItem().getDefaultInstance();
        if (result.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.cannot_absorb_block"), true);
            return false;
        }
        BlockEntity entity = player.serverLevel().getBlockEntity(pos);
        if (entity != null) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.block_entity_unsupported"), true);
            return false;
        }
        if (!data.canFit(result)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.space_full"), true);
            return false;
        }
        double cost = ProjectionManaCostService.cost(result);
        if (SakuraTypeMoonIntegration.currentMana(player) < cost) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return false;
        }
        long now = player.serverLevel().getGameTime();
        PENDING_BLOCKS.put(player.getUUID(), new PendingBlock(
                player.getUUID(),
                player.serverLevel().dimension(),
                pos.immutable(),
                state,
                result.copy(),
                now + GameplayConfig.BLOCK_ABSORPTION_TICKS
        ));
        BLOCK_CAST_HELD_UNTIL.put(player.getUUID(), now + 4L);
        player.displayClientMessage(Component.translatable("message.typemoonworld.block_absorption_started"), true);
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
                    player.displayClientMessage(Component.translatable("message.typemoonworld.block_absorption_canceled"), true);
                }
                continue;
            }

            BlockState state = level.getBlockState(pending.pos());
            ItemStack result = state.isAir() ? ItemStack.EMPTY : state.getBlock().asItem().getDefaultInstance();
            if (!state.equals(pending.snapshot())
                    || !ItemStack.matches(result, pending.result())
                    || level.getBlockEntity(pending.pos()) != null
                    || result.isEmpty()) {
                iterator.remove();
                player.displayClientMessage(Component.translatable("message.typemoonworld.block_absorption_canceled"), true);
                continue;
            }

            if (level.getGameTime() < pending.finishTick()) {
                continue;
            }

            ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
            if (!data.canFit(result)) {
                iterator.remove();
                player.displayClientMessage(Component.translatable("message.typemoonworld.space_full"), true);
                continue;
            }
            double cost = ProjectionManaCostService.cost(result);
            if (!tryConsumeMana(player, cost)) {
                iterator.remove();
                player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
                continue;
            }
            if (!removeBlock(player, level, pending, state, result, cost)) {
                iterator.remove();
                continue;
            }

            iterator.remove();
            AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
            addProficiency(player, GameplayConfig.PROFICIENCY_GAIN_PER_PRACTICE);
            player.displayClientMessage(Component.translatable("message.typemoonworld.block_absorbed"), true);
        }
    }

    private static boolean removeBlock(ServerPlayer player, ServerLevel level, PendingBlock pending, BlockState state, ItemStack result, double cost) {
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (!level.removeBlock(pending.pos(), false)) {
            refundMana(player, cost);
            player.displayClientMessage(Component.translatable("message.typemoonworld.block_absorption_canceled"), true);
            return false;
        }
        if (!data.insert(result).isEmpty()) {
            refundMana(player, cost);
            level.setBlock(pending.pos(), state, 3);
            player.displayClientMessage(Component.translatable("message.typemoonworld.space_full"), true);
            return false;
        }
        return true;
    }

    private static boolean toggleProtection(ServerPlayer player, ImaginarySpaceData data) {
        if (data.grailWormAscended()) {
            if (!data.togglePermanentProtection()) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.mode.protection_cooldown"), true);
                return false;
            }
        } else {
            data.activateProtection();
        }
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        player.displayClientMessage(Component.translatable(data.protectionActive()
                ? "message.typemoonworld.mode.protection"
                : "message.typemoonworld.mode.protection_disabled"), true);
        return true;
    }

    private static InteractionHand absorbableHand(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty()) {
            return InteractionHand.MAIN_HAND;
        }
        return player.getOffhandItem().isEmpty() ? null : InteractionHand.OFF_HAND;
    }

    private static ItemEntity rayTraceItem(ServerPlayer player) {
        double maxRange = 50.0D;
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 intendedEnd = start.add(look.scale(maxRange));
        BlockHitResult blockHit = player.level().clip(new ClipContext(start, intendedEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? intendedEnd : blockHit.getLocation();
        double maximumDistanceSquared = start.distanceToSqr(end);

        AABB search = player.getBoundingBox().expandTowards(look.scale(Math.sqrt(maximumDistanceSquared))).inflate(1.0D);
        ItemEntity closest = null;
        double closestDistanceSquared = maximumDistanceSquared;
        for (ItemEntity candidate : player.level().getEntitiesOfClass(ItemEntity.class, search, ItemEntity::isAlive)) {
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
        double maxRange = 50.0D;
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(maxRange));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static boolean stillAimingAt(ServerPlayer player, BlockPos expected) {
        BlockHitResult hit = rayTraceBlock(player);
        return hit != null && hit.getBlockPos().equals(expected);
    }

    private static boolean isBlockPending(ResourceKey<Level> dimension, BlockPos pos) {
        return PENDING_BLOCKS.values().stream()
                .anyMatch(pending -> pending.dimension().equals(dimension) && pending.pos().equals(pos));
    }

    private static boolean blockCastHeld(ServerPlayer player) {
        return player.serverLevel().getGameTime() <= BLOCK_CAST_HELD_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE);
    }

    private static boolean validCaster(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator();
    }

    private static net.minecraft.resources.ResourceLocation activeMagic(ServerPlayer player) {
        return player.getData(AddonAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated()
                ? SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION
                : SakuraTypeMoonIntegration.IMAGINARY_STORAGE;
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
}

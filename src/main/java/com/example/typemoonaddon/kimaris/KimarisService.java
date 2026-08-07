package com.example.typemoonaddon.kimaris;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import com.example.typemoonaddon.orias.OriasService;
import com.example.typemoonaddon.storage.StorageService;
import com.example.typemoonaddon.storm.StormService;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class KimarisService {
    public static final int FREEZE_DURATION_TICKS = 200;
    public static final float BROKEN_PHANTASM_TIER_DAMAGE = 250.0F;
    public static final float FREEZE_DAMAGE = BROKEN_PHANTASM_TIER_DAMAGE;
    public static final float THAW_DAMAGE = BROKEN_PHANTASM_TIER_DAMAGE;
    public static final double FREEZE_RADIUS = 50.0D;
    public static final int ICE_FIELD_RADIUS = 5;

    private static final int CAST_DEBOUNCE_TICKS = 2;
    private static final int PLAYER_CORRECTION_INTERVAL_TICKS = 5;
    private static final int MAX_BLOCK_PLACEMENTS_PER_TICK = 256;
    private static final double MAX_RESTORED_SPEED = 4.0D;
    private static final double PLAYER_POSITION_EPSILON_SQUARED = 0.0025D;
    private static final double VFX_OBSERVER_RADIUS = 128.0D;

    private static final String CHUNK_WAVE_EFFECT = "typemoonworld:kimaris_chunk_wave";
    private static final String SIGIL_EFFECT = "typemoonworld:kimaris_sigil";
    private static final String THAW_EFFECT = "typemoonworld:kimaris_thaw";

    private static final Map<ResourceKey<Level>, Set<UUID>> ACTIVE_ENTITIES = new HashMap<>();
    private static final Map<UUID, Long> LAST_SUCCESSFUL_CAST_TICK = new HashMap<>();
    private static final ArrayDeque<PendingIcePlacement> PENDING_ICE_PLACEMENTS = new ArrayDeque<>();
    private static final Set<PendingIceKey> PENDING_ICE_KEYS = new HashSet<>();
    private static final Set<PendingIceBlockKey> PENDING_ICE_BLOCK_KEYS = new HashSet<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, UUID>> TEMPORARY_ICE_INDEX = new HashMap<>();

    private KimarisService() {
    }

    /** Freezes eligible entities inside a 50-block sphere centered on the caster. */
    public static int cast(ServerPlayer caster) {
        if (caster == null || !caster.isAlive() || caster.isRemoved() || caster.isSpectator()) {
            return 0;
        }

        ServerLevel level = caster.serverLevel();
        long currentTick = level.getGameTime();
        Long lastCast = LAST_SUCCESSFUL_CAST_TICK.get(caster.getUUID());
        if (lastCast != null && currentTick >= lastCast && currentTick - lastCast < CAST_DEBOUNCE_TICKS) {
            return 0;
        }

        Vec3 center = caster.getBoundingBox().getCenter();
        double radiusSquared = FREEZE_RADIUS * FREEZE_RADIUS;
        AABB searchArea = new AABB(center, center).inflate(FREEZE_RADIUS);
        List<Entity> targets = level.getEntities(caster, searchArea, target ->
                isEligibleTarget(caster, target)
                        && target.getBoundingBox().getCenter().distanceToSqr(center) <= radiusSquared);

        int affected = 0;
        for (Entity target : targets) {
            if (freezeTarget(caster, target, currentTick)) {
                affected++;
                AddonSpellVisualPayload.showAttached(
                        level, AddonSpellVisualPayload.KIMARIS,
                        AddonSpellVisualPayload.ICE_SPHERE, caster.getUUID(), target,
                        ICE_FIELD_RADIUS, 1.0F, FREEZE_DURATION_TICKS,
                        target.getId(), VFX_OBSERVER_RADIUS);
            }
        }

        if (affected > 0) {
            LAST_SUCCESSFUL_CAST_TICK.put(caster.getUUID(), currentTick);
            Vec3 effectCenter = caster.position().add(0.0D, 0.12D, 0.0D);
            VFXServerEffects.spawn(level, CHUNK_WAVE_EFFECT, effectCenter, VFX_OBSERVER_RADIUS);
            VFXServerEffects.spawn(level, SIGIL_EFFECT, caster, VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showAttached(
                    level, AddonSpellVisualPayload.KIMARIS,
                    AddonSpellVisualPayload.SIGIL, caster.getUUID(), caster,
                    1.0F, 1.0F, 28, 0, VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showAt(
                    level, AddonSpellVisualPayload.KIMARIS,
                    AddonSpellVisualPayload.AFTERMATH, caster.getUUID(), effectCenter,
                    (float) FREEZE_RADIUS, 1.0F, 30, 0, VFX_OBSERVER_RADIUS);
            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    effectCenter.x,
                    effectCenter.y + 0.4D,
                    effectCenter.z,
                    42,
                    7.2D,
                    0.55D,
                    7.2D,
                    0.035D
            );
        }
        return affected;
    }

    public static boolean isFrozen(Entity entity) {
        if (entity == null || !entity.hasData(KimarisAttachments.FROZEN_STATE)) {
            return false;
        }
        return entity.getExistingData(KimarisAttachments.FROZEN_STATE)
                .map(KimarisFrozenData::isActive)
                .orElse(false);
    }

    public static void enforceFrozenState(Entity entity) {
        KimarisFrozenData data = existingData(entity);
        if (data == null || !data.isActive() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 locked = data.lockedPosition();
        boolean moved = entity.position().distanceToSqr(locked) > PLAYER_POSITION_EPSILON_SQUARED;
        boolean rotated = Math.abs(Mth.wrapDegrees(entity.getYRot() - data.lockedYaw())) > 1.0F
                || Math.abs(entity.getXRot() - data.lockedPitch()) > 1.0F;

        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
        entity.fallDistance = 0.0F;
        entity.setPos(locked.x, locked.y, locked.z);
        entity.setYRot(data.lockedYaw());
        entity.setXRot(data.lockedPitch());
        entity.hasImpulse = true;

        if (entity instanceof LivingEntity living) {
            living.setSprinting(false);
            living.stopUsingItem();
            living.setYHeadRot(data.lockedYaw());
        }
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        if (entity instanceof ServerPlayer player
                && (moved || rotated)
                && data.shouldCorrectPlayer(level.getGameTime(), PLAYER_CORRECTION_INTERVAL_TICKS)) {
            player.connection.teleport(
                    locked.x,
                    locked.y,
                    locked.z,
                    data.lockedYaw(),
                    data.lockedPitch()
            );
        }
    }

    public static void tick(MinecraftServer server) {
        processPendingIcePlacements(server, MAX_BLOCK_PLACEMENTS_PER_TICK);
        for (ServerLevel level : server.getAllLevels()) {
            Set<UUID> activeIds = ACTIVE_ENTITIES.get(level.dimension());
            if (activeIds == null || activeIds.isEmpty()) {
                continue;
            }

            long currentTick = level.getGameTime();
            for (UUID entityId : List.copyOf(activeIds)) {
                Entity entity = level.getEntity(entityId);
                if (entity == null) {
                    activeIds.remove(entityId);
                    continue;
                }

                KimarisFrozenData data = existingData(entity);
                if (data == null) {
                    activeIds.remove(entityId);
                    removeIndexedIce(level.dimension(), entityId);
                    continue;
                }
                if (!data.isActive()) {
                    restoreTemporaryIce(level, data);
                    if (!data.hasTemporaryIceBlocks()) {
                        entity.removeData(KimarisAttachments.FROZEN_STATE);
                        activeIds.remove(entityId);
                        removeIndexedIce(level.dimension(), entityId);
                    }
                    continue;
                }
                if (!entity.isAlive() || entity.isRemoved()
                        || !level.dimension().location().toString().equals(data.dimensionId())) {
                    restore(entity, data, false);
                    continue;
                }

                enforceFrozenState(entity);
                if (data.advance(currentTick)) {
                    restore(entity, data, true);
                }
            }
            if (activeIds.isEmpty()) {
                ACTIVE_ENTITIES.remove(level.dimension());
            }
        }
    }

    public static void handleEntityJoin(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        KimarisFrozenData data = existingData(entity);
        if (data == null) {
            return;
        }
        indexTemporaryIce(level, entity.getUUID(), data);
        if (!data.isActive() || !level.dimension().location().toString().equals(data.dimensionId())) {
            restore(entity, data, false);
            return;
        }
        register(entity);
        enforceFrozenState(entity);
        if (entity instanceof LivingEntity living && !data.hasTemporaryIceBlocks() && !hasPendingIce(entity.getUUID())) {
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(data.casterId());
            if (caster != null && caster.serverLevel() == level) {
                queueIceSphere(caster, living, data);
            }
        }
    }

    public static void handleEntityLeave(Entity entity) {
        unregister(entity);
        removePendingIce(entity.getUUID());
        KimarisFrozenData data = existingData(entity);
        if (data == null) {
            return;
        }
        if (entity.getRemovalReason() != Entity.RemovalReason.UNLOADED_TO_CHUNK) {
            restore(entity, data, false);
        }
    }

    public static void clearWithoutThawDamage(Entity entity) {
        KimarisFrozenData data = existingData(entity);
        if (data != null) {
            restore(entity, data, false);
        } else {
            unregister(entity);
        }
    }

    public static void clearRuntimeState() {
        ACTIVE_ENTITIES.clear();
        LAST_SUCCESSFUL_CAST_TICK.clear();
        PENDING_ICE_PLACEMENTS.clear();
        PENDING_ICE_KEYS.clear();
        PENDING_ICE_BLOCK_KEYS.clear();
        TEMPORARY_ICE_INDEX.clear();
    }

    private static boolean freezeTarget(
            ServerPlayer caster,
            Entity target,
            long currentTick
    ) {
        ServerLevel level = caster.serverLevel();
        String dimensionId = level.dimension().location().toString();
        KimarisFrozenData data = existingData(target);
        if (data != null && data.isActive()) {
            if (!dimensionId.equals(data.dimensionId())) {
                restore(target, data, false);
                if (data.hasTemporaryIceBlocks()) {
                    return false;
                }
                data = null;
            } else {
                data.refresh(caster.getUUID(), currentTick, FREEZE_DURATION_TICKS);
                register(target);
                enforceFrozenState(target);
                if (target instanceof LivingEntity living
                        && !data.hasTemporaryIceBlocks()
                        && !hasPendingIce(target.getUUID())) {
                    queueIceSphere(caster, living, data);
                }
                return true;
            }
        }

        if (data != null) {
            restore(target, data, false);
            if (data.hasTemporaryIceBlocks()) {
                return false;
            }
        }
        data = target.getData(KimarisAttachments.FROZEN_STATE);
        Vec3 originalVelocity = clampVelocity(target.getDeltaMovement());
        boolean hasMobState = target instanceof Mob;
        boolean originalNoAi = hasMobState && ((Mob) target).isNoAi();
        data.begin(
                caster.getUUID(),
                currentTick,
                FREEZE_DURATION_TICKS,
                originalVelocity,
                target.isNoGravity(),
                hasMobState,
                originalNoAi,
                target.position(),
                target.getYRot(),
                target.getXRot(),
                dimensionId
        );
        if (!data.isActive()) {
            target.removeData(KimarisAttachments.FROZEN_STATE);
            return false;
        }

        if (target instanceof ServerPlayer player) {
            cancelAddonChannels(player);
        }
        register(target);
        enforceFrozenState(target);
        if (target instanceof LivingEntity living) {
            queueIceSphere(caster, living, data);
            data.markFreezeDamageApplied();
            living.hurt(caster.damageSources().source(DamageTypes.MAGIC, caster), FREEZE_DAMAGE);
        }
        return true;
    }

    private static void restore(Entity entity, KimarisFrozenData data, boolean normalThaw) {
        if (entity == null || data == null) {
            return;
        }

        removePendingIce(entity.getUUID());
        UUID casterId = data.casterId();
        boolean applyThawDamage = normalThaw
                && data.freezeDamageApplied()
                && !data.thawDamageApplied()
                && entity instanceof LivingEntity
                && entity.isAlive()
                && !entity.isRemoved();
        if (applyThawDamage) {
            data.markThawDamageApplied();
        }

        if (!data.entityStateRestored()) {
            if (entity instanceof Mob mob && data.hasSavedMobState()) {
                mob.setNoAi(data.savedNoAi());
            }
            entity.setNoGravity(data.savedNoGravity());
            entity.setDeltaMovement(clampVelocity(data.savedVelocity()));
            entity.hasImpulse = true;
            data.markEntityStateRestored();
        }
        data.deactivate();

        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ServerLevel iceLevel = resolveIceLevel(level.getServer(), data);
        if (iceLevel != null) {
            restoreTemporaryIce(iceLevel, data);
        }
        if (data.hasTemporaryIceBlocks()) {
            register(entity);
        } else {
            unregister(entity);
            if (iceLevel != null) {
                removeIndexedIce(iceLevel.dimension(), entity.getUUID());
            }
            entity.removeData(KimarisAttachments.FROZEN_STATE);
        }
        if (normalThaw && entity instanceof LivingEntity) {
            spawnThawVfx(level, entity);
        }
        if (applyThawDamage && casterId != null && entity instanceof LivingEntity living) {
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);
            if (caster != null && caster.isAlive() && !caster.isRemoved() && caster.serverLevel() == level) {
                living.hurt(caster.damageSources().source(DamageTypes.MAGIC, caster), THAW_DAMAGE);
            }
        }
    }

    private static boolean isEligibleTarget(ServerPlayer caster, Entity target) {
        if (target == null || target == caster || target.isRemoved() || !target.isAlive()) {
            return false;
        }
        if (target instanceof LightningBolt
                || target instanceof Display
                || target instanceof HangingEntity
                || target instanceof ArmorStand
                || target instanceof AreaEffectCloud
                || target instanceof Marker
                || target instanceof Interaction) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (target instanceof LivingEntity living && living.isInvulnerable()) {
            return false;
        }
        if (isTechnicalEntity(target)) {
            return false;
        }
        return target instanceof LivingEntity
                || target instanceof Projectile
                || target instanceof ItemEntity
                || target instanceof ExperienceOrb
                || target instanceof PrimedTnt
                || target instanceof FallingBlockEntity
                || target instanceof VehicleEntity;
    }

    private static boolean isTechnicalEntity(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            return true;
        }
        String path = id.getPath();
        if (TypeMoonAddon.MOD_ID.equals(id.getNamespace())) {
            return path.contains("vfx") || path.contains("effect") || path.contains("controller");
        }
        return "typemoonworld".equals(id.getNamespace())
                && (path.contains("vfx")
                || path.contains("beam")
                || path.contains("controller")
                 || path.contains("world_border"));
    }

    public static boolean isTemporaryIce(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        Map<BlockPos, UUID> indexed = TEMPORARY_ICE_INDEX.get(level.dimension());
        return indexed != null && indexed.containsKey(pos);
    }

    public static void protectTemporaryIceFromExplosion(ServerLevel level, List<BlockPos> affectedBlocks) {
        if (level != null && affectedBlocks != null && !affectedBlocks.isEmpty()) {
            affectedBlocks.removeIf(pos -> isTemporaryIce(level, pos));
        }
    }

    public static boolean pistonTouchesTemporaryIce(ServerLevel level, PistonStructureResolver resolver) {
        if (level == null || resolver == null || !resolver.resolve()) {
            return false;
        }
        return resolver.getToPush().stream().anyMatch(pos -> isTemporaryIce(level, pos))
                || resolver.getToDestroy().stream().anyMatch(pos -> isTemporaryIce(level, pos));
    }

    private static void queueIceSphere(
            ServerPlayer caster,
            LivingEntity target,
            KimarisFrozenData data
    ) {
        if (caster == null || target == null || data == null || !data.isActive()) {
            return;
        }
        IcePlacementBudget budget = new IcePlacementBudget(KimarisFrozenData.MAX_TEMPORARY_ICE_BLOCKS);
        ResourceKey<Level> dimension = caster.serverLevel().dimension();
        ServerLevel level = caster.serverLevel();
        for (BlockPos pos : buildIceSpherePositions(target)) {
            if (!budget.tryConsume()) {
                return;
            }
            PendingIceKey key = new PendingIceKey(dimension, target.getUUID(), pos);
            PendingIceBlockKey blockKey = new PendingIceBlockKey(dimension, pos);
            if (data.hasTemporaryIceBlock(pos)
                    || isTemporaryIce(level, pos)
                    || !PENDING_ICE_KEYS.add(key)) {
                budget.refund();
                continue;
            }
            if (!PENDING_ICE_BLOCK_KEYS.add(blockKey)) {
                PENDING_ICE_KEYS.remove(key);
                budget.refund();
                continue;
            }
            PENDING_ICE_PLACEMENTS.addLast(new PendingIcePlacement(
                    key,
                    caster.getUUID(),
                    selectIceState(pos)
            ));
        }
    }

    private static Set<BlockPos> buildIceSpherePositions(LivingEntity target) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        int centerX = Mth.floor(target.getX());
        int centerY = Mth.floor(target.getBoundingBox().getCenter().y);
        int centerZ = Mth.floor(target.getZ());
        int radiusSquared = ICE_FIELD_RADIUS * ICE_FIELD_RADIUS;
        for (int xOffset = -ICE_FIELD_RADIUS; xOffset <= ICE_FIELD_RADIUS; xOffset++) {
            for (int yOffset = -ICE_FIELD_RADIUS; yOffset <= ICE_FIELD_RADIUS; yOffset++) {
                for (int zOffset = -ICE_FIELD_RADIUS; zOffset <= ICE_FIELD_RADIUS; zOffset++) {
                    if (xOffset * xOffset + yOffset * yOffset + zOffset * zOffset > radiusSquared) {
                        continue;
                    }
                    positions.add(new BlockPos(
                            centerX + xOffset,
                            centerY + yOffset,
                            centerZ + zOffset
                    ));
                    if (positions.size() >= KimarisFrozenData.MAX_TEMPORARY_ICE_BLOCKS) {
                        return positions;
                    }
                }
            }
        }
        return positions;
    }

    private static BlockState selectIceState(BlockPos pos) {
        int pattern = Math.floorMod(pos.getX() * 31 + pos.getY() * 17 + pos.getZ(), 7);
        return pattern == 0 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
    }

    private static void processPendingIcePlacements(MinecraftServer server, int budget) {
        int processed = 0;
        while (processed < Math.max(0, budget) && !PENDING_ICE_PLACEMENTS.isEmpty()) {
            PendingIcePlacement pending = PENDING_ICE_PLACEMENTS.removeFirst();
            PENDING_ICE_KEYS.remove(pending.key());
            PENDING_ICE_BLOCK_KEYS.remove(new PendingIceBlockKey(
                    pending.key().dimension(),
                    pending.key().pos()
            ));
            processed++;

            ServerLevel level = server.getLevel(pending.key().dimension());
            if (level == null) {
                continue;
            }
            Entity entity = level.getEntity(pending.key().targetId());
            KimarisFrozenData data = existingData(entity);
            ServerPlayer caster = server.getPlayerList().getPlayer(pending.casterId());
            if (!(entity instanceof LivingEntity target)
                    || data == null
                    || !data.isActive()
                    || caster == null
                    || caster.serverLevel() != level
                    || data.hasTemporaryIceBlock(pending.key().pos())
                    || !canPlaceTemporaryIce(level, caster, target, pending.key().pos())) {
                continue;
            }

            BlockPos pos = pending.key().pos();
            BlockState originalState = level.getBlockState(pos);
            if (!level.setBlock(pos, pending.placedState(), Block.UPDATE_CLIENTS)) {
                continue;
            }
            if (data.addTemporaryIceBlock(pos, originalState, pending.placedState())) {
                TEMPORARY_ICE_INDEX.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                        .put(pos.immutable(), target.getUUID());
            } else if (level.getBlockState(pos).equals(pending.placedState())) {
                level.setBlock(pos, originalState, Block.UPDATE_ALL);
            }
        }
    }

    private static boolean canPlaceTemporaryIce(
            ServerLevel level,
            ServerPlayer caster,
            LivingEntity target,
            BlockPos pos
    ) {
        if (pos.getY() < level.getMinBuildHeight()
                || pos.getY() >= level.getMaxBuildHeight()
                || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(caster, pos)
                || level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState current = level.getBlockState(pos);
        if ((!current.isAir() && !current.canBeReplaced()) || !current.getFluidState().isEmpty()) {
            return false;
        }
        AABB blockBox = new AABB(pos);
        if (blockBox.intersects(target.getBoundingBox())) {
            return false;
        }
        return level.getEntitiesOfClass(
                LivingEntity.class,
                blockBox,
                living -> living.isAlive() && !living.isSpectator()
        ).isEmpty();
    }

    private static void restoreTemporaryIce(ServerLevel level, KimarisFrozenData data) {
        for (KimarisFrozenData.TemporaryIceBlock block : data.temporaryIceBlocks()) {
            BlockPos pos = block.pos();
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (current.equals(block.placedState())) {
                level.setBlock(pos, block.originalState(), Block.UPDATE_ALL);
            }
            data.removeTemporaryIceBlock(pos);
            Map<BlockPos, UUID> indexed = TEMPORARY_ICE_INDEX.get(level.dimension());
            if (indexed != null) {
                indexed.remove(pos);
                if (indexed.isEmpty()) {
                    TEMPORARY_ICE_INDEX.remove(level.dimension());
                }
            }
        }
    }

    private static void indexTemporaryIce(ServerLevel level, UUID ownerId, KimarisFrozenData data) {
        for (KimarisFrozenData.TemporaryIceBlock block : data.temporaryIceBlocks()) {
            if (level.hasChunkAt(block.pos()) && level.getBlockState(block.pos()).equals(block.placedState())) {
                TEMPORARY_ICE_INDEX.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                        .put(block.pos(), ownerId);
            }
        }
    }

    private static ServerLevel resolveIceLevel(MinecraftServer server, KimarisFrozenData data) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(data.dimensionId());
        if (server == null || dimensionId == null) {
            return null;
        }
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
    }

    private static void removeIndexedIce(ResourceKey<Level> dimension, UUID ownerId) {
        Map<BlockPos, UUID> indexed = TEMPORARY_ICE_INDEX.get(dimension);
        if (indexed != null) {
            indexed.values().removeIf(ownerId::equals);
            if (indexed.isEmpty()) {
                TEMPORARY_ICE_INDEX.remove(dimension);
            }
        }
    }

    private static boolean hasPendingIce(UUID targetId) {
        return PENDING_ICE_KEYS.stream().anyMatch(key -> key.targetId().equals(targetId));
    }

    private static void removePendingIce(UUID targetId) {
        PENDING_ICE_PLACEMENTS.removeIf(pending -> {
            if (!pending.key().targetId().equals(targetId)) {
                return false;
            }
            PENDING_ICE_KEYS.remove(pending.key());
            PENDING_ICE_BLOCK_KEYS.remove(new PendingIceBlockKey(
                    pending.key().dimension(),
                    pending.key().pos()
            ));
            return true;
        });
    }

    private static void cancelAddonChannels(ServerPlayer player) {
        player.closeContainer();
        OriasService.cancelPlayer(player, true, false);
        StormService.stop(player, false);
        StorageService.clearCastState(player);
    }

    private static void register(Entity entity) {
        if (entity.level() instanceof ServerLevel level) {
            ACTIVE_ENTITIES.computeIfAbsent(level.dimension(), ignored -> new HashSet<>())
                    .add(entity.getUUID());
        }
    }

    private static void unregister(Entity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Set<UUID> active = ACTIVE_ENTITIES.get(level.dimension());
        if (active != null) {
            active.remove(entity.getUUID());
            if (active.isEmpty()) {
                ACTIVE_ENTITIES.remove(level.dimension());
            }
        }
    }

    private static KimarisFrozenData existingData(Entity entity) {
        if (entity == null || !entity.hasData(KimarisAttachments.FROZEN_STATE)) {
            return null;
        }
        return entity.getExistingData(KimarisAttachments.FROZEN_STATE).orElse(null);
    }

    private static Vec3 clampVelocity(Vec3 velocity) {
        if (velocity == null
                || !Double.isFinite(velocity.x)
                || !Double.isFinite(velocity.y)
                || !Double.isFinite(velocity.z)) {
            return Vec3.ZERO;
        }
        double lengthSqr = velocity.lengthSqr();
        double maxSqr = MAX_RESTORED_SPEED * MAX_RESTORED_SPEED;
        if (lengthSqr > maxSqr) {
            return velocity.normalize().scale(MAX_RESTORED_SPEED);
        }
        return velocity;
    }

    private static void spawnThawVfx(ServerLevel level, Entity target) {
        VFXServerEffects.spawn(level, THAW_EFFECT, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                VFX_OBSERVER_RADIUS);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                28,
                Math.min(2.5D, Math.max(0.35D, target.getBbWidth() * 0.65D)),
                Math.min(3.0D, Math.max(0.45D, target.getBbHeight() * 0.5D)),
                Math.min(2.5D, Math.max(0.35D, target.getBbWidth() * 0.65D)),
                0.12D
        );
    }

    private record PendingIceKey(ResourceKey<Level> dimension, UUID targetId, BlockPos pos) {
    }

    private record PendingIceBlockKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record PendingIcePlacement(PendingIceKey key, UUID casterId, BlockState placedState) {
    }

    private static final class IcePlacementBudget {
        private int remaining;

        private IcePlacementBudget(int remaining) {
            this.remaining = Math.max(0, remaining);
        }

        private boolean tryConsume() {
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }

        private void refund() {
            remaining++;
        }
    }
}

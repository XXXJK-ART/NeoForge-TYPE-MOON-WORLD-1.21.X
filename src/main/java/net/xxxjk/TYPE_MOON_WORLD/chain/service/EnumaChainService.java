package net.xxxjk.TYPE_MOON_WORLD.chain.service;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.compat.TypeMoonBridge;
import net.xxxjk.TYPE_MOON_WORLD.chain.config.ChainConfig;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.EnumaChainCrownEntity;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EnumaChainService {
    private static final String NPC_RELEASE = "EnkiduEnumaRelease";
    private static final String NPC_FINISH = "EnkiduEnumaFinish";
    private static final String PLAYER_ACTIVE = "ServantCardEnkiduEnumaActive";
    private static final String PLAYER_RELEASE = "ServantCardEnkiduEnumaReleaseTick";
    private static final String PLAYER_FINISH = "ServantCardEnkiduEnumaFinishTick";
    private static final Map<UUID, CastState> CASTS = new HashMap<>();

    public static void beginNpcEnuma(LivingEntity owner) {
        CompoundTag data = owner.getPersistentData();
        begin(owner, data.getLong(NPC_RELEASE), data.getLong(NPC_FINISH));
    }

    public static void beginPlayerEnuma(ServerPlayer owner) {
        CompoundTag data = owner.getPersistentData();
        if (data.getBoolean(PLAYER_ACTIVE)) {
            begin(owner, data.getLong(PLAYER_RELEASE), data.getLong(PLAYER_FINISH));
        }
    }

    private static void begin(LivingEntity owner, long releaseTick, long finishTick) {
        if (!(owner.level() instanceof ServerLevel level) || !ChainControlService.isEligibleEnumaOwner(owner)) {
            return;
        }
        long now = level.getGameTime();
        if (releaseTick <= now || finishTick <= releaseTick) {
            return;
        }
        CastState current = castForOwner(level.dimension(), owner.getUUID());
        if (current != null && current.releaseTick == releaseTick) {
            return;
        }
        if (current != null) {
            finishCast(level, current);
        }

        UUID castUuid = UUID.randomUUID();
        long seed = EnumaPatternMath.mix64(owner.getUUID().getMostSignificantBits()
            ^ owner.getUUID().getLeastSignificantBits() ^ now);
        Vec3 center = resolveFieldCenter(level, owner);
        CastState state = new CastState(
            castUuid,
            owner.getUUID(),
            level.dimension(),
            center,
            seed,
            releaseTick,
            finishTick
        );
        CASTS.put(castUuid, state);

        for (int index = 0; index < ChainConfig.ENUMA_CHAIN_COUNT; index++) {
            Vec3 gate = resolveGatePosition(center, seed, index);
            HeavenChainEntity chain = ModEntities.HEAVEN_CHAIN.get().create(level);
            if (chain == null) {
                continue;
            }
            chain.initializeEnuma(owner, castUuid, index, gate, center, seed, now, releaseTick, finishTick);
            if (level.addFreshEntity(chain)) {
                state.chainIds.add(chain.getUUID());
            }
        }

        EnumaChainCrownEntity crown = ModEntities.ENUMA_CHAIN_CROWN.get().create(level);
        if (crown != null) {
            crown.initialize(owner, castUuid, center, seed, releaseTick, finishTick);
            if (level.addFreshEntity(crown)) {
                state.crownId = crown.getUUID();
            }
        }
        if (state.chainIds.size() != ChainConfig.ENUMA_CHAIN_COUNT || state.crownId == null) {
            TYPE_MOON_WORLD.LOGGER.error(
                "Enuma cast {} spawned {} of {} chains; crown={}",
                castUuid,
                state.chainIds.size(),
                ChainConfig.ENUMA_CHAIN_COUNT,
                state.crownId != null
            );
        }
        TypeMoonBridge.spawnEnumaGreenColumn(level, owner);
    }

    public static void trackLoadedChain(ServerLevel level, HeavenChainEntity chain) {
        UUID castUuid = chain.enumaCastUuid();
        UUID ownerUuid = chain.ownerUuid();
        if (!chain.hasEnumaFlow() || chain.isEnumaCastFinished() || castUuid == null || ownerUuid == null) {
            return;
        }
        CastState state = CASTS.computeIfAbsent(castUuid, ignored -> new CastState(
            castUuid,
            ownerUuid,
            level.dimension(),
            chain.enumaFieldCenter(),
            chain.enumaSeed(),
            chain.enumaReleaseTick(),
            chain.enumaFinishTick()
        ));
        if (state.dimension.equals(level.dimension())) {
            state.chainIds.add(chain.getUUID());
        }
    }

    public static void trackLoadedCrown(ServerLevel level, EnumaChainCrownEntity crown) {
        UUID castUuid = crown.castUuid();
        UUID ownerUuid = crown.ownerUuid();
        if (!crown.isActiveCastVisual() || castUuid == null || ownerUuid == null) {
            return;
        }
        CastState state = CASTS.computeIfAbsent(castUuid, ignored -> new CastState(
            castUuid,
            ownerUuid,
            level.dimension(),
            crown.fieldCenter(),
            crown.castSeed(),
            crown.releaseTick(),
            crown.finishTick()
        ));
        if (state.dimension.equals(level.dimension())) {
            state.crownId = crown.getUUID();
            state.hitTargets.addAll(crown.hitTargets());
        }
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (CastState state : new ArrayList<>(CASTS.values())) {
            if (!state.dimension.equals(level.dimension())) {
                continue;
            }
            Entity ownerEntity = level.getEntity(state.ownerId);
            if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()
                || !ChainControlService.isEligibleEnumaOwner(owner)) {
                finishCast(level, state);
                continue;
            }
            if (!isUpstreamActive(owner, state, now)) {
                finishCast(level, state);
                continue;
            }
            resolveCollisions(level, state, owner, now);
        }
    }

    public static void bindImpactTarget(LivingEntity owner, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || !(owner.level() instanceof ServerLevel level)) {
            return;
        }
        CastState state = castForOwner(level.dimension(), owner.getUUID());
        if (state != null && ChainControlService.isEnemyForEnuma(owner, target)) {
            bindTarget(level, state, owner, target, target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D));
        }
    }

    public static boolean isCasting(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return false;
        }
        CastState state = castForOwner(level.dimension(), owner.getUUID());
        return state != null && isUpstreamActive(owner, state, level.getGameTime());
    }

    public static void cleanupOwner(ServerLevel level, UUID ownerId) {
        CastState state = castForOwner(level.dimension(), ownerId);
        if (state != null) {
            finishCast(level, state);
        }
    }

    public static void cleanupOwner(ServerPlayer player) {
        for (CastState state : new ArrayList<>(CASTS.values())) {
            if (!state.ownerId.equals(player.getUUID())) {
                continue;
            }
            ServerLevel level = player.server.getLevel(state.dimension);
            if (level != null) {
                finishCast(level, state);
            } else {
                CASTS.remove(state.castId, state);
            }
        }
    }

    public static void clearLevel(ServerLevel level) {
        CASTS.values().removeIf(state -> state.dimension.equals(level.dimension()));
    }

    private static void resolveCollisions(ServerLevel level, CastState state, LivingEntity owner, long now) {
        List<HeavenChainEntity> chains = state.chainIds.stream()
            .map(level::getEntity)
            .filter(HeavenChainEntity.class::isInstance)
            .map(HeavenChainEntity.class::cast)
            .filter(Entity::isAlive)
            .sorted(Comparator.comparingInt(HeavenChainEntity::enumaIndex))
            .toList();
        EnumaChainCrownEntity crown = null;
        Entity crownEntity = state.crownId == null ? null : level.getEntity(state.crownId);
        if (crownEntity instanceof EnumaChainCrownEntity liveCrown && liveCrown.isAlive()) {
            crown = liveCrown;
        }
        AABB scanBounds = null;
        for (HeavenChainEntity chain : chains) {
            if (chain.state() == HeavenChainEntity.ChainState.LATCHED
                || chain.state() == HeavenChainEntity.ChainState.RETRACTING
                || chain.state() == HeavenChainEntity.ChainState.BROKEN) {
                continue;
            }
            AABB chainBounds = new AABB(chain.previousServerPosition(), chain.position());
            if (now < state.releaseTick) {
                chainBounds = chainBounds.minmax(new AABB(chain.tetherOrigin(null), chain.position()));
            }
            chainBounds = chainBounds.inflate(ChainConfig.ENUMA_HEAD_COLLISION_RADIUS);
            scanBounds = scanBounds == null ? chainBounds : scanBounds.minmax(chainBounds);
        }
        if (crown != null && now >= state.releaseTick) {
            AABB crownBounds = new AABB(crown.previousServerPosition(), crown.position())
                .inflate(ChainConfig.ENUMA_CROWN_COLLISION_RADIUS);
            scanBounds = scanBounds == null ? crownBounds : scanBounds.minmax(crownBounds);
        }
        if (scanBounds == null) {
            return;
        }
        List<LivingEntity> candidates = level.getEntitiesOfClass(
            LivingEntity.class,
            scanBounds,
            target -> !state.hitTargets.contains(target.getUUID())
                && ChainControlService.isEnemyForEnuma(owner, target)
        );
        candidates.sort(Comparator.comparing(Entity::getUUID));
        for (HeavenChainEntity chain : chains) {
            if (chain.state() == HeavenChainEntity.ChainState.LATCHED
                || chain.state() == HeavenChainEntity.ChainState.RETRACTING
                || chain.state() == HeavenChainEntity.ChainState.BROKEN) {
                continue;
            }
            for (LivingEntity target : candidates) {
                if (state.hitTargets.contains(target.getUUID())) {
                    continue;
                }
                AABB targetBox = target.getBoundingBox().inflate(ChainConfig.ENUMA_HEAD_COLLISION_RADIUS);
                boolean headHit = segmentHits(targetBox, chain.previousServerPosition(), chain.position());
                boolean tetherHit = now < state.releaseTick
                    && segmentHits(targetBox, chain.tetherOrigin(null), chain.position());
                if (headHit || tetherHit) {
                    bindTarget(
                        level,
                        state,
                        owner,
                        target,
                        target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D)
                    );
                    break;
                }
            }
        }
        if (crown != null && now >= state.releaseTick) {
            for (LivingEntity target : candidates) {
                if (state.hitTargets.contains(target.getUUID())) {
                    continue;
                }
                AABB targetBox = target.getBoundingBox().inflate(ChainConfig.ENUMA_CROWN_COLLISION_RADIUS);
                if (segmentHits(targetBox, crown.previousServerPosition(), crown.position())) {
                    bindTarget(
                        level,
                        state,
                        owner,
                        target,
                        target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D)
                    );
                }
            }
        }
    }

    private static boolean segmentHits(AABB box, Vec3 from, Vec3 to) {
        return EnumaPatternMath.segmentHits(box, from, to);
    }

    private static void bindTarget(
        ServerLevel level,
        CastState state,
        LivingEntity owner,
        LivingEntity target,
        Vec3 hitPoint
    ) {
        // The first successful hit consumes the whole Enuma formation.  A
        // second target in the same collision/impact tick must not reuse the
        // representative chain or overwrite its merged gate layout.
        if (!target.isAlive() || !state.hitTargets.isEmpty()) {
            return;
        }
        long minimumBoundUntil = level.getGameTime() + ChainConfig.ENUMA_MIN_BIND_TICKS;
        List<HeavenChainEntity> survivingChains = new ArrayList<>(state.chainIds.size());
        for (UUID chainId : state.chainIds) {
            Entity entity = level.getEntity(chainId);
            if (!(entity instanceof HeavenChainEntity chain) || !chain.isAlive()
                || chain.state() == HeavenChainEntity.ChainState.BROKEN) {
                continue;
            }
            survivingChains.add(chain);
        }
        if (survivingChains.isEmpty()) {
            return;
        }
        state.hitTargets.add(target.getUUID());
        Entity crownEntity = state.crownId == null ? null : level.getEntity(state.crownId);
        if (crownEntity instanceof EnumaChainCrownEntity crown) {
            crown.recordHit(target.getUUID());
        }

        HeavenChainEntity bindingChain = survivingChains.stream()
            .min(Comparator.comparingDouble((HeavenChainEntity chain) -> chain.position().distanceToSqr(hitPoint))
                .thenComparingInt(HeavenChainEntity::enumaIndex))
            .orElseThrow();
        float aggregatedHealth = 0.0F;
        int logicalChainCount = 0;
        for (HeavenChainEntity chain : survivingChains) {
            aggregatedHealth += Math.max(0.0F, chain.chainHealth());
            logicalChainCount += chain.stackedChainCount();
        }

        List<Vec3> gateOrigins = new ArrayList<>(ChainConfig.ENUMA_CHAIN_COUNT);
        for (int index = 0; index < ChainConfig.ENUMA_CHAIN_COUNT; index++) {
            gateOrigins.add(resolveGatePosition(state.center, state.seed, index));
        }
        bindingChain.setEnumaAggregatedHealth(aggregatedHealth, logicalChainCount, gateOrigins);
        bindingChain.latchEnuma(hitPoint, minimumBoundUntil);
        BindingService.bind(level, owner, target, bindingChain, ChainConfig.ENUMA_MIN_BIND_TICKS);

        for (HeavenChainEntity chain : survivingChains) {
            if (chain != bindingChain) {
                chain.discardAfterEnumaAggregation();
            }
        }
        state.chainIds.clear();
        state.chainIds.add(bindingChain.getUUID());
    }

    private static boolean isUpstreamActive(LivingEntity owner, CastState state, long now) {
        CompoundTag data = owner.getPersistentData();
        if (owner instanceof ServerPlayer) {
            return data.getBoolean(PLAYER_ACTIVE)
                && data.getLong(PLAYER_RELEASE) == state.releaseTick;
        }
        return data.getLong(NPC_RELEASE) == state.releaseTick
            && data.getLong(NPC_FINISH) == state.finishTick
            && now <= state.finishTick + 60L;
    }

    private static void finishCast(ServerLevel level, CastState state) {
        if (!CASTS.remove(state.castId, state)) {
            return;
        }
        for (UUID chainId : state.chainIds) {
            Entity entity = level.getEntity(chainId);
            if (entity instanceof HeavenChainEntity chain) {
                chain.markEnumaCastFinished();
                chain.beginRetracting();
            }
        }
        Entity crownEntity = state.crownId == null ? null : level.getEntity(state.crownId);
        if (crownEntity instanceof EnumaChainCrownEntity crown) {
            crown.finishCast();
        }
    }

    @Nullable
    private static CastState castForOwner(ResourceKey<Level> dimension, UUID ownerId) {
        return CASTS.values().stream()
            .filter(state -> state.dimension.equals(dimension) && state.ownerId.equals(ownerId))
            .findFirst()
            .orElse(null);
    }

    private static Vec3 resolveFieldCenter(ServerLevel level, LivingEntity owner) {
        int x = owner.blockPosition().getX();
        int z = owner.blockPosition().getZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(owner.getX(), y + 0.05D, owner.getZ());
    }

    private static Vec3 resolveGatePosition(Vec3 center, long seed, int index) {
        Vec3 offset = EnumaPatternMath.gateOffset(
            seed,
            index,
            ChainConfig.ENUMA_CHAIN_COUNT,
            ChainConfig.ENUMA_FIELD_RADIUS
        );
        // All circles share the caster's resolved ground plane.  Sampling a
        // second heightmap per gate made the pattern climb hills and broke the
        // requested single-plane layout.
        return new Vec3(center.x + offset.x, center.y + 0.03D, center.z + offset.z);
    }

    private static final class CastState {
        private final UUID castId;
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final long seed;
        private final long releaseTick;
        private final long finishTick;
        private final Set<UUID> chainIds = new LinkedHashSet<>();
        private final Set<UUID> hitTargets = new HashSet<>();
        private UUID crownId;

        private CastState(
            UUID castId,
            UUID ownerId,
            ResourceKey<Level> dimension,
            Vec3 center,
            long seed,
            long releaseTick,
            long finishTick
        ) {
            this.castId = castId;
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.center = center;
            this.seed = seed;
            this.releaseTick = releaseTick;
            this.finishTick = finishTick;
        }
    }

    private EnumaChainService() {
    }
}


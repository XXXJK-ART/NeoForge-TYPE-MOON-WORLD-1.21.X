package net.xxxjk.TYPE_MOON_WORLD.chain.entity;

import net.xxxjk.TYPE_MOON_WORLD.chain.config.ChainConfig;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.ChainControlService;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.EnumaChainService;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.TargetingMath;
import net.xxxjk.TYPE_MOON_WORLD.chain.compat.TypeMoonBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HeavenChainEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DURABILITY_CAP = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Integer> STACKED_CHAIN_COUNT = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<CompoundTag> ANCHORS = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.COMPOUND_TAG
    );
    private static final EntityDataAccessor<Boolean> SKILL_CHAIN = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<CompoundTag> TETHER_ORIGIN = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.COMPOUND_TAG
    );
    private static final EntityDataAccessor<Boolean> ENUMA_CHAIN = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> ENUMA_FLOW = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Optional<UUID>> ENUMA_CAST_UUID = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Integer> ENUMA_INDEX = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Long> ENUMA_SEED = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Long> ENUMA_START_TICK = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Long> ENUMA_RELEASE_TICK = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Long> ENUMA_FINISH_TICK = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Long> ENUMA_LATCH_UNTIL = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Long> ENUMA_LATCH_TICK = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<CompoundTag> ENUMA_CENTER = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.COMPOUND_TAG
    );
    private static final EntityDataAccessor<CompoundTag> ENUMA_MERGED_GATES = SynchedEntityData.defineId(
        HeavenChainEntity.class, EntityDataSerializers.COMPOUND_TAG
    );
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("normal");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final HeavenChainPart[] hitParts = new HeavenChainPart[ChainConfig.HIT_PART_COUNT];
    private final List<UUID> targetQueue = new ArrayList<>();
    private final List<Vec3> anchors = new ArrayList<>();
    private int targetIndex;
    private int seekingTicks;
    private int launchDelay;
    private Vec3 gateDirection = Vec3.ZERO;
    private Vec3 previousServerPosition = Vec3.ZERO;
    private Vec3 enumaReleaseOffset = Vec3.ZERO;
    private boolean enumaReleaseOffsetCaptured;
    private boolean enumaCastFinished;
    private boolean dissolveParticlesSpawned;

    public HeavenChainEntity(EntityType<? extends HeavenChainEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
        for (int i = 0; i < hitParts.length; i++) {
            hitParts[i] = new HeavenChainPart(this, ChainConfig.HIT_PART_SIZE);
        }
        setId(ENTITY_COUNTER.getAndAdd(hitParts.length + 1) + 1);
    }

    public void initialize(LivingEntity owner, Vec3 origin, List<LivingEntity> targets, Vec3 fallbackDirection) {
        entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        entityData.set(OWNER_ID, owner.getId());
        setPos(origin);
        setDeltaMovement(fallbackDirection.normalize().scale(ChainConfig.LAUNCH_SPEED));
        setTargets(targets);
    }

    public void initializeFromGate(
        LivingEntity owner,
        Vec3 origin,
        LivingEntity target,
        Vec3 fallbackDirection,
        int delay
    ) {
        entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(SKILL_CHAIN, true);
        entityData.set(TETHER_ORIGIN, pointTag(origin));
        setPos(origin);
        setDeltaMovement(Vec3.ZERO);
        gateDirection = fallbackDirection.normalize();
        targetQueue.clear();
        targetQueue.add(target.getUUID());
        targetIndex = 0;
        seekingTicks = 0;
        launchDelay = Math.max(0, delay);
        setState(ChainState.LAUNCHING);
    }

    public void initializeEnuma(
        LivingEntity owner,
        UUID castUuid,
        int index,
        Vec3 gateOrigin,
        Vec3 fieldCenter,
        long seed,
        long startTick,
        long releaseTick,
        long finishTick
    ) {
        entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(ENUMA_CHAIN, true);
        entityData.set(ENUMA_FLOW, true);
        entityData.set(ENUMA_CAST_UUID, Optional.of(castUuid));
        entityData.set(ENUMA_INDEX, index);
        entityData.set(ENUMA_SEED, seed);
        entityData.set(ENUMA_START_TICK, startTick);
        entityData.set(ENUMA_RELEASE_TICK, releaseTick);
        entityData.set(ENUMA_FINISH_TICK, finishTick);
        entityData.set(ENUMA_CENTER, pointTag(fieldCenter));
        entityData.set(TETHER_ORIGIN, pointTag(gateOrigin));
        targetQueue.clear();
        targetIndex = 0;
        seekingTicks = 0;
        launchDelay = 0;
        gateDirection = new Vec3(0.0D, 1.0D, 0.0D);
        setPos(gateOrigin);
        previousServerPosition = gateOrigin;
        setDeltaMovement(Vec3.ZERO);
        entityData.set(STACKED_CHAIN_COUNT, 1);
        entityData.set(DURABILITY_CAP, ChainConfig.ENUMA_CHAIN_MAX_HEALTH);
        entityData.set(HEALTH, ChainConfig.ENUMA_CHAIN_MAX_HEALTH);
        setState(ChainState.LAUNCHING);
    }

    public void setTargets(List<LivingEntity> targets) {
        targetQueue.clear();
        targets.stream().map(Entity::getUUID).forEach(targetQueue::add);
        targetIndex = 0;
        seekingTicks = 0;
        setState(ChainState.SEEKING);
    }

    public UUID ownerUuid() {
        return entityData.get(OWNER_UUID).orElse(null);
    }

    @Nullable
    public Entity clientOwner() {
        return level().getEntity(entityData.get(OWNER_ID));
    }

    public ChainState state() {
        int ordinal = entityData.get(STATE);
        return ordinal >= 0 && ordinal < ChainState.values().length ? ChainState.values()[ordinal] : ChainState.SEEKING;
    }

    public void beginRetracting() {
        if (isEnumaChain() && level() instanceof ServerLevel level) {
            boolean hasBindings = BindingService.hasBindings(getUUID());
            if (hasBindings || isEnumaBindingProtected(level.getGameTime())) {
                markEnumaCastFinished();
                if (hasBindings) {
                    setState(ChainState.LATCHED);
                    setDeltaMovement(Vec3.ZERO);
                }
                return;
            }
        }
        if (state() != ChainState.BROKEN) {
            setState(ChainState.RETRACTING);
            setDeltaMovement(Vec3.ZERO);
        }
    }

    public float chainHealth() {
        return entityData.get(HEALTH);
    }

    public float chainMaxHealth() {
        return entityData.get(DURABILITY_CAP);
    }

    public int stackedChainCount() {
        return entityData.get(STACKED_CHAIN_COUNT);
    }

    public void setStackedChainCount(int count) {
        int clamped = clampStackedChainCount(count);
        float maximumHealth = ChainConfig.CHAIN_MAX_HEALTH * clamped;
        entityData.set(STACKED_CHAIN_COUNT, clamped);
        entityData.set(DURABILITY_CAP, maximumHealth);
        entityData.set(HEALTH, maximumHealth);
    }

    public void setStackedChainCountPreservingDamage(int count) {
        int clamped = clampStackedChainCount(count);
        float previousMissingHealth = Math.max(0.0F, chainMaxHealth() - chainHealth());
        float maximumHealth = ChainConfig.CHAIN_MAX_HEALTH * clamped;
        entityData.set(STACKED_CHAIN_COUNT, clamped);
        entityData.set(DURABILITY_CAP, maximumHealth);
        entityData.set(HEALTH, Mth.clamp(maximumHealth - previousMissingHealth, 0.0F, maximumHealth));
    }

    public void setEnumaAggregatedHealth(float health, int logicalChainCount, List<Vec3> gateOrigins) {
        if (!isEnumaChain() || state() == ChainState.BROKEN) {
            return;
        }
        int clamped = clampStackedChainCount(logicalChainCount);
        float maximumHealth = ChainConfig.ENUMA_CHAIN_MAX_HEALTH * clamped;
        float aggregated = Mth.clamp(health, 0.0F, maximumHealth);
        entityData.set(STACKED_CHAIN_COUNT, clamped);
        entityData.set(DURABILITY_CAP, maximumHealth);
        entityData.set(HEALTH, aggregated);
        entityData.set(ENUMA_MERGED_GATES, pointsTag(gateOrigins, ChainConfig.ENUMA_CHAIN_COUNT));
    }

    public void discardAfterEnumaAggregation() {
        if (!isEnumaChain() || state() == ChainState.BROKEN) {
            return;
        }
        setState(ChainState.BROKEN);
        entityData.set(HEALTH, 0.0F);
        entityData.set(ENUMA_FLOW, false);
        if (level() instanceof ServerLevel serverLevel) {
            BindingService.releaseByChain(serverLevel, getUUID());
        }
        discard();
    }

    public boolean needsTarget() {
        return !isSkillChain() && !isEnumaChain()
            && state() == ChainState.SEEKING && targetIndex >= targetQueue.size();
    }

    public boolean isSkillChain() {
        return entityData.get(SKILL_CHAIN);
    }

    public boolean isEnumaChain() {
        return entityData.get(ENUMA_CHAIN);
    }

    public boolean hasEnumaFlow() {
        return entityData.get(ENUMA_FLOW);
    }

    public boolean isEnumaCastFinished() {
        return enumaCastFinished;
    }

    @Nullable
    public UUID enumaCastUuid() {
        return entityData.get(ENUMA_CAST_UUID).orElse(null);
    }

    public int enumaIndex() {
        return entityData.get(ENUMA_INDEX);
    }

    public long enumaSeed() {
        return entityData.get(ENUMA_SEED);
    }

    public long enumaReleaseTick() {
        return entityData.get(ENUMA_RELEASE_TICK);
    }

    public long enumaStartTick() {
        return entityData.get(ENUMA_START_TICK);
    }

    public long enumaFinishTick() {
        return entityData.get(ENUMA_FINISH_TICK);
    }

    public Vec3 enumaFieldCenter() {
        CompoundTag point = entityData.get(ENUMA_CENTER);
        return new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z"));
    }

    public long minimumBoundUntil() {
        return Math.max(entityData.get(ENUMA_LATCH_UNTIL), BindingService.minimumBoundUntil(getUUID()));
    }

    public long enumaLatchTick() {
        return entityData.get(ENUMA_LATCH_TICK);
    }

    public Vec3 previousServerPosition() {
        return previousServerPosition;
    }

    public void latchEnuma(Vec3 hitPoint, long minimumBoundUntil) {
        if (!isEnumaChain() || state() == ChainState.LATCHED) {
            return;
        }
        setPos(hitPoint);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(ENUMA_LATCH_TICK, level().getGameTime());
        entityData.set(ENUMA_LATCH_UNTIL, minimumBoundUntil);
        setState(ChainState.LATCHED);
    }

    public void markEnumaCastFinished() {
        enumaCastFinished = true;
        if (isEnumaChain() && !BindingService.hasBindings(getUUID())) {
            entityData.set(ENUMA_FLOW, false);
        }
    }

    public Vec3 tetherOrigin(@Nullable LivingEntity owner) {
        // Once an Enuma chain is latched, its visual and physical tether must
        // remain connected to its own ground gate.  Using the caster's body as
        // the start point here made every Enuma chain collapse into one origin.
        if (isEnumaChain() && state() != ChainState.LATCHED && owner != null
            && level().getGameTime() >= enumaReleaseTick()) {
            return owner.position().add(0.0D, owner.getBbHeight() * 0.65D, 0.0D);
        }
        if (isSkillChain() || isEnumaChain()) {
            CompoundTag point = entityData.get(TETHER_ORIGIN);
            if (point.contains("X", Tag.TAG_DOUBLE)) {
                return new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z"));
            }
        }
        return owner == null ? position() : ownerHand(owner);
    }

    /** Fixed world-space origin of this Enuma chain's matching golden gate. */
    public Vec3 enumaGateOrigin() {
        CompoundTag point = entityData.get(TETHER_ORIGIN);
        if (point.contains("X", Tag.TAG_DOUBLE)) {
            return new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z"));
        }
        return position();
    }

    public List<Vec3> enumaGateOrigins() {
        ListTag points = entityData.get(ENUMA_MERGED_GATES).getList("Points", Tag.TAG_COMPOUND);
        if (points.isEmpty()) {
            return List.of(enumaGateOrigin());
        }
        List<Vec3> result = new ArrayList<>(Math.min(points.size(), ChainConfig.ENUMA_CHAIN_COUNT));
        for (int i = 0; i < points.size() && i < ChainConfig.ENUMA_CHAIN_COUNT; i++) {
            CompoundTag point = points.getCompound(i);
            result.add(new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z")));
        }
        return result;
    }

    public List<Vec3> anchorPoints() {
        if (!level().isClientSide) {
            return List.copyOf(anchors);
        }
        List<Vec3> result = new ArrayList<>();
        for (Tag value : entityData.get(ANCHORS).getList("Points", Tag.TAG_COMPOUND)) {
            CompoundTag point = (CompoundTag)value;
            result.add(new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z")));
        }
        return result;
    }

    public void addAnchor(Vec3 anchor) {
        if (anchors.stream().noneMatch(existing -> existing.distanceToSqr(anchor) < 1.0E-4D)) {
            anchors.add(anchor);
            syncAnchors();
        }
    }

    public void removeAnchor(Vec3 anchor) {
        if (anchors.removeIf(existing -> existing.distanceToSqr(anchor) < 1.0E-4D)) {
            syncAnchors();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(OWNER_ID, -1);
        builder.define(STATE, (byte)ChainState.SEEKING.ordinal());
        builder.define(HEALTH, ChainConfig.CHAIN_MAX_HEALTH);
        builder.define(DURABILITY_CAP, ChainConfig.CHAIN_MAX_HEALTH);
        builder.define(STACKED_CHAIN_COUNT, 1);
        builder.define(ANCHORS, new CompoundTag());
        builder.define(SKILL_CHAIN, false);
        builder.define(TETHER_ORIGIN, new CompoundTag());
        builder.define(ENUMA_CHAIN, false);
        builder.define(ENUMA_FLOW, false);
        builder.define(ENUMA_CAST_UUID, Optional.empty());
        builder.define(ENUMA_INDEX, -1);
        builder.define(ENUMA_SEED, 0L);
        builder.define(ENUMA_START_TICK, 0L);
        builder.define(ENUMA_RELEASE_TICK, 0L);
        builder.define(ENUMA_FINISH_TICK, 0L);
        builder.define(ENUMA_LATCH_UNTIL, 0L);
        builder.define(ENUMA_LATCH_TICK, 0L);
        builder.define(ENUMA_CENTER, new CompoundTag());
        builder.define(ENUMA_MERGED_GATES, new CompoundTag());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            updateHitParts(clientOwner());
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level();
        LivingEntity owner = resolveOwner(serverLevel);
        if (owner == null || !owner.isAlive()) {
            discardWithBindings(serverLevel);
            return;
        }
        if (entityData.get(OWNER_ID) != owner.getId()) {
            entityData.set(OWNER_ID, owner.getId());
        }
        previousServerPosition = position();
        if (isEnumaChain()) {
            EnumaChainService.trackLoadedChain(serverLevel, this);
            tickEnuma(serverLevel, owner);
            updateRotationFromVelocity();
            updateHitParts(owner);
            return;
        }
        if (isSkillChain() ? !ChainControlService.isEligibleSkillOwner(owner) : !ChainControlService.isEligibleOwner(owner)) {
            beginRetracting();
        }
        if (owner.distanceToSqr(this) > ChainConfig.MAX_EXTENSION_SQR) {
            beginRetracting();
        }
        if (isSkillChain() && state() != ChainState.BROKEN
            && tickCount % ChainConfig.SKILL_GATE_REFRESH_TICKS == 0) {
            refreshSkillGate(serverLevel);
        }

        if (state() == ChainState.LAUNCHING && launchDelay > 0) {
            launchDelay--;
            updateHitParts(owner);
            return;
        }
        if (state() == ChainState.LAUNCHING) {
            setState(ChainState.SEEKING);
        }

        switch (state()) {
            case LAUNCHING, SEEKING -> tickSeeking(serverLevel, owner);
            case LATCHED -> tickLatched(serverLevel, owner);
            case RETRACTING -> tickRetracting(serverLevel, owner);
            case BROKEN -> discardWithBindings(serverLevel);
        }
        updateRotationFromVelocity();
        updateHitParts(owner);
    }

    private void tickEnuma(ServerLevel level, LivingEntity owner) {
        if (!ChainControlService.isEligibleEnumaOwner(owner)) {
            discardWithBindings(level);
            return;
        }
        if (state() == ChainState.BROKEN) {
            discardWithBindings(level);
            return;
        }
        long now = level.getGameTime();
        if (state() == ChainState.LATCHED) {
            setDeltaMovement(Vec3.ZERO);
            long protectedUntil = minimumBoundUntil();
            if (BindingService.hasBindings(getUUID())) {
                if (!hasEnumaFlow()) {
                    entityData.set(ENUMA_FLOW, true);
                }
                return;
            }
            if (now < protectedUntil) {
                return;
            }
            if (enumaCastFinished || now >= enumaFinishTick()) {
                entityData.set(ENUMA_FLOW, false);
                setState(ChainState.RETRACTING);
            }
            return;
        }
        if (state() == ChainState.RETRACTING) {
            if (BindingService.hasBindings(getUUID()) || isEnumaBindingProtected(now)) {
                setState(ChainState.LATCHED);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            tickRetracting(level, owner);
            return;
        }
        if (enumaCastFinished && now < enumaReleaseTick()) {
            setState(ChainState.RETRACTING);
            return;
        }
        if (now < enumaReleaseTick()) {
            tickEnumaWindup(owner, now);
        } else if (!enumaCastFinished && now <= enumaFinishTick() + 60L) {
            tickEnumaReleased(owner);
        } else {
            markEnumaCastFinished();
            setState(ChainState.RETRACTING);
        }
    }

    private void tickEnumaWindup(LivingEntity owner, long now) {
        long startTick = enumaStartTick();
        if (startTick <= 0L) {
            startTick = enumaReleaseTick() - ChainConfig.ENUMA_WINDUP_TICKS;
        }
        double age = Math.max(0.0D, now - startTick);
        Vec3 gate = tetherOrigin(null);
        double rise = 7.0D + (enumaIndex() % 5) * 0.65D;
        double liftProgress = Mth.clamp(age / ChainConfig.ENUMA_ASCENT_TICKS, 0.0D, 1.0D);
        double phase = enumaIndex() * 2.399963229728653D + age * 0.075D;
        Vec3 hover = gate.add(Math.cos(phase) * 0.32D, rise * smoothStep(liftProgress), Math.sin(phase) * 0.32D);
        if (age <= ChainConfig.ENUMA_ASCENT_TICKS) {
            Vec3 launchTarget = new Vec3(gate.x, hover.y, gate.z);
            setDeltaMovement(launchTarget.subtract(position()));
            moveTo(launchTarget);
            setState(ChainState.LAUNCHING);
            return;
        }
        Vec3 ownerCenter = owner.position().add(0.0D, owner.getBbHeight() * 0.65D, 0.0D);
        Vec3 target = hover;
        if (position().distanceToSqr(ownerCenter) <= ChainConfig.ENUMA_GATHER_RADIUS * ChainConfig.ENUMA_GATHER_RADIUS) {
            double radius = 1.7D + (enumaIndex() % 6) * 0.28D;
            double vertical = ((enumaIndex() % 9) - 4) * 0.42D;
            target = ownerCenter.add(Math.cos(phase) * radius, vertical, Math.sin(phase) * radius);
        }
        moveEnumaToward(target, 0.62D);
        setState(ChainState.LAUNCHING);
    }

    private void tickEnumaReleased(LivingEntity owner) {
        if (!enumaReleaseOffsetCaptured) {
            enumaReleaseOffset = position().subtract(owner.position());
            enumaReleaseOffsetCaptured = true;
        }
        Vec3 target = owner.position().add(enumaReleaseOffset);
        Vec3 velocity = target.subtract(position());
        setDeltaMovement(velocity);
        moveTo(target);
        setState(ChainState.SEEKING);
    }

    private void moveEnumaToward(Vec3 target, double maxSpeed) {
        Vec3 delta = target.subtract(position());
        Vec3 velocity = delta.lengthSqr() > maxSpeed * maxSpeed ? delta.normalize().scale(maxSpeed) : delta;
        setDeltaMovement(velocity);
        moveTo(position().add(velocity));
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private void tickSeeking(ServerLevel level, LivingEntity owner) {
        seekingTicks++;
        if (needsTarget()) {
            ChainControlService.tryRefreshTargets(owner);
        }
        LivingEntity target = currentTarget(level, owner);
        if (target != null) {
            Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            Vec3 delta = targetPoint.subtract(position());
            Vec3 velocity = TargetingMath.steerToward(
                getDeltaMovement(), delta, ChainConfig.LAUNCH_SPEED, ChainConfig.MAX_STEERING_RADIANS
            );
            Vec3 nextPosition = position().add(velocity);
            Optional<Vec3> hitPoint = target.getBoundingBox().inflate(ChainConfig.CHAIN_HEAD_HITBOX_INFLATION)
                .clip(position(), nextPosition);
            if (hitPoint.isPresent()) {
                setPos(hitPoint.get());
                setDeltaMovement(velocity);
                BindingService.bind(level, owner, target, this);
                targetIndex++;
                seekingTicks = 0;
                if (targetIndex >= targetQueue.size()) {
                    setState(ChainState.LATCHED);
                    setDeltaMovement(Vec3.ZERO);
                }
            } else {
                setDeltaMovement(velocity);
                moveTo(nextPosition);
                if (targetPoint.subtract(nextPosition).dot(velocity) <= 0.0D) {
                    targetIndex++;
                }
            }
        } else if (targetIndex < targetQueue.size()) {
            targetIndex++;
        } else {
            moveTo(position().add(getDeltaMovement()));
            if (seekingTicks >= ChainConfig.SEEK_TIMEOUT_TICKS) {
                beginRetracting();
            }
        }
    }

    private void refreshSkillGate(ServerLevel level) {
        Vec3 direction = gateDirection.lengthSqr() > 1.0E-10D ? gateDirection : getDeltaMovement().normalize();
        if (direction.lengthSqr() <= 1.0E-10D) {
            direction = new Vec3(0.0D, -1.0D, 0.0D);
        }
        TypeMoonBridge.spawnGoldenGate(level, tetherOrigin(null), direction);
    }

    private void updateRotationFromVelocity() {
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() <= 1.0E-10D) {
            return;
        }
        setYRot((float)(Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F);
        setXRot((float)(-(Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG)));
    }

    private void tickLatched(ServerLevel level, LivingEntity owner) {
        if (!BindingService.hasBindings(getUUID())) {
            beginRetracting();
        } else if (owner.distanceToSqr(this) > ChainConfig.MAX_EXTENSION_SQR) {
            beginRetracting();
        }
    }

    private void tickRetracting(ServerLevel level, LivingEntity owner) {
        BindingService.releaseByChain(level, getUUID());
        Vec3 hand = tetherOrigin(owner);
        Vec3 delta = hand.subtract(position());
        if (delta.lengthSqr() <= ChainConfig.RETRACT_SPEED * ChainConfig.RETRACT_SPEED) {
            discardWithBindings(level);
            return;
        }
        setDeltaMovement(delta.normalize().scale(ChainConfig.RETRACT_SPEED));
        moveTo(position().add(getDeltaMovement()));
    }

    @Nullable
    private LivingEntity currentTarget(ServerLevel level, LivingEntity owner) {
        while (targetIndex < targetQueue.size()) {
            Entity entity = level.getEntity(targetQueue.get(targetIndex));
            if (entity instanceof LivingEntity living && living.isAlive()
                && living.distanceToSqr(owner) <= ChainConfig.MAX_EXTENSION_SQR) {
                return living;
            }
            targetIndex++;
        }
        return null;
    }

    @Nullable
    private LivingEntity resolveOwner(ServerLevel level) {
        UUID uuid = ownerUuid();
        Entity entity = uuid == null ? null : level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    public static Vec3 ownerHand(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-5D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        return owner.getEyePosition().add(right.normalize().scale(0.35D)).add(look.scale(0.35D)).add(0.0D, -0.35D, 0.0D);
    }

    private void updateHitParts(@Nullable Entity owner) {
        if (isEnumaChain() && Math.floorMod(tickCount + enumaIndex(), 4) != 0) {
            return;
        }
        Vec3 start = owner instanceof LivingEntity living ? tetherOrigin(living) : tetherOrigin(null);
        List<Vec3> path = new ArrayList<>();
        path.add(start);
        path.addAll(level().isClientSide ? anchorPoints() : anchors);
        if (path.get(path.size() - 1).distanceToSqr(position()) > 1.0E-5D) {
            path.add(position());
        }
        double totalLength = 0.0D;
        for (int i = 1; i < path.size(); i++) {
            totalLength += path.get(i).distanceTo(path.get(i - 1));
        }
        for (int i = 0; i < hitParts.length; i++) {
            double distance = totalLength * (i + 0.5D) / hitParts.length;
            Vec3 point = pointAlong(path, distance);
            HeavenChainPart part = hitParts[i];
            part.moveTo(point.x, point.y, point.z, getYRot(), getXRot());
        }
    }

    private static Vec3 pointAlong(List<Vec3> path, double distance) {
        for (int i = 1; i < path.size(); i++) {
            Vec3 from = path.get(i - 1);
            Vec3 to = path.get(i);
            double segmentLength = from.distanceTo(to);
            if (distance <= segmentLength || i == path.size() - 1) {
                return segmentLength < 1.0E-5D ? to : from.add(to.subtract(from).scale(distance / segmentLength));
            }
            distance -= segmentLength;
        }
        return path.get(path.size() - 1);
    }

    public boolean hurt(HeavenChainPart part, DamageSource source, float amount) {
        return hurt(source, amount);
    }

    public boolean hurtFromBinding(
        HeavenChainBindingEntity binding,
        LivingEntity ignoredBoundTarget,
        DamageSource source,
        float amount
    ) {
        if (!getUUID().equals(binding.chainUuid())) {
            return false;
        }
        return applyDamage(source, amount);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return applyDamage(source, amount);
    }

    private boolean applyDamage(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source) || state() == ChainState.BROKEN) {
            return false;
        }
        if (level() instanceof ServerLevel serverLevel) {
            LivingEntity owner = resolveOwner(serverLevel);
            Entity attacker = source.getEntity();
            if (owner != null && attacker != null && (attacker == owner || attacker.isAlliedTo(owner))) {
                return false;
            }
        }
        if (amount <= 0.0F) {
            return false;
        }
        if (isEnumaChain() && level() instanceof ServerLevel level
            && isEnumaBindingProtected(level.getGameTime())) {
            return false;
        }
        Entity sourceOwner = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (sourceOwner instanceof LivingEntity attacker
            && BindingService.isBoundByChain(attacker.getUUID(), getUUID())) {
            amount = BindingService.scaleDamageFromBoundTarget(attacker, source, amount);
        }
        float remaining = Math.max(0.0F, chainHealth() - amount);
        entityData.set(HEALTH, remaining);
        if (remaining <= 0.0F) {
            breakChain();
        }
        return true;
    }

    public void breakChain() {
        if (state() == ChainState.BROKEN) {
            return;
        }
        setState(ChainState.BROKEN);
        entityData.set(HEALTH, 0.0F);
        if (isEnumaChain()) {
            // The representative chain owns every merged gate and the single
            // binding visual, so breaking it ends all Enuma visuals together.
            entityData.set(ENUMA_FLOW, false);
        }
        if (level() instanceof ServerLevel serverLevel) {
            spawnDissolveParticles(serverLevel);
            BindingService.releaseByChain(serverLevel, getUUID());
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return hitParts;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < hitParts.length; i++) {
            hitParts[i].setId(id + i + 1);
        }
    }

    private void setState(ChainState state) {
        entityData.set(STATE, (byte)state.ordinal());
    }

    private void discardWithBindings(ServerLevel level) {
        spawnDissolveParticles(level);
        BindingService.releaseByChain(level, getUUID());
        discard();
    }

    private void spawnDissolveParticles(ServerLevel level) {
        if (dissolveParticlesSpawned) {
            return;
        }
        dissolveParticlesSpawned = true;
        LivingEntity owner = resolveOwner(level);
        List<Vec3> path = new ArrayList<>();
        path.add(tetherOrigin(owner));
        path.addAll(anchors);
        if (path.get(path.size() - 1).distanceToSqr(position()) > 1.0E-5D) {
            path.add(position());
        }
        double length = 0.0D;
        for (int i = 1; i < path.size(); i++) {
            length += path.get(i - 1).distanceTo(path.get(i));
        }
        int cap = isEnumaChain() ? ChainConfig.ENUMA_DISSOLVE_PARTICLE_CAP : ChainConfig.DISSOLVE_PARTICLE_CAP;
        int minimum = isEnumaChain() ? 4 : 12;
        int count = Math.min(cap, Math.max(minimum, (int)Math.ceil(length * 2.5D)));
        for (int i = 0; i < count; i++) {
            Vec3 point = pointAlong(path, length * (i + 0.5D) / count);
            level.sendParticles(ParticleTypes.WAX_ON, point.x, point.y, point.z, 1, 0.06D, 0.06D, 0.06D, 0.025D);
            if ((i & (isEnumaChain() ? 7 : 3)) == 0) {
                level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.04D, 0.04D, 0.04D, 0.01D);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
        entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        boolean savedEnumaChain = tag.getBoolean("EnumaChain");
        float perChainHealth = savedEnumaChain ? ChainConfig.ENUMA_CHAIN_MAX_HEALTH : ChainConfig.CHAIN_MAX_HEALTH;
        float maximumHealth = tag.contains("ChainMaxHealth", Tag.TAG_FLOAT)
            ? tag.getFloat("ChainMaxHealth")
            : perChainHealth;
        int stackedCount = tag.contains("StackedChainCount", Tag.TAG_INT)
            ? tag.getInt("StackedChainCount")
            : (int)Math.ceil(Math.max(maximumHealth, perChainHealth) / perChainHealth);
        stackedCount = clampStackedChainCount(stackedCount);
        maximumHealth = Math.max(maximumHealth, perChainHealth * stackedCount);
        maximumHealth = savedEnumaChain
            ? Mth.clamp(maximumHealth, perChainHealth, ChainConfig.ENUMA_AGGREGATED_MAX_HEALTH)
            : Math.max(maximumHealth, ChainConfig.CHAIN_MAX_HEALTH);
        entityData.set(STACKED_CHAIN_COUNT, stackedCount);
        entityData.set(DURABILITY_CAP, maximumHealth);
        float savedHealth = tag.contains("ChainHealth", Tag.TAG_FLOAT) ? tag.getFloat("ChainHealth") : maximumHealth;
        entityData.set(HEALTH, Mth.clamp(savedHealth, 0.0F, maximumHealth));
        int stateOrdinal = tag.getByte("ChainState");
        if (stateOrdinal >= 0 && stateOrdinal < ChainState.values().length) {
            setState(ChainState.values()[stateOrdinal]);
        }
        targetQueue.clear();
        ListTag targets = tag.getList("Targets", Tag.TAG_COMPOUND);
        for (Tag value : targets) {
            CompoundTag entry = (CompoundTag)value;
            if (entry.hasUUID("Id")) {
                targetQueue.add(entry.getUUID("Id"));
            }
        }
        targetIndex = Mth.clamp(tag.getInt("TargetIndex"), 0, targetQueue.size());
        seekingTicks = tag.getInt("SeekingTicks");
        launchDelay = Math.max(0, tag.getInt("LaunchDelay"));
        if (tag.contains("GateDirection", Tag.TAG_COMPOUND)) {
            CompoundTag direction = tag.getCompound("GateDirection");
            gateDirection = new Vec3(
                direction.getDouble("X"), direction.getDouble("Y"), direction.getDouble("Z")
            );
        }
        entityData.set(SKILL_CHAIN, tag.getBoolean("SkillChain"));
        if (tag.contains("TetherOrigin", Tag.TAG_COMPOUND)) {
            entityData.set(TETHER_ORIGIN, tag.getCompound("TetherOrigin"));
        }
        entityData.set(ENUMA_CHAIN, tag.getBoolean("EnumaChain"));
        entityData.set(ENUMA_FLOW, tag.getBoolean("EnumaFlow"));
        if (tag.hasUUID("EnumaCast")) {
            entityData.set(ENUMA_CAST_UUID, Optional.of(tag.getUUID("EnumaCast")));
        }
        entityData.set(ENUMA_INDEX, tag.getInt("EnumaIndex"));
        entityData.set(ENUMA_SEED, tag.getLong("EnumaSeed"));
        entityData.set(ENUMA_START_TICK, tag.getLong("EnumaStartTick"));
        entityData.set(ENUMA_RELEASE_TICK, tag.getLong("EnumaReleaseTick"));
        entityData.set(ENUMA_FINISH_TICK, tag.getLong("EnumaFinishTick"));
        entityData.set(ENUMA_LATCH_UNTIL, tag.getLong("EnumaLatchUntil"));
        entityData.set(ENUMA_LATCH_TICK, tag.getLong("EnumaLatchTick"));
        if (tag.contains("EnumaCenter", Tag.TAG_COMPOUND)) {
            entityData.set(ENUMA_CENTER, tag.getCompound("EnumaCenter"));
        }
        if (tag.contains("EnumaMergedGates", Tag.TAG_COMPOUND)) {
            ListTag savedGates = tag.getCompound("EnumaMergedGates").getList("Points", Tag.TAG_COMPOUND);
            List<Vec3> gateOrigins = new ArrayList<>(Math.min(savedGates.size(), ChainConfig.ENUMA_CHAIN_COUNT));
            for (int i = 0; i < savedGates.size() && i < ChainConfig.ENUMA_CHAIN_COUNT; i++) {
                CompoundTag point = savedGates.getCompound(i);
                gateOrigins.add(new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z")));
            }
            entityData.set(ENUMA_MERGED_GATES, pointsTag(gateOrigins, ChainConfig.ENUMA_CHAIN_COUNT));
        }
        if (tag.contains("EnumaReleaseOffset", Tag.TAG_COMPOUND)) {
            CompoundTag offset = tag.getCompound("EnumaReleaseOffset");
            enumaReleaseOffset = new Vec3(offset.getDouble("X"), offset.getDouble("Y"), offset.getDouble("Z"));
            enumaReleaseOffsetCaptured = true;
        }
        enumaCastFinished = tag.getBoolean("EnumaCastFinished");
        previousServerPosition = position();
        anchors.clear();
        for (Tag value : tag.getList("Anchors", Tag.TAG_COMPOUND)) {
            CompoundTag point = (CompoundTag)value;
            anchors.add(new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z")));
        }
        syncAnchors();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID owner = ownerUuid();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putFloat("ChainHealth", chainHealth());
        tag.putFloat("ChainMaxHealth", chainMaxHealth());
        tag.putInt("StackedChainCount", stackedChainCount());
        tag.putByte("ChainState", (byte)state().ordinal());
        ListTag targets = new ListTag();
        for (UUID target : targetQueue) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", target);
            targets.add(entry);
        }
        tag.put("Targets", targets);
        tag.putInt("TargetIndex", targetIndex);
        tag.putInt("SeekingTicks", seekingTicks);
        tag.putInt("LaunchDelay", launchDelay);
        tag.put("GateDirection", pointTag(gateDirection));
        tag.putBoolean("SkillChain", isSkillChain());
        tag.put("TetherOrigin", entityData.get(TETHER_ORIGIN).copy());
        tag.putBoolean("EnumaChain", isEnumaChain());
        tag.putBoolean("EnumaFlow", hasEnumaFlow());
        UUID castUuid = enumaCastUuid();
        if (castUuid != null) {
            tag.putUUID("EnumaCast", castUuid);
        }
        tag.putInt("EnumaIndex", enumaIndex());
        tag.putLong("EnumaSeed", enumaSeed());
        tag.putLong("EnumaStartTick", enumaStartTick());
        tag.putLong("EnumaReleaseTick", enumaReleaseTick());
        tag.putLong("EnumaFinishTick", enumaFinishTick());
        tag.putLong("EnumaLatchUntil", entityData.get(ENUMA_LATCH_UNTIL));
        tag.putLong("EnumaLatchTick", enumaLatchTick());
        tag.put("EnumaCenter", entityData.get(ENUMA_CENTER).copy());
        tag.put("EnumaMergedGates", entityData.get(ENUMA_MERGED_GATES).copy());
        if (enumaReleaseOffsetCaptured) {
            tag.put("EnumaReleaseOffset", pointTag(enumaReleaseOffset));
        }
        tag.putBoolean("EnumaCastFinished", enumaCastFinished);
        tag.put("Anchors", anchorsTag().getList("Points", Tag.TAG_COMPOUND));
    }

    private void syncAnchors() {
        entityData.set(ANCHORS, anchorsTag());
    }

    private CompoundTag anchorsTag() {
        return pointsTag(anchors, anchors.size());
    }

    private static CompoundTag pointsTag(List<Vec3> positions, int maximumPoints) {
        CompoundTag result = new CompoundTag();
        ListTag serialized = new ListTag();
        int count = Math.min(positions.size(), Math.max(0, maximumPoints));
        for (int i = 0; i < count; i++) {
            Vec3 anchor = positions.get(i);
            CompoundTag point = new CompoundTag();
            point.putDouble("X", anchor.x);
            point.putDouble("Y", anchor.y);
            point.putDouble("Z", anchor.z);
            serialized.add(point);
        }
        result.put("Points", serialized);
        return result;
    }

    private static CompoundTag pointTag(Vec3 point) {
        CompoundTag result = new CompoundTag();
        result.putDouble("X", point.x);
        result.putDouble("Y", point.y);
        result.putDouble("Z", point.z);
        return result;
    }

    private static int clampStackedChainCount(int count) {
        return Mth.clamp(count, 1, ChainConfig.MAX_FUSED_CHAIN_COUNT);
    }

    private boolean isEnumaBindingProtected(long gameTime) {
        return gameTime < entityData.get(ENUMA_LATCH_UNTIL)
            || BindingService.hasProtectedBindings(getUUID(), gameTime);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public enum ChainState {
        LAUNCHING,
        SEEKING,
        LATCHED,
        RETRACTING,
        BROKEN
    }
}


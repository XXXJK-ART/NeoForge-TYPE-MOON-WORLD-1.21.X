package net.xxxjk.TYPE_MOON_WORLD.chain.entity;

import net.xxxjk.TYPE_MOON_WORLD.chain.config.ChainConfig;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.EnumaChainService;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class EnumaChainCrownEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
        EnumaChainCrownEntity.class, EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
        EnumaChainCrownEntity.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Optional<UUID>> CAST_UUID = SynchedEntityData.defineId(
        EnumaChainCrownEntity.class, EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<CompoundTag> FIELD_CENTER = SynchedEntityData.defineId(
        EnumaChainCrownEntity.class, EntityDataSerializers.COMPOUND_TAG
    );
    private static final EntityDataAccessor<Long> CAST_SEED = SynchedEntityData.defineId(
        EnumaChainCrownEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Long> RELEASE_TICK = SynchedEntityData.defineId(
        EnumaChainCrownEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Long> FINISH_TICK = SynchedEntityData.defineId(
        EnumaChainCrownEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(
        EnumaChainCrownEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("normal");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 previousServerPosition = Vec3.ZERO;
    private int orphanTicks;
    private final Set<UUID> hitTargets = new HashSet<>();

    public EnumaChainCrownEntity(EntityType<? extends EnumaChainCrownEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public void initialize(
        LivingEntity owner,
        UUID castUuid,
        Vec3 fieldCenter,
        long seed,
        long releaseTick,
        long finishTick
    ) {
        entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(CAST_UUID, Optional.of(castUuid));
        entityData.set(FIELD_CENTER, pointTag(fieldCenter));
        entityData.set(CAST_SEED, seed);
        entityData.set(RELEASE_TICK, releaseTick);
        entityData.set(FINISH_TICK, finishTick);
        entityData.set(ACTIVE, true);
        setPos(crownPosition(owner, owner.getLookAngle()));
        previousServerPosition = position();
    }

    @Nullable
    public UUID ownerUuid() {
        return entityData.get(OWNER_UUID).orElse(null);
    }

    @Nullable
    public UUID castUuid() {
        return entityData.get(CAST_UUID).orElse(null);
    }

    public Vec3 fieldCenter() {
        CompoundTag tag = entityData.get(FIELD_CENTER);
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }

    public long castSeed() {
        return entityData.get(CAST_SEED);
    }

    public long releaseTick() {
        return entityData.get(RELEASE_TICK);
    }

    public long finishTick() {
        return entityData.get(FINISH_TICK);
    }

    public boolean isActiveCastVisual() {
        return entityData.get(ACTIVE);
    }

    public boolean shouldRenderCrown() {
        return isActiveCastVisual() && level().getGameTime() >= releaseTick();
    }

    public Vec3 previousServerPosition() {
        return previousServerPosition;
    }

    public Set<UUID> hitTargets() {
        return Set.copyOf(hitTargets);
    }

    public void recordHit(UUID targetId) {
        hitTargets.add(targetId);
    }

    public void finishCast() {
        entityData.set(ACTIVE, false);
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(OWNER_ID, -1);
        builder.define(CAST_UUID, Optional.empty());
        builder.define(FIELD_CENTER, new CompoundTag());
        builder.define(CAST_SEED, 0L);
        builder.define(RELEASE_TICK, 0L);
        builder.define(FINISH_TICK, 0L);
        builder.define(ACTIVE, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isActiveCastVisual() && level().getGameTime() <= finishTick()) {
                spawnClientFieldParticles();
            }
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level();
        EnumaChainService.trackLoadedCrown(serverLevel, this);
        LivingEntity owner = resolveOwner(serverLevel);
        if (owner == null || !owner.isAlive()) {
            if (++orphanTicks >= ChainConfig.ENUMA_ORPHAN_GRACE_TICKS) {
                finishCast();
            }
            return;
        }
        orphanTicks = 0;
        if (entityData.get(OWNER_ID) != owner.getId()) {
            entityData.set(OWNER_ID, owner.getId());
        }
        previousServerPosition = position();
        Vec3 direction = owner.getDeltaMovement();
        if (direction.lengthSqr() < 0.0625D) {
            direction = owner.getLookAngle();
        }
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = new Vec3(0.0D, -1.0D, 0.0D);
        }
        direction = direction.normalize();
        Vec3 next = crownPosition(owner, direction);
        setDeltaMovement(next.subtract(position()));
        setPos(next);
        updateRotation(direction);
    }

    private Vec3 crownPosition(LivingEntity owner, Vec3 direction) {
        Vec3 head = owner.getEyePosition().add(0.0D, 3.25D, 0.0D);
        return level().getGameTime() >= releaseTick() ? head.add(direction.normalize().scale(4.0D)) : head;
    }

    private void updateRotation(Vec3 direction) {
        setYRot((float)(Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F);
        setXRot((float)(-(Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG)));
    }

    private void spawnClientFieldParticles() {
        Vec3 center = fieldCenter();
        long tickSeed = mix64(castSeed() ^ level().getGameTime() * 0x9E3779B97F4A7C15L);
        for (int i = 0; i < ChainConfig.ENUMA_CLIENT_FIELD_PARTICLES_PER_TICK; i++) {
            long a = mix64(tickSeed + i * 0xD1B54A32D192ED03L);
            long b = mix64(a + 0x94D049BB133111EBL);
            double radius = Math.sqrt(unitDouble(a)) * ChainConfig.ENUMA_FIELD_RADIUS;
            double angle = unitDouble(b) * Math.PI * 2.0D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level().addParticle(
                ParticleTypes.HAPPY_VILLAGER,
                x,
                center.y + 0.08D + unitDouble(mix64(b)) * 0.12D,
                z,
                0.0D,
                0.012D,
                0.0D
            );
        }
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double unitDouble(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    @Nullable
    private LivingEntity resolveOwner(ServerLevel level) {
        UUID uuid = ownerUuid();
        Entity entity = uuid == null ? null : level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
        entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        if (tag.hasUUID("Cast")) {
            entityData.set(CAST_UUID, Optional.of(tag.getUUID("Cast")));
        }
        if (tag.contains("FieldCenter", Tag.TAG_COMPOUND)) {
            entityData.set(FIELD_CENTER, tag.getCompound("FieldCenter"));
        }
        entityData.set(CAST_SEED, tag.getLong("CastSeed"));
        entityData.set(RELEASE_TICK, tag.getLong("ReleaseTick"));
        entityData.set(FINISH_TICK, tag.getLong("FinishTick"));
        entityData.set(ACTIVE, tag.getBoolean("Active"));
        hitTargets.clear();
        for (Tag value : tag.getList("HitTargets", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag)value;
            if (entry.hasUUID("Id")) {
                hitTargets.add(entry.getUUID("Id"));
            }
        }
        previousServerPosition = position();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID owner = ownerUuid();
        UUID cast = castUuid();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        if (cast != null) {
            tag.putUUID("Cast", cast);
        }
        tag.put("FieldCenter", entityData.get(FIELD_CENTER).copy());
        tag.putLong("CastSeed", castSeed());
        tag.putLong("ReleaseTick", releaseTick());
        tag.putLong("FinishTick", finishTick());
        tag.putBoolean("Active", isActiveCastVisual());
        ListTag savedHits = new ListTag();
        for (UUID targetId : hitTargets) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", targetId);
            savedHits.add(entry);
        }
        tag.put("HitTargets", savedHits);
    }

    private static CompoundTag pointTag(Vec3 point) {
        CompoundTag result = new CompoundTag();
        result.putDouble("X", point.x);
        result.putDouble("Y", point.y);
        result.putDouble("Z", point.z);
        return result;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}


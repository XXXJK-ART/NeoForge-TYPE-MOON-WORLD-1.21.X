package com.example.typemoonaddon.entity;

import java.util.UUID;
import com.example.typemoonaddon.servant.GillesDeRaisCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardGillesDeRaisSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HugeSeaMonsterEntity extends PathfinderMob implements GeoEntity {
    private static final DustParticleOptions PURPLE_FOG =
            new DustParticleOptions(new Vector3f(0.34F, 0.04F, 0.58F), 4.0F);
    private static final DustParticleOptions DEEP_PURPLE_FOG =
            new DustParticleOptions(new Vector3f(0.08F, 0.01F, 0.14F), 3.2F);
    private static final EntityDataAccessor<Boolean> AWAKENED =
            SynchedEntityData.defineId(HugeSeaMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DISSOLVING =
            SynchedEntityData.defineId(HugeSeaMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int DISSOLVE_DURATION = 72;
    private static final int ATTACK_TERRAIN_BREAK_LIMIT = 40;
    private static final int TARGET_REFRESH_INTERVAL = 120;
    private static final int BOSS_ATTACK_INTERVAL = 40;
    private static final int COLOSSAL_SPIT_INTERVAL = 80;
    private static final int BROOD_SUMMON_INTERVAL = 480;
    private static final int BROOD_SMALL_BATCH = 20;
    private static final int BROOD_LARGE_BATCH = 10;
    private static final int BROOD_SMALL_LIMIT = 100;
    private static final int BROOD_LARGE_LIMIT = 10;
    private static final int FOG_INTERVAL = 45;
    private static final int HEAL_TICK_INTERVAL = 20;
    private static final float HEAL_PER_SECOND = 200.0F;
    private static final float HEAL_INTERRUPT_DAMAGE_THRESHOLD = 1000.0F;
    private static final int HEAL_INTERRUPT_WINDOW_TICKS = 100;
    private static final int HEAL_INTERRUPT_DURATION_TICKS = 1200;
    private static final double HUGE_MOVEMENT_SPEED = 0.14;
    private static final double CLOSE_ATTACK_RANGE = 72.0;
    private static final double COLOSSAL_SPIT_MIN_RANGE = 24.0;
    private static final double COLOSSAL_SPIT_RANGE = 120.0;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID sourceUuid;
    @Nullable
    private UUID masterUuid;
    @Nullable
    private LivingEntity cachedSourceEntity;
    private long cachedSourceEntityGameTime = Long.MIN_VALUE;
    private long healCooldownUntil;
    private long recentDamageWindowStart = Long.MIN_VALUE;
    private float recentDamageWindowTotal;
    private int dissolveTicks;
    private boolean deathEffectsApplied;

    public HugeSeaMonsterEntity(EntityType<HugeSeaMonsterEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0)
                .add(Attributes.ATTACK_DAMAGE, 60.0)
                .add(Attributes.MOVEMENT_SPEED, HUGE_MOVEMENT_SPEED)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKENED, true);
        builder.define(DISSOLVING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        if (this.isDissolving()) {
            this.tickDissolve();
            return;
        }
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        boolean nearbyPlayer = level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 128.0);
        if (!nearbyPlayer) {
            this.getNavigation().stop();
            return;
        }
        LivingEntity target = this.getTarget();
        if (this.isStaggeredTick(FOG_INTERVAL, 0)) {
            this.spawnUnknowableFog(level);
        }
        if (this.tickCount % HEAL_TICK_INTERVAL == 0) {
            this.tickHeal(level);
        }
        if (this.isStaggeredTick(TARGET_REFRESH_INTERVAL, 7) && !isValidTarget(target)) {
            LivingEntity revenge = this.getLastHurtByMob();
            this.setTarget(isValidTarget(revenge) ? revenge : this.findNearestTarget(COLOSSAL_SPIT_RANGE));
            target = this.getTarget();
        }
        if (this.isValidTarget(target) && this.distanceTo(target) <= COLOSSAL_SPIT_RANGE) {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 20.0F, 20.0F);
            double distance = this.distanceTo(target);
            if (distance > CLOSE_ATTACK_RANGE && this.tryColossalSpitAttack(level, target, distance)) {
                return;
            }
            if (distance <= CLOSE_ATTACK_RANGE && this.isStaggeredTick(BOSS_ATTACK_INTERVAL, 0)) {
                this.performMajorAttack(level, target);
            }
        } else {
            this.getNavigation().stop();
            this.setTarget(null);
        }
        if (this.isStaggeredTick(BROOD_SUMMON_INTERVAL, 40)) {
            this.trySummonBrood(level);
        }
    }

    public void setSource(@Nullable LivingEntity source) {
        this.sourceUuid = source == null ? null : source.getUUID();
        this.masterUuid = source instanceof GillesDeRaisEntity gilles && gilles.getEntityMaster() != null
                ? gilles.getEntityMaster().getUUID()
                : null;
        this.cachedSourceEntity = source;
        this.cachedSourceEntityGameTime = this.level().getGameTime();
    }

    public boolean isDissolving() {
        return this.entityData.get(DISSOLVING);
    }

    public float getDissolveProgress(float partialTick) {
        if (!this.isDissolving()) {
            return 0.0F;
        }
        return Math.min(1.0F, (this.dissolveTicks + partialTick) / (float) DISSOLVE_DURATION);
    }

    @Nullable
    public UUID getSourceUuid() {
        return this.sourceUuid;
    }

    @Nullable
    public UUID getMasterUuid() {
        return this.masterUuid;
    }

    public boolean isFriendlyTo(@Nullable Entity entity) {
        return this.isFriendly(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || this.isDissolving()) {
            return;
        }
        this.maintainOwnerPassenger(level);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && this.isAuthorizedPassenger(passenger);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        callback.accept(passenger, this.getX(), this.getY() + this.getBbHeight() * 0.18, this.getZ());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.isFriendly(target)) {
            return false;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isDissolving() || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)) {
            return false;
        }
        if (this.isFriendly(source.getEntity()) || this.isFriendly(source.getDirectEntity())) {
            return false;
        }
        LivingEntity attacker = this.livingAttacker(source);
        if (!this.level().isClientSide() && isValidTarget(attacker)) {
            this.setTarget(attacker);
            GillesDeRaisCombatHelper.shareSeaMonsterRetaliation(this, attacker);
        }
        if (!this.level().isClientSide() && amount > 0.0F) {
            this.recordRecentDamage(amount);
        }
        if (!this.level().isClientSide() && amount >= this.getHealth()) {
            this.beginDissolve(source);
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAttackable() {
        return !this.isDissolving() && super.isAttackable();
    }

    @Override
    public boolean isPickable() {
        return !this.isDissolving() && super.isPickable();
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    @Override
    public float getPickRadius() {
        return 18.0F;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || this.isFriendly(other);
    }

    @Override
    public void die(DamageSource cause) {
        this.beginDissolve(cause);
    }

    @Override
    protected void tickDeath() {
        if (this.isDissolving()) {
            this.tickDissolve();
            return;
        }
        super.tickDeath();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.sourceUuid != null) {
            tag.putUUID("GillesHugeSeaMonsterSource", this.sourceUuid);
        }
        if (this.masterUuid != null) {
            tag.putUUID("GillesHugeSeaMonsterMaster", this.masterUuid);
        }
        tag.putBoolean("GillesHugeSeaMonsterDissolving", this.isDissolving());
        tag.putInt("GillesHugeSeaMonsterDissolveTicks", this.dissolveTicks);
        tag.putBoolean("GillesHugeSeaMonsterDeathEffectsApplied", this.deathEffectsApplied);
        tag.putLong("GillesHugeSeaMonsterHealCooldownUntil", this.healCooldownUntil);
        tag.putLong("GillesHugeSeaMonsterRecentDamageWindowStart", this.recentDamageWindowStart);
        tag.putFloat("GillesHugeSeaMonsterRecentDamageWindowTotal", this.recentDamageWindowTotal);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("GillesHugeSeaMonsterSource")) {
            this.sourceUuid = tag.getUUID("GillesHugeSeaMonsterSource");
        }
        if (tag.hasUUID("GillesHugeSeaMonsterMaster")) {
            this.masterUuid = tag.getUUID("GillesHugeSeaMonsterMaster");
        }
        this.entityData.set(DISSOLVING, tag.getBoolean("GillesHugeSeaMonsterDissolving"));
        this.dissolveTicks = tag.getInt("GillesHugeSeaMonsterDissolveTicks");
        this.deathEffectsApplied = tag.getBoolean("GillesHugeSeaMonsterDeathEffectsApplied");
        this.healCooldownUntil = tag.getLong("GillesHugeSeaMonsterHealCooldownUntil");
        this.recentDamageWindowStart = tag.getLong("GillesHugeSeaMonsterRecentDamageWindowStart");
        this.recentDamageWindowTotal = tag.getFloat("GillesHugeSeaMonsterRecentDamageWindowTotal");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            String loop = event.isMoving() ? "animation.gilles_huge_sea_monster.walk" : "animation.gilles_huge_sea_monster.idle";
            return event.setAndContinue(RawAnimation.begin().thenLoop(loop));
        }));
        AnimationController<HugeSeaMonsterEntity> action = new AnimationController<>(this, "action_controller", 0, event -> PlayState.STOP);
        action.triggerableAnim("slam", RawAnimation.begin().thenPlay("animation.gilles_huge_sea_monster.slam"));
        action.triggerableAnim("grab", RawAnimation.begin().thenPlay("animation.gilles_huge_sea_monster.grab"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private void spawnUnknowableFog(ServerLevel level) {
        if (!level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 96.0)) {
            return;
        }
        double y = this.getY() + this.getBbHeight() * 0.62;
        level.sendParticles(PURPLE_FOG, this.getX(), y, this.getZ(), 24, 18.0, 14.0, 18.0, 0.02);
        level.sendParticles(DEEP_PURPLE_FOG, this.getX(), y + 2.0, this.getZ(), 18, 15.0, 12.0, 15.0, 0.015);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), y + 1.0, this.getZ(), 12, 17.0, 11.0, 17.0, 0.014);
    }

    private void tickHeal(ServerLevel level) {
        long now = level.getGameTime();
        if (now < this.healCooldownUntil) {
            this.pruneRecentDamageWindow(now);
            return;
        }
        this.pruneRecentDamageWindow(now);
        this.heal(HEAL_PER_SECOND);
    }

    private void recordRecentDamage(float amount) {
        if (amount <= 0.0F) {
            return;
        }
        long now = this.level().getGameTime();
        this.pruneRecentDamageWindow(now);
        if (this.recentDamageWindowStart == Long.MIN_VALUE) {
            this.recentDamageWindowStart = now;
        }
        this.recentDamageWindowTotal += amount;
        if (this.recentDamageWindowTotal > HEAL_INTERRUPT_DAMAGE_THRESHOLD) {
            this.healCooldownUntil = now + HEAL_INTERRUPT_DURATION_TICKS;
            this.recentDamageWindowStart = Long.MIN_VALUE;
            this.recentDamageWindowTotal = 0.0F;
        }
    }

    private void pruneRecentDamageWindow(long now) {
        if (this.recentDamageWindowStart == Long.MIN_VALUE) {
            return;
        }
        if (now - this.recentDamageWindowStart > HEAL_INTERRUPT_WINDOW_TICKS) {
            this.recentDamageWindowStart = now;
            this.recentDamageWindowTotal = 0.0F;
        }
    }

    private void performMajorAttack(ServerLevel level, LivingEntity target) {
        if (!level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 96.0)) {
            return;
        }
        if (!this.isValidTarget(target) || this.distanceToSqr(target) > 72.0 * 72.0) {
            return;
        }
        switch (Math.floorMod((this.tickCount / BOSS_ATTACK_INTERVAL) + this.getId(), 5)) {
            case 0 -> this.bossPulseAttack(level, target);
            case 1 -> this.grabAndLiftTarget(level, target);
            case 2 -> this.floodPollutionZone(level, target);
            case 3 -> this.crossTentacleSweep(level, target);
            default -> this.abyssalCataclysm(level, target);
        }
    }

    private void bossPulseAttack(ServerLevel level, LivingEntity target) {
        this.triggerAnim("action_controller", "slam");
        level.playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6F, 0.55F);
        this.breakTerrainAhead(level, 34.0, 19.0F, 18, 260);
        Vec3 center = target.position();
        AABB box = new AABB(center.x - 16.0, center.y - 6.0, center.z - 16.0,
                center.x + 16.0, center.y + 8.0, center.z + 16.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
                living -> this.isValidTarget(living) && living.distanceToSqr(center) <= 256.0)) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), 48.0F);
            living.push((living.getX() - this.getX()) * 0.36, 0.46, (living.getZ() - this.getZ()) * 0.36);
            living.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.8, target.getZ(), 12, 4.0, 0.8, 4.0, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 0.6, target.getZ(), 48, 4.0, 1.0, 4.0, 0.02);
    }

    private void grabAndLiftTarget(ServerLevel level, LivingEntity target) {
        this.triggerAnim("action_controller", "grab");
        level.playSound(null, this.blockPosition(), SoundEvents.GUARDIAN_HURT, SoundSource.HOSTILE, 1.5F, 0.6F);
        Vec3 pull = this.position().add(0.0, this.getBbHeight() * 0.45, 0.0).subtract(target.position());
        if (pull.lengthSqr() > 1.0E-4) {
            pull = pull.normalize();
        }
        target.invulnerableTime = 0;
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 1, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 3, false, true, true));
        target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 0.2, 0.2).add(pull.x * 1.8, 0.9, pull.z * 1.8));
        target.hurt(this.damageSources().mobAttack(this), 28.0F);
        target.hurtMarked = true;
        level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 0.6, target.getZ(), 80, 0.8, 1.2, 0.8, 0.03);
        level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 1.0, target.getZ(), 45, 0.7, 0.9, 0.7, 0.02);
    }

    private void floodPollutionZone(ServerLevel level, LivingEntity target) {
        this.triggerAnim("action_controller", "grab");
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.3F, 0.7F);
        Vec3 center = target.position();
        GillesDeRaisCombatHelper.addPollutionZone(level.dimension(), center, 22.0, 300, 14.0F, this.getSourceUuid(), this.getMasterUuid());
        this.breakCraterTerrain(level, center, 14.0, 5, 260);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(22.0), this::isValidTarget)) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0, false, true, true));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 2, false, true, true));
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true, true));
            living.hurt(this.damageSources().magic(), 14.0F);
        }
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 0.8, center.z, 260, 8.0, 1.4, 8.0, 0.04);
        level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 1.2, center.z, 180, 7.0, 1.2, 7.0, 0.03);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.4, center.z, 140, 7.5, 1.1, 7.5, 0.03);
    }

    private void crossTentacleSweep(ServerLevel level, LivingEntity target) {
        this.triggerAnim("action_controller", "slam");
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.4F, 0.5F);
        Vec3 center = target.position();
        Vec3 forward = center.subtract(this.position());
        if (forward.horizontalDistanceSqr() < 1.0E-4) {
            forward = this.getLookAngle();
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        this.breakTerrainCross(level, center, forward, right);
        AABB box = new AABB(center.x - 22.0, center.y - 5.0, center.z - 22.0, center.x + 22.0, center.y + 12.0, center.z + 22.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, this::isValidTarget)) {
            Vec3 delta = living.position().subtract(center);
            double along = Math.abs(delta.dot(forward));
            double side = Math.abs(delta.dot(right));
            if (along > 18.0 || side > 8.0) {
                continue;
            }
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), 32.0F);
            living.push(delta.x * 0.28, 0.38, delta.z * 0.28);
            living.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.8, center.z, 16, 6.0, 1.0, 6.0, 0.03);
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 0.8, center.z, 220, 7.0, 1.6, 7.0, 0.05);
        level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 1.2, center.z, 150, 6.0, 1.4, 6.0, 0.04);
    }

    private void abyssalCataclysm(ServerLevel level, LivingEntity target) {
        this.triggerAnim("action_controller", "slam");
        Vec3 center = target.position();
        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 1.8F, 0.45F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.7F);
        GillesDeRaisCombatHelper.addPollutionZone(level.dimension(), center, 26.0, 360, 16.0F, this.getSourceUuid(), this.getMasterUuid());
        this.breakCraterTerrain(level, center, 22.0, 9, 720);
        AABB box = new AABB(center.x - 28.0, center.y - 8.0, center.z - 28.0, center.x + 28.0, center.y + 14.0, center.z + 28.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, this::isValidTarget)) {
            double distance = Math.max(1.0, living.position().distanceTo(center));
            if (distance > 28.0) {
                continue;
            }
            double falloff = 1.0 - distance / 32.0;
            Vec3 away = living.position().subtract(center);
            if (away.horizontalDistanceSqr() < 1.0E-4) {
                away = living.getLookAngle();
            }
            away = away.normalize();
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().magic(), (float)(30.0 + 26.0 * falloff));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 3, false, true, true));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true, true));
            living.push(away.x * (1.0 + falloff), 0.65 + falloff * 0.35, away.z * (1.0 + falloff));
            living.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 1.0, center.z, 28, 9.0, 1.4, 9.0, 0.04);
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 1.2, center.z, 360, 11.0, 2.0, 11.0, 0.06);
        level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 1.4, center.z, 260, 10.0, 2.0, 10.0, 0.045);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.8, center.z, 220, 10.0, 1.7, 10.0, 0.04);
    }

    private boolean tryColossalSpitAttack(ServerLevel level, LivingEntity target, double distance) {
        if (!this.isStaggeredTick(COLOSSAL_SPIT_INTERVAL, 17) || distance < COLOSSAL_SPIT_MIN_RANGE
                || distance > COLOSSAL_SPIT_RANGE || !this.hasLineOfSight(target)
                || !level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 160.0)) {
            return false;
        }
        this.triggerAnim("action_controller", "slam");
        Vec3 targetPoint = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
        Vec3 mouth = this.position().add(0.0, this.getBbHeight() * 0.42, 0.0);
        Vec3 direction = targetPoint.subtract(mouth);
        if (direction.lengthSqr() < 1.0E-4) {
            direction = this.getLookAngle();
        }
        direction = direction.normalize();
        Vec3 spawn = mouth.add(direction.scale(8.0));
        SeaMonsterSpitEntity spit = new SeaMonsterSpitEntity(level, this);
        spit.setPos(spawn.x, spawn.y, spawn.z);
        spit.configure(30.0F, COLOSSAL_SPIT_RANGE + 16.0, 24.0, 600, 18.0F);
        spit.setDeltaMovement(direction.scale(1.95));
        level.addFreshEntity(spit);
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.7F, 0.5F);
        level.sendParticles(ParticleTypes.SQUID_INK, spawn.x, spawn.y, spawn.z, 180, 4.5, 2.2, 4.5, 0.08);
        level.sendParticles(ModParticles.ELEMENTAL_FOAM.get(), spawn.x, spawn.y, spawn.z, 120, 3.8, 1.8, 3.8, 0.07);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, spawn.x, spawn.y, spawn.z, 70, 4.0, 2.0, 4.0, 0.05);
        return true;
    }

    private void trySummonBrood(ServerLevel level) {
        if (!level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 96.0)) {
            return;
        }
        int small = 0;
        int large = 0;
        for (SeaMonsterEntity seaMonster : level.getEntitiesOfClass(SeaMonsterEntity.class, this.getBoundingBox().inflate(64.0),
                seaMonster -> seaMonster.isAlive() && this.getUUID().equals(seaMonster.getControllerUuid()))) {
            if (seaMonster.isLarge()) {
                large++;
            } else {
                small++;
            }
            if (small >= BROOD_SMALL_LIMIT && large >= BROOD_LARGE_LIMIT) {
                break;
            }
        }
        int smallToSpawn = Math.min(BROOD_SMALL_BATCH, Math.max(0, BROOD_SMALL_LIMIT - small));
        int largeToSpawn = Math.min(BROOD_LARGE_BATCH, Math.max(0, BROOD_LARGE_LIMIT - large));
        for (int i = 0; i < smallToSpawn; i++) {
            spawnBrood(level, false);
        }
        for (int i = 0; i < largeToSpawn; i++) {
            spawnBrood(level, true);
        }
    }

    private void spawnBrood(ServerLevel level, boolean large) {
        SeaMonsterEntity seaMonster = com.example.typemoonaddon.registry.AddonEntities.GILLES_SEA_MONSTER.get().create(level);
        if (seaMonster == null) {
            return;
        }
        Vec3 offset = new Vec3((this.random.nextDouble() - 0.5) * 10.0, 0.0, (this.random.nextDouble() - 0.5) * 10.0);
        seaMonster.moveTo(this.getX() + offset.x, this.getY(), this.getZ() + offset.z, this.getYRot(), 0.0F);
        seaMonster.setController(this);
        seaMonster.setLarge(large);
        level.addFreshEntity(seaMonster);
        level.sendParticles(ParticleTypes.SQUID_INK, seaMonster.getX(), seaMonster.getY() + 0.6, seaMonster.getZ(), large ? 10 : 5, 0.5, 0.5, 0.5, 0.05);
    }

    @Nullable
    private LivingEntity findNearestTarget(double radius) {
        AABB box = this.getBoundingBox().inflate(radius);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class, box, this::isValidTarget)) {
            double distance = this.distanceToSqr(candidate);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isValidTarget(@Nullable LivingEntity target) {
        return target != null && target != this && target.isAlive() && target.getVehicle() != this
                && !this.getPassengers().contains(target) && !target.isAlliedTo(this) && !this.isFriendly(target)
                && !EntityUtils.isImmunePlayerTarget(target);
    }

    private boolean isFriendly(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity == this || entity.getVehicle() == this || this.getPassengers().contains(entity)) {
            return true;
        }
        LivingEntity master = this.getMasterEntity();
        if (entity == master || master != null && master.isAlliedTo(entity)) {
            return true;
        }
        LivingEntity source = this.getSourceEntity();
        if (source != null && (entity == source || source.isAlliedTo(entity))) {
            return true;
        }
        if (source instanceof GillesDeRaisEntity gilles) {
            ServerPlayer sourceMaster = gilles.getEntityMaster();
            if (entity == sourceMaster || sourceMaster != null && sourceMaster.isAlliedTo(entity)) {
                return true;
            }
        }
        if (entity instanceof SeaMonsterEntity seaMonster) {
            UUID controller = seaMonster.getControllerUuid();
            return this.getUUID().equals(controller) || (this.sourceUuid != null && this.sourceUuid.equals(controller));
        }
        return entity instanceof HugeSeaMonsterEntity other
                && this.sourceUuid != null
                && this.sourceUuid.equals(other.sourceUuid);
    }

    private void followSource() {
        LivingEntity source = this.getSourceEntity();
        if (source == null || !source.isAlive()) {
            return;
        }
        double distance = this.distanceTo(source);
        if (distance > 12.0) {
            this.getNavigation().moveTo(source, 0.9);
        } else {
            this.getNavigation().stop();
        }
        this.getLookControl().setLookAt(source, 25.0F, 25.0F);
    }

    @Nullable
    private LivingEntity livingAttacker(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return source.getDirectEntity() instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private LivingEntity getMasterEntity() {
        if (this.masterUuid == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(this.masterUuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Nullable
    private LivingEntity getSourceEntity() {
        if (this.sourceUuid == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        long now = level.getGameTime();
        if (this.cachedSourceEntityGameTime == now) {
            return this.cachedSourceEntity != null && this.cachedSourceEntity.isAlive() ? this.cachedSourceEntity : null;
        }
        this.cachedSourceEntityGameTime = now;
        Entity entity = level.getEntity(this.sourceUuid);
        this.cachedSourceEntity = entity instanceof LivingEntity living && living.isAlive() ? living : null;
        return this.cachedSourceEntity;
    }

    private void beginDissolve(@Nullable DamageSource cause) {
        if (this.isDissolving()) {
            return;
        }
        if (!this.level().isClientSide() && !this.deathEffectsApplied) {
            this.breakSourceSpellbook();
            com.example.typemoonaddon.servant.GillesDeRaisCombatHelper.addPollutionZone(
                    this.level().dimension(), this.position(), 9.0, 60 * 20, 14.0F, this.sourceUuid, this.masterUuid);
            this.deathEffectsApplied = true;
        }
        this.entityData.set(DISSOLVING, true);
        this.dissolveTicks = 0;
        this.setHealth(Math.max(1.0F, this.getHealth()));
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        this.setNoGravity(true);
        this.hurtTime = 0;
        this.hurtDuration = 0;
    }

    private void maintainOwnerPassenger(ServerLevel level) {
        LivingEntity source = this.getSourceEntity();
        if (source == null || !source.isAlive()) {
            if (!this.getPassengers().isEmpty()) {
                this.ejectPassengers();
            }
            return;
        }
        if (this.getPassengers().size() > 1) {
            for (Entity passenger : java.util.List.copyOf(this.getPassengers()).subList(1, this.getPassengers().size())) {
                passenger.stopRiding();
            }
        }
        if (!this.getPassengers().isEmpty() && this.getPassengers().get(0) != source) {
            for (Entity passenger : java.util.List.copyOf(this.getPassengers())) {
                if (passenger != source) {
                    passenger.stopRiding();
                }
            }
        }
        if (!this.hasPassenger(source) && source.getVehicle() != this && this.isAuthorizedPassenger(source)) {
            source.startRiding(this, true);
        }
    }

    private boolean isClimbing() {
        return this.horizontalCollision && !this.isInWater();
    }

    private boolean isAuthorizedPassenger(Entity passenger) {
        if (this.sourceUuid == null || passenger == null) {
            return false;
        }
        if (passenger instanceof GillesDeRaisEntity gilles) {
            return this.sourceUuid.equals(gilles.getUUID());
        }
        return passenger instanceof ServerPlayer player && this.sourceUuid.equals(player.getUUID());
    }

    private void tickDissolve() {
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        this.setNoGravity(true);
        this.hurtTime = 0;
        this.hurtDuration = 0;
        if (!this.level().isClientSide()) {
            this.dissolveTicks++;
            if (this.dissolveTicks >= DISSOLVE_DURATION) {
                this.ejectPassengers();
                this.discard();
                ServantCardGillesDeRaisSkills.onHugeSeaMonsterDeath(this);
            }
        }
    }

    private void breakTerrainAhead(ServerLevel level, double forwardDistance, float halfWidth, int height, int maxBlocks) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        int broken = 0;
        int checks = 0;
        int maxChecks = maxBlocks * 6;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int startY = Mth.floor(this.getY());
        int stepHeight = Math.min(height, Mth.ceil(this.getBbHeight() * 0.45F));
        for (int y = 0; y < stepHeight && broken < maxBlocks && checks < maxChecks; y += 2) {
            for (double forwardOffset = 0.0; forwardOffset <= forwardDistance && broken < maxBlocks && checks < maxChecks; forwardOffset += 2.0) {
                for (double sideOffset = -halfWidth; sideOffset <= halfWidth && broken < maxBlocks && checks < maxChecks; sideOffset += 2.0) {
                    checks++;
                    Vec3 position = this.position()
                            .add(forward.scale(forwardOffset))
                            .add(right.scale(sideOffset));
                    mutable.set(Mth.floor(position.x), startY + y, Mth.floor(position.z));
                    if (this.tryBreakBlock(level, mutable)) {
                        broken++;
                    }
                }
            }
        }
    }

    private void breakTerrainCross(ServerLevel level, Vec3 center, Vec3 forward, Vec3 right) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        int broken = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int baseY = Mth.floor(center.y - 2.0);
        for (int ring = 0; ring < 5 && broken < 520; ring++) {
            double distance = 4.0 + ring * 3.0;
            for (double offset = -distance; offset <= distance && broken < 520; offset += 1.0) {
                broken += destroyCrossColumn(level, mutable, center.add(forward.scale(offset)).add(right.scale(distance)), baseY, 5);
                broken += destroyCrossColumn(level, mutable, center.add(forward.scale(offset)).add(right.scale(-distance)), baseY, 5);
                broken += destroyCrossColumn(level, mutable, center.add(forward.scale(distance)).add(right.scale(offset)), baseY, 5);
                broken += destroyCrossColumn(level, mutable, center.add(forward.scale(-distance)).add(right.scale(offset)), baseY, 5);
            }
        }
    }

    private void breakCraterTerrain(ServerLevel level, Vec3 center, double radius, int depth, int maxBlocks) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        int broken = 0;
        int checks = 0;
        int maxChecks = maxBlocks * 8;
        int horizontal = Mth.ceil(radius);
        double radiusSqr = radius * radius;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int baseY = Mth.floor(center.y + 2.0);
        for (int y = 0; y >= -depth && broken < maxBlocks && checks < maxChecks; y--) {
            double layerScale = 1.0 - Math.abs(y) / (double)(depth + 3);
            double layerRadiusSqr = radiusSqr * Math.max(0.22, layerScale);
            for (int x = -horizontal; x <= horizontal && broken < maxBlocks && checks < maxChecks; x++) {
                for (int z = -horizontal; z <= horizontal && broken < maxBlocks && checks < maxChecks; z++) {
                    double distanceSqr = x * x + z * z;
                    if (distanceSqr > layerRadiusSqr || (x + z + y + this.getId()) % 2 != 0) {
                        continue;
                    }
                    checks++;
                    mutable.set(Mth.floor(center.x) + x, baseY + y, Mth.floor(center.z) + z);
                    if (this.tryBreakBlock(level, mutable)) {
                        broken++;
                    }
                }
            }
        }
    }

    private int destroyCrossColumn(ServerLevel level, BlockPos.MutableBlockPos mutable, Vec3 pos, int baseY, int height) {
        int broken = 0;
        for (int y = 0; y < height && broken < 3; y++) {
            mutable.set(Mth.floor(pos.x), baseY + y, Mth.floor(pos.z));
            if (this.tryBreakBlock(level, mutable)) {
                broken++;
            }
        }
        return broken;
    }

    private boolean isStaggeredTick(int interval, int offset) {
        return Math.floorMod(this.tickCount + this.getId(), interval) == offset;
    }

    private boolean tryBreakBlock(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > 80.0F) {
            return false;
        }
        return level.destroyBlock(pos, false, this);
    }

    private void breakSourceSpellbook() {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof GillesDeRaisEntity gilles) {
                gilles.loseSpellbook();
            }
        }
        if (this.sourceUuid != null && this.level() instanceof ServerLevel level) {
            Entity source = level.getEntity(this.sourceUuid);
            if (source instanceof GillesDeRaisEntity gilles) {
                gilles.loseSpellbook();
            }
        }
    }
}

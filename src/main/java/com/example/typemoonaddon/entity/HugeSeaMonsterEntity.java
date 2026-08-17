package com.example.typemoonaddon.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HugeSeaMonsterEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> AWAKENED =
            SynchedEntityData.defineId(HugeSeaMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID sourceUuid;

    public HugeSeaMonsterEntity(EntityType<HugeSeaMonsterEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0)
                .add(Attributes.ATTACK_DAMAGE, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.08)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKENED, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.85, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.55));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount % 20 == 0) {
            this.pollutionAura(level);
        }
        if (this.tickCount % 100 == 0) {
            this.heal(50.0F);
        }
        if (this.tickCount % 20 == 7 && !isValidTarget(this.getTarget())) {
            this.setTarget(this.findNearestTarget(48.0));
        }
        if (this.tickCount % 60 == 0) {
            this.areaSweep(level);
        }
        if (this.tickCount % 80 == 20) {
            this.tentacleGrab(level);
        }
        if (this.tickCount % 100 == 40) {
            this.trySummonBrood(level);
        }
        GillesPollutionZoneService.tick(level);
    }

    public void setSource(@Nullable LivingEntity source) {
        this.sourceUuid = source == null ? null : source.getUUID();
    }

    @Nullable
    public UUID getSourceUuid() {
        return this.sourceUuid;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty()
                && passenger instanceof GillesDeRaisEntity gilles
                && this.sourceUuid != null
                && this.sourceUuid.equals(gilles.getUUID());
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        callback.accept(passenger, this.getX(), this.getY() + this.getBbHeight() * 0.42, this.getZ());
        passenger.setYRot(this.getYRot());
        if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(this.getYRot());
            living.setYHeadRot(this.getYRot());
            living.setXRot(0.0F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.swing(InteractionHand.MAIN_HAND);
            this.triggerAnim("action_controller", "slam");
        }
        return hit;
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource cause) {
        if (!this.level().isClientSide()) {
            this.breakSourceSpellbook();
            GillesPollutionZoneService.add(this.level().dimension(), this.position(), 9.0, 60 * 20, 14.0F);
        }
        super.die(cause);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.sourceUuid != null) {
            tag.putUUID("GillesHugeSeaMonsterSource", this.sourceUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("GillesHugeSeaMonsterSource")) {
            this.sourceUuid = tag.getUUID("GillesHugeSeaMonsterSource");
        }
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

    private void pollutionAura(ServerLevel level) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(10.0), this::isValidTarget)) {
            living.hurt(this.damageSources().magic(), 8.0F);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
        }
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 2.0, this.getZ(), 35, 2.4, 1.2, 2.4, 0.04);
    }

    private void areaSweep(ServerLevel level) {
        this.triggerAnim("action_controller", "slam");
        level.playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6F, 0.55F);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(9.0), this::isValidTarget)) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), 34.0F);
            living.push((living.getX() - this.getX()) * 0.28, 0.35, (living.getZ() - this.getZ()) * 0.28);
            living.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 8, 3.0, 0.4, 3.0, 0.02);
    }

    private void tentacleGrab(ServerLevel level) {
        this.triggerAnim("action_controller", "grab");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(13.0), this::isValidTarget)) {
            if (this.random.nextFloat() > 0.35F) {
                continue;
            }
            Vec3 pull = this.position().subtract(living.position()).normalize().scale(0.45);
            living.setDeltaMovement(living.getDeltaMovement().add(pull.x, 0.12, pull.z));
            living.hurtMarked = true;
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, true, true));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, true, true));
            living.hurt(this.damageSources().magic(), 12.0F);
            level.sendParticles(ParticleTypes.SCULK_SOUL, living.getX(), living.getY() + 0.3, living.getZ(), 18, 0.4, 0.6, 0.4, 0.04);
        }
    }

    private void trySummonBrood(ServerLevel level) {
        int small = countBrood(level, false);
        int large = countBrood(level, true);
        if (small < 100) {
            spawnBrood(level, false);
        }
        if (large < 10 && this.random.nextFloat() < 0.35F) {
            spawnBrood(level, true);
        }
    }

    private int countBrood(ServerLevel level, boolean large) {
        AABB box = this.getBoundingBox().inflate(96.0);
        return level.getEntitiesOfClass(SeaMonsterEntity.class, box, seaMonster ->
                seaMonster.isAlive() && seaMonster.isLarge() == large && this.getUUID().equals(seaMonster.getControllerUuid())).size();
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
        level.sendParticles(ParticleTypes.SQUID_INK, seaMonster.getX(), seaMonster.getY() + 0.6, seaMonster.getZ(), large ? 24 : 12, 0.5, 0.5, 0.5, 0.05);
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
                && !this.getPassengers().contains(target) && !EntityUtils.isImmunePlayerTarget(target);
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

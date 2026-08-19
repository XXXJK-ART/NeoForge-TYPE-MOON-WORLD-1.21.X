package com.example.typemoonaddon.entity;

import java.util.UUID;
import com.example.typemoonaddon.servant.GillesDeRaisCombatHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SeaMonsterEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> LARGE =
            SynchedEntityData.defineId(SeaMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DISSOLVING =
            SynchedEntityData.defineId(SeaMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int SMALL_LIFETIME = 20 * 60 * 5;
    private static final int LARGE_LIFETIME = 20 * 60 * 3;
    private static final int DISSOLVE_DURATION = 72;
    private static final int AURA_TICK_INTERVAL = 60;
    private static final int SMALL_TARGET_REFRESH_INTERVAL = 120;
    private static final int LARGE_TARGET_REFRESH_INTERVAL = 90;
    private static final int LARGE_SWEEP_INTERVAL = 180;
    private static final double SMALL_MOVEMENT_SPEED = 0.22;
    private static final double LARGE_MOVEMENT_SPEED = 0.15;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID controllerUuid;
    @Nullable
    private LivingEntity cachedController;
    private long cachedControllerGameTime = Long.MIN_VALUE;
    private int dissolveTicks;
    private boolean deathEffectsApplied;

    public SeaMonsterEntity(EntityType<? extends SeaMonsterEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, SMALL_MOVEMENT_SPEED)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LARGE, false);
        builder.define(DISSOLVING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
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
        if (!level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 96.0)) {
            if (!this.isValidTarget(this.getTarget())) {
                this.followController(this.getController());
            }
            return;
        }
        if (this.tickCount > (this.isLarge() ? LARGE_LIFETIME : SMALL_LIFETIME)) {
            this.beginDissolve(null);
            return;
        }
        if (this.controllerUuid != null && this.getController() == null) {
            this.beginDissolve(null);
            return;
        }
        LivingEntity controller = this.getController();
        if (controller instanceof GillesDeRaisEntity gilles && !gilles.hasUsableSpellbook()) {
            this.beginDissolve(null);
            return;
        }
        LivingEntity target = this.getTarget();
        boolean inCombat = this.isValidTarget(target);
        if (inCombat && this.isStaggeredTick(AURA_TICK_INTERVAL, 0)) {
            this.heal(this.isLarge() ? 15.0F : 5.0F);
            this.pollutionAura(level);
        }
        int targetRefreshInterval = this.isLarge() ? LARGE_TARGET_REFRESH_INTERVAL : SMALL_TARGET_REFRESH_INTERVAL;
        if (this.isStaggeredTick(targetRefreshInterval, 5)) {
            this.refreshTarget();
        }
        if (!this.isValidTarget(this.getTarget())) {
            this.followController(controller);
        }
        if (inCombat && this.isLarge() && this.isStaggeredTick(LARGE_SWEEP_INTERVAL, 0)) {
            this.largeSweep(level);
        }
    }

    public boolean isLarge() {
        return this.entityData.get(LARGE);
    }

    public void setLarge(boolean large) {
        this.entityData.set(LARGE, large);
        this.applyVariantAttributes();
        this.refreshDimensions();
        if (large && this.getHealth() < 1000.0F) {
            this.setHealth(this.getMaxHealth());
        }
    }

    public void setController(LivingEntity controller) {
        this.controllerUuid = controller.getUUID();
        this.cachedController = controller;
        this.cachedControllerGameTime = this.level().getGameTime();
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
    public UUID getControllerUuid() {
        return this.controllerUuid;
    }

    @Nullable
    public LivingEntity getController() {
        if (this.controllerUuid == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        long now = level.getGameTime();
        if (this.cachedControllerGameTime == now) {
            return this.cachedController != null && this.cachedController.isAlive() ? this.cachedController : null;
        }
        this.cachedControllerGameTime = now;
        Entity entity = level.getEntity(this.controllerUuid);
        this.cachedController = entity instanceof LivingEntity living && living.isAlive() ? living : null;
        return this.cachedController;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (super.isAlliedTo(other)) {
            return true;
        }
        if (this.isFriendly(other)) {
            return true;
        }
        LivingEntity controller = this.getController();
        if (controller == null) {
            return false;
        }
        if (other == controller || controller.isAlliedTo(other)) {
            return true;
        }
        return other instanceof SeaMonsterEntity seaMonster
                && this.controllerUuid != null
                && this.controllerUuid.equals(seaMonster.controllerUuid);
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
    public boolean doHurtTarget(Entity target) {
        if (this.isFriendly(target)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.swing(InteractionHand.MAIN_HAND);
            this.triggerAnim("action_controller", this.isLarge() ? "sweep" : "attack");
        }
        return hit;
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
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isLarge() ? EntityDimensions.fixed(2.0F, 3.7F) : EntityDimensions.fixed(1.0F, 1.85F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("GillesSeaMonsterLarge", this.isLarge());
        tag.putBoolean("GillesSeaMonsterDissolving", this.isDissolving());
        tag.putInt("GillesSeaMonsterDissolveTicks", this.dissolveTicks);
        tag.putBoolean("GillesSeaMonsterDeathEffectsApplied", this.deathEffectsApplied);
        if (this.controllerUuid != null) {
            tag.putUUID("GillesSeaMonsterController", this.controllerUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(LARGE, tag.getBoolean("GillesSeaMonsterLarge"));
        this.entityData.set(DISSOLVING, tag.getBoolean("GillesSeaMonsterDissolving"));
        this.dissolveTicks = tag.getInt("GillesSeaMonsterDissolveTicks");
        this.deathEffectsApplied = tag.getBoolean("GillesSeaMonsterDeathEffectsApplied");
        if (tag.hasUUID("GillesSeaMonsterController")) {
            this.controllerUuid = tag.getUUID("GillesSeaMonsterController");
        }
        this.applyVariantAttributes();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            String loop = event.isMoving() ? "animation.gilles_sea_monster.walk" : "animation.gilles_sea_monster.idle";
            return event.setAndContinue(RawAnimation.begin().thenLoop(loop));
        }));
        AnimationController<SeaMonsterEntity> action = new AnimationController<>(this, "action_controller", 0, event -> PlayState.STOP);
        action.triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.gilles_sea_monster.attack"));
        action.triggerableAnim("sweep", RawAnimation.begin().thenPlay("animation.gilles_sea_monster.sweep"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private void applyVariantAttributes() {
        this.setAttribute(Attributes.MAX_HEALTH, this.isLarge() ? 1000.0 : 200.0);
        this.setAttribute(Attributes.ATTACK_DAMAGE, this.isLarge() ? 30.0 : 12.0);
        this.setAttribute(Attributes.MOVEMENT_SPEED, this.isLarge() ? LARGE_MOVEMENT_SPEED : SMALL_MOVEMENT_SPEED);
        this.setAttribute(Attributes.KNOCKBACK_RESISTANCE, this.isLarge() ? 0.9 : 0.55);
    }

    private void setAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private void refreshTarget() {
        LivingEntity controller = this.getController();
        LivingEntity preferred = controller instanceof net.minecraft.world.entity.Mob mob ? mob.getTarget() : null;
        if (!isValidTarget(preferred) && controller != null) {
            preferred = controller.getLastHurtByMob();
        }
        if (isValidTarget(preferred)) {
            this.setTarget(preferred);
            return;
        }
        if (!isValidTarget(this.getTarget())) {
            LivingEntity nearest = this.findNearestTarget(24.0);
            this.setTarget(nearest);
            if (nearest == null) {
                this.getNavigation().stop();
            }
        }
    }

    private void followController(@Nullable LivingEntity controller) {
        if (controller == null || !controller.isAlive()) {
            return;
        }
        double distance = this.distanceTo(controller);
        if (distance > (this.isLarge() ? 7.0 : 4.5)) {
            this.getNavigation().moveTo(controller, this.isLarge() ? 0.95 : 1.05);
        } else {
            this.getNavigation().stop();
        }
        this.getLookControl().setLookAt(controller, 30.0F, 30.0F);
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
        return target != null && target != this && target.isAlive() && !target.isAlliedTo(this) && !this.isFriendly(target)
                && !EntityUtils.isImmunePlayerTarget(target);
    }

    private boolean isFriendly(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        LivingEntity controller = this.getController();
        if (entity == controller || entity == this) {
            return true;
        }
        if (controller instanceof HugeSeaMonsterEntity hugeSeaMonster && hugeSeaMonster.isFriendlyTo(entity)) {
            return true;
        }
        if (controller instanceof GillesDeRaisEntity gilles) {
            ServerPlayer master = gilles.getEntityMaster();
            if (entity == master || master != null && master.isAlliedTo(entity)) {
                return true;
            }
        }
        if (controller != null && controller.isAlliedTo(entity)) {
            return true;
        }
        if (entity instanceof HugeSeaMonsterEntity hugeSeaMonster && hugeSeaMonster.isAlliedTo(this)) {
            return true;
        }
        return entity instanceof SeaMonsterEntity seaMonster
                && this.controllerUuid != null
                && this.controllerUuid.equals(seaMonster.controllerUuid);
    }

    @Nullable
    private LivingEntity livingAttacker(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return source.getDirectEntity() instanceof LivingEntity living ? living : null;
    }

    private void beginDissolve(@Nullable DamageSource cause) {
        if (this.isDissolving()) {
            return;
        }
        if (!this.level().isClientSide() && this.isLarge() && !this.deathEffectsApplied) {
            GillesDeRaisCombatHelper.addPollutionZone(
                    this.level().dimension(), this.position(), 5.5, 60 * 20, 10.0F,
                    this.getPollutionSourceUuid(), this.getPollutionMasterUuid());
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
                this.discard();
            }
        }
    }

    private void pollutionAura(ServerLevel level) {
        if (!level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 72.0)) {
            return;
        }
        double radius = this.isLarge() ? 6.0 : 3.5;
        float damage = this.isLarge() ? 10.0F : 2.0F;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius), this::isValidTarget)) {
            living.hurt(this.damageSources().magic(), damage);
        }
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                this.isLarge() ? 18 : 7, radius * 0.25, 0.4, radius * 0.25, 0.03);
    }

    private void largeSweep(ServerLevel level) {
        if (!level.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 72.0)) {
            return;
        }
        this.triggerAnim("action_controller", "sweep");
        level.playSound(null, this.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.1F, 0.55F);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.5), this::isValidTarget)) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), 18.0F);
            living.push((living.getX() - this.getX()) * 0.18, 0.25, (living.getZ() - this.getZ()) * 0.18);
            living.hurtMarked = true;
        }
    }

    @Nullable
    private UUID getPollutionSourceUuid() {
        LivingEntity controller = this.getController();
        if (controller instanceof HugeSeaMonsterEntity hugeSeaMonster && hugeSeaMonster.getSourceUuid() != null) {
            return hugeSeaMonster.getSourceUuid();
        }
        return this.controllerUuid;
    }

    @Nullable
    private UUID getPollutionMasterUuid() {
        LivingEntity controller = this.getController();
        if (controller instanceof HugeSeaMonsterEntity hugeSeaMonster) {
            return hugeSeaMonster.getMasterUuid();
        }
        if (controller instanceof GillesDeRaisEntity gilles && gilles.getEntityMaster() != null) {
            return gilles.getEntityMaster().getUUID();
        }
        return null;
    }

    private boolean isStaggeredTick(int interval, int offset) {
        return Math.floorMod(this.tickCount + this.getId(), interval) == offset;
    }
}

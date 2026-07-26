package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.RatSwarmRules;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class RatSwarmEntity extends OwnedPaleRiderMob implements GeoEntity {
   private static final EntityDataAccessor<Integer> RAT_COUNT = SynchedEntityData.defineId(RatSwarmEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DOMAIN_SPAWNED = SynchedEntityData.defineId(RatSwarmEntity.class, EntityDataSerializers.BOOLEAN);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private long lastBiteTick;
   private long nextTargetScanTick;
   private long nextNavigationTick;

   public RatSwarmEntity(EntityType<? extends RatSwarmEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, RatSwarmRules.MAX_HEALTH)
         .add(Attributes.MOVEMENT_SPEED, 0.32)
         .add(Attributes.ATTACK_DAMAGE, 1.0)
         .add(Attributes.ARMOR, 0.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.35);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(RAT_COUNT, RatSwarmRules.MAX_RATS);
      builder.define(DOMAIN_SPAWNED, false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
   }

   @Override
   protected void customServerAiStep() {
      LivingEntity owner = this.getPaleRiderLivingOwner();
      if (owner == null || !owner.isAlive()) {
         this.discard();
         return;
      }
      if (PaleRiderInfectionService.isStationaryAnchor(this)) {
         PaleRiderInfectionService.holdStationaryAnchor(this);
         return;
      }
      super.customServerAiStep();
      if (owner instanceof net.minecraft.server.level.ServerPlayer player
         && PaleRiderInfectionService.isPaleRiderCardPlayer(player)) {
         int command = player.getPersistentData().getInt("PaleRiderCardCommand");
         if (command != 2) {
            this.setTarget(null);
            if (command == 1) this.getNavigation().stop();
            else if (command == 3 && this.canRefreshNavigation(20)) this.getNavigation().moveTo(player, 1.05);
            return;
         }
      }
      this.syncCasualties();
      if (this.getTarget() == null || !this.getTarget().isAlive() || this.getTarget().isAlliedTo(owner) || owner.isAlliedTo(this.getTarget())
         || net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(this.getTarget())) {
         this.setTarget(findEnemy(owner, 48.0));
      }
      if (this.tickCount % 10 == Math.floorMod(this.getId(), 10)) {
         this.tryMergeNearby();
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      boolean hurt = super.hurt(source, amount);
      if (hurt && this.isAlive() && !this.level().isClientSide()) {
         this.syncCasualties();
      }
      return hurt;
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (!(target instanceof LivingEntity living) || this.level().isClientSide()) {
         return false;
      }
      long now = this.level().getGameTime();
      if (now - this.lastBiteTick < 40L) {
         return false;
      }
      LivingEntity owner = this.getPaleRiderLivingOwner();
      if (owner == null || living.isAlliedTo(owner) || owner.isAlliedTo(living)) {
         return false;
      }
      this.lastBiteTick = now;
      living.hurt(owner.damageSources().source(net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.RAT_BITE, this, owner), this.getRatCount() * 0.5F);
      PaleRiderInfectionService.infect(living, owner, 1);
      return true;
   }

   private LivingEntity findEnemy(LivingEntity owner, double radius) {
      if (owner instanceof PaleRiderEntity rider) return rider.findPaleRiderEnemy(radius);
      long now = this.level().getGameTime();
      if (now < this.nextTargetScanTick) {
         return null;
      }
      this.nextTargetScanTick = now + 10L + Math.floorMod(this.getId(), 5);
      return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
         target -> target != this && target != owner && target.isAlive()
            && !PaleRiderInfectionService.arePaleRiderAllies(owner, target)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(target))
         .stream().min((left, right) -> Double.compare(left.distanceToSqr(this), right.distanceToSqr(this))).orElse(null);
   }

   private void syncCasualties() {
      int rats = RatSwarmRules.ratsForHealth(this.getHealth());
      if (rats <= 0) {
         return;
      }
      this.entityData.set(RAT_COUNT, rats);
      double maximum = rats * RatSwarmRules.HEALTH_PER_RAT;
      if (this.getAttribute(Attributes.MAX_HEALTH) != null && Math.abs(this.getAttributeBaseValue(Attributes.MAX_HEALTH) - maximum) > 0.01) {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maximum);
      }
      if (this.getHealth() > maximum) {
         this.setHealth((float)maximum);
      }
   }

   private void tryMergeNearby() {
      if (PaleRiderInfectionService.isStationaryAnchor(this)
         || !(this.level() instanceof ServerLevel level) || this.getHealth() >= RatSwarmRules.MAX_HEALTH) {
         return;
      }
      List<RatSwarmEntity> nearby = level.getEntitiesOfClass(RatSwarmEntity.class, this.getBoundingBox().inflate(0.8),
         other -> other != this && other.isAlive() && !PaleRiderInfectionService.isStationaryAnchor(other)
            && this.getPaleRiderOwnerUuid() != null && this.getPaleRiderOwnerUuid().equals(other.getPaleRiderOwnerUuid()));
      if (nearby.isEmpty()) {
         return;
      }
      RatSwarmEntity other = nearby.getFirst();
      RatSwarmRules.MergeResult result = RatSwarmRules.merge(this.getHealth(), other.getHealth());
      this.setSwarmHealth(result.primaryHealth());
      if (result.remainderHealth() <= 0.0F) {
         other.discard();
      } else {
         other.setSwarmHealth(result.remainderHealth());
      }
   }

   private boolean canRefreshNavigation(int interval) {
      long now = this.level().getGameTime();
      if (now < this.nextNavigationTick) return false;
      this.nextNavigationTick = now + interval + Math.floorMod(this.getId(), 5);
      return true;
   }

   public void setSwarmHealth(float health) {
      int rats = RatSwarmRules.ratsForHealth(health);
      if (rats <= 0) {
         this.discard();
         return;
      }
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(rats * RatSwarmRules.HEALTH_PER_RAT);
      this.setHealth(Math.min(health, this.getMaxHealth()));
      this.entityData.set(RAT_COUNT, rats);
   }

   public int getRatCount() {
      return this.entityData.get(RAT_COUNT);
   }

   public void setDomainSpawned(boolean value) {
      this.entityData.set(DOMAIN_SPAWNED, value);
   }

   public boolean isDomainSpawned() {
      return this.entityData.get(DOMAIN_SPAWNED);
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "swarm", 0,
         state -> state.setAndContinue(RawAnimation.begin().thenLoop(Integer.toString(this.getRatCount())))));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("RatCount", this.getRatCount());
      tag.putBoolean("DomainSpawned", this.isDomainSpawned());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(RAT_COUNT, Math.max(1, Math.min(20, tag.getInt("RatCount"))));
      this.entityData.set(DOMAIN_SPAWNED, tag.getBoolean("DomainSpawned"));
   }
}

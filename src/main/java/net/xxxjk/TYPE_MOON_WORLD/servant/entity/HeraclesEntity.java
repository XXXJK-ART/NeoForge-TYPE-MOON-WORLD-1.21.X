package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantTrueSweepService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.heracles.HeraclesAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;

public class HeraclesEntity extends ServantEntity {
   public static final String SERVANT_KEY = "heracles";
   private static final EntityDataAccessor<Boolean> FRENZY_ANIMATION = SynchedEntityData.defineId(
      HeraclesEntity.class, EntityDataSerializers.BOOLEAN
   );
   private int frenzySlashVariant;

   public HeraclesEntity(EntityType<HeraclesEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FRENZY_ANIMATION, false);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         LivingEntity target = this.getTarget();
         boolean attacking = HeraclesAbilityService.busy(this)
            || HeraclesAbilityService.enemy(this, target) && this.distanceToSqr(target) <= 49.0 && this.hasLineOfSight(target);
         this.entityData.set(FRENZY_ANIMATION, attacking && this.isAlive() && !this.isSpiritualTransitionLocked()
            && !HeraclesAbilityService.controlled(this) && !ServantCombatSystem.cannotAct(this));
      }
   }

   public boolean isFrenzyAnimationActive() {
      return this.entityData.get(FRENZY_ANIMATION);
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      // Synced display state keeps remote clients attacking between individual
      // strikes. This loop never deals damage or changes the combat cadence.
      return this.isFrenzyAnimationActive()
         ? animations.actionAnimation(moving ? "frenzy_walk" : "frenzy").orElse(null) : null;
   }

   @Override
   public void triggerBasicAttackAnimation() {
      if (!this.isAlive() || this.isSpiritualTransitionLocked() || HeraclesAbilityService.busy(this)
         || HeraclesAbilityService.controlled(this) || ServantCombatSystem.cannotAct(this)) return;
      this.triggerNamedActionAnimation("frenzy_slash_" + this.frenzySlashVariant++ % 3);
      ServantVoiceHelper.tryPlayAttack(this);
      if (this.level() instanceof ServerLevel level) {
         level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.85F);
      }
   }

   @Override
   public void knockback(double strength, double x, double z) {
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (target instanceof LivingEntity living && ServantMasterProtection.isProtectedMaster(this, living)) {
         return false;
      }
      return super.doHurtTarget(target);
   }

   @Override
   public boolean doBasicHurtTarget(LivingEntity target) {
      if (target == null || ServantMasterProtection.isProtectedMaster(this, target)) return false;
      if (ServantTrueSweepService.triggerNpcAttack(this, target, this.level().getGameTime())) {
         return true;
      }
      return super.doHurtTarget(target);
   }
}

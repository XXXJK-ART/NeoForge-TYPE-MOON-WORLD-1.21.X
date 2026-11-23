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
   private static final EntityDataAccessor<Long> GOD_HAND_REACTION = SynchedEntityData.defineId(
      HeraclesEntity.class, EntityDataSerializers.LONG
   );
   private int frenzySlashVariant;

   public HeraclesEntity(EntityType<HeraclesEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FRENZY_ANIMATION, false);
      builder.define(GOD_HAND_REACTION, 0L);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         LivingEntity target = this.getTarget();
         boolean advancing = HeraclesAbilityService.enemy(this, target) && this.distanceToSqr(target) > 6.25
            && this.distanceToSqr(target) <= 400.0 && this.hasLineOfSight(target);
         this.entityData.set(FRENZY_ANIMATION, advancing && this.isAlive() && !this.isSpiritualTransitionLocked()
            && !HeraclesAbilityService.controlled(this) && !ServantCombatSystem.cannotAct(this));
      }
   }

   public boolean isFrenzyAnimationActive() {
      return this.entityData.get(FRENZY_ANIMATION);
   }

   public void triggerGodHandReaction(boolean adaptation) {
      if (this.level().isClientSide()) return;
      int duration = adaptation ? 5 : 7;
      this.entityData.set(GOD_HAND_REACTION, (this.level().getGameTime()+duration)*2+(adaptation ? 1 : 0));
      if (!HeraclesAbilityService.busy(this)) this.triggerNamedActionAnimation(adaptation ? "adapt" : "revive");
   }

   /** Small additive motion also survives a revival during a long airborne attack. */
   public float godHandReactionStrength(float partialTick) {
      long reaction = this.entityData.get(GOD_HAND_REACTION);
      if (reaction == 0 || this.isSpiritualTransitionLocked()) return 0;
      int duration = (reaction & 1) == 1 ? 5 : 7;
      float remaining = (float)(reaction/2-this.level().getGameTime())-partialTick;
      if (remaining <= 0 || remaining > duration) return 0;
      float elapsed = duration-remaining;
      return Math.min(1, elapsed/2F)*Math.min(1,remaining/3F);
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      // Locomotion communicates pursuit; action animations own the actual strikes.
      return this.isFrenzyAnimationActive() && moving && this.onGround()
         ? animations.actionAnimation("frenzy_walk").orElse(null) : null;
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

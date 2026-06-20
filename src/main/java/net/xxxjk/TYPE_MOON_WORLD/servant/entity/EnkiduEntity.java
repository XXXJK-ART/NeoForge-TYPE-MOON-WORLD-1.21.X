package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EnkiduEntity extends ServantEntity {
   public static final String SERVANT_KEY = "enkidu";

   public EnkiduEntity(EntityType<EnkiduEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.72));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (!this.level().isClientSide() && this.isAlive() && !this.isSpiritualDissolving()) {
         EnkiduCombatHelper.tick(this);
      }
   }

   @Override
   protected boolean useFloatingAnimation() {
      return EnkiduCombatHelper.isFlying(this);
   }

   @Override
   protected String getLoopAnimationOverride(net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations animations, boolean moving) {
      if (EnkiduCombatHelper.isFlying(this)) {
         return animations.actionAnimation("float_idle").orElse(animations.actionAnimation("fly").orElse(null));
      }
      return null;
   }

   @Override
   public void die(DamageSource cause) {
      if (!this.level().isClientSide()) {
         EnkiduCombatHelper.cleanup(this);
      }
      super.die(cause);
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide() && reason != RemovalReason.CHANGED_DIMENSION) {
         EnkiduCombatHelper.cleanup(this);
      }
      super.remove(reason);
   }
}

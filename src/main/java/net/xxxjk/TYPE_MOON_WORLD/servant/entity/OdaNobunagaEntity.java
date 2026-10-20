package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaNobunagaActionAnimations.Action;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaNobunagaActionAnimations;

public class OdaNobunagaEntity extends ServantEntity {
   public static final String SERVANT_KEY = "oda_nobunaga";
   private static final EntityDataAccessor<String> ACTION_ANIMATION = SynchedEntityData.defineId(
      OdaNobunagaEntity.class, EntityDataSerializers.STRING
   );
   private static final EntityDataAccessor<Integer> ACTION_ANIMATION_START = SynchedEntityData.defineId(
      OdaNobunagaEntity.class, EntityDataSerializers.INT
   );

   public OdaNobunagaEntity(EntityType<OdaNobunagaEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ACTION_ANIMATION, "");
      builder.define(ACTION_ANIMATION_START, -1000);
   }

   @Override
   public void triggerNamedActionAnimation(String key) {
      super.triggerNamedActionAnimation(key);
      if (this.level().isClientSide()) return;
      String action = normalizeAction(key);
      if (action.isEmpty()) return;
      this.entityData.set(ACTION_ANIMATION, action);
      this.entityData.set(ACTION_ANIMATION_START, this.tickCount);
   }

   public String getActionAnimation() {
      return this.entityData.get(ACTION_ANIMATION);
   }

   public float getActionAnimationAge(float partialTick) {
      return this.tickCount + partialTick - this.entityData.get(ACTION_ANIMATION_START);
   }

   private static String normalizeAction(String key) {
      if (key == null) return "";
      for (Action action : Action.values()) {
         if (action.animationId().equals(key)) return key;
      }
      return switch (key) {
         case "bow_shot" -> Action.ATTACK_SHOT.animationId();
         case "hasebe_thrust", "hasebe_breakthrough" -> Action.ATTACK_SLASH.animationId();
         case "summon_guns" -> Action.SKILL_COMMAND.animationId();
         case "maou" -> Action.SKILL_MAOU.animationId();
         case "three_thousand" -> Action.NP_THREE_THOUSAND_CHANT.animationId();
         case "three_thousand_release" -> Action.NP_THREE_THOUSAND_RELEASE.animationId();
         case "hajun_chant" -> Action.NP_HAJUN_CHANT.animationId();
         case "hajun_release" -> Action.NP_HAJUN_RELEASE.animationId();
         default -> "";
      };
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.HOT_FLOOR)) {
         this.clearFire();
         return false;
      }
      return super.hurt(source, amount);
   }

   @Override
   public void die(DamageSource cause) {
      if (!this.level().isClientSide()) {
         OdaNobunagaCombatHelper.cleanupHajun(this);
      }
      super.die(cause);
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide() && reason != RemovalReason.CHANGED_DIMENSION) {
         OdaNobunagaCombatHelper.cleanupHajun(this);
      }
      super.remove(reason);
   }
}

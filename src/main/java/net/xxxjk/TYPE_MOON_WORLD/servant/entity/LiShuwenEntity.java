package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public class LiShuwenEntity extends ServantEntity {
   public static final String SERVANT_KEY = "li_shuwen";
   private static final EntityDataAccessor<Integer> COMBAT_PHASE = SynchedEntityData.defineId(LiShuwenEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> WU_ER_DA_TARGETING = SynchedEntityData.defineId(LiShuwenEntity.class, EntityDataSerializers.BOOLEAN);
   private static final String TAG_COMBAT_PHASE = "LiShuwenCombatPhase";

   public LiShuwenEntity(EntityType<LiShuwenEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(COMBAT_PHASE, 1);
      builder.define(WU_ER_DA_TARGETING, false);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         LiShuwenCombatHelper.applyChineseMartialArtsAttributes(this);
         int phase = this.computeHealthPhase();
         if (phase != this.getCombatPhase()) {
            this.setCombatPhase(phase);
            if (this.level() instanceof ServerLevel level) {
               VFXServerEffects.spawn(level, "servant_li_shuwen_yinyang", this, 64.0);
            }
         }
         LiShuwenCombatHelper.tickSelfState(this);
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (!(target instanceof LivingEntity living) || target == this || living.isAlliedTo(this)) {
         return super.doHurtTarget(target);
      }
      float armorPierceBonus = (float)(living.getAttributeValue(Attributes.ARMOR) * 0.08F);
      float damage = (float)(this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.30F + armorPierceBonus);
      living.invulnerableTime = 0;
      boolean hit = living.hurt(this.damageSources().mobAttack(this), damage);
      living.invulnerableTime = 0;
      if (hit) {
         ServantVoiceHelper.tryPlayAttack(this);
         LiShuwenCombatHelper.spawnMartialHitFx(this, living);
         if (this.hasEffect(MobEffects.INVISIBILITY)) {
            this.removeEffect(MobEffects.INVISIBILITY);
         }
      }
      return hit;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (amount > 0.0F && LiShuwenCombatHelper.tryConsumeCircleRealmDodge(this, source)) {
         return false;
      }
      return super.hurt(source, amount);
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      if (moving) {
         return animations.walkAnimation().orElseGet(() -> animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, true)));
      }
      return animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, false));
   }

   public int getCombatPhase() {
      return this.entityData.get(COMBAT_PHASE);
   }

   public void setCombatPhase(int phase) {
      this.entityData.set(COMBAT_PHASE, Math.max(1, Math.min(3, phase)));
   }

   public boolean isWuErDaTargeting() {
      return this.entityData.get(WU_ER_DA_TARGETING);
   }

   public void setWuErDaTargeting(boolean targeting) {
      this.entityData.set(WU_ER_DA_TARGETING, targeting);
   }

   public void triggerPunchAnimation() {
      this.triggerNamedActionAnimation("punch");
      ServantVoiceHelper.tryPlayAttack(this);
   }

   public void triggerStepAnimation() {
      this.triggerNamedActionAnimation("step");
   }

   public void triggerPursuitAnimation() {
      this.triggerNamedActionAnimation("pursuit");
      ServantVoiceHelper.tryPlayAttack(this);
   }

   public void triggerChargeAnimation() {
      this.triggerNamedActionAnimation("charge");
      ServantVoiceHelper.tryPlayAttack(this);
   }

   public void triggerInterruptAnimation() {
      this.triggerNamedActionAnimation("interrupt");
      ServantVoiceHelper.tryPlayAttack(this);
   }

   public void triggerWuErDaAnimation() {
      this.triggerNamedActionAnimation("wu_er_da");
      ServantVoiceHelper.tryPlayLiShuwenNp(this);
   }

   private int computeHealthPhase() {
      float max = Math.max(1.0F, this.getMaxHealth());
      float ratio = this.getHealth() / max;
      if (ratio <= 0.333F) {
         return 3;
      }
      return ratio <= 0.666F ? 2 : 1;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt(TAG_COMBAT_PHASE, this.getCombatPhase());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setCombatPhase(tag.getInt(TAG_COMBAT_PHASE));
   }
}

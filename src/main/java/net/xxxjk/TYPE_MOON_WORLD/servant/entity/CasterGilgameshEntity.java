package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import org.joml.Vector3f;

/** The Caster-class, post-journey King of Uruk. */
public final class CasterGilgameshEntity extends ServantEntity {
   public static final String SERVANT_KEY = "gilgamesh_caster";
   private static final EntityDataAccessor<Boolean> FLYING_MODE =
      SynchedEntityData.defineId(CasterGilgameshEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> COMBAT_PHASE =
      SynchedEntityData.defineId(CasterGilgameshEntity.class, EntityDataSerializers.INT);
   private static final String FLIGHT_WAS_AIRBORNE = "CasterGilgameshFlightWasAirborne";
   private static final String FLIGHT_LANDED_UNTIL = "CasterGilgameshFlightLandedUntil";
   private static final String TAG_COMBAT_PHASE = "CasterGilgameshCombatPhase";
   private static final DustParticleOptions GOLD =
      new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 1.25F);

   public CasterGilgameshEntity(EntityType<CasterGilgameshEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FLYING_MODE, false);
      builder.define(COMBAT_PHASE, 1);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (!this.level().isClientSide() && this.isAlive() && !this.isSpiritualDissolving()) {
         updateCombatPhase();
         if (this.isFlyingMode()) {
            if (!this.onGround()) {
               this.getPersistentData().putBoolean(FLIGHT_WAS_AIRBORNE, true);
            } else if (this.getPersistentData().getBoolean(FLIGHT_WAS_AIRBORNE)) {
               this.setFlyingMode(false);
               this.getPersistentData().putLong(FLIGHT_LANDED_UNTIL, this.level().getGameTime() + 80L);
            }
         } else {
            this.setNoGravity(false);
         }
         CasterGilgameshCombatHelper.tick(this);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setFlyingMode(tag.getBoolean("CasterGilgameshFlyingMode"));
      this.setCombatPhase(tag.contains(TAG_COMBAT_PHASE) ? tag.getInt(TAG_COMBAT_PHASE) : this.computeCombatPhase());
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("CasterGilgameshFlyingMode", this.isFlyingMode());
      tag.putInt(TAG_COMBAT_PHASE, this.getCombatPhase());
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 200.0)
         .add(Attributes.MOVEMENT_SPEED, 0.28)
         .add(Attributes.STEP_HEIGHT, 3.0)
         .add(Attributes.ATTACK_DAMAGE, 15.0)
         .add(Attributes.ARMOR, 6.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.0)
         .add(Attributes.FOLLOW_RANGE, 96.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   public boolean isFlyingMode() {
      return this.entityData.get(FLYING_MODE);
   }

   public void setFlyingMode(boolean flying) {
      if (flying && !this.level().isClientSide()
         && this.getPersistentData().getLong(FLIGHT_LANDED_UNTIL) > this.level().getGameTime()) {
         return;
      }
      this.entityData.set(FLYING_MODE, flying);
      if (!this.level().isClientSide()) {
         this.setNoGravity(flying);
         if (!flying) {
            this.getPersistentData().remove(FLIGHT_WAS_AIRBORNE);
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.35, 1.0));
         }
      }
   }

   public int getCombatPhase() {
      return this.entityData.get(COMBAT_PHASE);
   }

   public void setCombatPhase(int phase) {
      this.entityData.set(COMBAT_PHASE, Math.max(1, Math.min(3, phase)));
   }

   public int computeCombatPhase() {
      float ratio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
      if (ratio <= 0.333F) return 3;
      return ratio <= 0.666F ? 2 : 1;
   }

   private void updateCombatPhase() {
      int next = Math.max(this.getCombatPhase(), this.computeCombatPhase());
      if (next == this.getCombatPhase()) {
         return;
      }
      this.setCombatPhase(next);
      if (this.level() instanceof ServerLevel level) {
         double y = this.getY() + this.getBbHeight() * 0.62;
         level.sendParticles(GOLD, this.getX(), y, this.getZ(), 72 + next * 24, 0.8 + next * 0.3, 0.7, 0.8 + next * 0.3, 0.08);
         level.sendParticles(ParticleTypes.END_ROD, this.getX(), y, this.getZ(), 22 + next * 8, 0.45, 0.45, 0.45, 0.04);
      }
   }

   @Override
   protected boolean useFloatingAnimation() {
      return false;
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      if (!this.isFlyingMode()) {
         return null;
      }
      return moving
         ? animations.actionAnimation("fly").orElse(animations.actionAnimation("float_idle").orElse(null))
         : animations.actionAnimation("float_idle").orElse(animations.actionAnimation("fly").orElse(null));
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
         && attacker != this) {
         this.setTarget(attacker);
      }
      return super.hurt(source, amount);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean hit = super.doHurtTarget(target);
      if (hit) {
         ServantVoiceHelper.tryPlayAttack(this);
      }
      if (hit && target instanceof net.minecraft.world.entity.LivingEntity living
         && this.getPersistentData().getBoolean("DivinityActive")) {
         living.invulnerableTime = 0;
         living.hurt(this.damageSources().magic(),
            Math.max(0.0F, this.getPersistentData().getFloat("DivinityFlatDamage")));
         living.invulnerableTime = 0;
      }
      return hit;
   }

   @Override
   public boolean killedEntity(ServerLevel level, net.minecraft.world.entity.LivingEntity victim) {
      boolean result = super.killedEntity(level, victim);
      CasterGilgameshCombatHelper.tryPlayVictory(this, victim);
      return result;
   }
}

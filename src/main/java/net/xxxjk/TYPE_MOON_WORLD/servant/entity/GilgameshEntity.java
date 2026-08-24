package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.minecraft.world.level.Level;

/** The Archer-class Hero King. This is intentionally an NPC-only servant. */
public class GilgameshEntity extends ServantEntity {
   public static final String SERVANT_KEY = "gilgamesh";
   private static final EntityDataAccessor<Boolean> FLYING_MODE = SynchedEntityData.defineId(GilgameshEntity.class, EntityDataSerializers.BOOLEAN);
   private static final String FLIGHT_WAS_AIRBORNE_TAG = "GilgameshFlightWasAirborne";
   private static final String FLIGHT_LANDED_UNTIL_TAG = "GilgameshFlightLandedUntil";

   public GilgameshEntity(EntityType<GilgameshEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FLYING_MODE, false);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setFlyingMode(tag.getBoolean("GilgameshFlyingMode"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("GilgameshFlyingMode", this.isFlyingMode());
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 100.0)
          .add(Attributes.MOVEMENT_SPEED, 0.2)
          .add(Attributes.STEP_HEIGHT, 3.0)
          .add(Attributes.ATTACK_DAMAGE, 5.0)
          .add(Attributes.ATTACK_SPEED, 4.0)
          .add(Attributes.ARMOR, 12.0)
         .add(Attributes.ARMOR_TOUGHNESS, 8.0)
         .add(Attributes.FOLLOW_RANGE, 96.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (!this.level().isClientSide() && this.isAlive() && !this.isSpiritualDissolving()) {
         if (this.isFlyingMode()) {
            if (!this.onGround()) {
               this.getPersistentData().putBoolean(FLIGHT_WAS_AIRBORNE_TAG, true);
            } else if (this.getPersistentData().getBoolean(FLIGHT_WAS_AIRBORNE_TAG)) {
               this.setFlyingMode(false);
               this.getPersistentData().putLong(FLIGHT_LANDED_UNTIL_TAG, this.level().getGameTime() + 80L);
            }
         }
      }
   }

   public boolean isFlyingMode() {
      return this.entityData.get(FLYING_MODE);
   }

   public void setFlyingMode(boolean flying) {
      if (flying && !this.level().isClientSide() && this.getPersistentData().getLong(FLIGHT_LANDED_UNTIL_TAG) > this.level().getGameTime()) {
         return;
      }
      this.entityData.set(FLYING_MODE, flying);
      if (!this.level().isClientSide()) {
         this.setNoGravity(flying);
         if (!flying) {
            this.getPersistentData().remove(FLIGHT_WAS_AIRBORNE_TAG);
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.35, 1.0));
         }
      }
   }

   @Override protected boolean useFloatingAnimation() { return this.isFlyingMode(); }

   @Override protected String getLoopAnimationOverride(net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations animations, boolean moving) {
      return this.isFlyingMode()
         ? animations.actionAnimation("float_idle").orElse(animations.actionAnimation("fly").orElse(animations.actionAnimation("idle").orElse(null)))
         : null;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
         && attacker != this && attacker.position().subtract(this.position()).dot(this.getLookAngle()) < -0.25) {
         this.setTarget(attacker);
         this.setYRot(attacker.getYRot() + 180.0F);
         this.yRotO = this.getYRot();
      }
      boolean hurt = super.hurt(source, amount);
      if (hurt && this.isAlive() && !this.level().isClientSide()) {
         GilgameshCombatHelper.noteIncomingThreat(this, source, amount);
         GilgameshCombatHelper.tryRetaliatoryChains(this, source);
      }
      return hurt;
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof net.minecraft.world.entity.LivingEntity living) {
         if (GilgameshCombatHelper.isMeleeMode(this)) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), 25.0F);
            living.invulnerableTime = 0;
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
         }
         living.invulnerableTime = 0;
         living.hurt(this.damageSources().magic(), 5.0F);
         if (this.getRandom().nextFloat() < 0.15F) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), (float)this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.5F);
         }
      }
      return hit;
   }

   @Override
   public boolean killedEntity(ServerLevel level, net.minecraft.world.entity.LivingEntity victim) {
      boolean result = super.killedEntity(level, victim);
      if (this.getRandom().nextFloat() >= 0.15F) return result;
      net.minecraft.world.item.Item[] pool = {
         ModItems.DRAGON_FANG.get(), ModItems.DRAGONS_REVERSE_SCALE.get(), ModItems.PHOENIX_FEATHER.get(),
         ModItems.PROOF_OF_HERO.get(), ModItems.VOIDS_DUST.get(), ModItems.REMNANTS_OF_MADNESS.get(),
         ModItems.SEED_OF_YGGDRASIL.get(), ModItems.QP.get()
      };
      ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.3, victim.getZ(), new ItemStack(pool[this.getRandom().nextInt(pool.length)]));
      level.addFreshEntity(drop);
      return result;
   }
}

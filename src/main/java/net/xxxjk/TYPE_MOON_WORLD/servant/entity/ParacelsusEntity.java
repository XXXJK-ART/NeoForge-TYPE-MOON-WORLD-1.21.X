package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusWorkshopHelper;

public class ParacelsusEntity extends ServantEntity {
   public static final String SERVANT_KEY = "paracelsus";
   private static final EntityDataAccessor<Integer> COMBAT_PHASE = SynchedEntityData.defineId(ParacelsusEntity.class, EntityDataSerializers.INT);
   private static final String TAG_COMBAT_PHASE = "ParacelsusCombatPhase";
   private static final String TAG_DIAMOND_SHIELD_COUNT = "ParacelsusDiamondShieldCount";
   private static final int DIAMOND_SHIELD_STARTING_CHARGES = 2;
   private static final int DIAMOND_SHIELD_MAX_CHARGES = 3;

   public ParacelsusEntity(EntityType<ParacelsusEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(COMBAT_PHASE, 1);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.tickCount <= 2 && this.getMainHandItem().isEmpty()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PARACELSUS_SWORD.get()));
         }
         if (this.tickCount == 1 && this.getPersistentData().getInt(TAG_DIAMOND_SHIELD_COUNT) <= 0) {
            this.getPersistentData().putInt(TAG_DIAMOND_SHIELD_COUNT, DIAMOND_SHIELD_STARTING_CHARGES);
         }
         if (this.getPersistentData().getInt(TAG_DIAMOND_SHIELD_COUNT) > DIAMOND_SHIELD_MAX_CHARGES) {
            this.getPersistentData().putInt(TAG_DIAMOND_SHIELD_COUNT, DIAMOND_SHIELD_MAX_CHARGES);
         }
         int phase = this.computeCombatPhase();
         if (phase != this.getCombatPhase()) {
            this.setCombatPhase(phase);
         }
         this.setNoGravity(false);
      }
   }

   @Override
   protected boolean useFloatingAnimation() {
      return false;
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      if (moving) {
         return animations.walkAnimation().orElseGet(() -> animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, true)));
      }
      if (this.getCombatPhase() >= 3) {
         return animations.actionAnimation("idle_cast").orElseGet(() ->
            animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, false))
         );
      }
      return animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, false));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt(TAG_COMBAT_PHASE, this.getCombatPhase());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setCombatPhase(tag.contains(TAG_COMBAT_PHASE) ? tag.getInt(TAG_COMBAT_PHASE) : this.computeCombatPhase());
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide && amount >= 18.0F && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         int shields = Math.min(this.getPersistentData().getInt(TAG_DIAMOND_SHIELD_COUNT), DIAMOND_SHIELD_MAX_CHARGES);
         if (shields > 0) {
            this.getPersistentData().putInt(TAG_DIAMOND_SHIELD_COUNT, shields - 1);
            if (this.level() instanceof ServerLevel level) {
               this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, false, true, true));
               level.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.0, this.getZ(), 36, 0.65, 0.5, 0.65, 0.03);
               level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.0, this.getZ(), 18, 0.35, 0.35, 0.35, 0.02);
               level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            return false;
         }
      }
      return super.hurt(source, amount);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (!(target instanceof net.minecraft.world.entity.LivingEntity living)) {
         return false;
      }
      float damage = ParacelsusBalanceRules.reduceDamage((float)this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
      return living.hurt(this.damageSources().mobAttack(this), damage);
   }

   @Override
   public double getMaxMp() {
      return Math.max(200.0, super.getMaxMp());
   }

   public boolean isInsideWorkshop() {
      return this.getPersistentData().getBoolean(ParacelsusWorkshopHelper.TAG_INSIDE_WORKSHOP);
   }

   public int getCombatPhase() {
      return this.entityData.get(COMBAT_PHASE);
   }

   public void setCombatPhase(int phase) {
      this.entityData.set(COMBAT_PHASE, Math.max(1, Math.min(3, phase)));
   }

   public int computeCombatPhase() {
      float maxHealth = Math.max(1.0F, this.getMaxHealth());
      float ratio = this.getHealth() / maxHealth;
      if (ratio <= 0.333F) {
         return 3;
      }
      return ratio <= 0.666F ? 2 : 1;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.jetbrains.annotations.Nullable;

public class UshiwakamaruRiderEntity extends ServantEntity {
   public static final String SERVANT_KEY = "ushiwakamaru_rider";
   public static final String TAG_PHASE = "UshiwakamaruCombatPhase";
   public static final String TAG_CLONE = "UshiwakamaruClone";
   public static final String TAG_CLONE_OWNER = "UshiwakamaruCloneOwner";
   public static final String TAG_CLONE_EXPIRES = "UshiwakamaruCloneExpires";
   public static final String TAG_CLONE_UUIDS = "UshiwakamaruCloneUuids";
   public static final String TAG_EIGHT_BOAT_TARGET = "UshiwakamaruEightBoatTarget";
   public static final String TAG_SHIELD_HP = "UshiwakamaruBenkeiShieldHp";
   public static final String TAG_SHIELD_EXPIRES = "UshiwakamaruBenkeiShieldExpires";
   private static final EntityDataAccessor<Integer> COMBAT_PHASE = SynchedEntityData.defineId(UshiwakamaruRiderEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> CLONE = SynchedEntityData.defineId(UshiwakamaruRiderEntity.class, EntityDataSerializers.BOOLEAN);
   @Nullable
   private LivingEntity eightBoatTarget;

   public UshiwakamaruRiderEntity(EntityType<? extends UshiwakamaruRiderEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(COMBAT_PHASE, 1);
      builder.define(CLONE, false);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      if (!this.isClone()) {
         this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPIDER_CUTTER.get()));
         this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
      }
      return result;
   }

   @Override
   protected void customServerAiStep() {
      if (this.isClone()) {
         return;
      }
      super.customServerAiStep();
      if (!this.level().isClientSide() && this.isAlive() && !this.isSpiritualDissolving()) {
         UshiwakamaruCombatHelper.tick(this);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      if (this.isClone()) {
         UshiwakamaruCombatHelper.tickPersistentState(this);
         LivingEntity owner = this.getOwnerEntity();
         boolean playerOwned = owner instanceof ServerPlayer;
         if (level.getGameTime() >= this.getPersistentData().getLong(TAG_CLONE_EXPIRES)
            || owner == null || !owner.isAlive()
            || !playerOwned && !UshiwakamaruCombatHelper.isEightBoatTargetAlive(this)) {
            this.discard();
         } else if (this.isAlive()) {
            UshiwakamaruCombatHelper.tick(this);
         }
         return;
      }
      int next = UshiwakamaruCombatRules.phaseFor(this.getHealth(), this.getMaxHealth(), this.getCombatPhase());
      if (next != this.getCombatPhase()) {
         this.setCombatPhase(next);
         VFXServerEffects.spawn(level, "servant_ushiwakamaru_phase", this, 96.0);
         level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.0, this.getZ(), 24, 0.7, 0.7, 0.7, 0.03);
      }
      UshiwakamaruCombatHelper.tickPersistentState(this);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean bypassDodge = this.getRandom().nextFloat() < 0.15F;
      if (bypassDodge) {
         this.getPersistentData().putLong(UshiwakamaruCombatHelper.TAG_GUARANTEED_HIT_UNTIL, this.level().getGameTime() + 1L);
      }
      boolean hit;
      try {
         hit = super.doHurtTarget(target);
      } finally {
         if (bypassDodge) {
            this.getPersistentData().remove(UshiwakamaruCombatHelper.TAG_GUARANTEED_HIT_UNTIL);
         }
      }
      if (hit) {
         triggerSlashAnimation();
         if (this.level() instanceof ServerLevel level) {
            Vec3 direction = target.position().subtract(this.position());
            VFXServerEffects.spawnOriented(level, "servant_ushiwakamaru_slash",
               this.position().add(0.0, this.getBbHeight() * 0.48, 0.0), direction, 96.0);
         }
         if (!this.isClone()) {
            ServantVoiceHelper.tryPlayAttack(this);
         }
      }
      return hit;
   }

   @Override
   public void die(DamageSource cause) {
      if (this.isClone()) {
         this.discard();
         return;
      }
      UshiwakamaruCombatHelper.cleanup(this);
      super.die(cause);
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide() && !this.isClone()) {
         UshiwakamaruCombatHelper.cleanup(this);
      }
      super.remove(reason);
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      return moving
         ? animations.walkAnimation().orElseGet(() -> animations.idleAnimation().orElse(null))
         : animations.idleAnimation().orElse(null);
   }

   public int getCombatPhase() {
      return this.entityData.get(COMBAT_PHASE);
   }

   public void setCombatPhase(int phase) {
      this.entityData.set(COMBAT_PHASE, Math.max(1, Math.min(3, phase)));
   }

   public boolean isClone() {
      return this.entityData.get(CLONE);
   }

   public void setClone(boolean clone) {
      this.entityData.set(CLONE, clone);
      this.getPersistentData().putBoolean(TAG_CLONE, clone);
   }

   @Nullable
   public LivingEntity getOwnerEntity() {
      if (!this.getPersistentData().hasUUID(TAG_CLONE_OWNER) || !(this.level() instanceof ServerLevel level)) {
         return null;
      }
      Entity owner = level.getEntity(this.getPersistentData().getUUID(TAG_CLONE_OWNER));
      return owner instanceof LivingEntity living ? living : null;
   }

   public void initClone(UshiwakamaruRiderEntity owner, long expiresAt) {
      this.initClone(owner, owner.getTarget(), expiresAt);
   }

   public void initClone(LivingEntity owner, @Nullable LivingEntity target, long expiresAt) {
      this.setClone(true);
      this.getPersistentData().putUUID(TAG_CLONE_OWNER, owner.getUUID());
      this.getPersistentData().putLong(TAG_CLONE_EXPIRES, expiresAt);
      this.getPersistentData().putLong(UshiwakamaruCombatHelper.TAG_EIGHT_BOAT_UNTIL, expiresAt);
      this.getPersistentData().putLong(UshiwakamaruCombatHelper.TAG_EIGHT_BOAT_NEXT_DASH, this.level().getGameTime());
      if (target != null) {
         this.getPersistentData().putUUID(TAG_EIGHT_BOAT_TARGET, target.getUUID());
         this.setEightBoatTarget(target);
         this.setTarget(target);
      }
      if (owner instanceof UshiwakamaruRiderEntity rider) {
         this.setCombatPhase(rider.getCombatPhase());
         this.setCurrentMp(rider.getCurrentMp());
      } else {
         this.setCombatPhase(3);
         this.setCurrentMp(0.0);
      }
      copyBaseAttribute(owner, Attributes.MAX_HEALTH);
      copyBaseAttribute(owner, Attributes.ATTACK_DAMAGE);
      copyBaseAttribute(owner, Attributes.MOVEMENT_SPEED);
      copyBaseAttribute(owner, Attributes.ARMOR);
      copyBaseAttribute(owner, Attributes.ARMOR_TOUGHNESS);
      copyBaseAttribute(owner, Attributes.FOLLOW_RANGE);
      copyBaseAttribute(owner, Attributes.KNOCKBACK_RESISTANCE);
      copyBaseAttribute(owner, Attributes.STEP_HEIGHT);
      copyPersistentInt(owner, net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper.MAGIC_RESISTANCE_LEVEL_TAG);
      copyPersistentFloat(owner, net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper.MAGIC_RESISTANCE_DAMAGE_REDUCTION_TAG);
      copyPersistentFloat(owner, net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper.MAGIC_RESISTANCE_DEBUFF_RESIST_TAG);
      this.setHealth(this.getMaxHealth());
      this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPIDER_CUTTER.get()));
      this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
      this.setPersistenceRequired();
   }

   @Nullable
   public LivingEntity getEightBoatTarget() {
      return this.eightBoatTarget;
   }

   public void setEightBoatTarget(@Nullable LivingEntity target) {
      this.eightBoatTarget = target;
   }

   private void copyBaseAttribute(LivingEntity owner,
                                  net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
      var source = owner.getAttribute(attribute);
      var destination = this.getAttribute(attribute);
      if (source != null && destination != null) {
         destination.setBaseValue(owner instanceof ServerPlayer ? owner.getAttributeValue(attribute) : source.getBaseValue());
      }
   }

   private void copyPersistentInt(LivingEntity owner, String key) {
      if (owner.getPersistentData().contains(key)) {
         this.getPersistentData().putInt(key, owner.getPersistentData().getInt(key));
      }
   }

   private void copyPersistentFloat(LivingEntity owner, String key) {
      if (owner.getPersistentData().contains(key)) {
         this.getPersistentData().putFloat(key, owner.getPersistentData().getFloat(key));
      }
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      if (other instanceof UshiwakamaruRiderEntity rider) {
         CompoundTag thisData = this.getPersistentData();
         CompoundTag otherData = rider.getPersistentData();
         if (!this.isClone() && rider.isClone() && otherData.hasUUID(TAG_CLONE_OWNER)
            && this.getUUID().equals(otherData.getUUID(TAG_CLONE_OWNER))) {
            return true;
         }
         if (this.isClone() && thisData.hasUUID(TAG_CLONE_OWNER)) {
            java.util.UUID ownerId = thisData.getUUID(TAG_CLONE_OWNER);
            if (ownerId.equals(rider.getUUID()) || rider.isClone() && otherData.hasUUID(TAG_CLONE_OWNER)
               && ownerId.equals(otherData.getUUID(TAG_CLONE_OWNER))) {
               return true;
            }
         }
      }
      LivingEntity owner = this.getOwnerEntity();
      return owner != null && (other == owner || owner.isAlliedTo(other));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt(TAG_PHASE, this.getCombatPhase());
      tag.putBoolean(TAG_CLONE, this.isClone());
      CompoundTag data = this.getPersistentData();
      if (data.hasUUID(TAG_CLONE_OWNER)) tag.putUUID(TAG_CLONE_OWNER, data.getUUID(TAG_CLONE_OWNER));
      if (data.hasUUID(TAG_EIGHT_BOAT_TARGET)) tag.putUUID(TAG_EIGHT_BOAT_TARGET, data.getUUID(TAG_EIGHT_BOAT_TARGET));
      tag.putLong(TAG_CLONE_EXPIRES, data.getLong(TAG_CLONE_EXPIRES));
      tag.putFloat(TAG_SHIELD_HP, data.getFloat(TAG_SHIELD_HP));
      tag.putLong(TAG_SHIELD_EXPIRES, data.getLong(TAG_SHIELD_EXPIRES));
      if (data.contains(TAG_CLONE_UUIDS, 9)) tag.put(TAG_CLONE_UUIDS, data.getList(TAG_CLONE_UUIDS, 8).copy());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setCombatPhase(tag.contains(TAG_PHASE) ? tag.getInt(TAG_PHASE) : UshiwakamaruCombatRules.phaseFor(this.getHealth(), this.getMaxHealth(), 1));
      this.setClone(tag.getBoolean(TAG_CLONE));
      if (tag.hasUUID(TAG_CLONE_OWNER)) this.getPersistentData().putUUID(TAG_CLONE_OWNER, tag.getUUID(TAG_CLONE_OWNER));
      if (tag.hasUUID(TAG_EIGHT_BOAT_TARGET)) this.getPersistentData().putUUID(TAG_EIGHT_BOAT_TARGET, tag.getUUID(TAG_EIGHT_BOAT_TARGET));
      this.getPersistentData().putLong(TAG_CLONE_EXPIRES, tag.getLong(TAG_CLONE_EXPIRES));
      this.getPersistentData().putFloat(TAG_SHIELD_HP, tag.getFloat(TAG_SHIELD_HP));
      this.getPersistentData().putLong(TAG_SHIELD_EXPIRES, tag.getLong(TAG_SHIELD_EXPIRES));
      if (tag.contains(TAG_CLONE_UUIDS, 9)) this.getPersistentData().put(TAG_CLONE_UUIDS, tag.getList(TAG_CLONE_UUIDS, 8).copy());
   }

   public void triggerSlashAnimation() {
      this.triggerNamedActionAnimation("slash");
   }

   public void triggerDashAnimation() {
      this.triggerNamedActionAnimation("dash");
   }

   public void triggerShieldAnimation() {
      this.triggerNamedActionAnimation("shield");
   }

   public void triggerEightBoatAnimation() {
      this.triggerNamedActionAnimation("eight_boat");
   }

   public void triggerSpiderSlayerAnimation() {
      this.triggerNamedActionAnimation("spider_slayer");
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import org.jetbrains.annotations.Nullable;

public class CursedArmHassanEntity extends ServantEntity {
   public static final String SERVANT_KEY = "cursed_arm_hassan";
   private static final EntityDataAccessor<Boolean> NO_CAPE = SynchedEntityData.defineId(CursedArmHassanEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> NO_BANDAGES = SynchedEntityData.defineId(CursedArmHassanEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> SELF_MODIFICATION_LEVEL = SynchedEntityData.defineId(CursedArmHassanEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ZABANIYA_TARGET_ID = SynchedEntityData.defineId(CursedArmHassanEntity.class, EntityDataSerializers.INT);
   private static final String TAG_NO_CAPE = "CursedArmNoCape";
   private static final String TAG_NO_BANDAGES = "CursedArmNoBandages";
   private static final String TAG_SELF_MODIFICATION_LEVEL = "CursedArmSelfModificationLevel";
   private static final net.minecraft.resources.ResourceLocation HEALTH_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "cursed_arm_self_mod_health");
   private static final net.minecraft.resources.ResourceLocation ATTACK_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "cursed_arm_self_mod_attack");

   public CursedArmHassanEntity(EntityType<? extends CursedArmHassanEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(NO_CAPE, false);
      builder.define(NO_BANDAGES, false);
      builder.define(SELF_MODIFICATION_LEVEL, 0);
      builder.define(ZABANIYA_TARGET_ID, 0);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      this.applySelfModificationAttributes();
      this.enforceEmptyRightHand();
      return data;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (!this.hasNoCape() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.setNoCape(true);
            this.triggerNamedActionAnimation("no_cape");
         }
         long now = this.level().getGameTime();
         if (this.hasNoBandages() && this.getPersistentData().getLong("CursedArmBandagesRestoreTick") <= now) {
            this.setNoBandages(false);
         }
         this.enforceEmptyRightHand();
      }
   }

   @Override
   public void setItemInHand(InteractionHand hand, ItemStack stack) {
      if (hand == InteractionHand.MAIN_HAND && !stack.isEmpty()) {
         ItemStack rightHandStack = stack.copy();
         if (rightHandStack.is(ModItems.DIRK_SMALL_KNIFE.get())) {
            rightHandStack.setCount(Math.max(30, rightHandStack.getCount()));
         }
         if (this.getOffhandItem().isEmpty()) {
            super.setItemInHand(InteractionHand.OFF_HAND, rightHandStack);
         } else {
            this.spawnAtLocation(rightHandStack);
         }
         super.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         return;
      }
      super.setItemInHand(hand, stack);
   }

   private void enforceEmptyRightHand() {
      ItemStack mainHand = this.getMainHandItem();
      if (mainHand.isEmpty()) {
         return;
      }
      ItemStack moved = mainHand.copy();
      if (moved.is(ModItems.DIRK_SMALL_KNIFE.get())) {
         moved.setCount(Math.max(30, moved.getCount()));
      }
      if (this.getOffhandItem().isEmpty()) {
         super.setItemInHand(InteractionHand.OFF_HAND, moved);
      } else {
         this.spawnAtLocation(moved);
      }
      super.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (amount <= 0.0F) {
         return false;
      }
      if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && CursedArmHassanCombatHelper.tryDodge(this, source)) {
         return false;
      }
      return super.hurt(source, amount);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (target instanceof net.minecraft.world.entity.LivingEntity living && CursedArmHassanCombatHelper.isProtectedPigKind(living)) {
         this.setTarget(null);
         return false;
      }
      return super.doHurtTarget(target);
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      if (moving) {
         return animations.walkAnimation().orElseGet(() -> animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, true)));
      }
      return animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, false));
   }

   @Override
   protected EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
      EntityDimensions base = super.getDefaultDimensions(pose);
      float scale = this.getVisualScale();
      return base.scale(scale);
   }

   public boolean hasNoCape() {
      return this.entityData.get(NO_CAPE);
   }

   public void setNoCape(boolean noCape) {
      this.entityData.set(NO_CAPE, noCape);
   }

   public boolean hasNoBandages() {
      return this.entityData.get(NO_BANDAGES);
   }

   public void setNoBandages(boolean noBandages) {
      this.entityData.set(NO_BANDAGES, noBandages);
   }

   public int getSelfModificationLevel() {
      return this.entityData.get(SELF_MODIFICATION_LEVEL);
   }

   public int getZabaniyaTargetId() {
      return this.entityData.get(ZABANIYA_TARGET_ID);
   }

   public void setZabaniyaTargetId(int targetId) {
      this.entityData.set(ZABANIYA_TARGET_ID, Math.max(0, targetId));
   }

   public void setSelfModificationLevel(int level) {
      this.entityData.set(SELF_MODIFICATION_LEVEL, Math.max(0, Math.min(3, level)));
      this.applySelfModificationAttributes();
      this.refreshDimensions();
   }

   public float getVisualScale() {
      return Math.min(1.05F, 0.9F + this.getSelfModificationLevel() * 0.05F);
   }

   public double getZabaniyaRange() {
      return 5.0 + this.getSelfModificationLevel();
   }

   public void triggerZabaniyaAnimation() {
      this.triggerNamedActionAnimation("zabaniya");
      ServantVoiceHelper.tryPlayZabaniya(this);
   }

   public void triggerDirkThrowAnimation() {
      this.triggerNamedActionAnimation("dirk_throw");
      ServantVoiceHelper.tryPlayAttack(this);
   }

   public void triggerAssassinStabAnimation() {
      this.triggerNamedActionAnimation("assassin_stab");
      ServantVoiceHelper.tryPlayAttack(this);
   }

   public void triggerShadowStepAnimation() {
      this.triggerNamedActionAnimation("shadow_step");
   }

   private void applySelfModificationAttributes() {
      int level = this.getSelfModificationLevel();
      AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
      if (maxHealth != null) {
         maxHealth.removeModifier(HEALTH_ID);
         if (level > 0) {
            maxHealth.addPermanentModifier(new AttributeModifier(HEALTH_ID, level * 50.0, AttributeModifier.Operation.ADD_VALUE));
         }
      }
      AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) {
         attack.removeModifier(ATTACK_ID);
         if (level > 0) {
            attack.addPermanentModifier(new AttributeModifier(ATTACK_ID, level * 5.0, AttributeModifier.Operation.ADD_VALUE));
         }
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean(TAG_NO_CAPE, this.hasNoCape());
      tag.putBoolean(TAG_NO_BANDAGES, this.hasNoBandages());
      tag.putInt(TAG_SELF_MODIFICATION_LEVEL, this.getSelfModificationLevel());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setNoCape(tag.getBoolean(TAG_NO_CAPE));
      this.setNoBandages(tag.getBoolean(TAG_NO_BANDAGES));
      this.setSelfModificationLevel(tag.getInt(TAG_SELF_MODIFICATION_LEVEL));
   }
}

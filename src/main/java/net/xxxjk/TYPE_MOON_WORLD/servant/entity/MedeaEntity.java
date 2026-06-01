package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class MedeaEntity extends ServantEntity {
   public static final String SERVANT_KEY = "medea";
   private static final EntityDataAccessor<Boolean> FLYING_MODE = SynchedEntityData.defineId(MedeaEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> BARRIER_ACTIVE = SynchedEntityData.defineId(MedeaEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> TEMPORARY_FOCUS_ITEM = SynchedEntityData.defineId(MedeaEntity.class, EntityDataSerializers.INT);
   private static final String TAG_BARRIER_HP = "MedeaBarrierHp";

   public MedeaEntity(EntityType<MedeaEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   public enum FocusItem {
      NONE(0),
      HECATES_STAFF(1),
      RULE_BREAKER(2);

      private final int id;

      FocusItem(int id) {
         this.id = id;
      }

      public int id() {
         return this.id;
      }

      public static FocusItem fromId(int id) {
         for (FocusItem value : values()) {
            if (value.id == id) {
               return value;
            }
         }
         return NONE;
      }
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
         this,
         net.minecraft.world.entity.monster.Monster.class,
         true
      ));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FLYING_MODE, false);
      builder.define(BARRIER_ACTIVE, false);
      builder.define(TEMPORARY_FOCUS_ITEM, FocusItem.NONE.id());
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
      SpawnGroupData spawnData = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      this.applyMedeaBaseAdjustments();
      MedeaWorkshopHelper.initializeStartingStocks(this);
      return spawnData;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(FLYING_MODE, tag.getBoolean("MedeaFlyingMode"));
      this.entityData.set(BARRIER_ACTIVE, tag.getBoolean("MedeaBarrierActive"));
      this.entityData.set(TEMPORARY_FOCUS_ITEM, tag.getInt("MedeaTemporaryFocusItem"));
      this.getPersistentData().putFloat(TAG_BARRIER_HP, tag.getFloat(TAG_BARRIER_HP));
      this.applyMedeaBaseAdjustments();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("MedeaFlyingMode", this.isFlyingMode());
      tag.putBoolean("MedeaBarrierActive", this.isBarrierActive());
      tag.putInt("MedeaTemporaryFocusItem", this.getTemporaryFocusItem().id());
      tag.putFloat(TAG_BARRIER_HP, this.getBarrierStrength());
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (this.isFlyingMode()) {
         this.setNoGravity(true);
         this.fallDistance = 0.0F;
      } else if (!this.isSpiritualDissolving()) {
         this.setNoGravity(false);
      }
   }

   @Override
   public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
      if (isFriendlyDragonfangDamage(source)) {
         return false;
      }
      if (this.isBarrierActive() && amount > 0.0F && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         float barrier = this.getBarrierStrength();
         if (barrier > 0.0F) {
            float absorbed = Math.min(barrier, amount);
            this.setBarrierStrength(barrier - absorbed);
            amount -= absorbed;
            if (this.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(
                  net.minecraft.core.particles.ParticleTypes.ENCHANT,
                  this.getX(),
                  this.getY() + this.getBbHeight() * 0.65,
                  this.getZ(),
                  12,
                  0.4,
                  0.3,
                  0.4,
                  0.02
               );
            }
            if (amount <= 0.0F) {
               return false;
            }
         } else {
            this.setBarrierActive(false);
         }
      }
      return super.hurt(source, amount);
   }

   @Override
   public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      return isOwnedDragonfang(other);
   }

   @Override
   public double getMaxMp() {
      return this.isInsideWorkshop() ? 1000.0 : 500.0;
   }

   @Override
   protected boolean useFloatingAnimation() {
      return this.isFlyingMode();
   }

   public boolean isFlyingMode() {
      return this.entityData.get(FLYING_MODE);
   }

   public void setFlyingMode(boolean flying) {
      this.entityData.set(FLYING_MODE, flying);
      if (!this.level().isClientSide()) {
         this.setNoGravity(flying);
         if (!flying) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.35, 1.0));
         }
      }
   }

   public boolean isBarrierActive() {
      return this.entityData.get(BARRIER_ACTIVE);
   }

   public void setBarrierActive(boolean active) {
      this.entityData.set(BARRIER_ACTIVE, active);
      if (!active) {
         this.getPersistentData().putFloat(TAG_BARRIER_HP, 0.0F);
      }
   }

   public float getBarrierStrength() {
      return this.getPersistentData().getFloat(TAG_BARRIER_HP);
   }

   public void setBarrierStrength(float value) {
      float clamped = Math.max(0.0F, value);
      this.getPersistentData().putFloat(TAG_BARRIER_HP, clamped);
      this.entityData.set(BARRIER_ACTIVE, clamped > 0.0F);
   }

   public FocusItem getTemporaryFocusItem() {
      return FocusItem.fromId(this.entityData.get(TEMPORARY_FOCUS_ITEM));
   }

   public void setTemporaryFocusItem(FocusItem focusItem) {
      FocusItem previous = this.getTemporaryFocusItem();
      this.entityData.set(TEMPORARY_FOCUS_ITEM, focusItem.id());
      if (!this.level().isClientSide()) {
         switch (focusItem) {
            case HECATES_STAFF -> this.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.HECATES_STAFF.get()));
            case RULE_BREAKER -> this.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.RULE_BREAKER.get()));
            default -> this.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
         }
         if (previous != focusItem) {
            if (focusItem == FocusItem.HECATES_STAFF) {
               spawnHecateStaffFx(true);
            } else if (previous == FocusItem.HECATES_STAFF) {
               spawnHecateStaffFx(false);
            }
         }
      }
   }

   public void clearTemporaryFocusItem() {
      this.setTemporaryFocusItem(FocusItem.NONE);
   }

   public boolean isInsideWorkshop() {
      return this.getPersistentData().getBoolean(MedeaWorkshopHelper.TAG_INSIDE_WORKSHOP);
   }

   private void applyMedeaBaseAdjustments() {
      if (this.getAttribute(Attributes.ARMOR) != null) {
         this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
      }
      if (this.getTemporaryFocusItem() == FocusItem.NONE) {
         this.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
      } else {
         this.setTemporaryFocusItem(this.getTemporaryFocusItem());
      }
   }

   private boolean isFriendlyDragonfangDamage(net.minecraft.world.damagesource.DamageSource source) {
      net.minecraft.world.entity.Entity attacker = source.getEntity();
      net.minecraft.world.entity.Entity direct = source.getDirectEntity();
      return isOwnedDragonfang(attacker) || isOwnedDragonfang(direct);
   }

   private boolean isOwnedDragonfang(net.minecraft.world.entity.Entity entity) {
      return entity instanceof net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity dragonfang
         && this.getUUID().equals(dragonfang.getSummonerUuid());
   }

   private void spawnHecateStaffFx(boolean summon) {
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      double x = this.getX();
      double y = this.getY() + this.getBbHeight() * 0.72;
      double z = this.getZ();
      if (summon) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, x, y, z, 16, 0.18, 0.35, 0.18, 0.02);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, x, y, z, 22, 0.26, 0.42, 0.26, 0.03);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, x, y - 0.15, z, 10, 0.12, 0.2, 0.12, 0.01);
         level.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 1.45F);
      } else {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, x, y, z, 18, 0.2, 0.3, 0.2, 0.03);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, x, y, z, 8, 0.14, 0.2, 0.14, 0.01);
         level.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.HOSTILE, 0.6F, 1.55F);
      }
   }
}

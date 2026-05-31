package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import org.jetbrains.annotations.Nullable;

public class MedusaEntity extends ServantEntity {
   public static final String SERVANT_KEY = "medusa";
   private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(MedusaEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Boolean> BLINDFOLD_SEALED = SynchedEntityData.defineId(MedusaEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> CROUCH_POSE = SynchedEntityData.defineId(MedusaEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> EYES_RELEASED = SynchedEntityData.defineId(MedusaEntity.class, EntityDataSerializers.BOOLEAN);
   private static final String TAG_PEGASUS_UUID = "MedusaPegasusUuid";
   private static final String TAG_BELLEROPHON_RIDE_UNTIL = "MedusaBellerophonRideUntil";

   public MedusaEntity(EntityType<? extends MedusaEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_FLAGS_ID, (byte)0);
      builder.define(BLINDFOLD_SEALED, true);
      builder.define(CROUCH_POSE, false);
      builder.define(EYES_RELEASED, false);
   }

   @Override
   protected PathNavigation createNavigation(Level level) {
      return new WallClimberNavigation(this, level);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
      SpawnGroupData spawnData = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      this.applyMedusaBaseAdjustments();
      return spawnData;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(BLINDFOLD_SEALED, tag.getBoolean("MedusaBlindfoldSealed"));
      this.entityData.set(CROUCH_POSE, tag.getBoolean("MedusaCrouchPose"));
      this.entityData.set(EYES_RELEASED, tag.getBoolean("MedusaEyesReleased"));
      if (tag.hasUUID(TAG_PEGASUS_UUID)) {
         this.getPersistentData().putUUID(TAG_PEGASUS_UUID, tag.getUUID(TAG_PEGASUS_UUID));
      } else {
         this.getPersistentData().remove(TAG_PEGASUS_UUID);
      }
      this.getPersistentData().putLong(TAG_BELLEROPHON_RIDE_UNTIL, tag.getLong(TAG_BELLEROPHON_RIDE_UNTIL));
      this.applyMedusaBaseAdjustments();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("MedusaBlindfoldSealed", this.isBlindfoldSealed());
      tag.putBoolean("MedusaCrouchPose", this.isCrouchPose());
      tag.putBoolean("MedusaEyesReleased", this.isEyesReleased());
      if (this.getPersistentData().hasUUID(TAG_PEGASUS_UUID)) {
         tag.putUUID(TAG_PEGASUS_UUID, this.getPersistentData().getUUID(TAG_PEGASUS_UUID));
      }
      tag.putLong(TAG_BELLEROPHON_RIDE_UNTIL, this.getPersistentData().getLong(TAG_BELLEROPHON_RIDE_UNTIL));
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (amount <= 0.0F) {
         return false;
      }
      if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && MedusaCombatHelper.tryBlock(this, source)) {
         return false;
      }
      if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && this.isBellerophonMountedActive()) {
         amount *= 0.6F;
      }
      return super.hurt(source, amount);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         this.setClimbing(this.horizontalCollision);
         if (this.isClimbing() && this.getTarget() != null && this.getTarget().getY() > this.getY()) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, Math.max(motion.y, 0.22), motion.z);
            this.hasImpulse = true;
         }
      }
   }

   @Override
   public boolean onClimbable() {
      return this.isClimbing();
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      if (this.isCrouchPose()) {
         return animations.actionAnimation("bend_over").orElse(null);
      }
      return super.getLoopAnimationOverride(animations, moving);
   }

   public boolean isBlindfoldSealed() {
      return this.entityData.get(BLINDFOLD_SEALED);
   }

   public boolean isClimbing() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setClimbing(boolean climbing) {
      byte flags = this.entityData.get(DATA_FLAGS_ID);
      if (climbing) {
         flags = (byte)(flags | 1);
      } else {
         flags = (byte)(flags & -2);
      }
      this.entityData.set(DATA_FLAGS_ID, flags);
   }

   public void setBlindfoldSealed(boolean sealed) {
      this.entityData.set(BLINDFOLD_SEALED, sealed);
   }

   public boolean isCrouchPose() {
      return this.entityData.get(CROUCH_POSE);
   }

   public void setCrouchPose(boolean crouchPose) {
      this.entityData.set(CROUCH_POSE, crouchPose);
   }

   public boolean isEyesReleased() {
      return this.entityData.get(EYES_RELEASED);
   }

   public void setEyesReleased(boolean eyesReleased) {
      this.entityData.set(EYES_RELEASED, eyesReleased);
   }

   public void setPegasusUuid(@Nullable UUID uuid) {
      if (uuid == null) {
         this.getPersistentData().remove(TAG_PEGASUS_UUID);
      } else {
         this.getPersistentData().putUUID(TAG_PEGASUS_UUID, uuid);
      }
   }

   @Nullable
   public UUID getPegasusUuid() {
      return this.getPersistentData().hasUUID(TAG_PEGASUS_UUID) ? this.getPersistentData().getUUID(TAG_PEGASUS_UUID) : null;
   }

   @Nullable
   public MedusaPegasusEntity getPegasus() {
      UUID uuid = this.getPegasusUuid();
      if (uuid == null || !(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
         return null;
      }
      Entity entity = serverLevel.getEntity(uuid);
      return entity instanceof MedusaPegasusEntity pegasus ? pegasus : null;
   }

   public void clearPegasusReference() {
      this.setPegasusUuid(null);
   }

   public boolean isRidingPegasus() {
      return this.getVehicle() instanceof MedusaPegasusEntity;
   }

   public boolean isBellerophonMountedActive() {
      return this.getPersistentData().getLong(TAG_BELLEROPHON_RIDE_UNTIL) > this.level().getGameTime() && this.isRidingPegasus();
   }

   public void setBellerophonRideUntil(long rideUntil) {
      this.getPersistentData().putLong(TAG_BELLEROPHON_RIDE_UNTIL, rideUntil);
   }

   public long getBellerophonRideUntil() {
      return this.getPersistentData().getLong(TAG_BELLEROPHON_RIDE_UNTIL);
   }

   private void applyMedusaBaseAdjustments() {
      if (this.getAttribute(Attributes.ARMOR) != null) {
         this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
      }
   }
}

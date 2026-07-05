package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UbwSkyGearEntity extends Entity {
   private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(UbwSkyGearEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(UbwSkyGearEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(UbwSkyGearEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> ROTATION_SPEED = SynchedEntityData.defineId(UbwSkyGearEntity.class, EntityDataSerializers.FLOAT);
   @Nullable
   private UUID ownerUUID;

   public UbwSkyGearEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public UbwSkyGearEntity(Level level, double x, double y, double z, int variant, float scale, int duration) {
      this(level, x, y, z, variant, scale, duration, null);
   }

   public UbwSkyGearEntity(Level level, double x, double y, double z, int variant, float scale, int duration, @Nullable UUID ownerUUID) {
      this(ModEntities.UBW_SKY_GEAR.get(), level);
      this.setPos(x, y, z);
      this.entityData.set(VARIANT, variant);
      this.entityData.set(SCALE, scale);
      this.entityData.set(DURATION, duration);
      this.entityData.set(ROTATION_SPEED, 0.0F);
      this.ownerUUID = ownerUUID;
      this.setYRot(level.random.nextFloat() * 360.0F);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(VARIANT, 0);
      builder.define(SCALE, 6.0F);
      builder.define(DURATION, 20 * 20);
      builder.define(ROTATION_SPEED, 0.8F);
   }

   public int getVariant() {
      return this.entityData.get(VARIANT);
   }

   public float getGearScale() {
      return this.entityData.get(SCALE);
   }

   public float getRotationSpeed() {
      return this.entityData.get(ROTATION_SPEED);
   }

   public boolean isOwnedBy(UUID ownerUUID) {
      return ownerUUID.equals(this.ownerUUID);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      int duration = this.entityData.get(DURATION);
      if (duration > 0 && this.tickCount >= duration) {
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(VARIANT, tag.getInt("Variant"));
      this.entityData.set(SCALE, tag.getFloat("Scale"));
      this.entityData.set(DURATION, tag.getInt("Duration"));
      this.entityData.set(ROTATION_SPEED, tag.getFloat("RotationSpeed"));
      if (tag.hasUUID("Owner")) {
         this.ownerUUID = tag.getUUID("Owner");
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("Variant", this.entityData.get(VARIANT));
      tag.putFloat("Scale", this.entityData.get(SCALE));
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putFloat("RotationSpeed", this.entityData.get(ROTATION_SPEED));
      if (this.ownerUUID != null) {
         tag.putUUID("Owner", this.ownerUUID);
      }
   }
}

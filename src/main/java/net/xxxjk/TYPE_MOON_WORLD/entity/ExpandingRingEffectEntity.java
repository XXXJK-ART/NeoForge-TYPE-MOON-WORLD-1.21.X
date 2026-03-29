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

public class ExpandingRingEffectEntity extends Entity {
   private static final EntityDataAccessor<Float> START_RADIUS = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_RADIUS = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> THICKNESS = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> BASE_ALPHA = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> UPWARD_SPEED = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TILT_DEGREES = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> YAW_DEGREES = SynchedEntityData.defineId(ExpandingRingEffectEntity.class, EntityDataSerializers.FLOAT);

   public ExpandingRingEffectEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public ExpandingRingEffectEntity(
      Level level, double x, double y, double z, float startRadius, float endRadius, float thickness, int duration, int color, float baseAlpha, float upwardSpeed
   ) {
      this(level, x, y, z, startRadius, endRadius, thickness, duration, color, baseAlpha, upwardSpeed, 0.0F, 0.0F);
   }

   public ExpandingRingEffectEntity(
      Level level,
      double x,
      double y,
      double z,
      float startRadius,
      float endRadius,
      float thickness,
      int duration,
      int color,
      float baseAlpha,
      float upwardSpeed,
      float tiltDegrees,
      float yawDegrees
   ) {
      this(ModEntities.EXPANDING_RING_EFFECT.get(), level);
      this.setPos(x, y, z);
      this.entityData.set(START_RADIUS, startRadius);
      this.entityData.set(END_RADIUS, endRadius);
      this.entityData.set(THICKNESS, thickness);
      this.entityData.set(DURATION, Math.max(1, duration));
      this.entityData.set(COLOR, color);
      this.entityData.set(BASE_ALPHA, baseAlpha);
      this.entityData.set(UPWARD_SPEED, upwardSpeed);
      this.entityData.set(TILT_DEGREES, tiltDegrees);
      this.entityData.set(YAW_DEGREES, yawDegrees);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(START_RADIUS, 0.0F);
      builder.define(END_RADIUS, 2.0F);
      builder.define(THICKNESS, 0.18F);
      builder.define(DURATION, 20);
      builder.define(COLOR, 16777215);
      builder.define(BASE_ALPHA, 0.8F);
      builder.define(UPWARD_SPEED, 0.0F);
      builder.define(TILT_DEGREES, 0.0F);
      builder.define(YAW_DEGREES, 0.0F);
   }

   public float getCurrentRadius(float partialTick) {
      float progress = Math.min(1.0F, (this.tickCount + partialTick) / Math.max(1.0F, this.entityData.get(DURATION)));
      return this.entityData.get(START_RADIUS) + (this.entityData.get(END_RADIUS) - this.entityData.get(START_RADIUS)) * progress;
   }

   public float getThickness() {
      return this.entityData.get(THICKNESS);
   }

   public int getColor() {
      return this.entityData.get(COLOR);
   }

   public float getCurrentAlpha(float partialTick) {
      float progress = Math.min(1.0F, (this.tickCount + partialTick) / Math.max(1.0F, this.entityData.get(DURATION)));
      return this.entityData.get(BASE_ALPHA) * (1.0F - progress);
   }

   public float getTiltDegrees() {
      return this.entityData.get(TILT_DEGREES);
   }

   public float getYawDegrees() {
      return this.entityData.get(YAW_DEGREES);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         float up = this.entityData.get(UPWARD_SPEED);
         if (Math.abs(up) > 1.0E-4F) {
            this.setPos(this.getX(), this.getY() + up, this.getZ());
         }

         if (this.tickCount >= this.entityData.get(DURATION)) {
            this.discard();
         }
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(START_RADIUS, tag.getFloat("StartRadius"));
      this.entityData.set(END_RADIUS, tag.getFloat("EndRadius"));
      this.entityData.set(THICKNESS, tag.getFloat("Thickness"));
      this.entityData.set(DURATION, Math.max(1, tag.getInt("Duration")));
      this.entityData.set(COLOR, tag.getInt("Color"));
      this.entityData.set(BASE_ALPHA, tag.getFloat("BaseAlpha"));
      this.entityData.set(UPWARD_SPEED, tag.getFloat("UpwardSpeed"));
      this.entityData.set(TILT_DEGREES, tag.contains("TiltDegrees") ? tag.getFloat("TiltDegrees") : 0.0F);
      this.entityData.set(YAW_DEGREES, tag.contains("YawDegrees") ? tag.getFloat("YawDegrees") : 0.0F);
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("StartRadius", this.entityData.get(START_RADIUS));
      tag.putFloat("EndRadius", this.entityData.get(END_RADIUS));
      tag.putFloat("Thickness", this.entityData.get(THICKNESS));
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putInt("Color", this.entityData.get(COLOR));
      tag.putFloat("BaseAlpha", this.entityData.get(BASE_ALPHA));
      tag.putFloat("UpwardSpeed", this.entityData.get(UPWARD_SPEED));
      tag.putFloat("TiltDegrees", this.entityData.get(TILT_DEGREES));
      tag.putFloat("YawDegrees", this.entityData.get(YAW_DEGREES));
   }
}

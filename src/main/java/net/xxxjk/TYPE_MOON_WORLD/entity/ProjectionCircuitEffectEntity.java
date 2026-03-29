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

public class ProjectionCircuitEffectEntity extends Entity {
   private static final EntityDataAccessor<Float> START_RADIUS = SynchedEntityData.defineId(ProjectionCircuitEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_RADIUS = SynchedEntityData.defineId(ProjectionCircuitEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> ALPHA = SynchedEntityData.defineId(ProjectionCircuitEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ProjectionCircuitEffectEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(ProjectionCircuitEffectEntity.class, EntityDataSerializers.INT);

   public ProjectionCircuitEffectEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public ProjectionCircuitEffectEntity(Level level, double x, double y, double z, float startRadius, float endRadius, float alpha, int duration, int color) {
      this(ModEntities.PROJECTION_CIRCUIT_EFFECT.get(), level);
      this.setPos(x, y, z);
      this.entityData.set(START_RADIUS, startRadius);
      this.entityData.set(END_RADIUS, endRadius);
      this.entityData.set(ALPHA, alpha);
      this.entityData.set(DURATION, Math.max(1, duration));
      this.entityData.set(COLOR, color);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(START_RADIUS, 0.2F);
      builder.define(END_RADIUS, 1.0F);
      builder.define(ALPHA, 0.7F);
      builder.define(DURATION, 12);
      builder.define(COLOR, 0x5DB9FF);
   }

   public float getCurrentRadius(float partialTick) {
      float progress = Math.min(1.0F, (this.tickCount + partialTick) / Math.max(1.0F, this.entityData.get(DURATION)));
      return this.entityData.get(START_RADIUS) + (this.entityData.get(END_RADIUS) - this.entityData.get(START_RADIUS)) * progress;
   }

   public float getCurrentAlpha(float partialTick) {
      float progress = Math.min(1.0F, (this.tickCount + partialTick) / Math.max(1.0F, this.entityData.get(DURATION)));
      return this.entityData.get(ALPHA) * (1.0F - progress);
   }

   public int getColor() {
      return this.entityData.get(COLOR);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.tickCount >= this.entityData.get(DURATION)) {
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(START_RADIUS, tag.getFloat("StartRadius"));
      this.entityData.set(END_RADIUS, tag.getFloat("EndRadius"));
      this.entityData.set(ALPHA, tag.getFloat("Alpha"));
      this.entityData.set(DURATION, Math.max(1, tag.getInt("Duration")));
      this.entityData.set(COLOR, tag.getInt("Color"));
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("StartRadius", this.entityData.get(START_RADIUS));
      tag.putFloat("EndRadius", this.entityData.get(END_RADIUS));
      tag.putFloat("Alpha", this.entityData.get(ALPHA));
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putInt("Color", this.entityData.get(COLOR));
   }
}

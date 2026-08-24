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

public class BloodstainEffectEntity extends Entity {
   private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(BloodstainEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(BloodstainEffectEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> ROTATION = SynchedEntityData.defineId(BloodstainEffectEntity.class, EntityDataSerializers.FLOAT);

   public BloodstainEffectEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public BloodstainEffectEntity(Level level, double x, double y, double z, float scale, int duration, float rotation) {
      this(ModEntities.BLOODSTAIN_EFFECT.get(), level);
      this.setPos(x, y, z);
      this.entityData.set(SCALE, scale);
      this.entityData.set(DURATION, Math.max(1, duration));
      this.entityData.set(ROTATION, rotation);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(SCALE, 0.55F);
      builder.define(DURATION, 600);
      builder.define(ROTATION, 0.0F);
   }

   public float getScale() {
      return this.entityData.get(SCALE);
   }

   public float getRotation() {
      return this.entityData.get(ROTATION);
   }

   public float getCurrentAlpha(float partialTick) {
      float duration = Math.max(1.0F, this.entityData.get(DURATION));
      float remaining = Math.max(0.0F, 1.0F - (this.tickCount + partialTick) / duration);
      return Math.min(1.0F, remaining * 1.25F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.tickCount >= this.entityData.get(DURATION)) {
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(SCALE, tag.getFloat("Scale"));
      this.entityData.set(DURATION, Math.max(1, tag.getInt("Duration")));
      this.entityData.set(ROTATION, tag.getFloat("Rotation"));
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("Scale", this.entityData.get(SCALE));
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putFloat("Rotation", this.entityData.get(ROTATION));
   }
}

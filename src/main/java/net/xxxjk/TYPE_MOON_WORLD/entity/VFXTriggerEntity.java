package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

public class VFXTriggerEntity extends Entity {
   private static final EntityDataAccessor<String> EFFECT_ID = SynchedEntityData.defineId(VFXTriggerEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(VFXTriggerEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> SEED_LOW = SynchedEntityData.defineId(VFXTriggerEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> SEED_HIGH = SynchedEntityData.defineId(VFXTriggerEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(VFXTriggerEntity.class, EntityDataSerializers.INT);

   public VFXTriggerEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public VFXTriggerEntity(Level level, String effectId, double x, double y, double z, int durationTicks, long seed) {
      this(level, effectId, x, y, z, durationTicks, seed, -1);
   }

   public VFXTriggerEntity(Level level, String effectId, double x, double y, double z, int durationTicks, long seed, int targetId) {
      this(ModEntities.VFX_TRIGGER.get(), level);
      this.setPos(x, y, z);
      this.setEffectId(effectId);
      this.setDurationTicks(durationTicks);
      this.setSeed(seed);
      this.setTargetId(targetId);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(EFFECT_ID, "");
      builder.define(DURATION, 20);
      builder.define(SEED_LOW, 0);
      builder.define(SEED_HIGH, 0);
      builder.define(TARGET_ID, -1);
   }

   public String getEffectId() {
      return this.entityData.get(EFFECT_ID);
   }

   public int getDurationTicks() {
      return this.entityData.get(DURATION);
   }

   public long getSeed() {
      return ((long)this.entityData.get(SEED_HIGH) << 32) | (this.entityData.get(SEED_LOW) & 0xFFFFFFFFL);
   }

   public int getTargetId() {
      return this.entityData.get(TARGET_ID);
   }

   private void setEffectId(String effectId) {
      this.entityData.set(EFFECT_ID, effectId == null ? "" : effectId);
   }

   private void setDurationTicks(int durationTicks) {
      this.entityData.set(DURATION, Math.max(1, durationTicks));
   }

   private void setSeed(long seed) {
      this.entityData.set(SEED_LOW, (int)seed);
      this.entityData.set(SEED_HIGH, (int)(seed >>> 32));
   }

   private void setTargetId(int targetId) {
      this.entityData.set(TARGET_ID, targetId);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.tickCount > this.getDurationTicks()) {
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.setEffectId(tag.getString("EffectId"));
      this.setDurationTicks(tag.getInt("Duration"));
      this.setSeed(tag.getLong("Seed"));
      this.setTargetId(tag.getInt("TargetId"));
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putString("EffectId", this.getEffectId());
      tag.putInt("Duration", this.getDurationTicks());
      tag.putLong("Seed", this.getSeed());
      tag.putInt("TargetId", this.getTargetId());
   }
}

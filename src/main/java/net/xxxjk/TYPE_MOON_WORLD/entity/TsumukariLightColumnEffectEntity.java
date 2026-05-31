package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

public class TsumukariLightColumnEffectEntity extends Entity {
   private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(TsumukariLightColumnEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(TsumukariLightColumnEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> ALPHA = SynchedEntityData.defineId(TsumukariLightColumnEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(TsumukariLightColumnEffectEntity.class, EntityDataSerializers.INT);

   public TsumukariLightColumnEffectEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public TsumukariLightColumnEffectEntity(Level level, double x, double y, double z, float height, float width, float alpha, int duration) {
      this(ModEntities.TSUMUKARI_LIGHT_COLUMN_EFFECT.get(), level);
      this.setPos(x, y, z);
      this.entityData.set(HEIGHT, height);
      this.entityData.set(WIDTH, width);
      this.entityData.set(ALPHA, alpha);
      this.entityData.set(DURATION, Math.max(1, duration));
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(HEIGHT, 4.0F);
      builder.define(WIDTH, 0.2F);
      builder.define(ALPHA, 0.82F);
      builder.define(DURATION, 20);
   }

   public float getHeightValue() {
      return this.entityData.get(HEIGHT);
   }

   public float getWidthValue() {
      return this.entityData.get(WIDTH);
   }

   public float getCurrentAlpha(float partialTick) {
      float progress = Math.min(1.0F, (this.tickCount + partialTick) / Math.max(1.0F, this.entityData.get(DURATION)));
      return this.entityData.get(ALPHA) * (1.0F - progress * 0.8F);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide() && this.tickCount % 3 == 0) {
         this.level().addParticle(
            ParticleTypes.END_ROD,
            this.getX(),
            this.getY() + this.getHeightValue() * (0.2 + this.random.nextDouble() * 0.6),
            this.getZ(),
            0.0,
            0.02,
            0.0
         );
      }

      if (!this.level().isClientSide() && this.tickCount >= this.entityData.get(DURATION)) {
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(HEIGHT, tag.getFloat("Height"));
      this.entityData.set(WIDTH, tag.getFloat("Width"));
      this.entityData.set(ALPHA, tag.getFloat("Alpha"));
      this.entityData.set(DURATION, Math.max(1, tag.getInt("Duration")));
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("Height", this.entityData.get(HEIGHT));
      tag.putFloat("Width", this.entityData.get(WIDTH));
      tag.putFloat("Alpha", this.entityData.get(ALPHA));
      tag.putInt("Duration", this.entityData.get(DURATION));
   }
}

package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

public class GravityFieldShellEffectEntity extends Entity {
   private static final EntityDataAccessor<Float> RADIUS_XZ = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> RADIUS_Y = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> ALPHA = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.INT);

   public GravityFieldShellEffectEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public GravityFieldShellEffectEntity(Level level, double x, double y, double z, float radiusXZ, float radiusY, float alpha, int duration) {
      this(ModEntities.GRAVITY_FIELD_SHELL_EFFECT.get(), level);
      this.setPos(x, y, z);
      this.entityData.set(RADIUS_XZ, radiusXZ);
      this.entityData.set(RADIUS_Y, radiusY);
      this.entityData.set(ALPHA, alpha);
      this.entityData.set(DURATION, Math.max(1, duration));
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(RADIUS_XZ, 1.2F);
      builder.define(RADIUS_Y, 1.5F);
      builder.define(ALPHA, 0.45F);
      builder.define(DURATION, 20);
   }

   public float getRadiusXZ() {
      return this.entityData.get(RADIUS_XZ);
   }

   public float getRadiusY() {
      return this.entityData.get(RADIUS_Y);
   }

   public float getCurrentAlpha(float partialTick) {
      float progress = Math.min(1.0F, (this.tickCount + partialTick) / Math.max(1.0F, this.entityData.get(DURATION)));
      return this.entityData.get(ALPHA) * (1.0F - progress * 0.55F);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide() && this.tickCount % 2 == 0) {
         spawnAmbientParticles();
      }
      if (!this.level().isClientSide() && this.tickCount >= this.entityData.get(DURATION)) {
         this.discard();
      }
   }

   private void spawnAmbientParticles() {
      float radiusXZ = this.getRadiusXZ();
      float radiusY = this.getRadiusY();
      float alpha = this.getCurrentAlpha(0.0F);
      if (alpha <= 0.03F) {
         return;
      }

      double angle = this.tickCount * 0.22 + this.random.nextDouble() * Math.PI * 2.0;
      double orbitRadius = radiusXZ * (0.52 + this.random.nextDouble() * 0.28);
      double x = this.getX() + Math.cos(angle) * orbitRadius;
      double z = this.getZ() + Math.sin(angle) * orbitRadius;
      double y = this.getY() + radiusY * (0.3 + this.random.nextDouble() * 0.5);
      this.level().addParticle(ParticleTypes.SQUID_INK, x, y, z, 0.0, -0.035, 0.0);

      if (this.random.nextFloat() < 0.22F) {
         double innerX = this.getX() + (this.random.nextDouble() - 0.5) * radiusXZ * 0.55;
         double innerZ = this.getZ() + (this.random.nextDouble() - 0.5) * radiusXZ * 0.55;
         double innerY = this.getY() + radiusY * (0.18 + this.random.nextDouble() * 0.32);
         this.level().addParticle(ParticleTypes.PORTAL, innerX, innerY, innerZ, 0.0, -0.02, 0.0);
      }

      if (this.random.nextFloat() < 0.12F) {
         this.level().addParticle(
            ParticleTypes.FALLING_OBSIDIAN_TEAR,
            this.getX() + (this.random.nextDouble() - 0.5) * radiusXZ * 0.42,
            this.getY() + radiusY * (0.55 + this.random.nextDouble() * 0.2),
            this.getZ() + (this.random.nextDouble() - 0.5) * radiusXZ * 0.42,
            0.0,
            -0.05,
            0.0
         );
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(RADIUS_XZ, tag.getFloat("RadiusXZ"));
      this.entityData.set(RADIUS_Y, tag.getFloat("RadiusY"));
      this.entityData.set(ALPHA, tag.getFloat("Alpha"));
      this.entityData.set(DURATION, Math.max(1, tag.getInt("Duration")));
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("RadiusXZ", this.entityData.get(RADIUS_XZ));
      tag.putFloat("RadiusY", this.entityData.get(RADIUS_Y));
      tag.putFloat("Alpha", this.entityData.get(ALPHA));
      tag.putInt("Duration", this.entityData.get(DURATION));
   }
}

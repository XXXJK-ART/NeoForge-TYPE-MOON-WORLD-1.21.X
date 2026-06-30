package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

public class GravityFieldShellEffectEntity extends Entity {
   private static final EntityDataAccessor<Float> RADIUS_XZ = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> RADIUS_Y = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> ALPHA = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> REVEAL_TICKS = SynchedEntityData.defineId(GravityFieldShellEffectEntity.class, EntityDataSerializers.INT);

   public GravityFieldShellEffectEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public GravityFieldShellEffectEntity(Level level, double x, double y, double z, float radiusXZ, float radiusY, float alpha, int duration) {
      this(level, x, y, z, radiusXZ, radiusY, alpha, duration, 0.15F, 0.05F, 0.21F, 0);
   }

   public GravityFieldShellEffectEntity(
      Level level,
      double x,
      double y,
      double z,
      float radiusXZ,
      float radiusY,
      float alpha,
      int duration,
      float red,
      float green,
      float blue,
      int revealTicks
   ) {
      this(ModEntities.GRAVITY_FIELD_SHELL_EFFECT.get(), level);
      this.setPos(x, y, z);
      this.entityData.set(RADIUS_XZ, radiusXZ);
      this.entityData.set(RADIUS_Y, radiusY);
      this.entityData.set(ALPHA, alpha);
      this.entityData.set(DURATION, Math.max(1, duration));
      this.entityData.set(COLOR_R, red);
      this.entityData.set(COLOR_G, green);
      this.entityData.set(COLOR_B, blue);
      this.entityData.set(REVEAL_TICKS, Math.max(0, revealTicks));
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(RADIUS_XZ, 1.2F);
      builder.define(RADIUS_Y, 1.5F);
      builder.define(ALPHA, 0.45F);
      builder.define(DURATION, 20);
      builder.define(COLOR_R, 0.15F);
      builder.define(COLOR_G, 0.05F);
      builder.define(COLOR_B, 0.21F);
      builder.define(REVEAL_TICKS, 0);
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

   public float getColorR() {
      return this.entityData.get(COLOR_R);
   }

   public float getColorG() {
      return this.entityData.get(COLOR_G);
   }

   public float getColorB() {
      return this.entityData.get(COLOR_B);
   }

   public float getRevealFraction(float partialTick) {
      int revealTicks = this.entityData.get(REVEAL_TICKS);
      if (revealTicks <= 0) {
         return 1.0F;
      }
      return Math.min(1.0F, (this.tickCount + partialTick) / Math.max(1.0F, revealTicks));
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
      float reveal = this.getRevealFraction(0.0F);
      if (alpha <= 0.03F) {
         return;
      }

      spawnShellRibParticles(radiusXZ, radiusY, alpha, reveal);
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

   private void spawnShellRibParticles(float radiusXZ, float radiusY, float alpha, float reveal) {
      DustParticleOptions ribParticle = new DustParticleOptions(
         new Vector3f(
            Math.min(1.0F, this.getColorR() * 1.15F),
            Math.max(0.0F, this.getColorG() * 0.65F),
            Math.max(0.0F, this.getColorB() * 0.82F)
         ),
         1.2F + alpha * 0.35F
      );
      int ribCount = Math.max(7, Math.round(radiusXZ * 0.75F));
      for (int rib = 0; rib < ribCount; rib++) {
         double angle = (Math.PI * 2.0 * rib) / ribCount + this.tickCount * 0.015 + (rib % 2 == 0 ? 0.0 : Math.PI / ribCount);
         for (double t = 0.02; t <= reveal; t += 0.14) {
            double elevation = t * (Math.PI * 0.5);
            double localRadius = Math.cos(elevation) * radiusXZ;
            double x = this.getX() + Math.cos(angle) * localRadius;
            double y = this.getY() + Math.sin(elevation) * radiusY;
            double z = this.getZ() + Math.sin(angle) * localRadius;
            this.level().addParticle(ribParticle, x, y, z, 0.0, 0.0, 0.0);
            if ((rib + this.tickCount) % 3 == 0) {
               this.level().addParticle(ParticleTypes.FALLING_OBSIDIAN_TEAR, x, y, z, 0.0, -0.01, 0.0);
            }
         }
      }

      spawnShellBandParticles(radiusXZ, radiusY, alpha, reveal, ribParticle);

      if (this.random.nextFloat() < 0.65F) {
         int arcCount = Math.max(4, ribCount / 2);
         for (int arc = 0; arc < arcCount; arc++) {
            double baseAngle = this.random.nextDouble() * Math.PI * 2.0;
            double drift = (this.random.nextDouble() - 0.5) * 0.18;
            for (double t = 0.18; t <= reveal; t += 0.18) {
               double elevation = t * (Math.PI * 0.5);
               double arcRadius = Math.cos(elevation) * radiusXZ;
               double x = this.getX() + Math.cos(baseAngle + drift * t * 6.0) * arcRadius;
               double y = this.getY() + Math.sin(elevation) * radiusY;
               double z = this.getZ() + Math.sin(baseAngle + drift * t * 6.0) * arcRadius;
               this.level().addParticle(ribParticle, x, y, z, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private void spawnShellBandParticles(float radiusXZ, float radiusY, float alpha, float reveal, DustParticleOptions ribParticle) {
      int bandCount = Math.max(3, Math.round(radiusY * 0.14F));
      int segmentCount = Math.max(8, Math.round(radiusXZ * 0.55F));
      for (int band = 0; band < bandCount; band++) {
         float t = Math.min(reveal, 0.18F + band * 0.18F);
         if (t >= reveal) {
            break;
         }
         double elevation = t * (Math.PI * 0.5);
         double y = this.getY() + Math.sin(elevation) * radiusY;
         double localRadius = Math.cos(elevation) * radiusXZ;
         double offset = this.tickCount * 0.018 * (band % 2 == 0 ? 1.0 : -1.0);
         for (int segment = 0; segment < segmentCount; segment++) {
            if ((segment + band) % 2 != 0) {
               continue;
            }
            double baseAngle = (Math.PI * 2.0 * segment) / segmentCount + offset;
            double arcSpan = Math.PI * 2.0 / segmentCount * 0.42;
            for (double u = -arcSpan; u <= arcSpan; u += arcSpan * 0.5) {
               double x = this.getX() + Math.cos(baseAngle + u) * localRadius;
               double z = this.getZ() + Math.sin(baseAngle + u) * localRadius;
               this.level().addParticle(ribParticle, x, y, z, 0.0, 0.0, 0.0);
               if ((segment + this.tickCount) % 5 == 0) {
                  this.level().addParticle(ParticleTypes.DRIPPING_LAVA, x, y, z, 0.0, -0.005, 0.0);
               }
            }
         }
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(RADIUS_XZ, tag.getFloat("RadiusXZ"));
      this.entityData.set(RADIUS_Y, tag.getFloat("RadiusY"));
      this.entityData.set(ALPHA, tag.getFloat("Alpha"));
      this.entityData.set(DURATION, Math.max(1, tag.getInt("Duration")));
      this.entityData.set(COLOR_R, tag.contains("ColorR") ? tag.getFloat("ColorR") : 0.15F);
      this.entityData.set(COLOR_G, tag.contains("ColorG") ? tag.getFloat("ColorG") : 0.05F);
      this.entityData.set(COLOR_B, tag.contains("ColorB") ? tag.getFloat("ColorB") : 0.21F);
      this.entityData.set(REVEAL_TICKS, Math.max(0, tag.getInt("RevealTicks")));
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("RadiusXZ", this.entityData.get(RADIUS_XZ));
      tag.putFloat("RadiusY", this.entityData.get(RADIUS_Y));
      tag.putFloat("Alpha", this.entityData.get(ALPHA));
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putFloat("ColorR", this.entityData.get(COLOR_R));
      tag.putFloat("ColorG", this.entityData.get(COLOR_G));
      tag.putFloat("ColorB", this.entityData.get(COLOR_B));
      tag.putInt("RevealTicks", this.entityData.get(REVEAL_TICKS));
   }
}

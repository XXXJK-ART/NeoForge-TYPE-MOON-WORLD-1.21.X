package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class TsumukariWaveProjectileEntity extends Projectile {
   private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(TsumukariWaveProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(TsumukariWaveProjectileEntity.class, EntityDataSerializers.INT);
   public final java.util.List<Vec3> tracePos = new LinkedList<>();
   private final Set<Integer> hitEntityIds = new HashSet<>();
   private int maxLifetime = 26;
   private boolean initialColumnSpawned = false;

   public TsumukariWaveProjectileEntity(EntityType<? extends Projectile> entityType, Level level) {
      super(entityType, level);
      this.noPhysics = true;
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public TsumukariWaveProjectileEntity(Level level, LivingEntity shooter, int charge, int color) {
      this(ModEntities.TSUMUKARI_WAVE_PROJECTILE.get(), level);
      this.setOwner(shooter);
      this.entityData.set(CHARGE, charge);
      this.entityData.set(COLOR, color);
      this.setPos(shooter.getX(), shooter.getEyeY() - 0.4, shooter.getZ());
      this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 2.0F, 0.0F);
      this.maxLifetime = Math.max(10, (int)Math.ceil(this.getSlashMaxDistance() / 2.0));
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(CHARGE, 0);
      builder.define(COLOR, 0xD63A3A);
   }

   public int getCharge() {
      return this.entityData.get(CHARGE);
   }

   public int getVisualColorRgb() {
      return this.entityData.get(COLOR);
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 prevPos = new Vec3(this.getX(), this.getY(), this.getZ());
      Vec3 motion = this.getDeltaMovement();
      this.setPos(prevPos.add(motion));
      if (this.level().isClientSide()) {
         this.captureTrace();
      } else {
         if (!this.initialColumnSpawned) {
            this.spawnLightColumn(prevPos, this.maxLifetime + 2);
            this.initialColumnSpawned = true;
         }

         if (this.tickCount > 0) {
            this.spawnLightColumn(this.position(), 18 + this.getCharge() / 12);
         }

         if (this.tickCount == this.maxLifetime - 1) {
            this.spawnFinisherColumns(this.position(), motion);
         }

         if (this.tickCount >= this.maxLifetime) {
            this.discard();
         }
      }
   }

   private void captureTrace() {
      Vec3 pos = this.position();
      if (this.tracePos.isEmpty() || this.tracePos.get(this.tracePos.size() - 1).distanceToSqr(pos) >= 0.01) {
         this.tracePos.add(pos);
         if (this.tracePos.size() > 90) {
            this.tracePos.remove(0);
         }
      }
   }

   private void spawnLightColumn(Vec3 center, int duration) {
      if (!(this.level() instanceof ServerLevel serverLevel)) {
         return;
      }

      double progress = Math.min(1.0, this.tickCount * 2.0 / this.getSlashMaxDistance());
      double gaussian = Math.exp(-Math.pow((progress - 0.5) / 0.22, 2.0) * 0.5);
      float minHeight = Math.max(6.0F, this.getSlashMaxHeight() * 0.22F);
      float maxHeight = this.getSlashMaxHeight() * 0.95F;
      float height = (float)(minHeight + (maxHeight - minHeight) * gaussian);
      float width = Math.max(0.22F, this.getSlashWidth() * 0.04F);
      double baseY = resolveColumnBaseY(serverLevel, center);
      serverLevel.addFreshEntity(new TsumukariLightColumnEffectEntity(serverLevel, center.x, baseY, center.z, height, width, 0.88F, duration));
      serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, baseY + height * 0.5, center.z, 3, width * 0.3, height * 0.22, width * 0.3, 0.01);
   }

   private void spawnFinisherColumns(Vec3 center, Vec3 motion) {
      if (!(this.level() instanceof ServerLevel serverLevel)) {
         return;
      }

      Vec3 dir = motion.lengthSqr() < 1.0E-6 ? this.getDeltaMovement() : motion;
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-6) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      } else {
         horizontal = horizontal.normalize();
      }

      float baseHeight = Math.max(10.0F, this.getSlashMaxHeight() * 0.38F);
      float width = Math.max(0.18F, this.getSlashWidth() * 0.03F);
      double[] offsets = new double[]{0.8, 1.5, 2.1};
      float[] heights = new float[]{baseHeight * 0.7F, baseHeight * 0.42F, baseHeight * 0.2F};

      for (int i = 0; i < offsets.length; i++) {
         Vec3 tipPos = center.add(horizontal.scale(offsets[i]));
         double baseY = resolveColumnBaseY(serverLevel, tipPos);
         serverLevel.addFreshEntity(new TsumukariLightColumnEffectEntity(serverLevel, tipPos.x, baseY, tipPos.z, heights[i], width, 0.82F - i * 0.18F, 12 - i * 2));
      }
   }

   private double resolveColumnBaseY(ServerLevel level, Vec3 center) {
      BlockPos samplePos = BlockPos.containing(center);
      int groundY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, samplePos.getX(), samplePos.getZ());
      return groundY <= level.getMinBuildHeight() ? center.y - 1.0 : groundY + 0.04;
   }

   private int getSlashMaxDistance() {
      return Math.max(20, Math.min(300, (int)(this.getCharge() * 3.0)));
   }

   private float getSlashWidth() {
      return Math.min(10.0F, 1.0F + this.getCharge() / 10.0F);
   }

   private float getSlashMaxHeight() {
      return Math.min(100.0F, Math.max(5.0F, (float)this.getCharge()));
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(CHARGE, tag.getInt("Charge"));
      this.entityData.set(COLOR, tag.contains("Color") ? tag.getInt("Color") : 0xD63A3A);
      this.maxLifetime = tag.contains("Lifetime") ? tag.getInt("Lifetime") : this.maxLifetime;
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("Charge", this.getCharge());
      tag.putInt("Color", this.getVisualColorRgb());
      tag.putInt("Lifetime", this.maxLifetime);
   }
}

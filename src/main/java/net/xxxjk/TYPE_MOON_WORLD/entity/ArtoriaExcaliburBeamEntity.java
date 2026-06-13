package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class ArtoriaExcaliburBeamEntity extends Entity {
   private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.INT);
   private static final double LENGTH = 50.0;
   private static final double HALF_WIDTH = 7.5;
   private static final double HALF_HEIGHT = 4.5;
   private static final double POINT_BLANK_DAMAGE_LENGTH = 3.0;
   private static final int DAMAGE_INTERVAL = 5;
   private static final float DAMAGE_PER_PULSE = 4000.0F / (150.0F / DAMAGE_INTERVAL);
   private static final float CRATER_DAMAGE = 180.0F;
   private UUID ownerUuid;
   private boolean craterQueued;

   public ArtoriaExcaliburBeamEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public ArtoriaExcaliburBeamEntity(Level level, ArtoriaPendragonEntity owner, Vec3 start, int duration) {
      this(ModEntities.ARTORIA_EXCALIBUR_BEAM.get(), level);
      this.ownerUuid = owner.getUUID();
      this.setPos(start);
      this.entityData.set(DURATION, duration);
      this.updateEndFromOwner(owner);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(END_X, 0.0F);
      builder.define(END_Y, 0.0F);
      builder.define(END_Z, 0.0F);
      builder.define(DURATION, 150);
   }

   public Vec3 getEndPos() {
      return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
   }

   public int getDuration() {
      return this.entityData.get(DURATION);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         ArtoriaPendragonEntity owner = this.getOwner(level);
         if (owner == null || !owner.isAlive()) {
            this.queueCrater(level, null, this.getEndPos());
            this.discard();
            return;
         }
         this.updateEndFromOwner(owner);
         this.setPos(owner.position().add(0.0, owner.getBbHeight() * 0.66, 0.0).add(horizontalLook(owner).scale(1.2)));
         if (this.tickCount % DAMAGE_INTERVAL == 0) {
            this.applyBeamDamage(level, owner);
         }
         this.destroyBeamBlocks(level, owner);
         this.spawnServerFx(level);
      }

      if (this.tickCount >= this.getDuration()) {
         if (this.level() instanceof ServerLevel level) {
            this.queueCrater(level, this.getOwner(level), this.getEndPos());
         }
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(END_X, tag.getFloat("EndX"));
      this.entityData.set(END_Y, tag.getFloat("EndY"));
      this.entityData.set(END_Z, tag.getFloat("EndZ"));
      this.entityData.set(DURATION, tag.getInt("Duration"));
      this.craterQueued = tag.getBoolean("CraterQueued");
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("EndX", this.entityData.get(END_X));
      tag.putFloat("EndY", this.entityData.get(END_Y));
      tag.putFloat("EndZ", this.entityData.get(END_Z));
      tag.putInt("Duration", this.entityData.get(DURATION));
      tag.putBoolean("CraterQueued", this.craterQueued);
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 131072.0;
   }

   @Override
   public AABB getBoundingBoxForCulling() {
      Vec3 start = this.position();
      Vec3 end = this.getEndPos();
      return new AABB(start, start).minmax(new AABB(end, end)).inflate(10.0);
   }

   private ArtoriaPendragonEntity getOwner(ServerLevel level) {
      if (this.ownerUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof ArtoriaPendragonEntity artoria ? artoria : null;
   }

   private void updateEndFromOwner(ArtoriaPendragonEntity owner) {
      Vec3 end = this.position().add(horizontalLook(owner).scale(LENGTH));
      this.entityData.set(END_X, (float)end.x);
      this.entityData.set(END_Y, (float)end.y);
      this.entityData.set(END_Z, (float)end.z);
   }

   private void applyBeamDamage(ServerLevel level, ArtoriaPendragonEntity owner) {
      Vec3 start = this.position();
      Vec3 dir = this.getEndPos().subtract(start);
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 forward = horizontal.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      AABB search = new AABB(owner.position(), owner.position())
         .minmax(new AABB(this.getEndPos(), this.getEndPos()))
         .inflate(HALF_WIDTH + 2.0, HALF_HEIGHT + 2.0, HALF_WIDTH + 2.0);
      DamageSource source = owner.damageSources().mobProjectile(this, owner);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(start);
         double along = rel.dot(forward);
         if (along < -POINT_BLANK_DAMAGE_LENGTH || along > LENGTH) {
            continue;
         }
         double side = Math.abs(rel.dot(right));
         double vertical = Math.abs(rel.y);
         double beamAlong = Math.max(0.0, along);
         double widthScale = Math.max(0.22, Math.sin(Math.PI * beamAlong / LENGTH));
         double allowedWidth = along < 0.0 ? 2.8 : HALF_WIDTH * Math.pow(widthScale, 0.35);
         if (side <= allowedWidth && vertical <= HALF_HEIGHT) {
            hurtWithoutIFrames(living, source, DAMAGE_PER_PULSE);
            living.push(forward.x * 0.15, 0.0, forward.z * 0.15);
            living.hurtMarked = true;
         }
      }
   }

   private void destroyBeamBlocks(ServerLevel level, ArtoriaPendragonEntity owner) {
      Vec3 start = this.position();
      Vec3 forward = horizontalLook(owner);
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      int phase = this.tickCount % 6;
      int broken = 0;
      int maxBroken = 120;
      for (double along = phase + 1.5; along <= LENGTH && broken < maxBroken; along += 6.0) {
         double widthScale = Math.max(0.25, Math.sin(Math.PI * along / LENGTH));
         double allowedWidth = HALF_WIDTH * Math.pow(widthScale, 0.35);
         for (double side = -allowedWidth; side <= allowedWidth && broken < maxBroken; side += 1.0) {
            for (double y = -2.0; y <= 3.5 && broken < maxBroken; y += 1.0) {
               Vec3 sample = start.add(forward.scale(along)).add(right.scale(side)).add(0.0, y, 0.0);
               if (owner.position().distanceToSqr(sample) <= 9.0) {
                  continue;
               }
               BlockPos pos = BlockPos.containing(sample);
               if (destroyBlock(level, owner, pos)) {
                  broken++;
               }
            }
         }
      }
   }

   private boolean destroyBlock(ServerLevel level, LivingEntity owner, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness > 100.0F || state.getExplosionResistance(level, pos, null) >= 1200.0F) {
         return false;
      }
      level.levelEvent(2001, pos, Block.getId(state));
      return level.removeBlock(pos, false);
   }

   private void spawnServerFx(ServerLevel level) {
      if (this.tickCount % 2 != 0) {
         return;
      }
      Vec3 start = this.position();
      Vec3 end = this.getEndPos();
      for (double t = 0.08; t <= 1.0; t += 0.24) {
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, fxCount(2), 0.45, 0.25, 0.45, 0.02);
         if (this.random.nextBoolean()) {
            level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private void queueCrater(ServerLevel level, LivingEntity owner, Vec3 center) {
      if (this.craterQueued || center == null) {
         return;
      }
      this.craterQueued = true;
      level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 5.0F, 0.45F);
      level.playSound(null, BlockPos.containing(center), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.6F, 1.35F);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 10, 1.0, 0.8, 1.0, 0.0);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 6, 0.35, 0.35, 0.35, 0.0);
      int radius = 15;
      int halfHeight = 25;
      int batchHeight = 3;
      Set<BlockPos> visited = new HashSet<>();
      for (int yStart = -halfHeight; yStart <= halfHeight; yStart += batchHeight) {
         final int fromY = yStart;
         TYPE_MOON_WORLD.queueServerWork((yStart + halfHeight) / batchHeight, () -> {
            int broken = 0;
            int maxBroken = 620;
            for (int x = -radius; x <= radius && broken < maxBroken; x++) {
               for (int y = fromY; y < fromY + batchHeight && y <= halfHeight && broken < maxBroken; y++) {
                  for (int z = -radius; z <= radius && broken < maxBroken; z++) {
                     double normalized = (x * x + z * z) / (double)(radius * radius) + (y * y) / (double)(halfHeight * halfHeight);
                     if (normalized > 1.0) {
                        continue;
                     }
                     BlockPos pos = BlockPos.containing(center.x + x, center.y + y, center.z + z);
                     if (!visited.add(pos.immutable())) {
                        continue;
                     }
                     if (owner != null && pos.closerToCenterThan(owner.position(), 3.0)) {
                        continue;
                     }
                     BlockState state = level.getBlockState(pos);
                     float hardness = state.getDestroySpeed(level, pos);
                     if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness > 120.0F || state.getExplosionResistance(level, pos, null) >= 1200.0F) {
                        continue;
                     }
                     if (level.removeBlock(pos, false)) {
                        broken++;
                     }
                  }
               }
            }
            damageCraterSlice(level, owner, center, radius, halfHeight, fromY, batchHeight);
            spawnCraterSliceFx(level, center, radius, fromY, broken);
         });
      }
   }

   private static void damageCraterSlice(ServerLevel level, LivingEntity owner, Vec3 center, int radius, int halfHeight, int fromY, int batchHeight) {
      double yMin = center.y + fromY - 1.5;
      double yMax = center.y + Math.min(halfHeight, fromY + batchHeight) + 1.5;
      AABB box = new AABB(center.x - radius, yMin, center.z - radius, center.x + radius, yMax, center.z + radius);
      DamageSource source = level.damageSources().explosion(null, owner);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(center);
         double normalized = (rel.x * rel.x + rel.z * rel.z) / (double)(radius * radius) + (rel.y * rel.y) / (double)(halfHeight * halfHeight);
         if (normalized > 1.18) {
            continue;
         }
         float scaledDamage = (float)(CRATER_DAMAGE * Math.max(0.35, 1.15 - normalized));
         hurtWithoutIFrames(living, source, scaledDamage);
         Vec3 horizontal = new Vec3(rel.x, 0.0, rel.z);
         Vec3 push = horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 0.0) : horizontal.normalize().scale(0.42);
         living.push(push.x, 0.38, push.z);
         living.hurtMarked = true;
      }
   }

   private static void spawnCraterSliceFx(ServerLevel level, Vec3 center, int radius, int fromY, int broken) {
      double y = center.y + fromY;
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, y, center.z, 4, radius * 0.22, 0.9, radius * 0.22, 0.0);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, y, center.z, 95, radius * 0.55, 1.9, radius * 0.55, 0.055);
      level.sendParticles(ParticleTypes.CLOUD, center.x, y + 0.4, center.z, 80, radius * 0.48, 1.2, radius * 0.48, 0.11);
      level.sendParticles(ParticleTypes.END_ROD, center.x, y + 0.5, center.z, 70, radius * 0.38, 1.6, radius * 0.38, 0.08);
      level.sendParticles(ParticleTypes.FLASH, center.x, y + 0.6, center.z, 2, radius * 0.08, 0.4, radius * 0.08, 0.0);
      if (broken > 0) {
         level.levelEvent(2008, BlockPos.containing(center.x, y, center.z), 0);
      }
      if (fromY % 9 == 0) {
         level.playSound(null, BlockPos.containing(center.x, y, center.z), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.8F, 0.72F + (fromY + 25) * 0.006F);
      }
   }

   private static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
   }

   private static void hurtWithoutIFrames(LivingEntity target, DamageSource source, float damage) {
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
      target.hurt(source, damage);
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
   }

   private static int fxCount(int count) {
      return Math.max(1, Math.round(count * 0.67F));
   }
}

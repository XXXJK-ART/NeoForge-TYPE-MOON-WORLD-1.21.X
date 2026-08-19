package net.xxxjk.TYPE_MOON_WORLD.entity;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public class SpiritronCannonBeamEntity extends Entity {
   public static final int WINDUP_TICKS = 200;
   public static final int DURATION_TICKS = 320;
   private static final int DAMAGE_INTERVAL = 5;
   private static final int BLOCK_DESTROY_PHASE_TICKS = 5;
   private static final double LENGTH = 140.0;
   private static final double HALF_WIDTH = 10.0;
   private static final double HALF_HEIGHT = 6.0;
   private static final double POINT_BLANK_DAMAGE_LENGTH = 3.0;
   private static final float DAMAGE_PER_PULSE = 3500.0F / ((DURATION_TICKS - WINDUP_TICKS) / (float)DAMAGE_INTERVAL);
   private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(SpiritronCannonBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(SpiritronCannonBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(SpiritronCannonBeamEntity.class, EntityDataSerializers.FLOAT);
   private UUID ownerUuid;
   private boolean releaseFxSpawned;

   public SpiritronCannonBeamEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public SpiritronCannonBeamEntity(Level level, LivingEntity owner, Vec3 start, Vec3 direction) {
      this(ModEntities.SPIRITRON_CANNON_BEAM.get(), level);
      this.ownerUuid = owner.getUUID();
      Vec3 forward = normalize(direction);
      this.setPos(start);
      Vec3 end = start.add(forward.scale(LENGTH));
      this.entityData.set(END_X, (float)end.x);
      this.entityData.set(END_Y, (float)end.y);
      this.entityData.set(END_Z, (float)end.z);
      if (level instanceof ServerLevel serverLevel) {
         VFXServerEffects.spawnOriented(serverLevel, "leff_spiritron_cannon_charge", start, forward, 192.0);
         serverLevel.playSound(null, BlockPos.containing(start), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2F, 0.65F);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(END_X, 0.0F);
      builder.define(END_Y, 0.0F);
      builder.define(END_Z, 0.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         LivingEntity owner = getOwner(level);
         if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
         }
         if (this.tickCount == WINDUP_TICKS) {
            spawnReleaseFx(level);
         }
         if (isBeamActive()) {
            if (this.tickCount % DAMAGE_INTERVAL == 0) {
               applyBeamDamage(level, owner);
            }
            destroyBeamBlocks(level, owner);
         }
      }
      if (this.tickCount >= DURATION_TICKS) {
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(END_X, tag.getFloat("EndX"));
      this.entityData.set(END_Y, tag.getFloat("EndY"));
      this.entityData.set(END_Z, tag.getFloat("EndZ"));
      this.releaseFxSpawned = tag.getBoolean("ReleaseFxSpawned");
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("EndX", this.entityData.get(END_X));
      tag.putFloat("EndY", this.entityData.get(END_Y));
      tag.putFloat("EndZ", this.entityData.get(END_Z));
      tag.putBoolean("ReleaseFxSpawned", this.releaseFxSpawned);
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 262144.0;
   }

   @Override
   public AABB getBoundingBoxForCulling() {
      return new AABB(this.position(), this.position()).minmax(new AABB(getEndPos(), getEndPos())).inflate(16.0);
   }

   public Vec3 getEndPos() {
      return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
   }

   private boolean isBeamActive() {
      return this.tickCount >= WINDUP_TICKS;
   }

   private LivingEntity getOwner(ServerLevel level) {
      if (this.ownerUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   private Vec3 forward() {
      return normalize(getEndPos().subtract(this.position()));
   }

   private void spawnReleaseFx(ServerLevel level) {
      if (this.releaseFxSpawned) {
         return;
      }
      this.releaseFxSpawned = true;
      Vec3 forward = forward();
      VFXServerEffects.spawnOriented(level, "leff_spiritron_cannon", this.position(), forward, 256.0);
      level.playSound(null, BlockPos.containing(this.position()), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.3F, 0.42F);
      level.playSound(null, BlockPos.containing(this.position().add(forward.scale(16.0))), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.82F);
      level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
   }

   private void applyBeamDamage(ServerLevel level, LivingEntity owner) {
      Vec3 start = this.position();
      Vec3 forward = forward();
      Vec3 worldUp = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(worldUp);
      if (right.lengthSqr() < 1.0E-4) {
         right = forward.cross(new Vec3(1.0, 0.0, 0.0));
      }
      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();
      AABB search = new AABB(start, start).minmax(new AABB(getEndPos(), getEndPos())).inflate(HALF_WIDTH + 2.0, HALF_HEIGHT + 2.0, HALF_WIDTH + 2.0);
      DamageSource source = owner.damageSources().mobProjectile(this, owner);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(start);
         double along = rel.dot(forward);
         if (along < -POINT_BLANK_DAMAGE_LENGTH || along > LENGTH) {
            continue;
         }
         double side = Math.abs(rel.dot(right));
         double vertical = Math.abs(rel.dot(up));
         double beamAlong = Math.max(0.0, along);
         double widthScale = Math.max(0.22, Math.sin(Math.PI * beamAlong / LENGTH));
         double allowedWidth = along < 0.0 ? 2.8 : HALF_WIDTH * Math.pow(widthScale, 0.35);
         if (side <= allowedWidth && vertical <= HALF_HEIGHT) {
            hurtWithoutIFrames(living, source, DAMAGE_PER_PULSE);
            living.push(forward.x * 0.18, 0.0, forward.z * 0.18);
            living.hurtMarked = true;
         }
      }
   }

   private void destroyBeamBlocks(ServerLevel level, LivingEntity owner) {
      Vec3 start = this.position();
      Vec3 forward = forward();
      Vec3 worldUp = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(worldUp);
      if (right.lengthSqr() < 1.0E-4) {
         right = forward.cross(new Vec3(1.0, 0.0, 0.0));
      }
      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();
      int phase = this.tickCount % BLOCK_DESTROY_PHASE_TICKS;
      int broken = 0;
      int maxBroken = 220;
      for (double along = phase + 0.75; along <= LENGTH && broken < maxBroken; along += BLOCK_DESTROY_PHASE_TICKS) {
         double widthScale = Math.max(0.25, Math.sin(Math.PI * along / LENGTH));
         double allowedWidth = HALF_WIDTH * Math.pow(widthScale, 0.35);
         for (double side = -allowedWidth; side <= allowedWidth && broken < maxBroken; side += 0.85) {
            for (double y = -HALF_HEIGHT * 0.34; y <= HALF_HEIGHT * 0.6 && broken < maxBroken; y += 0.85) {
               Vec3 sample = start.add(forward.scale(along)).add(right.scale(side)).add(up.scale(y));
               if (owner.position().distanceToSqr(sample) <= 2.25) {
                  continue;
               }
               if (destroyBlock(level, BlockPos.containing(sample))) {
                  broken++;
               }
            }
         }
      }
   }

   private boolean destroyBlock(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness > 100.0F || state.getExplosionResistance(level, pos, null) >= 1200.0F) {
         return false;
      }
      return level.removeBlock(pos, false);
   }

   private static Vec3 normalize(Vec3 direction) {
      if (direction == null || direction.lengthSqr() < 1.0E-4) {
         return new Vec3(0.0, 0.0, 1.0);
      }
      return direction.normalize();
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
}

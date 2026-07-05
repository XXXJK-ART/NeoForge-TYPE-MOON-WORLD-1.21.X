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
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;

public class ArtoriaExcaliburBeamEntity extends Entity {
   private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DAMAGE_START_TICK = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> POWER_SCALE = SynchedEntityData.defineId(ArtoriaExcaliburBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final double LENGTH = 150.0;
   private static final double HALF_WIDTH = 12.0;
   private static final double HALF_HEIGHT = 7.0;
   private static final double POINT_BLANK_DAMAGE_LENGTH = 3.0;
   private static final int DAMAGE_INTERVAL = 5;
   private static final int BLOCK_DESTROY_PHASE_TICKS = 5;
   private static final float DAMAGE_PER_PULSE = 4000.0F / (150.0F / DAMAGE_INTERVAL);
   private static final float CRATER_DAMAGE = 180.0F;
   private UUID ownerUuid;
   private boolean craterQueued;

   public ArtoriaExcaliburBeamEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public ArtoriaExcaliburBeamEntity(Level level, LivingEntity owner, Vec3 start, int duration) {
      this(level, owner, start, duration, 0);
   }

   public ArtoriaExcaliburBeamEntity(Level level, LivingEntity owner, Vec3 start, int duration, int damageStartTick) {
      this(level, owner, start, duration, damageStartTick, 1.0F);
   }

   public ArtoriaExcaliburBeamEntity(Level level, LivingEntity owner, Vec3 start, int duration, int damageStartTick, float powerScale) {
      this(ModEntities.ARTORIA_EXCALIBUR_BEAM.get(), level);
      this.ownerUuid = owner.getUUID();
      this.setPos(start);
      this.entityData.set(DURATION, duration);
      this.entityData.set(DAMAGE_START_TICK, Math.max(0, Math.min(duration, damageStartTick)));
      this.entityData.set(POWER_SCALE, Math.max(0.2F, Math.min(1.0F, powerScale)));
      this.updateEndFromOwner(owner);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(END_X, 0.0F);
      builder.define(END_Y, 0.0F);
      builder.define(END_Z, 0.0F);
      builder.define(DURATION, 150);
      builder.define(DAMAGE_START_TICK, 0);
      builder.define(POWER_SCALE, 1.0F);
   }

   public Vec3 getEndPos() {
      return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
   }

   public int getDuration() {
      return this.entityData.get(DURATION);
   }

   public int getDamageStartTick() {
      return this.entityData.get(DAMAGE_START_TICK);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         LivingEntity owner = this.getOwner(level);
         if (owner == null || !owner.isAlive()) {
            if (this.isBeamActive()) {
               this.queueCrater(level, null, this.getEndPos());
            }
            this.discard();
            return;
         }
         this.updateEndFromOwner(owner);
         this.setPos(owner.position().add(0.0, owner.getBbHeight() * 0.66, 0.0).add(ArtoriaPendragonCombatHelper.excaliburLook(owner).scale(1.2)));
         if (this.isBeamActive() && this.tickCount % DAMAGE_INTERVAL == 0) {
            this.applyBeamDamage(level, owner);
         }
         if (this.isBeamActive()) {
            this.destroyBeamBlocks(level, owner);
         }
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
      this.entityData.set(DAMAGE_START_TICK, tag.getInt("DamageStartTick"));
      this.entityData.set(POWER_SCALE, Math.max(0.2F, Math.min(1.0F, tag.contains("PowerScale") ? tag.getFloat("PowerScale") : 1.0F)));
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
      tag.putInt("DamageStartTick", this.entityData.get(DAMAGE_START_TICK));
      tag.putFloat("PowerScale", this.entityData.get(POWER_SCALE));
      tag.putBoolean("CraterQueued", this.craterQueued);
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
      Vec3 start = this.position();
      Vec3 end = this.getEndPos();
      AABB box = new AABB(start, start).minmax(new AABB(end, end));
      if (this.getDamageStartTick() > 0 && this.tickCount < this.getDamageStartTick()) {
         box = box.minmax(new AABB(start.add(0.0, this.beamLength(), 0.0), start.add(0.0, this.beamLength(), 0.0)));
      }
      return box.inflate(16.0);
   }

   private LivingEntity getOwner(ServerLevel level) {
      if (this.ownerUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   private void updateEndFromOwner(LivingEntity owner) {
      Vec3 end = this.position().add(ArtoriaPendragonCombatHelper.excaliburLook(owner).scale(this.beamLength()));
      this.entityData.set(END_X, (float)end.x);
      this.entityData.set(END_Y, (float)end.y);
      this.entityData.set(END_Z, (float)end.z);
   }

   private float powerScale() {
      return Math.max(0.2F, Math.min(1.0F, this.entityData.get(POWER_SCALE)));
   }

   private double beamLength() {
      return LENGTH * (0.35 + this.powerScale() * 0.65);
   }

   private double beamHalfWidth() {
      return HALF_WIDTH * (0.28 + this.powerScale() * 0.72);
   }

   private double beamHalfHeight() {
      return HALF_HEIGHT * (0.35 + this.powerScale() * 0.65);
   }

   private boolean isBeamActive() {
      return this.tickCount >= this.entityData.get(DAMAGE_START_TICK);
   }

   private void applyBeamDamage(ServerLevel level, LivingEntity owner) {
      Vec3 start = this.position();
      Vec3 dir = this.getEndPos().subtract(start);
      if (dir.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 forward = dir.normalize();
      Vec3 worldUp = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(worldUp);
      if (right.lengthSqr() < 1.0E-4) {
         right = forward.cross(new Vec3(1.0, 0.0, 0.0));
      }
      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();
      AABB search = new AABB(owner.position(), owner.position())
         .minmax(new AABB(this.getEndPos(), this.getEndPos()))
         .inflate(this.beamHalfWidth() + 2.0, this.beamHalfHeight() + 2.0, this.beamHalfWidth() + 2.0);
      DamageSource source = owner.damageSources().mobProjectile(this, owner);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, search, e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(start);
         double along = rel.dot(forward);
         double length = this.beamLength();
         if (along < -POINT_BLANK_DAMAGE_LENGTH || along > length) {
            continue;
         }
         double side = Math.abs(rel.dot(right));
         double vertical = Math.abs(rel.dot(up));
         double beamAlong = Math.max(0.0, along);
         double widthScale = Math.max(0.22, Math.sin(Math.PI * beamAlong / length));
         double allowedWidth = along < 0.0 ? 2.8 : this.beamHalfWidth() * Math.pow(widthScale, 0.35);
         if (side <= allowedWidth && vertical <= this.beamHalfHeight()) {
            hurtWithoutIFrames(living, source, DAMAGE_PER_PULSE * this.powerScale());
            living.push(forward.x * 0.15, 0.0, forward.z * 0.15);
            living.hurtMarked = true;
         }
      }
   }

   private void destroyBeamBlocks(ServerLevel level, LivingEntity owner) {
      Vec3 start = this.position();
      Vec3 forward = ArtoriaPendragonCombatHelper.excaliburLook(owner);
      Vec3 worldUp = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(worldUp);
      if (right.lengthSqr() < 1.0E-4) {
         right = forward.cross(new Vec3(1.0, 0.0, 0.0));
      }
      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();
      int phase = this.tickCount % BLOCK_DESTROY_PHASE_TICKS;
      int broken = 0;
      int maxBroken = Math.max(36, (int)Math.floor(260.0F * this.powerScale()));
      double length = this.beamLength();
      for (double along = phase + 0.75; along <= length && broken < maxBroken; along += BLOCK_DESTROY_PHASE_TICKS) {
         double widthScale = Math.max(0.25, Math.sin(Math.PI * along / length));
         double allowedWidth = this.beamHalfWidth() * Math.pow(widthScale, 0.35);
         for (double side = -allowedWidth; side <= allowedWidth && broken < maxBroken; side += 0.85) {
            for (double y = -this.beamHalfHeight() * 0.34; y <= this.beamHalfHeight() * 0.6 && broken < maxBroken; y += 0.85) {
               Vec3 sample = start.add(forward.scale(along)).add(right.scale(side)).add(up.scale(y));
               if (owner.position().distanceToSqr(sample) <= 2.25) {
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
      return level.removeBlock(pos, false);
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
      VFXServerEffects.spawn(level, "artoria_excalibur_impact", center, 192.0);
      float powerScale = this.powerScale();
      int radius = Math.max(6, (int)Math.floor(28.0F * (0.25F + powerScale * 0.75F)));
      int halfHeight = Math.max(18, (int)Math.floor(112.0F * (0.22F + powerScale * 0.78F)));
      int batchHeight = 3;
      DeferredTerrainDestruction.queueEllipsoid(level, center, radius, halfHeight, 120.0F, 80);
      for (int yStart = -halfHeight; yStart <= halfHeight; yStart += batchHeight) {
         final int fromY = yStart;
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork((yStart + halfHeight) / batchHeight, () -> {
            damageCraterSlice(this, level, owner, center, radius, halfHeight, fromY, batchHeight, powerScale);
            spawnCraterSliceFx(level, center, radius, fromY);
         });
      }
   }

   private static void damageCraterSlice(ArtoriaExcaliburBeamEntity beam, ServerLevel level, LivingEntity owner, Vec3 center, int radius, int halfHeight, int fromY, int batchHeight, float powerScale) {
      double yMin = center.y + fromY - 1.5;
      double yMax = center.y + Math.min(halfHeight, fromY + batchHeight) + 1.5;
      AABB box = new AABB(center.x - radius, yMin, center.z - radius, center.x + radius, yMax, center.z + radius);
      DamageSource source = level.damageSources().explosion(beam, owner);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(center);
         double normalized = (rel.x * rel.x + rel.z * rel.z) / (double)(radius * radius) + (rel.y * rel.y) / (double)(halfHeight * halfHeight);
         if (normalized > 1.18) {
            continue;
         }
         float scaledDamage = (float)(CRATER_DAMAGE * powerScale * Math.max(0.35, 1.15 - normalized));
         hurtWithoutIFrames(living, source, scaledDamage);
         Vec3 horizontal = new Vec3(rel.x, 0.0, rel.z);
         Vec3 push = horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 0.0) : horizontal.normalize().scale(0.42);
         living.push(push.x, 0.38, push.z);
         living.hurtMarked = true;
      }
   }

   private static void spawnCraterSliceFx(ServerLevel level, Vec3 center, int radius, int fromY) {
      double y = center.y + fromY;
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, y, center.z, 4, radius * 0.22, 0.9, radius * 0.22, 0.0);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, y, center.z, 95, radius * 0.55, 1.9, radius * 0.55, 0.055);
      level.sendParticles(ParticleTypes.CLOUD, center.x, y + 0.4, center.z, 80, radius * 0.48, 1.2, radius * 0.48, 0.11);
      level.sendParticles(ParticleTypes.END_ROD, center.x, y + 0.5, center.z, 70, radius * 0.38, 1.6, radius * 0.38, 0.08);
      level.sendParticles(ParticleTypes.FLASH, center.x, y + 0.6, center.z, 2, radius * 0.08, 0.4, radius * 0.08, 0.0);
      level.levelEvent(2008, BlockPos.containing(center.x, y, center.z), 0);
      if (fromY % 9 == 0) {
         level.playSound(null, BlockPos.containing(center.x, y, center.z), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.8F, 0.72F + (fromY + 25) * 0.006F);
      }
   }

   private static Vec3 horizontalLook(LivingEntity entity) {
      Vec3 look = entity.getLookAngle();
      return look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
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

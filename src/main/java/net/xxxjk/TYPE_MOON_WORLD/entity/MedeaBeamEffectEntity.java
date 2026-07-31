package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;

public class MedeaBeamEffectEntity extends Entity {
   private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(MedeaBeamEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(MedeaBeamEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(MedeaBeamEffectEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(MedeaBeamEffectEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> BREAK_BLOCKS = SynchedEntityData.defineId(MedeaBeamEffectEntity.class, EntityDataSerializers.BOOLEAN);
   private UUID ownerUuid;
   private float damage;
   private boolean damageApplied;

   public MedeaBeamEffectEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public MedeaBeamEffectEntity(Level level, LivingEntity owner, Vec3 start, Vec3 end, float damage, int duration) {
      this(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MEDEA_BEAM_EFFECT.get(), level);
      this.ownerUuid = owner == null ? null : owner.getUUID();
      this.damage = damage;
      this.setPos(start);
      this.entityData.set(END_X, (float)end.x);
      this.entityData.set(END_Y, (float)end.y);
      this.entityData.set(END_Z, (float)end.z);
      this.entityData.set(DURATION, duration);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(END_X, 0.0F);
      builder.define(END_Y, 0.0F);
      builder.define(END_Z, 0.0F);
      builder.define(DURATION, 8);
      builder.define(BREAK_BLOCKS, false);
   }

   public Vec3 getEndPos() {
      return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
   }

   public int getDuration() {
      return this.entityData.get(DURATION);
   }

   public float getDamage() {
      return this.damage;
   }

   public void setBreakBlocks(boolean breakBlocks) {
      this.entityData.set(BREAK_BLOCKS, breakBlocks);
   }

   public boolean shouldBreakBlocks() {
      return this.entityData.get(BREAK_BLOCKS);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && !this.damageApplied) {
         this.damageApplied = true;
         this.applyBeamDamage();
      }
      if (this.tickCount >= this.getDuration()) {
         this.discard();
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(END_X, tag.getFloat("EndX"));
      this.entityData.set(END_Y, tag.getFloat("EndY"));
      this.entityData.set(END_Z, tag.getFloat("EndZ"));
      this.entityData.set(DURATION, tag.getInt("Duration"));
      this.damage = tag.getFloat("Damage");
      this.entityData.set(BREAK_BLOCKS, tag.getBoolean("BreakBlocks"));
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
      tag.putFloat("Damage", this.damage);
      tag.putBoolean("BreakBlocks", this.shouldBreakBlocks());
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 65536.0;
   }

   @Override
   public AABB getBoundingBoxForCulling() {
      Vec3 end = this.getEndPos();
      return this.getBoundingBox().minmax(new AABB(end, end).inflate(1.0));
   }

   private void applyBeamDamage() {
      if (!(this.level() instanceof ServerLevel serverLevel)) {
         return;
      }

      LivingEntity owner = this.ownerUuid != null && serverLevel.getEntity(this.ownerUuid) instanceof LivingEntity living ? living : null;
      DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().magic();
      Set<Integer> hitIds = new HashSet<>();
      Vec3 start = this.position();
      Vec3 end = this.getEndPos();

      if (this.shouldBreakBlocks()) {
         this.destroyBlocksAlongBeam(serverLevel, owner, start, end);
      }

      for (double step = 0.0; step <= 1.0; step += 0.08) {
         Vec3 sample = start.lerp(end, step);
         AABB segmentBox = new AABB(sample, sample).inflate(0.85, 0.85, 0.85);
         for (LivingEntity living : serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            segmentBox,
            candidate -> candidate.isAlive()
               && candidate != owner
               && !(owner != null && ServantMasterTargeting.isContractMaster(owner, candidate))
               && (owner == null || !candidate.isAlliedTo(owner))
               && !EntityUtils.isImmunePlayerTarget(candidate)
         )) {
            if (hitIds.add(living.getId())) {
               living.hurt(source, this.damage);
               living.invulnerableTime = 0;
            }
         }
      }
   }

   private void destroyBlocksAlongBeam(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 end) {
      Set<BlockPos> visited = new HashSet<>();
      for (double step = 0.0; step <= 1.0; step += 0.04) {
         Vec3 sample = start.lerp(end, step);
         BlockPos center = BlockPos.containing(sample);
         for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (!visited.add(pos.immutable())) {
               continue;
            }
            if (pos.distToCenterSqr(sample.x, sample.y, sample.z) > 1.35 * 1.35) {
               continue;
            }
            if (owner != null && pos.closerToCenterThan(owner.position(), 1.0)) {
               continue;
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness >= 85.0F) {
               continue;
            }
            level.destroyBlock(pos, false, owner);
         }
      }
   }
}

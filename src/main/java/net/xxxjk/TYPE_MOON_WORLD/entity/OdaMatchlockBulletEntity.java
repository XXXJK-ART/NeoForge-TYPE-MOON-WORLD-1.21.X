package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class OdaMatchlockBulletEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> DIRECT_DAMAGE = SynchedEntityData.defineId(OdaMatchlockBulletEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> SOURCE_GUN_ID = SynchedEntityData.defineId(OdaMatchlockBulletEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> BULLET_KIND = SynchedEntityData.defineId(OdaMatchlockBulletEntity.class, EntityDataSerializers.INT);
   private static final int MAX_LIFE = 60;
   private static final int BREAK_BLOCK_COUNT = 4;
   public final List<Vec3> tracePos = new LinkedList<>();

   public OdaMatchlockBulletEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public OdaMatchlockBulletEntity(Level level, LivingEntity owner, Entity sourceGun, float damage) {
      super(ModEntities.ODA_MATCHLOCK_BULLET.get(), level);
      this.setNoGravity(true);
      if (owner != null) {
         this.setOwner(owner);
      }
      this.entityData.set(DIRECT_DAMAGE, Math.max(0.0F, damage));
      this.entityData.set(SOURCE_GUN_ID, sourceGun == null ? -1 : sourceGun.getId());
   }

   public OdaMatchlockBulletEntity setBulletKind(int kind) {
      this.entityData.set(BULLET_KIND, Math.max(0, kind));
      return this;
   }

   public float getVisualScale() {
      return switch (this.entityData.get(BULLET_KIND)) {
         case 2 -> 1.8F;
         case 3 -> 1.35F;
         default -> 1.0F;
      };
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DIRECT_DAMAGE, 10.0F);
      builder.define(SOURCE_GUN_ID, -1);
      builder.define(BULLET_KIND, 0);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.FIRE_CHARGE;
   }

   @Override
   public boolean isNoGravity() {
      return true;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      if (entity == null || entity == this.getOwner() || entity instanceof OdaMatchlockGunEntity || EntityUtils.isImmunePlayerTarget(entity)) {
         return false;
      }
      if (entity.getId() == this.entityData.get(SOURCE_GUN_ID)) {
         return false;
      }
      if (this.getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && owner.isAlliedTo(living)) {
         return false;
      }
      return super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      super.tick();
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() > 1.0E-4) {
         this.setYRot((float)(Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
         this.setXRot((float)(Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
      }

      if (this.level().isClientSide) {
         captureTrace();
         this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         this.level().addParticle(ParticleTypes.SMALL_FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      } else if (this.tickCount > MAX_LIFE) {
         this.discard();
      } else if (this.level() instanceof ServerLevel level && this.tickCount % 2 == 0) {
         Vec3 back = motion.lengthSqr() > 1.0E-4 ? motion.normalize().scale(-0.12) : Vec3.ZERO;
         Vec3 pos = this.position().add(back);
         level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
         level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0.0);
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().thrown(this, this.getOwner()), this.entityData.get(DIRECT_DAMAGE));
         target.invulnerableTime = 0;
         if (this.getOwner() instanceof LivingEntity owner) {
            OdaNobunagaCombatHelper.applyDivineDefenseBreak(owner, target);
         }
         impact(result.getLocation(), false);
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.level().isClientSide) {
         breakBlocksFromImpact(result);
         impact(result.getLocation(), true);
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
   }

   private void impact(Vec3 pos, boolean blockImpact) {
      if (this.level() instanceof ServerLevel level) {
         int kind = this.entityData.get(BULLET_KIND);
         int intensity = kind == 2 ? 3 : kind == 3 ? 2 : 1;
         level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, intensity, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 10 + intensity * 6, 0.12 * intensity, 0.12 * intensity, 0.12 * intensity, 0.04);
         level.sendParticles(blockImpact ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 8 + intensity * 8, 0.18 * intensity, 0.16 * intensity, 0.18 * intensity, 0.02);
         level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 4 + intensity * 5, 0.1 * intensity, 0.1 * intensity, 0.1 * intensity, 0.02);
         level.playSound(null, pos.x, pos.y, pos.z, kind == 2 ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, kind == 2 ? 0.8F : 0.55F, kind == 2 ? 1.2F : 1.75F);
      }
      this.discard();
   }

   private void breakBlocksFromImpact(BlockHitResult hitResult) {
      Vec3 motion = this.getDeltaMovement();
      Direction forward = motion.lengthSqr() > 1.0E-6 ? Direction.getNearest(motion.x, motion.y, motion.z) : hitResult.getDirection().getOpposite();
      Entity breaker = this.getOwner() != null ? this.getOwner() : this;
      BlockPos start = hitResult.getBlockPos();
      int breakCount = this.entityData.get(BULLET_KIND) == 2 ? 8 : BREAK_BLOCK_COUNT;
      for (int i = 0; i < breakCount; i++) {
         BlockPos target = start.relative(forward, i);
         BlockState state = this.level().getBlockState(target);
         if (canBreak(state, target)) {
            this.level().destroyBlock(target, false, breaker);
         }
      }
   }

   private boolean canBreak(BlockState state, BlockPos pos) {
      return !state.isAir()
         && !state.hasBlockEntity()
         && !state.is(Blocks.BEDROCK)
         && !state.is(Blocks.END_PORTAL_FRAME)
         && state.getDestroySpeed(this.level(), pos) >= 0.0F
         && state.getDestroySpeed(this.level(), pos) <= 18.0F;
   }

   private void captureTrace() {
      Vec3 pos = this.position();
      if (this.tracePos.isEmpty() || this.tracePos.get(this.tracePos.size() - 1).distanceToSqr(pos) >= 0.01) {
         this.tracePos.add(pos);
         while (this.tracePos.size() > 32) {
            this.tracePos.remove(0);
         }
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(DIRECT_DAMAGE, tag.contains("DirectDamage") ? tag.getFloat("DirectDamage") : 10.0F);
      this.entityData.set(SOURCE_GUN_ID, tag.getInt("SourceGunId"));
      this.entityData.set(BULLET_KIND, tag.getInt("BulletKind"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("DirectDamage", this.entityData.get(DIRECT_DAMAGE));
      tag.putInt("SourceGunId", this.entityData.get(SOURCE_GUN_ID));
      tag.putInt("BulletKind", this.entityData.get(BULLET_KIND));
   }
}

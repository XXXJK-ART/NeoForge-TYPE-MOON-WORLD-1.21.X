package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public class GaeBulgProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(GaeBulgProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(GaeBulgProjectileEntity.class, EntityDataSerializers.INT);
   private int lifeTime = 0;

   public GaeBulgProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public GaeBulgProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.GAE_BULG_PROJECTILE.get(), shooter, level);
      this.setItem(new ItemStack(ModItems.GAE_BULG.get()));
   }

   public enum Mode {
      SINGLE(0),
      ARMY(1);

      private final int id;

      Mode(int id) {
         this.id = id;
      }

      public int id() {
         return this.id;
      }

      public static Mode fromId(int id) {
         return id == 1 ? ARMY : SINGLE;
      }
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(MODE, 0);
      builder.define(TARGET_ID, -1);
   }

   public void setMode(Mode mode) {
      this.entityData.set(MODE, mode.id());
   }

   public Mode getMode() {
      return Mode.fromId(this.entityData.get(MODE));
   }

   public void setTrackedTarget(LivingEntity target) {
      this.entityData.set(TARGET_ID, target == null ? -1 : target.getId());
   }

   private LivingEntity getTrackedTarget() {
      Entity entity = this.level().getEntity(this.entityData.get(TARGET_ID));
      return entity instanceof LivingEntity living ? living : null;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && super.canHitEntity(entity);
   }

   @Override
   protected Item getDefaultItem() {
      return ModItems.GAE_BULG.get();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         if (this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
         return;
      }

      this.lifeTime++;
      LivingEntity target = this.getTrackedTarget();
      if (target != null && target.isAlive()) {
         if (this.getMode() == Mode.SINGLE) {
            this.steerToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0), 0.85, 0.4);
            this.clearPathObstacles(2.4);
         } else {
            this.steerToward(target.position().add(0.0, target.getBbHeight() * 0.3, 0.0), 0.35, 0.18);
         }
         this.syncRotationToMotion();

         if (this.distanceToSqr(target) <= 2.25) {
            if (this.getMode() == Mode.SINGLE) {
               this.resolveSingleTargetHit(target);
            } else {
               this.resolveArmyExplosion(this.position());
            }
            return;
         }
      }

      if ((this.getMode() == Mode.SINGLE && this.lifeTime > 120) || (this.getMode() == Mode.ARMY && this.lifeTime > 80)) {
         if (this.getMode() == Mode.SINGLE && target != null && target.isAlive()) {
            this.resolveSingleTargetHit(target);
         } else if (this.getMode() == Mode.ARMY) {
            this.resolveArmyExplosion(this.position());
         } else {
            this.discard();
         }
      }
   }

   private void steerToward(Vec3 targetPos, double strength, double blend) {
      Vec3 toTarget = targetPos.subtract(this.position());
      if (toTarget.lengthSqr() < 1.0E-4) {
         return;
      }

      Vec3 current = this.getDeltaMovement();
      double speed = Math.max(strength, current.length());
      Vec3 desired = toTarget.normalize().scale(speed);
      Vec3 next = current.scale(1.0 - blend).add(desired.scale(blend));
      this.setDeltaMovement(next);
      this.hasImpulse = true;
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (this.level().isClientSide()) {
         return;
      }

      if (result.getEntity() instanceof LivingEntity living) {
         if (this.getMode() == Mode.SINGLE) {
            this.resolveSingleTargetHit(living);
         } else {
            this.resolveArmyExplosion(result.getLocation());
         }
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (this.level().isClientSide()) {
         return;
      }

      if (result.getType() == HitResult.Type.BLOCK) {
         if (this.getMode() == Mode.ARMY) {
            this.resolveArmyExplosion(result.getLocation());
         } else {
            if (result instanceof BlockHitResult blockHit && this.tryDestroyBlock(blockHit.getBlockPos())) {
               this.setPos(this.getX() + this.getDeltaMovement().x * 0.1, this.getY() + this.getDeltaMovement().y * 0.1, this.getZ() + this.getDeltaMovement().z * 0.1);
               return;
            }

            LivingEntity target = this.getTrackedTarget();
            if (target != null && target.isAlive()) {
               this.nudgeAroundObstacle(target);
               return;
            }

            this.discard();
         }
      }
   }

   private void syncRotationToMotion() {
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-4) {
         return;
      }

      double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 180.0F / (float)Math.PI));
      this.setXRot((float)(Mth.atan2(motion.y, horizontal) * 180.0F / (float)Math.PI));
   }

   private void clearPathObstacles(double distance) {
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-4) {
         return;
      }

      Vec3 direction = motion.normalize();
      for (double step = 0.35; step <= distance; step += 0.35) {
         BlockPos pos = BlockPos.containing(this.position().add(direction.scale(step)));
         if (this.tryDestroyBlock(pos)) {
            return;
         }
      }
   }

   private boolean tryDestroyBlock(BlockPos pos) {
      BlockState state = this.level().getBlockState(pos);
      float hardness = state.getDestroySpeed(this.level(), pos);
      if (state.isAir() || hardness < 0.0F || hardness > 50.0F || state.is(Blocks.BEDROCK)) {
         return false;
      }

      if (this.level() instanceof ServerLevel sl) {
         sl.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
      }
      this.level().removeBlock(pos, false);
      return true;
   }

   private void nudgeAroundObstacle(LivingEntity target) {
      Vec3 motion = this.getDeltaMovement();
      Vec3 toTarget = target.position().subtract(this.position());
      Vec3 side = new Vec3(-motion.z, 0.0, motion.x);
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(-toTarget.z, 0.0, toTarget.x);
      }
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(this.random.nextBoolean() ? 1.0 : -1.0, 0.0, 0.0);
      }

      double speed = Math.max(1.2, motion.length());
      Vec3 adjusted = motion.scale(0.35).add(side.normalize().scale(speed)).add(0.0, toTarget.y > 1.0 ? 0.3 : 0.1, 0.0);
      this.setDeltaMovement(adjusted.normalize().scale(speed));
      this.hasImpulse = true;
      this.syncRotationToMotion();
   }

   private void resolveSingleTargetHit(LivingEntity target) {
      LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
      float before = target.getHealth();
      DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().generic();
      target.invulnerableTime = 0;
      target.hurt(source, 250.0F);
      target.invulnerableTime = 0;
      float desiredHealth = Math.max(0.0F, before - 250.0F);
      if (target.getHealth() > desiredHealth) {
         target.setHealth(desiredHealth);
      }

      if (target.isAlive() && this.random.nextFloat() < CuChulainnCombatHelper.getDeathThornChance(target)) {
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
         if (target.isAlive()) {
            target.setHealth(0.0F);
            target.die(this.damageSources().genericKill());
         }
      }

      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.CRIT,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            18, 0.25, 0.25, 0.25, 0.15);
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
            4, 0.0, 0.0, 0.0, 0.0);
      }
      this.discard();
   }

   private void resolveArmyExplosion(Vec3 center) {
      LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
      DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().generic();
      double radius = 15.0;
      AABB box = new AABB(center, center).inflate(radius);
      List<LivingEntity> entities = this.level().getEntitiesOfClass(
         LivingEntity.class,
         box,
         e -> e.isAlive() && e != owner
      );
      for (LivingEntity living : entities) {
         double dist = Math.sqrt(living.distanceToSqr(center.x, center.y, center.z));
         if (dist > radius) {
            continue;
         }

         float falloff = (float)Math.max(0.7, 1.0 - 0.3 * (dist / radius));
         float damage = 400.0F * falloff;
         float before = living.getHealth();
         living.invulnerableTime = 0;
         living.hurt(source, damage);
         living.invulnerableTime = 0;
         float desiredHealth = Math.max(0.0F, before - damage);
         if (living.getHealth() > desiredHealth) {
            living.setHealth(desiredHealth);
         }
      }

      if (owner instanceof ServantEntity servant) {
         CuChulainnCombatHelper.applyExhaustion(servant, 200);
      }

      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 6, 1.5, 1.0, 1.5, 0.0);
         sl.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.5, center.z, 60, 5.0, 2.0, 5.0, 0.25);
         sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.5, center.z, 24, 2.2, 1.0, 2.2, 0.08);
         sl.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.7F);
      }
      this.discard();
   }
}

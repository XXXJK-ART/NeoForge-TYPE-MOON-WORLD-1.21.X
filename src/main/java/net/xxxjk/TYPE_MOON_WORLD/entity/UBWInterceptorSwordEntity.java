package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ProjectileVisualEffectHelper;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;

public class UBWInterceptorSwordEntity extends ThrowableItemProjectile {
   private static final double MIN_TRACKING_SPEED = 1.35;
   private static final double SPEED_ADVANTAGE = 0.75;
   private static final double MAX_TRACKING_SPEED = 3.2;
   private static final double INTERCEPT_DISTANCE = 0.9;
   private static final int MAX_LIFETIME = 120;
   private static final int LOST_TARGET_LIFETIME = 60;
   private int targetEntityId = -1;
   private UUID ubwOwnerId;
   private boolean tracking = true;
   private int lostTargetTicks;
   public final List<Vec3> tracePos = new LinkedList<>();

   public UBWInterceptorSwordEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setItem(new ItemStack(Items.IRON_SWORD));
      this.setNoGravity(true);
   }

   public UBWInterceptorSwordEntity(Level level, Entity target, UUID ubwOwnerId, Vec3 spawnPos) {
      super(ModEntities.UBW_INTERCEPTOR_SWORD.get(), spawnPos.x, spawnPos.y, spawnPos.z, level);
      this.setItem(new ItemStack(Items.IRON_SWORD));
      this.ubwOwnerId = ubwOwnerId;
      this.targetEntityId = target.getId();
      this.setNoGravity(true);
      this.setInitialVelocity(target);
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return true;
   }

   public boolean isPushedByFluid() {
      return false;
   }

   protected Item getDefaultItem() {
      return Items.IRON_SWORD;
   }

   public void tick() {
      if (!this.level().isClientSide) {
         this.updateTrackingVelocity();
      }

      super.tick();
      this.updateRotationFromMotion();
      if (!this.level().isClientSide) {
         if (this.tickCount > MAX_LIFETIME || !this.tracking && this.lostTargetTicks > LOST_TARGET_LIFETIME) {
            this.discard();
         }
      } else {
         ProjectileVisualEffectHelper.captureTrace(this.tracePos, this, 50);
      }
   }

   protected boolean canHitEntity(Entity entity) {
      return this.tracking && entity instanceof Projectile && entity.getId() == this.targetEntityId && super.canHitEntity(entity);
   }

   protected void onHitEntity(EntityHitResult result) {
      if (!this.level().isClientSide && result.getEntity() instanceof Projectile && result.getEntity().getId() == this.targetEntityId) {
         this.cancelTarget(result.getEntity());
      }
   }

   protected void onHit(HitResult result) {
      if (this.level().isClientSide) {
         return;
      }

      if (result.getType() == Type.ENTITY) {
         this.onHitEntity((EntityHitResult)result);
         return;
      }

      if (result.getType() == Type.BLOCK) {
         this.placeSword((BlockHitResult)result);
         this.discard();
         return;
      }

      this.discard();
   }

   private void updateTrackingVelocity() {
      Entity target = this.targetEntityId >= 0 ? this.level().getEntity(this.targetEntityId) : null;
      if (!this.tracking || !(target instanceof Projectile) || !target.isAlive()) {
         this.loseTarget();
         return;
      }

      Vec3 targetPos = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 toTarget = targetPos.subtract(this.position());
      double distance = toTarget.length();
      if (distance <= INTERCEPT_DISTANCE || this.getBoundingBox().inflate(0.35).intersects(target.getBoundingBox())) {
         this.cancelTarget(target);
         return;
      }

      Vec3 desiredDirection = toTarget.normalize();
      Vec3 current = this.getDeltaMovement();
      Vec3 currentDirection = current.lengthSqr() > 1.0E-4 ? current.normalize() : desiredDirection;
      double targetSpeed = target.getDeltaMovement().length();
      double speed = Mth.clamp(Math.max(MIN_TRACKING_SPEED, targetSpeed + SPEED_ADVANTAGE), MIN_TRACKING_SPEED, MAX_TRACKING_SPEED);
      Vec3 blendedDirection = currentDirection.scale(0.38).add(desiredDirection.scale(0.62)).normalize();
      this.setNoGravity(true);
      this.setDeltaMovement(blendedDirection.scale(speed));
   }

   private void setInitialVelocity(Entity target) {
      Vec3 targetPos = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 direction = targetPos.subtract(this.position());
      if (direction.lengthSqr() < 1.0E-4) {
         direction = new Vec3(0.0, -0.15, 1.0);
      }

      double speed = Mth.clamp(Math.max(MIN_TRACKING_SPEED, target.getDeltaMovement().length() + SPEED_ADVANTAGE), MIN_TRACKING_SPEED, MAX_TRACKING_SPEED);
      this.setDeltaMovement(direction.normalize().scale(speed));
      this.updateRotationFromMotion();
   }

   private void loseTarget() {
      if (this.tracking) {
         this.tracking = false;
         this.setNoGravity(false);
         Vec3 motion = this.getDeltaMovement();
         if (motion.lengthSqr() < 1.0E-4) {
            this.setDeltaMovement(Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(0.9));
         }
      }

      this.lostTargetTicks++;
   }

   private void cancelTarget(Entity target) {
      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 12, 0.15, 0.15, 0.15, 0.08);
         serverLevel.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35F, 1.65F);
      }

      target.discard();
      this.discard();
   }

   private void placeSword(BlockHitResult blockHit) {
      BlockPos hitPos = blockHit.getBlockPos();
      BlockState hitState = this.level().getBlockState(hitPos);
      if (hitState.getCollisionShape(this.level(), hitPos).isEmpty() || !hitState.canOcclude()) {
         return;
      }

      if (hitState.getBlock() instanceof UBWWeaponBlock) {
         return;
      }

      BlockPos placePos = hitPos.relative(blockHit.getDirection());
      BlockState placeState = this.level().getBlockState(placePos);
      if (!placeState.canBeReplaced()) {
         return;
      }

      Direction facing = Direction.fromYRot(this.getYRot()).getOpposite();
      if (facing == Direction.UP || facing == Direction.DOWN) {
         facing = Direction.NORTH;
      }

      BlockState newState = (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((UBWWeaponBlock)ModBlocks.UBW_WEAPON_BLOCK
                              .get())
                           .defaultBlockState()
                           .setValue(UBWWeaponBlock.FACING, facing))
                        .setValue(UBWWeaponBlock.ROTATION_A, this.random.nextBoolean()))
                     .setValue(UBWWeaponBlock.ROTATION_B, this.random.nextBoolean()))
                  .setValue(UBWWeaponBlock.ROTATION_C, this.random.nextBoolean()))
               .setValue(UBWWeaponBlock.ROTATION_D, this.random.nextBoolean()))
            .setValue(UBWWeaponBlock.ROTATION_E, this.random.nextBoolean()))
         .setValue(UBWWeaponBlock.ROTATION_F, this.random.nextBoolean()))
      .setValue(UBWWeaponBlock.ROTATION_G, this.random.nextBoolean());
      if (this.level().setBlock(placePos, newState, 3)) {
         if (this.level().getBlockEntity(placePos) instanceof UBWWeaponBlockEntity tile) {
            tile.setStoredItem(new ItemStack(Items.IRON_SWORD));
         }

         if (this.ubwOwnerId != null) {
            ChantHandler.registerPlacedSword(this.ubwOwnerId, placePos);
         }
      }
   }

   private void updateRotationFromMotion() {
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() > 1.0E-4) {
         double horizontal = motion.horizontalDistance();
         this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 180.0F / (float)Math.PI));
         this.setXRot((float)(Mth.atan2(motion.y, horizontal) * 180.0F / (float)Math.PI));
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }
   }
}

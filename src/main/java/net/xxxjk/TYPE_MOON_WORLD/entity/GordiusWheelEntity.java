package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GordiusWheelEntity extends IskandarMountEntity {
   private static final double MODEL_RIDER_POINT_Z = 2.12;
   private static final double MODEL_PASSENGER_POINT_Z = 1.32;
   private static final double MODEL_RIDER_POINT_Y = 1.38;
   private static final double MODEL_PASSENGER_POINT_Y = 1.24;
   private double rearX;
   private double rearY;
   private double rearZ;

   public GordiusWheelEntity(EntityType<? extends GordiusWheelEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMountAttributes(3000.0, 0.62);
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         tickRearBodyPhysics();
         tickLightningAura(level);
      }
   }

   @Override
   public boolean isNoGravity() {
      return true;
   }

   @Override
   protected void followIskandarCombatIntent(net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity iskandar) {
      LivingEntity target = iskandar.getTarget();
      if (target != null && target.isAlive()) {
         Vec3 delta = target.position().add(0.0, Math.min(3.0, target.getBbHeight() + 1.0), 0.0).subtract(this.position());
         this.moveTowardAir(delta, getCombatSpeed());
         return;
      }
      LivingEntity master = iskandar.getEntityMaster();
      if (master != null && master.isAlive() && this.distanceToSqr(master) > 36.0) {
         this.moveTowardAir(master.position().add(0.0, 2.0, 0.0).subtract(this.position()), getFollowSpeed());
         return;
      }
      this.setDeltaMovement(Vec3.ZERO);
   }

   private void moveTowardAir(Vec3 direction, double speed) {
      if (direction.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 unit = direction.normalize();
      float yaw = (float)(Math.atan2(-unit.x, unit.z) * 180.0 / Math.PI);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      this.move(net.minecraft.world.entity.MoverType.SELF, unit.scale(speed));
      this.setDeltaMovement(Vec3.ZERO);
   }

   private void tickRearBodyPhysics() {
      Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 desired = this.position().subtract(forward.scale(2.7));
      double lateralLag = this.onGround() ? 0.18 : 0.10;
      double verticalLag = this.onGround() ? 0.28 : 1.0;
      this.rearX += (desired.x - this.rearX) * lateralLag;
      this.rearY += (desired.y - this.rearY) * verticalLag;
      this.rearZ += (desired.z - this.rearZ) * lateralLag;
   }

   private void tickLightningAura(ServerLevel level) {
      if (this.tickCount % 4 == 0) {
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 8, 1.4, 0.7, 1.4, 0.08);
      }
      if (this.tickCount % 10 != 0) {
         return;
      }
      AABB area = this.getBoundingBox().inflate(2.2, 1.2, 2.2);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         entity -> entity != this && !this.getPassengers().contains(entity) && entity.isAlive() && !this.isAlliedTo(entity))) {
         target.hurt(this.damageSources().lightningBolt(), 5.0F);
      }
   }

   @Override
   public void performCharge(ServerLevel level, LivingEntity source, float damage, double width) {
      super.performCharge(level, source, damage, width);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 45, 2.0, 1.0, 2.0, 0.18);
      level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.0F, 1.35F);
   }

   public Vec3 getRearBodyAnchor(float partialTick) {
      return new Vec3(this.rearX, this.rearY, this.rearZ);
   }

   public void snapRearBodyToCurrentPosition() {
      Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      Vec3 rear = this.position().subtract(forward.normalize().scale(2.7));
      this.rearX = rear.x;
      this.rearY = rear.y;
      this.rearZ = rear.z;
   }

   @Override
   protected double getSeatForwardOffset(boolean passengerSeat) {
      // Matches the exported "骑乘点" deck marker on the chariot model.
      return passengerSeat ? MODEL_PASSENGER_POINT_Z : MODEL_RIDER_POINT_Z;
   }

   @Override
   protected double getSeatHeight(boolean passengerSeat) {
      return passengerSeat ? MODEL_PASSENGER_POINT_Y : MODEL_RIDER_POINT_Y;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putDouble("GordiusRearX", this.rearX);
      tag.putDouble("GordiusRearY", this.rearY);
      tag.putDouble("GordiusRearZ", this.rearZ);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.rearX = tag.contains("GordiusRearX") ? tag.getDouble("GordiusRearX") : tag.contains("GordiusFrontX") ? tag.getDouble("GordiusFrontX") : this.getX();
      this.rearY = tag.contains("GordiusRearY") ? tag.getDouble("GordiusRearY") : tag.contains("GordiusFrontY") ? tag.getDouble("GordiusFrontY") : this.getY();
      this.rearZ = tag.contains("GordiusRearZ") ? tag.getDouble("GordiusRearZ") : tag.contains("GordiusFrontZ") ? tag.getDouble("GordiusFrontZ") : this.getZ();
   }

   @Override
   protected String getLoopAnimation() {
      return "animation";
   }
}

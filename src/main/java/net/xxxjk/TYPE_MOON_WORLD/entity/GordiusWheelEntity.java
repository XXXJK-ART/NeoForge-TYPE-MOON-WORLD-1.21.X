package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GordiusWheelEntity extends IskandarMountEntity {
   private static final double MODEL_UNIT = 8.0;
   private static final double CHARIOT_RIDER_POINT_X = -2.0 / MODEL_UNIT;
   private static final double CHARIOT_RIDER_POINT_Z = 17.5 / MODEL_UNIT;
   private static final double CHARIOT_RIDER_POINT_Y = 11.00391 / MODEL_UNIT;
   private static final double CHARIOT_PASSENGER_POINT_X = 2.0 / MODEL_UNIT;
   private static final double CHARIOT_PASSENGER_POINT_Z = 16.2 / MODEL_UNIT;
   private static final double CHARIOT_PASSENGER_POINT_Y = 11.00391 / MODEL_UNIT;
   private static final double THUNDER_STRIKE_RADIUS = 18.0;
   private static final int THUNDER_STRIKE_MIN_DELAY = 35;
   private static final int THUNDER_STRIKE_RANDOM_DELAY = 65;
   private double rearX;
   private double rearY;
   private double rearZ;
   private long nextThunderStrikeTick;

   public GordiusWheelEntity(EntityType<? extends GordiusWheelEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(false);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMountAttributes(3000.0, 0.62);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         snapToNearbyGround();
         tickRearBodyPhysics();
         tickLightningAura(level);
      }
   }

   @Override
   public boolean isNoGravity() {
      return false;
   }

   @Override
   protected void followIskandarCombatIntent(net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity iskandar) {
      LivingEntity target = iskandar.getTarget();
      if (target != null && target.isAlive()) {
         moveTowardGround(target.position().subtract(this.position()), getCombatSpeed());
         return;
      }
      LivingEntity master = iskandar.getEntityMaster();
      if (master != null && master.isAlive() && this.distanceToSqr(master) > 36.0) {
         moveTowardGround(master.position().subtract(this.position()), getFollowSpeed());
         return;
      }
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
   }

   private void moveTowardGround(Vec3 direction, double speed) {
      Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
      if (flat.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 unit = flat.normalize();
      float yaw = (float)(Math.atan2(-unit.x, unit.z) * 180.0 / Math.PI);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      Vec3 motion = this.getDeltaMovement();
      double y = this.onGround() ? 0.0 : Math.max(-0.42, motion.y);
      this.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(unit.x * speed, y, unit.z * speed));
      this.setDeltaMovement(0.0, y, 0.0);
   }

   private void snapToNearbyGround() {
      if (this.onGround() || this.getDeltaMovement().y > 0.0) {
         return;
      }
      double maxSnapDistance = 1.6;
      Vec3 snap = Entity.collideBoundingBox(this, new Vec3(0.0, -maxSnapDistance, 0.0), this.getBoundingBox(), this.level(), java.util.List.of());
      if (snap.y >= -1.0E-3 || snap.y <= -maxSnapDistance + 1.0E-3) {
         return;
      }
      this.setPos(this.getX(), this.getY() + snap.y, this.getZ());
      Vec3 motion = this.getDeltaMovement();
      this.setDeltaMovement(motion.x, 0.0, motion.z);
      this.setOnGround(true);
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
      tickThunderStrike(level);
      if (this.tickCount % 10 != 0) {
         return;
      }
      AABB area = this.getBoundingBox().inflate(2.2, 1.2, 2.2);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         entity -> entity != this && !this.getPassengers().contains(entity) && entity.isAlive() && !this.isAlliedTo(entity))) {
         target.hurt(this.damageSources().lightningBolt(), 5.0F);
      }
   }

   private void tickThunderStrike(ServerLevel level) {
      long now = level.getGameTime();
      if (this.nextThunderStrikeTick <= 0L) {
         scheduleNextThunderStrike(level, now);
      }
      if (now < this.nextThunderStrikeTick) {
         return;
      }
      scheduleNextThunderStrike(level, now);
      LivingEntity target = selectThunderStrikeTarget(level);
      Vec3 strikePos = target != null ? target.position() : randomThunderStrikePosition();
      spawnVisualThunderbolt(level, strikePos);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strikePos.x, strikePos.y + 0.25, strikePos.z, 36, 1.2, 0.35, 1.2, 0.18);
      AABB area = new AABB(strikePos, strikePos).inflate(2.0, 2.5, 2.0);
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::canThunderStrike)) {
         victim.invulnerableTime = 0;
         victim.hurt(this.damageSources().lightningBolt(), 14.0F);
      }
   }

   private void scheduleNextThunderStrike(ServerLevel level, long now) {
      this.nextThunderStrikeTick = now + THUNDER_STRIKE_MIN_DELAY + level.random.nextInt(THUNDER_STRIKE_RANDOM_DELAY + 1);
   }

   private LivingEntity selectThunderStrikeTarget(ServerLevel level) {
      AABB area = this.getBoundingBox().inflate(THUNDER_STRIKE_RADIUS, 6.0, THUNDER_STRIKE_RADIUS);
      LivingEntity selected = null;
      double bestDistance = Double.MAX_VALUE;
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area, this::canThunderStrike)) {
         double distance = candidate.distanceToSqr(this);
         if (distance < bestDistance) {
            bestDistance = distance;
            selected = candidate;
         }
      }
      return selected;
   }

   private boolean canThunderStrike(LivingEntity entity) {
      return entity != this && entity.isAlive() && !this.getPassengers().contains(entity) && !this.isAlliedTo(entity);
   }

   private Vec3 randomThunderStrikePosition() {
      double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
      double radius = 3.0 + this.getRandom().nextDouble() * (THUNDER_STRIKE_RADIUS - 3.0);
      return new Vec3(this.getX() + Math.cos(angle) * radius, this.getY(), this.getZ() + Math.sin(angle) * radius);
   }

   private static void spawnVisualThunderbolt(ServerLevel level, Vec3 pos) {
      LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
      if (lightning == null) {
         return;
      }
      lightning.moveTo(pos.x, pos.y, pos.z);
      lightning.setVisualOnly(true);
      level.addFreshEntity(lightning);
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
   protected double getSeatSideOffset(boolean passengerSeat) {
      return passengerSeat ? CHARIOT_PASSENGER_POINT_X : CHARIOT_RIDER_POINT_X;
   }

   @Override
   protected double getSeatForwardOffset(boolean passengerSeat) {
      // Matches the exported "骑乘点" deck marker on the chariot model.
      return passengerSeat ? CHARIOT_PASSENGER_POINT_Z : CHARIOT_RIDER_POINT_Z;
   }

   @Override
   protected double getSeatHeight(boolean passengerSeat) {
      return passengerSeat ? CHARIOT_PASSENGER_POINT_Y : CHARIOT_RIDER_POINT_Y;
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

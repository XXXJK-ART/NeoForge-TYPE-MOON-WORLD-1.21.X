package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class IskandarMountEntity extends PathfinderMob implements GeoEntity {
   protected static final EntityDataAccessor<Boolean> MOVING = SynchedEntityData.defineId(IskandarMountEntity.class, EntityDataSerializers.BOOLEAN);
   protected static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(IskandarMountEntity.class, EntityDataSerializers.BOOLEAN);
   protected static final double RIDER_SEAT_FORWARD = 0.18;
   protected static final double PASSENGER_SEAT_FORWARD = -0.72;
   protected static final double RIDER_SEAT_HEIGHT = 1.15;
   protected static final double PASSENGER_SEAT_HEIGHT = 1.02;
   private static final String TAG_ORBIT_TARGET = "IskandarMountOrbitTarget";
   private static final String TAG_ORBIT_SIGN = "IskandarMountOrbitSign";
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final Set<UUID> chargeHitTargets = new HashSet<>();
   @Nullable protected UUID iskandarUuid;
   @Nullable protected UUID masterUuid;
   private Vec3 chargeDirection = Vec3.ZERO;
   private int chargeTicksRemaining;
   private float chargeDamage;
   private double chargeWidth;

   protected IskandarMountEntity(EntityType<? extends IskandarMountEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static AttributeSupplier.Builder createMountAttributes(double health, double speed) {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, health)
         .add(Attributes.MOVEMENT_SPEED, speed)
         .add(Attributes.STEP_HEIGHT, 2.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.FOLLOW_RANGE, 96.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(MOVING, false);
      builder.define(CHARGING, false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
   }

   public void bindIskandar(IskandarEntity iskandar, @Nullable LivingEntity master) {
      this.iskandarUuid = iskandar.getUUID();
      this.masterUuid = master != null ? master.getUUID() : null;
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      for (Entity passenger : List.copyOf(this.getPassengers())) {
         if (passenger instanceof Player player && player.isShiftKeyDown()) {
            player.stopRiding();
         }
      }
      if (this.tickCount % 10 == 0) {
         this.ejectUnauthorizedPassengers();
      }
      IskandarEntity iskandar = this.getIskandar(level);
      if (iskandar == null || !iskandar.isAlive()) {
         this.ejectPassengers();
         this.discard();
         return;
      }
      if (iskandar.getVehicle() != this) {
         this.ejectPassengers();
         this.discard();
         return;
      }
      if (this.isCharging()) {
         tickCharge(level, iskandar);
      } else {
         followIskandarCombatIntent(level, iskandar);
      }
      this.entityData.set(MOVING, this.isCharging() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4);
      this.fallDistance = 0.0F;
   }

   protected void followIskandarCombatIntent(ServerLevel level, IskandarEntity iskandar) {
      LivingEntity target = resolveCombatTarget(level, iskandar);
      if (target != null && target.isAlive()) {
         moveAroundTarget(target);
         return;
      }
      LivingEntity master = iskandar.getEntityMaster();
      if (master != null && master.isAlive() && this.distanceToSqr(master) > 36.0) {
         moveToward(master.position().subtract(this.position()), getFollowSpeed());
         return;
      }
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
   }

   @Nullable
   protected LivingEntity resolveCombatTarget(ServerLevel level, IskandarEntity iskandar) {
      LivingEntity target = iskandar.getTarget();
      if (isValidMountCombatTarget(iskandar, target)) {
         return target;
      }
      LivingEntity attacker = iskandar.getLastHurtByMob();
      if (isValidMountCombatTarget(iskandar, attacker) && iskandar.tickCount - iskandar.getLastHurtByMobTimestamp() <= 200) {
         return attacker;
      }
      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      AABB area = this.getBoundingBox().inflate(42.0);
      for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
         mob -> (mob.getTarget() == iskandar || mob.getTarget() == this) && isValidMountCombatTarget(iskandar, mob))) {
         double distance = mob.distanceToSqr(this);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = mob;
         }
      }
      return best;
   }

   protected boolean isValidMountCombatTarget(IskandarEntity iskandar, @Nullable LivingEntity target) {
      return target != null
         && target.isAlive()
         && target != this
         && target != iskandar
         && !this.getPassengers().contains(target)
         && !EntityUtils.isImmunePlayerTarget(target)
         && !EntityUtils.isUntargetableServantTransition(target)
         && !this.isAlliedTo(target)
         && !iskandar.isAlliedTo(target);
   }

   private void moveAroundTarget(LivingEntity target) {
      CompoundTag data = this.getPersistentData();
      if (data.getInt(TAG_ORBIT_TARGET) != target.getId()) {
         data.putInt(TAG_ORBIT_TARGET, target.getId());
         data.putInt(TAG_ORBIT_SIGN, this.getRandom().nextBoolean() ? 1 : -1);
      } else if (this.tickCount % 100 == 0) {
         data.putInt(TAG_ORBIT_SIGN, -data.getInt(TAG_ORBIT_SIGN));
      }

      Vec3 fromTarget = horizontal(target.position().subtract(this.position()));
      if (fromTarget.lengthSqr() < 1.0E-4) {
         fromTarget = horizontal(this.getLookAngle());
      }
      double distance = this.distanceTo(target);
      double desiredRadius = getCombatOrbitRadius();
      Vec3 tangent = new Vec3(-fromTarget.z, 0.0, fromTarget.x).scale(data.getInt(TAG_ORBIT_SIGN));
      double radialError = distance - desiredRadius;
      Vec3 desired = tangent.scale(0.9);
      if (Math.abs(radialError) > 1.5) {
         desired = desired.add(fromTarget.scale(Math.signum(radialError) * Math.min(1.0, Math.abs(radialError) * 0.28)));
      }
      if (distance < 4.0) {
         desired = fromTarget.scale(-1.0);
      }
      moveToward(desired, getCombatSpeed());
   }

   protected double getCombatOrbitRadius() {
      return 8.0;
   }

   protected void moveToward(Vec3 direction, double speed) {
      Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
      if (flat.lengthSqr() < 1.0E-4) {
         return;
      }
      flat = flat.normalize();
      float yaw = (float)(Math.atan2(-flat.x, flat.z) * 180.0 / Math.PI);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      this.move(MoverType.SELF, flat.scale(speed));
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
   }

   protected double getCombatSpeed() {
      return this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.6;
   }

   protected double getFollowSpeed() {
      return this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.1;
   }

   public boolean isCharging() {
      return this.entityData.get(CHARGING);
   }

   public boolean canStartCharge() {
      return !this.isCharging() && this.chargeTicksRemaining <= 0;
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND && !player.isSecondaryUseActive()) {
         if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
         }
         if (this.canPlayerMount(player) && player.startRiding(this, true)) {
            return InteractionResult.SUCCESS;
         }
         return InteractionResult.FAIL;
      }
      return super.mobInteract(player, hand);
   }

   @Override
   protected boolean canAddPassenger(Entity passenger) {
      if (this.getPassengers().size() >= 2) {
         return false;
      }
      if (passenger instanceof IskandarEntity iskandar) {
         return this.getPassengers().isEmpty() && (this.iskandarUuid == null || this.iskandarUuid.equals(iskandar.getUUID()));
      }
      if (passenger instanceof Player player) {
         return canPlayerMount(player);
      }
      return false;
   }

   public boolean canPlayerMount(Player player) {
      return this.getPassengers().size() < 2 && this.isAuthorizedPlayer(player);
   }

   protected boolean isAuthorizedPlayer(Player player) {
      if (!(this.level() instanceof ServerLevel level)) {
         return true;
      }
      IskandarEntity iskandar = this.getIskandar(level);
      if (iskandar == null || this.getPassengers().isEmpty() || this.getPassengers().get(0) != iskandar) {
         return false;
      }
      return this.masterUuid != null && this.masterUuid.equals(player.getUUID());
   }

   protected void ejectUnauthorizedPassengers() {
      for (Entity passenger : List.copyOf(this.getPassengers())) {
         if (passenger instanceof IskandarEntity iskandar) {
            if (this.iskandarUuid == null || !this.iskandarUuid.equals(iskandar.getUUID()) || this.getPassengers().get(0) != passenger) {
               passenger.stopRiding();
            }
         } else if (passenger instanceof Player player) {
            if (!isAuthorizedPlayer(player)) {
               passenger.stopRiding();
            }
         } else {
            passenger.stopRiding();
         }
      }
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction callback) {
      boolean passengerSeat = this.masterUuid != null && this.masterUuid.equals(passenger.getUUID());
      double localX = getSeatSideOffset(passengerSeat);
      double localZ = getSeatForwardOffset(passengerSeat);
      double seatY = getSeatHeight(passengerSeat);
      float yaw = this.getYRot() * ((float)Math.PI / 180.0F);
      double x = Math.cos(yaw) * localX - Math.sin(yaw) * localZ;
      double z = Math.sin(yaw) * localX + Math.cos(yaw) * localZ;
      callback.accept(passenger, this.getX() + x, this.getY() + seatY, this.getZ() + z);
      passenger.setYRot(this.getYRot());
      passenger.setYHeadRot(this.getYRot());
      if (passenger instanceof LivingEntity living) {
         living.setYBodyRot(this.getYRot());
         living.setXRot(0.0F);
      }
   }

   public boolean shouldRedirectPassengerDamage(Entity passenger) {
      return this.isAlive() && this.getPassengers().contains(passenger)
         && (passenger instanceof IskandarEntity || this.masterUuid != null && this.masterUuid.equals(passenger.getUUID()));
   }

   public boolean isBoundCompanion(@Nullable Entity entity) {
      if (entity == null) {
         return false;
      }
      if (entity == this || this.getPassengers().contains(entity)) {
         return true;
      }
      UUID uuid = entity.getUUID();
      return this.iskandarUuid != null && this.iskandarUuid.equals(uuid)
         || this.masterUuid != null && this.masterUuid.equals(uuid);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (this.level().isClientSide || amount <= 0.0F) {
         return false;
      }
      if (this.isBoundCompanion(source.getEntity()) || this.isBoundCompanion(source.getDirectEntity())) {
         return false;
      }
      return super.hurt(source, amount);
   }

   public void performCharge(ServerLevel level, LivingEntity source, float damage, double width) {
      if (!canStartCharge()) {
         return;
      }
      this.entityData.set(CHARGING, true);
      LivingEntity target = source instanceof Mob mob ? mob.getTarget() : null;
      Vec3 direction = target != null && target.isAlive()
         ? target.position().subtract(this.position())
         : this.getLookAngle();
      direction = direction.multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) {
         direction = new Vec3(0.0, 0.0, 1.0);
      }
      this.chargeDirection = direction.normalize();
      this.chargeTicksRemaining = getChargeDurationTicks();
      this.chargeDamage = damage;
      this.chargeWidth = width;
      this.chargeHitTargets.clear();
      this.setYRot((float)(Math.atan2(-this.chargeDirection.x, this.chargeDirection.z) * 180.0 / Math.PI));
      this.setYBodyRot(this.getYRot());
      this.setYHeadRot(this.getYRot());
      level.playSound(null, this.blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.HOSTILE, 1.1F, 0.78F);
   }

   private void tickCharge(ServerLevel level, IskandarEntity source) {
      if (this.chargeTicksRemaining <= 0) {
         finishCharge();
         return;
      }
      double speed = getChargeSpeed();
      this.move(MoverType.SELF, this.chargeDirection.scale(speed));
      AABB hitBox = this.getBoundingBox()
         .expandTowards(this.chargeDirection.scale(speed + 1.1))
         .inflate(this.chargeWidth, 0.95, this.chargeWidth);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitBox,
         entity -> entity != this && !this.getPassengers().contains(entity) && entity.isAlive() && !this.isAlliedTo(entity))) {
         if (!this.chargeHitTargets.add(target.getUUID())) {
            continue;
         }
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().mobAttack(source), this.chargeDamage);
         target.push(this.chargeDirection.x * getChargeKnockback(), 0.28, this.chargeDirection.z * getChargeKnockback());
         target.hurtMarked = true;
      }
      this.chargeTicksRemaining--;
      if (this.chargeTicksRemaining <= 0) {
         finishCharge();
      }
   }

   private void finishCharge() {
      this.chargeTicksRemaining = 0;
      this.chargeDirection = Vec3.ZERO;
      this.chargeHitTargets.clear();
      this.entityData.set(CHARGING, false);
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
   }

   protected int getChargeDurationTicks() {
      return 12;
   }

   protected double getChargeSpeed() {
      return Math.max(0.72, getCombatSpeed() * 1.35);
   }

   protected double getChargeKnockback() {
      return 1.6;
   }

   private static Vec3 horizontal(Vec3 vector) {
      return new Vec3(vector.x, 0.0, vector.z);
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      if (this.isBoundCompanion(other)) {
         return true;
      }
      if (other instanceof MacedonianSoldierEntity soldier && this.iskandarUuid != null) {
         return this.iskandarUuid.equals(soldier.getPersistentData().hasUUID("IonioiHetairoiOwner")
            ? soldier.getPersistentData().getUUID("IonioiHetairoiOwner")
            : null);
      }
      if (this.level() instanceof ServerLevel level) {
         IskandarEntity iskandar = this.getIskandar(level);
         return iskandar != null && iskandar.isAlliedTo(other);
      }
      return false;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.iskandarUuid != null) {
         tag.putUUID("IskandarMountOwner", this.iskandarUuid);
      }
      if (this.masterUuid != null) {
         tag.putUUID("IskandarMountMaster", this.masterUuid);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("IskandarMountOwner")) {
         this.iskandarUuid = tag.getUUID("IskandarMountOwner");
      }
      if (tag.hasUUID("IskandarMountMaster")) {
         this.masterUuid = tag.getUUID("IskandarMountMaster");
      }
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
      boolean passengerSeat = this.masterUuid != null && this.masterUuid.equals(passenger.getUUID());
      return new Vec3(getSeatSideOffset(passengerSeat), getSeatHeight(passengerSeat), getSeatForwardOffset(passengerSeat));
   }

   protected double getSeatSideOffset(boolean passengerSeat) {
      return 0.0;
   }

   protected double getSeatForwardOffset(boolean passengerSeat) {
      return passengerSeat ? PASSENGER_SEAT_FORWARD : RIDER_SEAT_FORWARD;
   }

   protected double getSeatHeight(boolean passengerSeat) {
      return passengerSeat ? PASSENGER_SEAT_HEIGHT : RIDER_SEAT_HEIGHT;
   }

   @Nullable
   protected IskandarEntity getIskandar(ServerLevel level) {
      if (this.iskandarUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.iskandarUuid);
      return entity instanceof IskandarEntity iskandar ? iskandar : null;
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> {
         String animation = this.isCharging()
            ? getChargeAnimation()
            : this.entityData.get(MOVING) ? getMovingAnimation() : getLoopAnimation();
         return event.setAndContinue(RawAnimation.begin().thenLoop(animation));
      }));
   }

   protected abstract String getLoopAnimation();

   protected String getMovingAnimation() {
      return getLoopAnimation();
   }

   protected String getChargeAnimation() {
      return getMovingAnimation();
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

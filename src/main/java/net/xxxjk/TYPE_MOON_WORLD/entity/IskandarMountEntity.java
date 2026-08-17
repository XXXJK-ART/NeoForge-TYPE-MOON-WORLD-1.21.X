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
import net.minecraft.util.Mth;
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
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardIskandarSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
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
   private static final int COMBAT_DIRECT_TICKS = 48;
   private static final int COMBAT_ORBIT_TICKS = 32;
   private static final double COMBAT_ORBIT_MAX_DISTANCE = 24.0;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final Set<UUID> chargeHitTargets = new HashSet<>();
   @Nullable protected UUID iskandarUuid;
   @Nullable protected UUID masterUuid;
   @Nullable protected UUID cardOwnerUuid;
   private Vec3 chargeDirection = Vec3.ZERO;
   private int chargeTicksRemaining;
   private float chargeDamage;
   private double chargeWidth;
   private int blockedMoveTicks;
   private int combatMovementTargetId = -1;
   private int combatMovementStartTick;
   private int combatOrbitSign = 1;

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
      this.cardOwnerUuid = null;
   }

   public void bindCardOwner(ServerPlayer owner) {
      this.iskandarUuid = null;
      this.masterUuid = null;
      this.cardOwnerUuid = owner.getUUID();
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      for (Entity passenger : List.copyOf(this.getPassengers())) {
         if (passenger instanceof Player player && player.isShiftKeyDown() && !keepsShiftForCardControl(player)) {
            player.stopRiding();
         }
      }
      if (this.tickCount % 10 == 0) {
         this.ejectUnauthorizedPassengers();
      }
      ServerPlayer cardOwner = this.getCardOwner(level);
      if (cardOwner != null) {
         tickCardOwnerMount(level, cardOwner);
         return;
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

   private boolean keepsShiftForCardControl(Player player) {
      return this instanceof GordiusWheelEntity && isCardOwner(player);
   }

   private void tickCardOwnerMount(ServerLevel level, ServerPlayer owner) {
      TypeMoonWorldModVariables.PlayerVariables vars = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!owner.isAlive() || !vars.servant_card_transformed || !ServantCardIskandarSkills.SERVANT_ID.equals(vars.servant_card_id)) {
         this.ejectPassengers();
         this.discard();
         return;
      }
      if (owner.getVehicle() != this) {
         if (this instanceof BucephalusEntity) {
            ServantCardIskandarSkills.storeAndDiscardBucephalus(owner, this, false);
         } else if (!this.hasPassenger(owner)) {
            ServantCardIskandarSkills.storeAndDiscardGordiusWheel(owner, this, false);
         }
         return;
      }
      if (this.isCharging()) {
         tickCharge(level, owner);
      } else {
         followCardOwnerInput(owner);
      }
      this.entityData.set(MOVING, this.isCharging() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4);
      this.fallDistance = 0.0F;
   }

   protected void followCardOwnerInput(ServerPlayer owner) {
      float forwardInput = owner.zza;
      float strafeInput = owner.xxa;
      Vec3 forward = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      // Server-side xxa is the player's left impulse, so invert the lateral vector.
      Vec3 desired = forward.scale(forwardInput).add(right.scale(-strafeInput));
      double speed = Math.max(0.0, getCombatSpeed() * 1.1);
      if (desired.lengthSqr() > 1.0E-4) {
         desired = desired.normalize();
         float yaw = (float)(Math.atan2(-desired.x, desired.z) * 180.0 / Math.PI);
         this.setYRot(yaw);
         this.setYBodyRot(yaw);
         this.setYHeadRot(yaw);
         this.move(MoverType.SELF, desired.scale(speed));
      }
      this.setDeltaMovement(0.0, cardOwnerVerticalMotion(owner), 0.0);
      this.hasImpulse = true;
   }

   protected double cardOwnerVerticalMotion(ServerPlayer owner) {
      return this.getDeltaMovement().y;
   }

   protected void followIskandarCombatIntent(ServerLevel level, IskandarEntity iskandar) {
      LivingEntity target = resolveCombatTarget(level, iskandar);
      if (target != null && target.isAlive()) {
         moveTowardCombatTarget(target);
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
      boolean ionioiMarkedTarget = iskandar.isHostileIonioiTarget(target);
      return target != null
         && target.isAlive()
         && target != this
         && target != iskandar
         && !this.getPassengers().contains(target)
         && (!EntityUtils.isImmunePlayerTarget(target) || ionioiMarkedTarget)
         && !EntityUtils.isUntargetableServantTransition(target)
         && !ServantMasterProtection.isProtectedMaster(iskandar, target)
         && !this.isAlliedTo(target)
         && !iskandar.isAlliedTo(target);
   }

   private void moveTowardCombatTarget(LivingEntity target) {
      Vec3 toTarget = horizontal(target.position().subtract(this.position()));
      if (toTarget.lengthSqr() < 1.0E-4) {
         this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
         return;
      }

      if (this.combatMovementTargetId != target.getId()) {
         this.combatMovementTargetId = target.getId();
         this.combatMovementStartTick = this.tickCount;
         this.combatOrbitSign = this.getRandom().nextBoolean() ? 1 : -1;
      }

      double stopDistance = this instanceof GordiusWheelEntity ? 10.0 : 6.0;
      double distance = this.distanceTo(target);
      int cycleTicks = COMBAT_DIRECT_TICKS + COMBAT_ORBIT_TICKS;
      int phase = Math.floorMod(this.tickCount - this.combatMovementStartTick, cycleTicks);
      boolean orbitPhase = phase >= COMBAT_DIRECT_TICKS;
      if (orbitPhase && distance >= stopDistance * 0.85 && distance <= COMBAT_ORBIT_MAX_DISTANCE) {
         moveAroundTarget(target, stopDistance);
         return;
      }
      if (distance > stopDistance) {
         moveToward(toTarget, getCombatSpeed());
         return;
      }

      Vec3 facing = toTarget.normalize();
      float yaw = (float)(Math.atan2(-facing.x, facing.z) * 180.0 / Math.PI);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
   }

   private void moveAroundTarget(LivingEntity target, double stopDistance) {
      Vec3 toTarget = horizontal(target.position().subtract(this.position()));
      if (toTarget.lengthSqr() < 1.0E-4) {
         this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
         return;
      }

      double orbitRadius = Math.max(stopDistance + 1.5, this instanceof GordiusWheelEntity ? 12.0 : 8.0);
      Vec3 radial = toTarget.normalize();
      Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x).scale(this.combatOrbitSign);
      double radialError = this.distanceTo(target) - orbitRadius;
      Vec3 desired = tangent.scale(0.72);
      if (Math.abs(radialError) > 1.25) {
         desired = desired.add(radial.scale(Mth.clamp(radialError * 0.22, -0.9, 0.9)));
      }
      moveToward(desired, getCombatSpeed() * 0.82);
   }

   protected void moveToward(Vec3 direction, double speed) {
      Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
      if (flat.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 before = this.position();
      flat = flat.normalize();
      float yaw = (float)(Math.atan2(-flat.x, flat.z) * 180.0 / Math.PI);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      this.move(MoverType.SELF, flat.scale(speed));
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
      recoverBlockedMountedMove(before, flat);
   }

   protected void recoverBlockedMountedMove(Vec3 before, Vec3 intendedDirection) {
      double movedSqr = horizontal(this.position().subtract(before)).lengthSqr();
      if ((movedSqr < 0.0025 && this.horizontalCollision) || this.isInWall()) {
         this.blockedMoveTicks++;
      } else {
         this.blockedMoveTicks = 0;
         return;
      }
      if (this.blockedMoveTicks < 4) {
         this.setDeltaMovement(this.getDeltaMovement().x, Math.max(0.18, this.getDeltaMovement().y), this.getDeltaMovement().z);
         return;
      }
      for (double up = 0.5; up <= 3.0; up += 0.5) {
         if (this.level().noCollision(this, this.getBoundingBox().move(0.0, up, 0.0))) {
            this.setPos(this.getX() + intendedDirection.x * 0.35, this.getY() + up, this.getZ() + intendedDirection.z * 0.35);
            this.setDeltaMovement(0.0, 0.12, 0.0);
            this.blockedMoveTicks = 0;
            return;
         }
      }
      this.setDeltaMovement(intendedDirection.x * 0.18, 0.32, intendedDirection.z * 0.18);
      this.blockedMoveTicks = 0;
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
         return canPlayerMount(player) || isCardOwner(player);
      }
      return false;
   }

   public boolean canPlayerMount(Player player) {
      return this.getPassengers().size() < 2 && (this.isAuthorizedPlayer(player) || isCardOwner(player));
   }

   protected boolean isAuthorizedPlayer(Player player) {
      if (isCardOwner(player)) {
         return true;
      }
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
         if (!(passenger instanceof Player)) {
            living.setXRot(0.0F);
         }
      }
   }

   public boolean shouldRedirectPassengerDamage(Entity passenger) {
      return this.isAlive() && this.getPassengers().contains(passenger)
         && (passenger instanceof IskandarEntity
            || this.masterUuid != null && this.masterUuid.equals(passenger.getUUID())
            || this.cardOwnerUuid != null && this.cardOwnerUuid.equals(passenger.getUUID()));
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
         || this.masterUuid != null && this.masterUuid.equals(uuid)
         || this.cardOwnerUuid != null && this.cardOwnerUuid.equals(uuid);
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

   @Override
   public void die(DamageSource source) {
      releasePassengersBeforeRemoval();
      super.die(source);
   }

   @Override
   public void remove(RemovalReason reason) {
      releasePassengersBeforeRemoval();
      super.remove(reason);
   }

   private void releasePassengersBeforeRemoval() {
      for (Entity passenger : List.copyOf(this.getPassengers())) {
         passenger.stopRiding();
      }
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
      this.chargeDamage = damage * 2.0F;
      this.chargeWidth = width;
      this.chargeHitTargets.clear();
      this.setYRot((float)(Math.atan2(-this.chargeDirection.x, this.chargeDirection.z) * 180.0 / Math.PI));
      this.setYBodyRot(this.getYRot());
      this.setYHeadRot(this.getYRot());
      level.playSound(null, this.blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.HOSTILE, 1.1F, 0.78F);
   }

   private void tickCharge(ServerLevel level, LivingEntity source) {
      if (this.chargeTicksRemaining <= 0) {
         finishCharge(level, source);
         return;
      }
      double speed = getChargeSpeed();
      this.move(MoverType.SELF, this.chargeDirection.scale(speed));
      onChargeStep(level, source, this.chargeDirection, speed);
      AABB hitBox = this.getBoundingBox()
         .expandTowards(this.chargeDirection.scale(speed + 1.1))
         .inflate(this.chargeWidth, 0.95, this.chargeWidth);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitBox,
         entity -> entity != this && !this.getPassengers().contains(entity) && entity.isAlive()
            && !EntityUtils.isImmunePlayerTarget(entity)
            && !this.isAlliedTo(entity)
            && !ServantMasterProtection.isProtectedMaster(source, entity)
            && this.hasLineOfSight(entity))) {
         if (!this.chargeHitTargets.add(target.getUUID())) {
            continue;
         }
         target.invulnerableTime = 0;
         DamageSource damageSource = source instanceof ServerPlayer player
            ? player.damageSources().playerAttack(player)
            : this.damageSources().mobAttack(source);
         target.hurt(damageSource, this.chargeDamage);
         target.push(this.chargeDirection.x * getChargeKnockback(), 0.28, this.chargeDirection.z * getChargeKnockback());
         target.hurtMarked = true;
         onChargeHit(level, source, target);
      }
      this.chargeTicksRemaining--;
      if (this.chargeTicksRemaining <= 0) {
         finishCharge(level, source);
      }
   }

   private void finishCharge(ServerLevel level, LivingEntity source) {
      this.chargeTicksRemaining = 0;
      this.chargeDirection = Vec3.ZERO;
      this.chargeHitTargets.clear();
      this.entityData.set(CHARGING, false);
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
      onChargeFinished(level, source);
   }

   protected void onChargeStep(ServerLevel level, LivingEntity source, Vec3 direction, double speed) {
   }

   protected void onChargeHit(ServerLevel level, LivingEntity source, LivingEntity target) {
   }

   protected void onChargeFinished(ServerLevel level, LivingEntity source) {
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
      if (this.cardOwnerUuid != null) {
         tag.putUUID("IskandarCardMountOwner", this.cardOwnerUuid);
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
      if (tag.hasUUID("IskandarCardMountOwner")) {
         this.cardOwnerUuid = tag.getUUID("IskandarCardMountOwner");
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

   @Nullable
   protected ServerPlayer getCardOwner(ServerLevel level) {
      return this.cardOwnerUuid == null ? null : level.getServer().getPlayerList().getPlayer(this.cardOwnerUuid);
   }

   public boolean isCardOwner(Entity entity) {
      return entity != null && this.cardOwnerUuid != null && this.cardOwnerUuid.equals(entity.getUUID());
   }

   public boolean isBoundToMaster(LivingEntity target) {
      return target != null && this.masterUuid != null && this.masterUuid.equals(target.getUUID());
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

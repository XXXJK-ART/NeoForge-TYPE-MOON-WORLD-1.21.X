package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.List;
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
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   @Nullable protected UUID iskandarUuid;
   @Nullable protected UUID masterUuid;

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
      followIskandarCombatIntent(iskandar);
      this.entityData.set(MOVING, this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4);
      this.fallDistance = 0.0F;
   }

   protected void followIskandarCombatIntent(IskandarEntity iskandar) {
      LivingEntity target = iskandar.getTarget();
      if (target != null && target.isAlive()) {
         moveToward(target.position().subtract(this.position()), getCombatSpeed());
         return;
      }
      LivingEntity master = iskandar.getEntityMaster();
      if (master != null && master.isAlive() && this.distanceToSqr(master) > 36.0) {
         moveToward(master.position().subtract(this.position()), getFollowSpeed());
         return;
      }
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
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
      double localZ = getSeatForwardOffset(passengerSeat);
      double seatY = getSeatHeight(passengerSeat);
      float yaw = this.getYRot() * ((float)Math.PI / 180.0F);
      double x = -Math.sin(yaw) * localZ;
      double z = Math.cos(yaw) * localZ;
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
      this.entityData.set(CHARGING, true);
      Vec3 direction = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (direction.lengthSqr() < 1.0E-4) {
         direction = new Vec3(0.0, 0.0, 1.0);
      }
      direction = direction.normalize();
      this.move(MoverType.SELF, direction.scale(getCombatSpeed() * 2.7));
      AABB area = this.getBoundingBox().inflate(width, 0.8, width);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         entity -> entity != this && !this.getPassengers().contains(entity) && entity.isAlive() && !this.isAlliedTo(entity))) {
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().mobAttack(source), damage);
         target.push(direction.x * 1.8, 0.25, direction.z * 1.8);
         target.hurtMarked = true;
      }
      level.playSound(null, this.blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.HOSTILE, 1.1F, 0.78F);
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
      return new Vec3(0.0, getSeatHeight(passengerSeat), getSeatForwardOffset(passengerSeat));
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
      controllers.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(RawAnimation.begin().thenLoop(getLoopAnimation()))));
   }

   protected abstract String getLoopAnimation();

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Persistent white dragon mount used by Zhao Yun. */
public final class ZhaoYunHakuryuEntity extends PathfinderMob implements GeoEntity {
   public static final String TAG_RIDER = "ZhaoYunRider";
   public static final String TAG_MASTER = "ZhaoYunMaster";
   private static final EntityDataAccessor<Boolean> NP_ACTIVE = SynchedEntityData.defineId(
      ZhaoYunHakuryuEntity.class, EntityDataSerializers.BOOLEAN);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   @Nullable private UUID riderUuid;
   @Nullable private UUID masterUuid;

   public ZhaoYunHakuryuEntity(EntityType<? extends ZhaoYunHakuryuEntity> type, Level level) {
      super(type, level);
      setPersistenceRequired();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 2000.0)
         .add(Attributes.MOVEMENT_SPEED, 0.35)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.FOLLOW_RANGE, 64.0);
   }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(NP_ACTIVE, false);
   }

   @Override protected void registerGoals() { this.goalSelector.addGoal(0, new FloatGoal(this)); }

   @Override public void tick() {
      super.tick();
      if (!(level() instanceof ServerLevel level)) return;
      ZhaoYunRiderEntity rider = getRider(level);
      if (rider == null || !rider.isAlive()) {
         if (getPassengers().isEmpty()) discard();
         return;
      }
      riderUuid = rider.getUUID();
      LivingEntity master = rider.getEntityMaster();
      masterUuid = master == null ? null : master.getUUID();
      double riderSpeed = rider.getAttributeValue(Attributes.MOVEMENT_SPEED);
      double speedRatio = Math.max(0.35, riderSpeed / 0.36);
      getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35 * speedRatio);
      setNoGravity(false);
      if (rider.isChangbanpoActive()) {
         setNpActive(true);
         rider.tickChangbanpoMount(this);
      } else {
         setNpActive(false);
         followRiderIntent(rider, master);
      }
      fallDistance = 0.0F;
   }

   private void followRiderIntent(ZhaoYunRiderEntity rider, @Nullable LivingEntity master) {
      LivingEntity target = rider.getTarget();
      Vec3 direction = target != null && target.isAlive()
         ? target.position().subtract(position())
         : master != null && master.isAlive() && distanceToSqr(master) > 16.0
            ? master.position().subtract(position()) : rider.getLookAngle();
      Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
      if (flat.lengthSqr() < 1.0E-4) return;
      flat = flat.normalize();
      setYRot((float)(Math.atan2(-flat.x, flat.z) * 180.0 / Math.PI));
      Vec3 velocity = flat.scale(Math.min(0.55, getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.8));
      move(net.minecraft.world.entity.MoverType.SELF, velocity);
      setDeltaMovement(velocity.scale(0.5));
   }

   @Override protected boolean canAddPassenger(Entity passenger) {
      if (getPassengers().size() >= 2) return false;
      if (passenger instanceof ZhaoYunRiderEntity rider) return riderUuid == null || rider.getUUID().equals(riderUuid);
      if (passenger instanceof net.minecraft.world.entity.player.Player player) {
         ZhaoYunRiderEntity rider = level() instanceof ServerLevel server ? getRider(server) : null;
         LivingEntity master = rider == null ? null : rider.getEntityMaster();
         return rider != null && ((masterUuid != null && masterUuid.equals(player.getUUID()))
            || master != null && master.getUUID().equals(player.getUUID()));
      }
      return false;
   }

   @Override protected void positionRider(Entity passenger, MoveFunction callback) {
      int index = getPassengers().indexOf(passenger);
      double y = getY() + 1.05 + index * 0.35;
      double z = getZ() - 0.15 + index * 0.35;
      callback.accept(passenger, getX(), y, z);
   }

   @Override public boolean hurt(DamageSource source, float amount) {
      if (level().isClientSide || amount <= 0.0F) return false;
      boolean hurt = super.hurt(source, amount);
      if (hurt && !isAlive()) ejectPassengers();
      return hurt;
   }

   public boolean isNpActive() { return entityData.get(NP_ACTIVE); }
   public void setNpActive(boolean active) { entityData.set(NP_ACTIVE, active); }
   @Nullable public UUID getRiderUuid() { return riderUuid; }
   @Nullable public UUID getMasterUuid() { return masterUuid; }

   @Nullable private ZhaoYunRiderEntity getRider(ServerLevel level) {
      for (Entity passenger : getPassengers()) if (passenger instanceof ZhaoYunRiderEntity rider) return rider;
      if (riderUuid != null && level.getEntity(riderUuid) instanceof ZhaoYunRiderEntity rider) return rider;
      return null;
   }

   @Override public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) return true;
      return other instanceof ZhaoYunRiderEntity rider && riderUuid != null && riderUuid.equals(rider.getUUID())
         || other.getUUID().equals(masterUuid);
   }

   @Override public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (riderUuid != null) tag.putUUID(TAG_RIDER, riderUuid);
      if (masterUuid != null) tag.putUUID(TAG_MASTER, masterUuid);
      tag.putBoolean("ChangbanpoActive", isNpActive());
   }

   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID(TAG_RIDER)) riderUuid = tag.getUUID(TAG_RIDER);
      if (tag.hasUUID(TAG_MASTER)) masterUuid = tag.getUUID(TAG_MASTER);
      setNpActive(tag.getBoolean("ChangbanpoActive"));
   }

   @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
      int index = getPassengers().indexOf(passenger);
      return new Vec3(0.0, 1.05 + index * 0.35, -0.15 + index * 0.35);
   }

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> {
         event.getController().setAnimation(RawAnimation.begin().thenLoop(
            isNpActive() ? "gallop" : event.isMoving() ? "walk" : "standing"));
         return PlayState.CONTINUE;
      }));
   }

   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

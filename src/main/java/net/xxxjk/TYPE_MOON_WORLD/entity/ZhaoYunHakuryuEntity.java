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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
   public static final String TAG_SKILL_OWNER = "ZhaoYunSkillOwner";
   private static final EntityDataAccessor<Boolean> NP_ACTIVE = SynchedEntityData.defineId(
      ZhaoYunHakuryuEntity.class, EntityDataSerializers.BOOLEAN);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   @Nullable private UUID riderUuid;
   @Nullable private UUID masterUuid;
   @Nullable private UUID skillOwnerUuid;

   public ZhaoYunHakuryuEntity(EntityType<? extends ZhaoYunHakuryuEntity> type, Level level) {
      super(type, level);
      setPersistenceRequired();
      setNoGravity(false);
   }

   /** Hakuryu is a ground mount, never a flying entity. */
   @Override public boolean isNoGravity() { return false; }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 2000.0)
         .add(Attributes.MOVEMENT_SPEED, 0.35)
         // Allow the ground mount to step up three-block ledges like Zhao Yun.
         .add(Attributes.STEP_HEIGHT, 3.0)
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
      if (skillOwnerUuid != null) {
         Entity ownerEntity = level.getEntity(skillOwnerUuid);
         if (!(ownerEntity instanceof net.minecraft.server.level.ServerPlayer owner) || !owner.isAlive()
            || getPassengers().isEmpty()) {
            discard();
            return;
         }
         if (getPassengers().get(0) == owner && !isNpActive()) {
            float forwardInput = owner.zza;
            float strafeInput = owner.xxa;
            float yaw = owner.getYRot();
            Vec3 forward = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw)));
            Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
            Vec3 intent = forward.scale(forwardInput).add(right.scale(strafeInput));
            if (intent.lengthSqr() > 1.0E-4) {
               intent = intent.normalize();
               setYRot(yaw);
               double speed = getAttributeValue(Attributes.MOVEMENT_SPEED) * (owner.isSprinting() ? 1.25 : 1.0);
               move(net.minecraft.world.entity.MoverType.SELF, intent.scale(speed));
               setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
               if (horizontalCollision && onGround()) getJumpControl().jump();
            } else {
               preserveGravityWhileStopping();
            }
            fallDistance = 0.0F;
         }
         return;
      }
      ZhaoYunRiderEntity rider = getRider(level);
      if (rider == null || !rider.isAlive()) {
         // The mount is persistent even while Zhao Yun is still on foot.
         if ((riderUuid == null || rider != null) && getPassengers().isEmpty()) discard();
         preserveGravityWhileStopping();
         return;
      }
      riderUuid = rider.getUUID();
      LivingEntity master = rider.getEntityMaster();
      masterUuid = master == null ? null : master.getUUID();
      if (rider.getVehicle() != this) {
         setNpActive(false);
         rider.onHakuryuDismounted(this);
         discard();
         preserveGravityWhileStopping();
         fallDistance = 0.0F;
         return;
      }
      double riderSpeed = rider.getAttributeValue(Attributes.MOVEMENT_SPEED);
      if (rider.getPersistentData().getBoolean("RidingAPlusActive")) {
         riderSpeed *= 1.0 + rider.getPersistentData().getDouble("RidingAPlusSpeedBonus");
      }
      double speedRatio = Math.max(0.35, riderSpeed / 0.2);
      getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35 * speedRatio);
      setNoGravity(false);
      if (rider.isChangbanpoCharging()) {
         // Chanting does not immobilize Zhao Yun. Follow the normal target or
         // master destination until the invocation releases the charge.
         setNpActive(false);
         followRiderIntent(rider, master);
      } else if (rider.isChangbanpoActive()) {
         // Changbanpo supplies only horizontal thrust; normal gravity remains
         // active so Hakuryu cannot hover.
         setNpActive(true);
         rider.tickChangbanpoMount(this);
      } else {
         setNpActive(false);
         followRiderIntent(rider, master);
      }
      fallDistance = 0.0F;
   }

   @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND && !player.isSecondaryUseActive()
         && canAddPassenger(player) && player.startRiding(this, true)) {
         return InteractionResult.sidedSuccess(level().isClientSide);
      }
      return super.mobInteract(player, hand);
   }

   private void followRiderIntent(ZhaoYunRiderEntity rider, @Nullable LivingEntity master) {
      LivingEntity target = rider.getTarget();
      Vec3 direction;
      if (target != null && target.isAlive()) {
         direction = target.position().subtract(position());
      } else if (master != null && master.isAlive() && distanceToSqr(master) > 16.0) {
         direction = master.position().subtract(position());
      } else {
         // Do not treat Zhao Yun's facing as a movement command. This keeps
         // the mount stationary when there is no target or follow destination.
         preserveGravityWhileStopping();
         return;
      }
      Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
      if (flat.lengthSqr() < 1.0E-4) {
         preserveGravityWhileStopping();
         return;
      }
      flat = flat.normalize();
      setYRot((float)(Math.atan2(-flat.x, flat.z) * 180.0 / Math.PI));
      Vec3 velocity = flat.scale(getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.8);
      move(net.minecraft.world.entity.MoverType.SELF, velocity);
      if (horizontalCollision && onGround()) {
         getJumpControl().jump();
      }
      setDeltaMovement(velocity.x * 0.5, getDeltaMovement().y, velocity.z * 0.5);
   }

   private void preserveGravityWhileStopping() {
      Vec3 current = getDeltaMovement();
      setDeltaMovement(0.0, current.y, 0.0);
   }

   /** Binds the persistent mount to its Zhao Yun owner before either rider mounts. */
   public void bindRider(ZhaoYunRiderEntity rider, @Nullable LivingEntity master) {
      riderUuid = rider.getUUID();
      if (master != null) masterUuid = master.getUUID();
   }

   /** Binds this horse to a transformed Zhao Yun card player. */
   public void bindSkillOwner(net.minecraft.server.level.ServerPlayer owner) {
      skillOwnerUuid = owner.getUUID();
      getPersistentData().putUUID(TAG_SKILL_OWNER, skillOwnerUuid);
   }

   public boolean isSkillMount() {
      return skillOwnerUuid != null;
   }

   @Override protected boolean canAddPassenger(Entity passenger) {
      if (getPassengers().size() >= 2) return false;
      if (passenger instanceof ZhaoYunRiderEntity rider) {
         // Before the first server tick only the first Zhao Yun may claim this mount.
         return riderUuid != null ? rider.getUUID().equals(riderUuid) : getPassengers().isEmpty();
      }
      if (passenger instanceof net.minecraft.world.entity.player.Player player) {
         if (skillOwnerUuid != null) {
            return skillOwnerUuid.equals(player.getUUID()) && getPassengers().isEmpty();
         }
         // The client cannot resolve the server-side servant/master link. Let it
         // predict the mount interaction; the server performs the real check.
         if (!(level() instanceof ServerLevel)) return true;
         ZhaoYunRiderEntity rider = level() instanceof ServerLevel server ? getRider(server) : null;
         LivingEntity master = rider == null ? null : rider.getEntityMaster();
         return rider != null && ((masterUuid != null && masterUuid.equals(player.getUUID()))
            || master != null && master.getUUID().equals(player.getUUID()));
      }
      return false;
   }

   @Override protected void positionRider(Entity passenger, MoveFunction callback) {
      int index = getPassengers().indexOf(passenger);
      double localZ = -0.15 + index * 0.35;
      float yaw = getYRot() * ((float)Math.PI / 180.0F);
      // Entity yaw 0 faces +Z, so rotate the local seat offset around the mount.
      double offsetX = -Math.sin(yaw) * localZ;
      double offsetZ = Math.cos(yaw) * localZ;
      double seatY = 0.82 + index * 0.35;
      if (passenger instanceof Player) seatY -= 0.5;
      callback.accept(passenger, getX() + offsetX, getY() + seatY, getZ() + offsetZ);
      if (passenger instanceof ZhaoYunRiderEntity rider) {
         float forwardYaw = getYRot();
         rider.setYRot(forwardYaw);
         rider.setYBodyRot(forwardYaw);
         rider.setYHeadRot(forwardYaw);
         rider.setXRot(0.0F);
      }
   }

   @Override public boolean hurt(DamageSource source, float amount) {
      if (level().isClientSide || amount <= 0.0F) return false;
      if (isNpActive()) {
         amount *= 0.05F;
      }
      boolean hurt = super.hurt(source, amount);
      if (hurt && !isAlive()) {
         markSkillOwnerDead();
         if (level() instanceof ServerLevel server) {
            ZhaoYunRiderEntity rider = getRider(server);
            if (rider != null) rider.onHakuryuDeath(this);
         }
         ejectPassengers();
      }
      return hurt;
   }

   @Override public void die(DamageSource source) {
      markSkillOwnerDead();
      if (level() instanceof ServerLevel server) {
         ZhaoYunRiderEntity rider = getRider(server);
         if (rider != null) rider.onHakuryuDeath(this);
      }
      super.die(source);
   }

   @Override public void remove(RemovalReason reason) {
      if (!level().isClientSide() && reason != RemovalReason.UNLOADED_TO_CHUNK
         && reason != RemovalReason.UNLOADED_WITH_PLAYER && level() instanceof ServerLevel server) {
         if (!isAlive()) markSkillOwnerDead();
         ZhaoYunRiderEntity rider = getRider(server);
         if (rider != null && !isAlive()) rider.onHakuryuDeath(this);
      }
      super.remove(reason);
   }

   private void markSkillOwnerDead() {
      if (skillOwnerUuid != null && level() instanceof ServerLevel server
         && server.getEntity(skillOwnerUuid) instanceof net.minecraft.server.level.ServerPlayer owner) {
         owner.getPersistentData().putLong(
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardZhaoYunSkills.TAG_SKILL_MOUNT_COOLDOWN,
            level().getGameTime() + 30L * 20L);
      }
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
      if (skillOwnerUuid != null && skillOwnerUuid.equals(other.getUUID())) return true;
      return other instanceof ZhaoYunRiderEntity rider && riderUuid != null && riderUuid.equals(rider.getUUID())
         || other.getUUID().equals(masterUuid);
   }

   @Override public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (riderUuid != null) tag.putUUID(TAG_RIDER, riderUuid);
      if (masterUuid != null) tag.putUUID(TAG_MASTER, masterUuid);
      if (skillOwnerUuid != null) tag.putUUID(TAG_SKILL_OWNER, skillOwnerUuid);
      tag.putBoolean("ChangbanpoActive", isNpActive());
   }

   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID(TAG_RIDER)) riderUuid = tag.getUUID(TAG_RIDER);
      if (tag.hasUUID(TAG_MASTER)) masterUuid = tag.getUUID(TAG_MASTER);
      if (tag.hasUUID(TAG_SKILL_OWNER)) skillOwnerUuid = tag.getUUID(TAG_SKILL_OWNER);
      setNpActive(tag.getBoolean("ChangbanpoActive"));
   }

   @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
      int index = getPassengers().indexOf(passenger);
      double seatY = 0.82 + index * 0.35;
      if (passenger instanceof Player) seatY -= 0.5;
      return new Vec3(0.0, seatY, -0.15 + index * 0.35);
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

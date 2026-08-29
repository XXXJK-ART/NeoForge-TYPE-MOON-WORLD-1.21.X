package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.mixin.LivingEntityInputAccessor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun.ZhaoYunHakuryuRideService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;
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
   /** Ground speed in blocks per tick; deliberately above a sprinting player's speed. */
   private static final double HAKURYU_BASE_SPEED = 0.65;
   private static final double HAKURYU_CARD_SPEED_SCALE = 0.55;
   private static final double HAKURYU_NPC_SPEED_SCALE = 0.45;
   private static final double RIDER_SEAT_FORWARD = 0.18;
   private static final double RIDER_SEAT_HEIGHT = 0.82;
   private static final double MASTER_SEAT_FORWARD = 0.72;
   private static final double MASTER_SEAT_HEIGHT = 0.68;
   private static final int RIDER_RELINK_GRACE_TICKS = 20;
   private static final int COMBAT_DIRECT_TICKS = 36;
   private static final int COMBAT_ORBIT_TICKS = 24;
   private static final double COMBAT_ORBIT_MAX_DISTANCE = 16.0;
   private static final double COMBAT_STOP_DISTANCE = 4.6;
   /** Matches Zhao Yun's Rider-card big-jump vertical impulse (A agility). */
   private static final double ZHAO_YUN_BIG_JUMP_VERTICAL = 1.14;
   public static final String TAG_RIDER = "ZhaoYunRider";
   public static final String TAG_MASTER = "ZhaoYunMaster";
   public static final String TAG_SKILL_OWNER = "ZhaoYunSkillOwner";
   private static final String TAG_NP_MOUNT = "ZhaoYunNoblePhantasmMount";
   private static final EntityDataAccessor<Boolean> NP_ACTIVE = SynchedEntityData.defineId(
      ZhaoYunHakuryuEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> MOVING = SynchedEntityData.defineId(
      ZhaoYunHakuryuEntity.class, EntityDataSerializers.BOOLEAN);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   @Nullable private UUID riderUuid;
   @Nullable private UUID masterUuid;
   @Nullable private UUID skillOwnerUuid;
   /** True while this is the temporary card Noble Phantasm horse. */
   private boolean npMount;
   private boolean jumpInputPrevious;
   private int jumpCount;
   private boolean landingGrounded = true;
   private int airborneTicks;
   private double airborneMaxY;
   private long lastLandingImpactTick = Long.MIN_VALUE;
   private int riderRelinkGraceTicks;
   private int combatMovementTargetId = -1;
   private int combatMovementStartTick;
   private int combatOrbitSign = 1;
   private int blockedCombatMoveTicks;

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
         .add(Attributes.MOVEMENT_SPEED, HAKURYU_BASE_SPEED)
         // Allow the ground mount to step up three-block ledges like Zhao Yun.
         .add(Attributes.STEP_HEIGHT, 3.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.FOLLOW_RANGE, 64.0);
   }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(NP_ACTIVE, false);
      builder.define(MOVING, false);
   }

   @Override protected void registerGoals() { this.goalSelector.addGoal(0, new FloatGoal(this)); }

   @Override public void tick() {
      super.tick();
      if (!(level() instanceof ServerLevel level)) return;
      if (skillOwnerUuid != null) {
         refreshSkillOwnerMaster(level);
      }
      // Make Shift dismount authoritative on the mount itself as well as the
      // player tick/mixin path. This also works when Zhao Yun occupies seat 0
      // and the master occupies seat 1.
      for (Entity passenger : java.util.List.copyOf(getPassengers())) {
         // Authorization is checked when mounting. Once a player is already
         // seated, Shift must always be able to detach that passenger,
         // including the secondary seat behind Zhao Yun.
         if (passenger instanceof Player player && player.isShiftKeyDown()) {
            player.stopRiding();
         }
      }
      if (tickCount % 5 == 0) {
         ejectUnauthorizedPassengers(level);
      }
      tickLandingImpact(level);
      if (skillOwnerUuid != null) {
         Entity ownerEntity = level.getEntity(skillOwnerUuid);
         if (!(ownerEntity instanceof net.minecraft.server.level.ServerPlayer owner) || !owner.isAlive()
            || getPassengers().isEmpty()) {
            setMovingState(false);
            discard();
            return;
         }
         refreshSkillOwnerMaster(owner);
         // Safety for old/stale NP horses after reloads or interrupted ticks:
         // the 95% reduction belongs only to the timed Noble Phantasm window.
         if (!npMount && isNpActive()) {
            setNpActive(false);
         } else if (npMount && owner.getPersistentData().getLong(
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardZhaoYunSkills.TAG_NP_UNTIL)
            <= level.getGameTime()) {
            promoteToSkillMount(owner);
         }
         if (getPassengers().get(0) == owner && !isNpActive()) {
            float forwardInput = owner.zza;
            float strafeInput = owner.xxa;
            float yaw = owner.getYRot();
            double ownerSpeed = owner.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double ownerBaseSpeed = Math.max(0.1, owner.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
            double speedRatio = Math.max(0.35, ownerSpeed / ownerBaseSpeed);
            // Keep the mount clearly faster than both walking and sprinting,
            // while preserving every movement-speed modifier on the card.
            double mountSpeed = Math.max(HAKURYU_BASE_SPEED, HAKURYU_CARD_SPEED_SCALE * speedRatio);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(mountSpeed);
            // A mounted player's look direction is the mount's direction, even
            // while standing still. Keep body/head rotation in sync so the
            // horse never drifts away from the rider's facing.
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
            handleControlledJump(owner);
            Vec3 forward = new Vec3(-Math.sin(Math.toRadians(yaw)), 0.0, Math.cos(Math.toRadians(yaw)));
            Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
            // Server-side xxa is the player's left impulse. The old mapping
            // treated it as a right impulse, so A/left made Hakuryu move
            // right. Invert only the lateral component; forward/back stays
            // unchanged.
            Vec3 intent = forward.scale(forwardInput).add(right.scale(-strafeInput));
            if (intent.lengthSqr() > 1.0E-4) {
               intent = intent.normalize();
               double speed = mountSpeed * (owner.isSprinting() ? 1.35 : 1.0);
               move(net.minecraft.world.entity.MoverType.SELF, intent.scale(speed));
               setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
               if (horizontalCollision && onGround()) jumpWithZhaoYunPower();
               setMovingState(true);
            } else {
               preserveGravityWhileStopping();
               setMovingState(false);
            }
            fallDistance = 0.0F;
         } else {
            jumpInputPrevious = false;
            if (onGround()) jumpCount = 0;
            setMovingState(isNpActive());
         }
         return;
      }
      ZhaoYunRiderEntity rider = getRider(level);
      if (rider == null || !rider.isAlive()) {
         // A servant mount must never become an orphan. This also covers a
         // dead Zhao Yun still present in the passenger list and an unloaded
         // rider UUID that can no longer be resolved on the server.
         if (riderUuid != null && riderRelinkGraceTicks++ < RIDER_RELINK_GRACE_TICKS) {
            preserveGravityWhileStopping();
            setMovingState(false);
            return;
         }
         if (rider != null) rider.onHakuryuDeath(this);
         ejectPassengers();
         setNpActive(false);
         discard();
         preserveGravityWhileStopping();
         setMovingState(false);
         return;
      }
      riderRelinkGraceTicks = 0;
      riderUuid = rider.getUUID();
      LivingEntity master = rider.getEntityMaster();
      if (master != null) masterUuid = master.getUUID();
      if (rider.getVehicle() != this) {
         if (tryRelinkRider(level, rider)) {
            preserveGravityWhileStopping();
            setMovingState(false);
            return;
         }
         if (riderRelinkGraceTicks++ < RIDER_RELINK_GRACE_TICKS) {
            preserveGravityWhileStopping();
            setMovingState(false);
            return;
         }
         setNpActive(false);
         rider.onHakuryuDismounted(this);
         discard();
         preserveGravityWhileStopping();
         setMovingState(false);
         fallDistance = 0.0F;
         return;
      }
      riderRelinkGraceTicks = 0;
      double riderSpeed = rider.getAttributeValue(Attributes.MOVEMENT_SPEED);
      if (rider.getPersistentData().getBoolean("RidingAPlusActive")) {
         riderSpeed *= 1.0 + rider.getPersistentData().getDouble("RidingAPlusSpeedBonus");
      }
      double speedRatio = Math.max(0.35, riderSpeed / 0.2);
      getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
         Math.max(HAKURYU_BASE_SPEED, HAKURYU_NPC_SPEED_SCALE * speedRatio));
      setNoGravity(false);
      if (rider.isChangbanpoCharging()) {
         // Chanting does not immobilize Zhao Yun. Follow the normal target or
         // master destination until the invocation releases the charge.
         setNpActive(false);
         followRiderIntent(level, rider, master);
      } else if (rider.isChangbanpoActive()) {
         // Changbanpo supplies only horizontal thrust; normal gravity remains
         // active so Hakuryu cannot hover.
         setNpActive(true);
         rider.tickChangbanpoMount(this);
      } else {
         setNpActive(false);
         followRiderIntent(level, rider, master);
      }
      setMovingState(isNpActive() || getDeltaMovement().horizontalDistanceSqr() > 1.0E-4);
      fallDistance = 0.0F;
   }

   /** One ground jump plus two additional jumps while airborne. */
   private void handleControlledJump(net.minecraft.server.level.ServerPlayer owner) {
      boolean jumpPressed = ((LivingEntityInputAccessor)owner).typemoonworld$isJumping();
      if (onGround() && getDeltaMovement().y <= 0.0) {
         jumpCount = 0;
      }
      if (jumpPressed && !jumpInputPrevious) {
         if (onGround()) {
            jumpWithZhaoYunPower();
            jumpCount = 1;
         } else if (jumpCount < 3) {
            jumpWithZhaoYunPower();
            jumpCount++;
         }
      }
      jumpInputPrevious = jumpPressed;
   }

   /** Uses the same vertical impulse as Zhao Yun's card big jump. */
   public void jumpWithZhaoYunPower() {
      jumpFromGround();
      Vec3 motion = getDeltaMovement();
      setDeltaMovement(motion.x, Math.max(motion.y, ZHAO_YUN_BIG_JUMP_VERTICAL), motion.z);
      hasImpulse = true;
      fallDistance = 0.0F;
      if (level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.12, getZ(),
            18, 0.65, 0.12, 0.65, 0.08);
         level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.35, getZ(),
            10, 0.32, 0.18, 0.32, 0.05);
         level.playSound(null, blockPosition(), SoundEvents.HORSE_JUMP, SoundSource.HOSTILE, 0.9F, 0.9F);
      }
   }

   private void tickLandingImpact(ServerLevel level) {
      boolean grounded = onGround();
      if (!grounded) {
         if (landingGrounded) {
            airborneMaxY = getY();
            airborneTicks = 0;
         }
         airborneMaxY = Math.max(airborneMaxY, getY());
         airborneTicks++;
      } else if (!landingGrounded) {
         double drop = Math.max(0.0, airborneMaxY - getY());
         if (!isNpActive() && airborneTicks >= 8 && drop >= 5.0
            && level.getGameTime() - lastLandingImpactTick >= 10L) {
            performLandingImpact(level, drop);
            lastLandingImpactTick = level.getGameTime();
         }
         airborneTicks = 0;
         airborneMaxY = getY();
      }
      landingGrounded = grounded;
   }

   private void performLandingImpact(ServerLevel level, double drop) {
      LivingEntity attacker = getPassengers().stream()
         .filter(LivingEntity.class::isInstance)
         .map(LivingEntity.class::cast)
         .findFirst()
         .orElse(this);
      DamageSource source = attacker instanceof Player player
         ? player.damageSources().playerAttack(player)
         : damageSources().mobAttack(attacker);
      float damage = (float)Math.min(42.0, 26.0 + Math.max(0.0, drop - 5.0) * 3.0);
      AABB area = getBoundingBox().inflate(3.2, 1.2, 3.2);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         entity -> entity != this && !getPassengers().contains(entity) && entity.isAlive()
            && !isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(entity))) {
         target.hurt(source, damage);
         Vec3 away = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() < 1.0E-4) away = getLookAngle().multiply(1.0, 0.0, 1.0);
         away = away.normalize();
         target.push(away.x * 1.15, 0.42, away.z * 1.15);
         target.hurtMarked = true;
      }
      TerrainImpactService.impact(level, attacker, position().add(0.0, 0.2, 0.0),
         TerrainImpactProfile.of(TerrainImpactProfile.Tier.MEDIUM),
         TerrainImpactService.Shape.GROUND_LOWER_HEMISPHERE);
      level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.16, getZ(),
         30, 1.1, 0.18, 1.1, 0.12);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.12, getZ(),
         14, 0.9, 0.16, 0.9, 0.06);
      level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.3, getZ(),
         18, 1.0, 0.25, 1.0, 0.08);
      level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
         SoundSource.HOSTILE, 0.8F, 0.72F);
      level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
         SoundSource.HOSTILE, 0.9F, 0.65F);
   }

   @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (hand == InteractionHand.MAIN_HAND && !player.isSecondaryUseActive()) {
         if (level().isClientSide) {
            return InteractionResult.SUCCESS;
         }
         if (player instanceof ServerPlayer serverPlayer
            && ZhaoYunHakuryuRideService.tryToggle(serverPlayer, this)) {
            return InteractionResult.SUCCESS;
         }
         return InteractionResult.FAIL;
      }
      return super.mobInteract(player, hand);
   }

   private void followRiderIntent(ServerLevel level, ZhaoYunRiderEntity rider, @Nullable LivingEntity master) {
      LivingEntity target = resolveRiderCombatTarget(level, rider);
      if (target != null) {
         moveTowardCombatTarget(target, rider);
         return;
      }
      resetCombatMovement();
      if (master != null && master.isAlive() && distanceToSqr(master) > 16.0) {
         moveToward(master.position().subtract(position()), getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.55);
         return;
      }
      // Do not treat Zhao Yun's facing as a movement command. This keeps the
      // mount stationary when there is no target or follow destination.
      preserveGravityWhileStopping();
   }

   @Nullable
   private LivingEntity resolveRiderCombatTarget(ServerLevel level, ZhaoYunRiderEntity rider) {
      LivingEntity target = EntityUtils.redirectMountedCombatTarget(rider, rider.getTarget());
      if (isValidRiderCombatTarget(rider, target)) {
         return target;
      }

      LivingEntity attacker = rider.getLastHurtByMob();
      if (isValidRiderCombatTarget(rider, attacker)
         && rider.tickCount - rider.getLastHurtByMobTimestamp() <= 200) {
         return attacker;
      }

      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(
         net.minecraft.world.entity.Mob.class,
         getBoundingBox().inflate(42.0),
         candidate -> (candidate.getTarget() == rider || candidate.getTarget() == this)
            && isValidRiderCombatTarget(rider, candidate))) {
         double distance = distanceToSqr(mob);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = mob;
         }
      }
      return best;
   }

   private boolean isValidRiderCombatTarget(ZhaoYunRiderEntity rider, @Nullable LivingEntity target) {
      return target != null
         && target.isAlive()
         && target != this
         && target != rider
         && !getPassengers().contains(target)
         && !EntityUtils.isImmunePlayerTarget(target)
         && !EntityUtils.isUntargetableServantTransition(target)
         && !isAlliedTo(target)
         && !rider.isAlliedTo(target);
   }

   private void moveTowardCombatTarget(LivingEntity target, ZhaoYunRiderEntity rider) {
      if (combatMovementTargetId != target.getId()) {
         combatMovementTargetId = target.getId();
         combatMovementStartTick = tickCount;
         combatOrbitSign = getRandom().nextBoolean() ? 1 : -1;
      }

      double distance = distanceTo(target);
      boolean forceMelee = rider.getPersistentData().getLong(
         ZhaoYunRiderEntity.TAG_FORCE_MELEE_UNTIL) > level().getGameTime();
      int cycleTicks = COMBAT_DIRECT_TICKS + COMBAT_ORBIT_TICKS;
      int phase = Math.floorMod(tickCount - combatMovementStartTick, cycleTicks);
      boolean orbitPhase = !forceMelee && phase >= COMBAT_DIRECT_TICKS;
      if (orbitPhase && distance >= COMBAT_STOP_DISTANCE * 0.85
         && distance <= COMBAT_ORBIT_MAX_DISTANCE) {
         moveAroundCombatTarget(target);
         return;
      }

      Vec3 toTarget = horizontal(target.position().subtract(position()));
      if (distance > COMBAT_STOP_DISTANCE && toTarget.lengthSqr() > 1.0E-4) {
         moveToward(toTarget, Math.max(0.55, getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.6));
         return;
      }

      resetBlockedCombatMove();
      faceHorizontal(toTarget);
      preserveGravityWhileStopping();
   }

   private void moveAroundCombatTarget(LivingEntity target) {
      Vec3 toTarget = horizontal(target.position().subtract(position()));
      if (toTarget.lengthSqr() < 1.0E-4) {
         preserveGravityWhileStopping();
         return;
      }
      Vec3 radial = toTarget.normalize();
      Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x).scale(combatOrbitSign);
      double orbitRadius = 7.0;
      double radialError = distanceTo(target) - orbitRadius;
      Vec3 desired = tangent.scale(0.72);
      if (Math.abs(radialError) > 1.0) {
         desired = desired.add(radial.scale(Mth.clamp(radialError * 0.24, -0.9, 0.9)));
      }
      moveToward(desired, Math.max(0.5, getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.25));
   }

   private void moveToward(Vec3 direction, double speed) {
      Vec3 flat = horizontal(direction);
      if (flat.lengthSqr() < 1.0E-4) {
         preserveGravityWhileStopping();
         return;
      }
      Vec3 before = position();
      flat = flat.normalize();
      faceHorizontal(flat);
      move(net.minecraft.world.entity.MoverType.SELF, flat.scale(speed));
      setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
      recoverBlockedCombatMove(before, flat);
   }

   private void recoverBlockedCombatMove(Vec3 before, Vec3 intendedDirection) {
      double movedSqr = horizontal(position().subtract(before)).lengthSqr();
      if ((movedSqr < 0.0025 && this.horizontalCollision) || this.isInWall()) {
         blockedCombatMoveTicks++;
      } else {
         blockedCombatMoveTicks = 0;
         return;
      }
      if (blockedCombatMoveTicks < 4) {
         return;
      }
      if (onGround()) {
         jumpFromGround();
         setDeltaMovement(0.0, Math.max(0.72, getDeltaMovement().y), 0.0);
      } else {
         Vec3 escape = new Vec3(-intendedDirection.z, 0.0, intendedDirection.x)
            .scale(combatOrbitSign * 0.45);
         move(net.minecraft.world.entity.MoverType.SELF, escape);
      }
      blockedCombatMoveTicks = 0;
   }

   private void faceHorizontal(Vec3 direction) {
      Vec3 flat = horizontal(direction);
      if (flat.lengthSqr() < 1.0E-4) {
         return;
      }
      float yaw = (float)(Math.atan2(-flat.x, flat.z) * 180.0 / Math.PI);
      setYRot(yaw);
      setYBodyRot(yaw);
      setYHeadRot(yaw);
   }

   private void resetCombatMovement() {
      combatMovementTargetId = -1;
      combatMovementStartTick = tickCount;
      resetBlockedCombatMove();
   }

   private void resetBlockedCombatMove() {
      blockedCombatMoveTicks = 0;
   }

   private static Vec3 horizontal(Vec3 vector) {
      return new Vec3(vector.x, 0.0, vector.z);
   }

   private void preserveGravityWhileStopping() {
      Vec3 current = getDeltaMovement();
      setDeltaMovement(0.0, current.y, 0.0);
   }

   public void requestRiderRelinkGrace() {
      riderRelinkGraceTicks = 0;
   }

   private boolean tryRelinkRider(ServerLevel level, ZhaoYunRiderEntity rider) {
      if (rider.isPassenger() || rider.level() != level || !rider.isAlive()) {
         return false;
      }
      List<Entity> previousPassengers = List.copyOf(getPassengers());
      for (Entity passenger : previousPassengers) {
         if (passenger != rider) passenger.stopRiding();
      }
      if (rider.distanceToSqr(this) > 16.0) {
         rider.teleportTo(getX(), getY() + RIDER_SEAT_HEIGHT, getZ());
      }
      if (!rider.startRiding(this, true)) {
         return false;
      }
      for (Entity passenger : previousPassengers) {
         if (passenger instanceof Player player && player.isAlive() && canPlayerMount(player)) {
            player.startRiding(this, true);
         }
      }
      syncPassengerPacket(level);
      return true;
   }

   private void syncPassengerPacket(ServerLevel level) {
      ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(this);
      for (ServerPlayer observer : level.players()) {
         if (observer.distanceToSqr(this) <= 128.0 * 128.0) {
            observer.connection.send(packet);
         }
      }
   }

   /** Binds the persistent mount to its Zhao Yun owner before either rider mounts. */
   public void bindRider(ZhaoYunRiderEntity rider, @Nullable LivingEntity master) {
      riderUuid = rider.getUUID();
      if (master != null) masterUuid = master.getUUID();
      skillOwnerUuid = null;
      npMount = false;
      setNpActive(false);
      getPersistentData().remove(TAG_SKILL_OWNER);
      getPersistentData().remove(TAG_NP_MOUNT);
   }

   /** Binds this horse to a transformed Zhao Yun card player. */
   public void bindSkillOwner(net.minecraft.server.level.ServerPlayer owner) {
      riderUuid = null;
      skillOwnerUuid = owner.getUUID();
      refreshSkillOwnerMaster(owner);
      npMount = false;
      setNpActive(false);
      getPersistentData().putUUID(TAG_SKILL_OWNER, skillOwnerUuid);
      getPersistentData().remove(TAG_NP_MOUNT);
   }

   /** Binds a temporary Noble Phantasm horse without arming the skill-mount cooldown. */
   public void bindNoblePhantasmOwner(net.minecraft.server.level.ServerPlayer owner) {
      riderUuid = null;
      skillOwnerUuid = owner.getUUID();
      refreshSkillOwnerMaster(owner);
      npMount = true;
      getPersistentData().putUUID(TAG_SKILL_OWNER, skillOwnerUuid);
      getPersistentData().putBoolean(TAG_NP_MOUNT, true);
   }

   /** Converts a surviving Noble Phantasm horse into the persistent skill mount. */
   public void promoteToSkillMount(net.minecraft.server.level.ServerPlayer owner) {
      bindSkillOwner(owner);
   }

   public boolean isSkillMount() {
      return skillOwnerUuid != null && !npMount;
   }

   public boolean isDamageProtectedWhileMounted() {
      // The NPC's shared mount is protected as one unit while occupied.
      // A Zhao Yun servant-card mount must remain a normal damageable entity
      // so its 2000 HP and death/cooldown behavior work correctly.
      return riderUuid != null && !isNpActive() && !getPassengers().isEmpty();
   }

   /** True when the entity is Zhao Yun, the bound master/owner, or already seated on this mount. */
   public boolean isBoundCompanion(@Nullable Entity entity) {
      if (entity == null) return false;
      if (entity == this || getPassengers().contains(entity)) return true;
      UUID uuid = entity.getUUID();
      return riderUuid != null && riderUuid.equals(uuid)
         || masterUuid != null && masterUuid.equals(uuid)
         || skillOwnerUuid != null && skillOwnerUuid.equals(uuid);
   }

   public boolean shouldRedirectPassengerDamage(Entity passenger) {
      if (!getPassengers().contains(passenger)) return false;
      UUID passengerUuid = passenger.getUUID();
      return passenger instanceof ZhaoYunRiderEntity
         || riderUuid != null && riderUuid.equals(passengerUuid)
         || skillOwnerUuid != null && skillOwnerUuid.equals(passengerUuid)
         || masterUuid != null && masterUuid.equals(passengerUuid);
   }

   @Override protected boolean canAddPassenger(Entity passenger) {
      if (getPassengers().size() >= 2) return false;
      if (passenger instanceof ZhaoYunRiderEntity rider) {
         // Before the first server tick only the first Zhao Yun may claim this mount.
         return getPassengers().isEmpty() && (riderUuid == null || rider.getUUID().equals(riderUuid));
      }
      if (passenger instanceof net.minecraft.world.entity.player.Player player) {
         if (!(level() instanceof ServerLevel)) {
            return getPassengers().size() < 2;
         }
         return canPlayerMount(player);
      }
      return false;
   }

   private boolean isAuthorizedPlayerPassenger(Player player) {
      // An NPC Hakuryu always wins this precedence check: stale card-owner
      // data must never let an unrelated player mount Zhao Yun's horse.
      if (riderUuid != null) {
         if (!(level() instanceof ServerLevel server)) return true;
         ZhaoYunRiderEntity rider = getRider(server);
         return rider != null
            && riderUuid.equals(rider.getUUID())
            && getPassengers().size() == 1
            && getPassengers().get(0) == rider
            && masterUuid != null && masterUuid.equals(player.getUUID());
      }
      if (skillOwnerUuid != null) {
         if (!(level() instanceof ServerLevel server)) return true;
         refreshSkillOwnerMaster(server);
         if (getPassengers().isEmpty() && skillOwnerUuid.equals(player.getUUID())) return true;
         if (getPassengers().size() == 1 && getPassengers().get(0).getUUID().equals(skillOwnerUuid)) {
            Entity ownerEntity = server.getEntity(skillOwnerUuid);
            if (ownerEntity instanceof ServerPlayer owner) {
               TypeMoonWorldModVariables.PlayerVariables ownerVars =
                  owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
               if (player.getUUID().toString().equals(ownerVars.servant_card_master_uuid)
                  && player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_active) {
                  return true;
               }
            }
         }
         return false;
      }
      return false;
   }

   private boolean mayRemainPassenger(Player player, ServerLevel server) {
      if (riderUuid != null) {
         ZhaoYunRiderEntity rider = getRider(server);
         return rider != null
            && riderUuid.equals(rider.getUUID())
            && getPassengers().size() == 2
            && getPassengers().get(0) == rider
            && getPassengers().get(1) == player
            && masterUuid != null
            && masterUuid.equals(player.getUUID());
      }
      if (skillOwnerUuid == null) return false;
      refreshSkillOwnerMaster(server);
      if (getPassengers().isEmpty() || !getPassengers().get(0).getUUID().equals(skillOwnerUuid)) {
         return false;
      }
      return getPassengers().get(0) == player && skillOwnerUuid.equals(player.getUUID())
         || getPassengers().size() == 2
            && getPassengers().get(1) == player
            && masterUuid != null
            && masterUuid.equals(player.getUUID());
   }

   public void sanitizePassengers() {
      if (level() instanceof ServerLevel server) {
         ejectUnauthorizedPassengers(server);
      }
   }

   private void ejectUnauthorizedPassengers(ServerLevel server) {
      for (Entity passenger : java.util.List.copyOf(getPassengers())) {
         if (passenger instanceof ZhaoYunRiderEntity rider) {
            if (riderUuid == null || !riderUuid.equals(rider.getUUID()) || getPassengers().get(0) != rider) {
               rider.stopRiding();
            }
         } else if (passenger instanceof Player player) {
            if (!mayRemainPassenger(player, server)) {
               player.stopRiding();
            }
         } else {
            passenger.stopRiding();
         }
      }
   }

   /** Server-authoritative player boarding check, including force-mount paths. */
   public boolean canPlayerMount(Player player) {
      return getPassengers().size() < 2 && isAuthorizedPlayerPassenger(player);
   }

   private void refreshSkillOwnerMaster(ServerLevel level) {
      if (skillOwnerUuid == null) return;
      Entity ownerEntity = level.getEntity(skillOwnerUuid);
      if (ownerEntity instanceof ServerPlayer owner) {
         refreshSkillOwnerMaster(owner);
      } else {
         masterUuid = null;
      }
   }

   private void refreshSkillOwnerMaster(ServerPlayer owner) {
      TypeMoonWorldModVariables.PlayerVariables vars = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ServerPlayer master = MasterServantLinkService.getLinkedMaster(owner, vars);
      masterUuid = master != null && master.isAlive() && master.level() == owner.level()
         ? master.getUUID()
         : null;
   }

   private boolean isControllingPlayer(Player player) {
      // Only a card owner directly controls a card Hakuryu. The player in the
      // NPC mount's secondary seat is the master/passenger and keeps their own
      // camera yaw instead of being snapped to the horse's forward direction.
      return skillOwnerUuid != null && skillOwnerUuid.equals(player.getUUID())
         && riderUuid == null;
   }

   @Override protected void positionRider(Entity passenger, MoveFunction callback) {
      boolean masterSeat = isMasterPassenger(passenger);
      double localZ = masterSeat ? MASTER_SEAT_FORWARD : RIDER_SEAT_FORWARD;
      float yaw = getYRot() * ((float)Math.PI / 180.0F);
      // Entity yaw 0 faces +Z, so rotate the local seat offset around the mount.
      double offsetX = -Math.sin(yaw) * localZ;
      double offsetZ = Math.cos(yaw) * localZ;
      double seatY = masterSeat ? MASTER_SEAT_HEIGHT : RIDER_SEAT_HEIGHT;
      callback.accept(passenger, getX() + offsetX, getY() + seatY, getZ() + offsetZ);
      if (passenger instanceof Player player && isControllingPlayer(player)) {
         player.setYRot(getYRot());
         player.setYBodyRot(getYRot());
         player.setYHeadRot(getYRot());
         player.setXRot(0.0F);
      } else if (passenger instanceof ZhaoYunRiderEntity rider) {
         float forwardYaw = getYRot();
         rider.setYRot(forwardYaw);
         rider.setYBodyRot(forwardYaw);
         rider.setYHeadRot(forwardYaw);
         rider.setXRot(0.0F);
      }
   }

   private boolean isMasterPassenger(Entity passenger) {
      return masterUuid != null && masterUuid.equals(passenger.getUUID());
   }

   @Override public boolean hurt(DamageSource source, float amount) {
      if (level().isClientSide || amount <= 0.0F) return false;
      if (isBoundCompanion(source.getEntity()) || isBoundCompanion(source.getDirectEntity())) {
         return false;
      }
      if (isNpActive()) {
         amount *= 0.10F;
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
      if (isSkillMount() && level() instanceof ServerLevel server
         && server.getEntity(skillOwnerUuid) instanceof net.minecraft.server.level.ServerPlayer owner) {
         owner.getPersistentData().putLong(
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardZhaoYunSkills.TAG_SKILL_MOUNT_COOLDOWN,
            level().getGameTime() + 30L * 20L);
      }
   }

   public boolean isNpActive() { return entityData.get(NP_ACTIVE); }
   public void setNpActive(boolean active) { entityData.set(NP_ACTIVE, active); }
   private void setMovingState(boolean moving) {
      if (!level().isClientSide()) entityData.set(MOVING, moving);
   }
   @Nullable public UUID getRiderUuid() { return riderUuid; }
   @Nullable public UUID getMasterUuid() { return masterUuid; }
   @Nullable public UUID getSkillOwnerUuid() { return skillOwnerUuid; }

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
      tag.putBoolean(TAG_NP_MOUNT, npMount);
      tag.putBoolean("ChangbanpoActive", isNpActive());
   }

   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID(TAG_RIDER)) riderUuid = tag.getUUID(TAG_RIDER);
      if (tag.hasUUID(TAG_MASTER)) masterUuid = tag.getUUID(TAG_MASTER);
      if (tag.hasUUID(TAG_SKILL_OWNER)) skillOwnerUuid = tag.getUUID(TAG_SKILL_OWNER);
      npMount = tag.getBoolean(TAG_NP_MOUNT);
      setNpActive(tag.getBoolean("ChangbanpoActive"));
   }

   @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
      boolean masterSeat = isMasterPassenger(passenger);
      return new Vec3(0.0,
         masterSeat ? MASTER_SEAT_HEIGHT : RIDER_SEAT_HEIGHT,
         masterSeat ? MASTER_SEAT_FORWARD : RIDER_SEAT_FORWARD);
   }

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> {
         event.getController().setAnimation(RawAnimation.begin().thenLoop(
            isNpActive() ? "gallop" : entityData.get(MOVING) ? "walk" : "standing"));
         return PlayState.CONTINUE;
      }));
   }

   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

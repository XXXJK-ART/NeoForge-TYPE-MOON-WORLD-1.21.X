package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource.Ability;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaCombatEvents;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class OdaMatchlockGunEntity extends Entity implements GeoEntity {
   private static final EntityDataAccessor<Integer> SHOTS_LEFT = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> LIFE_TICKS = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> FOLLOW_MODE = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ORBIT_INDEX = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ORBIT_COUNT = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> VOLLEY_MODE = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> VOLLEY_TARGET_ID = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> FOOT_SUPPORT_MODE = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> MOUNT_MODE = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> CHANT_LIFT = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Long> CHANT_START = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.LONG);
   private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> MOVE_FORWARD = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> MOVE_STRAFE = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> MOVE_VERTICAL = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> STATIC_VOLLEY_MODE = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.BOOLEAN);
   private static final float MOUNT_MAX_HEALTH = 120.0F;
   private static final float MOUNT_INPUT_SYNC_EPSILON = 0.001F;
   private static final float AIM_SYNC_EPSILON_DEGREES = 0.5F;
   private static final double MOUNT_TRAIL_MIN_SPEED_SQR = 0.0025;
   private static final int MOUNT_TRAIL_PARTICLE_INTERVAL = 12;
   private static final int FOOT_SUPPORT_TRAIL_PARTICLE_INTERVAL = 12;
   private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("1");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID ownerUuid;
   private int shootDelay;
   private int volleyNoTargetTicks;
   private boolean dissolving;
   private static final EntityDataAccessor<String> ABILITY = SynchedEntityData.defineId(OdaMatchlockGunEntity.class, EntityDataSerializers.STRING);
   private int shotInterval = 30;
   private Vec3 fixedDirection;
   private Vec3 fieldCenter;
   private UUID pursuitId;
   private int pursuitIndex = -1;
   private boolean shotReported;
   private boolean legacy;
   private Vec3 riderDashDirection = Vec3.ZERO;
   private double riderDashPower;
   private int riderDashTicks;
   private int riderDashDuration;
   private Vec3 chantOrigin;
   private boolean temporaryChantMount;
   private long chantReturnAt = -1;
   private double chantReturnY;
   private boolean playerChantAutoLift;

   public OdaMatchlockGunEntity configure(Ability ability, int shots, int interval, int life) {
      this.entityData.set(ABILITY, ability.name());
      this.entityData.set(SHOTS_LEFT, shots);
      this.entityData.set(LIFE_TICKS, life);
      this.shotInterval = interval;
      return this;
   }
   public OdaMatchlockGunEntity fixedDirection(Vec3 direction) { this.fixedDirection = direction; return this; }
   public OdaMatchlockGunEntity field(Vec3 center) { this.fieldCenter = center; return this; }
   public OdaMatchlockGunEntity pursuit(UUID id, int index) { pursuitId = id; pursuitIndex = index; return this; }
   public Ability ability() { try { return Ability.valueOf(this.entityData.get(ABILITY)); } catch (IllegalArgumentException e) { return Ability.ORBIT; } }
   public boolean isOwnedBy(UUID id) { return id != null && id.equals(ownerUuid); }
   public boolean isCombatFor(UUID id) { return isOwnedBy(id) && !isRemoved() && !isMountMode() && !this.entityData.get(FOOT_SUPPORT_MODE) && getGunHealth() > 0; }
   public boolean isOrbitFor(UUID id) { return isCombatFor(id) && this.entityData.get(FOLLOW_MODE) && ability() == Ability.ORBIT; }
   public float getGunHealth() { return this.entityData.get(HEALTH); }
   public int getShotsLeft() { return this.entityData.get(SHOTS_LEFT); }
   public int getFormationIndex() { return this.entityData.get(ORBIT_INDEX); }
   public OdaMatchlockGunEntity formationIndex(int index) {
      this.entityData.set(ORBIT_INDEX, index);
      this.entityData.set(ORBIT_COUNT, OdaRules.THREE_THOUSAND_TOTAL_GUNS);
      return this;
   }
   public int getPursuitIndex() { return pursuitIndex; }
   public float absorb(float damage) {
      float used = Math.min(Math.max(0, damage), getGunHealth());
      this.entityData.set(HEALTH, getGunHealth() - used);
      if (getGunHealth() <= 0 && level() instanceof ServerLevel level) dissolveAndDiscard(level);
      return damage - used;
   }

   public OdaMatchlockGunEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public OdaMatchlockGunEntity(Level level, LivingEntity owner, Vec3 pos, Vec3 facing, int initialDelay) {
      this(ModEntities.ODA_MATCHLOCK_GUN.get(), level);
      this.ownerUuid = owner == null ? null : owner.getUUID();
      this.setPos(pos.x, pos.y, pos.z);
      this.setFacing(facing);
      this.shootDelay = Math.max(0, initialDelay);
   }

   public static OdaMatchlockGunEntity floating(Level level, LivingEntity owner, int orbitIndex, int orbitCount, int lifeTicks, int initialDelay) {
      Vec3 pos = owner.position().add(0.0, owner.getBbHeight() * 0.72, 0.0);
      OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, owner, pos, owner.getLookAngle(), initialDelay);
      gun.entityData.set(FOLLOW_MODE, true);
      gun.entityData.set(ORBIT_INDEX, Math.max(0, orbitIndex));
      gun.entityData.set(ORBIT_COUNT, Math.max(1, orbitCount));
      gun.entityData.set(LIFE_TICKS, Math.max(20, lifeTicks));
      gun.entityData.set(SHOTS_LEFT, 40);
      gun.configure(Ability.ORBIT, 40, 30, lifeTicks);
      return gun;
   }

   public static OdaMatchlockGunEntity threeThousandWorlds(Level level, LivingEntity owner, LivingEntity target, Vec3 pos, Vec3 facing, int initialDelay) {
      OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, owner, pos, facing, initialDelay);
      gun.entityData.set(VOLLEY_MODE, true);
      gun.entityData.set(VOLLEY_TARGET_ID, target == null ? -1 : target.getId());
      gun.entityData.set(SHOTS_LEFT, OdaRules.THREE_THOUSAND_SHOTS_PER_GUN);
      gun.configure(Ability.THREE_THOUSAND, OdaRules.THREE_THOUSAND_SHOTS_PER_GUN, OdaRules.THREE_THOUSAND_SHOT_INTERVAL, OdaRules.THREE_THOUSAND_MAX_LIFE);
      return gun;
   }

   public static OdaMatchlockGunEntity oneShotTracking(Level level, LivingEntity owner, LivingEntity target, Vec3 pos, int initialDelay) {
      Vec3 facing = target == null ? owner.getLookAngle() : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(pos);
      OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, owner, pos, facing, initialDelay);
      gun.entityData.set(VOLLEY_MODE, true);
      gun.entityData.set(VOLLEY_TARGET_ID, target == null ? -1 : target.getId());
      gun.entityData.set(SHOTS_LEFT, 1);
      gun.configure(Ability.PURSUIT, 1, 4, 80);
      gun.entityData.set(LIFE_TICKS, 20 * 4);
      return gun;
   }

   public static OdaMatchlockGunEntity threeThousandWorldsFollow(Level level, LivingEntity owner, LivingEntity target, int orbitIndex, int orbitCount, int initialDelay) {
      OdaMatchlockGunEntity gun = floating(level, owner, orbitIndex, orbitCount, OdaRules.THREE_THOUSAND_MAX_LIFE, initialDelay);
      gun.entityData.set(VOLLEY_MODE, true);
      gun.entityData.set(VOLLEY_TARGET_ID, target == null ? -1 : target.getId());
      gun.entityData.set(SHOTS_LEFT, OdaRules.THREE_THOUSAND_SHOTS_PER_GUN);
      gun.configure(Ability.THREE_THOUSAND, OdaRules.THREE_THOUSAND_SHOTS_PER_GUN, OdaRules.THREE_THOUSAND_SHOT_INTERVAL, OdaRules.THREE_THOUSAND_MAX_LIFE);
      gun.updateFollowPosition(owner);
      return gun;
   }

   public static OdaMatchlockGunEntity footSupport(Level level, LivingEntity owner) {
      OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, owner, owner.position(), owner.getLookAngle(), 0);
      gun.entityData.set(FOOT_SUPPORT_MODE, true);
      gun.entityData.set(SHOTS_LEFT, 9999);
      gun.entityData.set(LIFE_TICKS, 20 * 4);
      return gun;
   }

   public static OdaMatchlockGunEntity flightMount(Level level, LivingEntity owner) {
      OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, owner, owner.position().add(0.0, 0.08, 0.0), owner.getLookAngle(), 0);
      gun.entityData.set(MOUNT_MODE, true);
      gun.entityData.set(SHOTS_LEFT, 0);
      gun.entityData.set(LIFE_TICKS, 20 * 60);
      gun.entityData.set(HEALTH, MOUNT_MAX_HEALTH);
      gun.setBoundingBox(new AABB(gun.getX() - 0.9, gun.getY() - 0.25, gun.getZ() - 0.9, gun.getX() + 0.9, gun.getY() + 0.55, gun.getZ() + 0.9));
      return gun;
   }

   public boolean isFloatingFor(UUID ownerId) {
      return ownerId != null && this.entityData.get(FOLLOW_MODE) && ownerId.equals(this.ownerUuid);
   }

   public boolean isFootSupportFor(UUID ownerId) {
      return ownerId != null && this.entityData.get(FOOT_SUPPORT_MODE) && ownerId.equals(this.ownerUuid);
   }

   public boolean isMountFor(UUID ownerId) {
      return ownerId != null && this.entityData.get(MOUNT_MODE) && ownerId.equals(this.ownerUuid);
   }

   public boolean isMountMode() {
      return this.entityData.get(MOUNT_MODE);
   }

   public boolean isChantLift() { return this.entityData.get(CHANT_LIFT); }
   public float chantPoseProgress(float partialTick) {
      return (float)Math.clamp((level().getGameTime() - this.entityData.get(CHANT_START) + partialTick) / 12.0, 0.0, 1.0);
   }

   public static void beginThreeThousandChant(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      if (!"oda_three_thousand".equals(owner.getPersistentData().getString("TypeMoonNpCastAction"))) return;
      boolean playerWasFlying = false;
      if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
         var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         playerWasFlying = vars.servantCardState().flying();
         if (!net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills.startChantFlight(player, vars)) return;
      }
      OdaMatchlockGunEntity gun;
      if (owner.getVehicle() instanceof OdaMatchlockGunEntity current && current.isMountFor(owner.getUUID())) {
         gun = current;
         gun.temporaryChantMount = false;
      } else {
         if (owner.isPassenger()) return;
         gun = flightMount(level, owner);
         gun.temporaryChantMount = true;
      }
      gun.chantOrigin = owner.position().add(0, -.18, 0);
      gun.playerChantAutoLift = owner instanceof net.minecraft.server.level.ServerPlayer && !playerWasFlying;
      gun.chantReturnAt = -1;
      gun.entityData.set(CHANT_LIFT, true);
      gun.entityData.set(CHANT_START, level.getGameTime());
      if (!playerWasFlying) {
         gun.setPos(gun.chantOrigin);
         gun.setDeltaMovement(Vec3.ZERO);
      }
      gun.setFacing(owner.getLookAngle().multiply(1, 0, 1));
      if (gun.temporaryChantMount) {
         level.addFreshEntity(gun);
         if (!owner.startRiding(gun, true)) { gun.discardSilently(); return; }
      }
      for (var support : level.getEntitiesOfClass(OdaMatchlockGunEntity.class, owner.getBoundingBox().inflate(4),
         entity -> entity.isFootSupportFor(owner.getUUID()))) support.discardSilently();
      level.playSound(null, owner.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, .7F, .7F);
   }

   public void discardSilently() {
      this.dissolving = true;
      this.discard();
   }

   public void setMountInput(double forward, double strafe, double vertical) {
      setSynchedFloatIfChanged(MOVE_FORWARD, (float)Mth.clamp(forward, -1.0, 1.0), MOUNT_INPUT_SYNC_EPSILON);
      setSynchedFloatIfChanged(MOVE_STRAFE, (float)Mth.clamp(strafe, -1.0, 1.0), MOUNT_INPUT_SYNC_EPSILON);
      setSynchedFloatIfChanged(MOVE_VERTICAL, (float)Mth.clamp(vertical, -1.0, 1.0), MOUNT_INPUT_SYNC_EPSILON);
   }

   public void queueRiderDash(Vec3 direction, double power, int duration) {
      if (!this.entityData.get(MOUNT_MODE) || direction == null || direction.lengthSqr() < 1.0E-6) return;
      this.riderDashDirection = new Vec3(direction.x, 0.0, direction.z).normalize();
      this.riderDashPower = Math.max(0.0, power);
      this.riderDashTicks = Math.max(1, duration);
      this.riderDashDuration = this.riderDashTicks;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(SHOTS_LEFT, 3);
      builder.define(LIFE_TICKS, 20 * 8);
      builder.define(FOLLOW_MODE, false);
      builder.define(ORBIT_INDEX, 0);
      builder.define(ORBIT_COUNT, 1);
      builder.define(AIM_YAW, 0.0F);
      builder.define(AIM_PITCH, 0.0F);
      builder.define(VOLLEY_MODE, false);
      builder.define(VOLLEY_TARGET_ID, -1);
      builder.define(FOOT_SUPPORT_MODE, false);
      builder.define(MOUNT_MODE, false);
      builder.define(CHANT_LIFT, false);
      builder.define(CHANT_START, 0L);
      builder.define(HEALTH, 30.0F);
      builder.define(ABILITY, Ability.ORBIT.name());
      builder.define(MOVE_FORWARD, 0.0F);
      builder.define(MOVE_STRAFE, 0.0F);
      builder.define(MOVE_VERTICAL, 0.0F);
      builder.define(STATIC_VOLLEY_MODE, false);
   }

   public float getAimYaw() {
      return this.entityData.get(AIM_YAW);
   }

   public float getAimPitch() {
      return this.entityData.get(AIM_PITCH);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity owner = getOwnerLiving(level);
      boolean riddenMount = this.entityData.get(MOUNT_MODE)
         && this.getPassengers().stream().anyMatch(Player.class::isInstance);
      if (legacy || owner == null || !owner.isAlive() || (!OdaCombatEvents.isOda(owner) && !riddenMount)) {
         dissolveAndDiscard(level);
         return;
      }
      if (this.entityData.get(MOUNT_MODE)) {
         if (isChantLift()) tickChantLift(level, owner);
         else tickMount(level, owner);
         return;
      }
      if (this.entityData.get(FOOT_SUPPORT_MODE)) {
         if (owner == null || !owner.isAlive() || !owner.isNoGravity()) {
            dissolveAndDiscard(level);
            return;
         }
         if (this.entityData.get(LIFE_TICKS) != 20 * 4) {
            this.entityData.set(LIFE_TICKS, 20 * 4);
         }
         updateFootSupportPosition(owner);
         if (this.tickCount % FOOT_SUPPORT_TRAIL_PARTICLE_INTERVAL == 0 && owner.getDeltaMovement().lengthSqr() >= 0.0004) {
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.08, this.getZ(), 1, 0.1, 0.03, 0.1, 0.008);
            if (this.tickCount % (FOOT_SUPPORT_TRAIL_PARTICLE_INTERVAL * 2) == 0) {
               level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.08, this.getZ(), 1, 0.06, 0.02, 0.06, 0.004);
            }
         }
         return;
      }
      int life = this.entityData.get(LIFE_TICKS) - 1;
      this.entityData.set(LIFE_TICKS, life);
      if (life <= 0 || this.entityData.get(SHOTS_LEFT) <= 0) {
         dissolveAndDiscard(level);
         return;
      }
      if (this.entityData.get(FOLLOW_MODE)) {
         if (!owner.isAlive()) {
            dissolveAndDiscard(level);
            return;
         }
         updateFollowPosition(owner);
      }

      LivingEntity target = fieldCenter != null ? findFieldTarget(level, owner) : this.entityData.get(VOLLEY_MODE) ? getVolleyTarget(level, owner) : findTarget(level, owner);
      if (fixedDirection != null) target = null;
      if (this.entityData.get(VOLLEY_MODE) && fixedDirection == null && fieldCenter == null && ability() != Ability.THREE_THOUSAND) {
         if (target == null) {
            this.volleyNoTargetTicks++;
            if (this.volleyNoTargetTicks >= 40) {
               dissolveAndDiscard(level);
               return;
            }
         } else {
            this.volleyNoTargetTicks = 0;
         }
      }
      if (fixedDirection != null) {
         setFacing(fixedDirection);
      } else if (target != null) {
         setFacing(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(this.position()));
      } else if (owner != null) {
         setFacing(owner.getLookAngle());
      }

      if (this.shootDelay > 0) {
         this.shootDelay--;
         return;
      }

      if (target == null && fixedDirection == null) {
         this.shootDelay = 1;
         return;
      }
      fireAt(level, owner, target);
      this.entityData.set(SHOTS_LEFT, this.entityData.get(SHOTS_LEFT) - 1);
      this.shootDelay = Math.max(0, this.shotInterval - 1);
      if (this.entityData.get(SHOTS_LEFT) <= 0) dissolveAndDiscard(level);
   }

   private void tickChantLift(ServerLevel level, LivingEntity owner) {
      if (chantOrigin == null || owner.getVehicle() != this) { dissolveAndDiscard(level); return; }
      long now = level.getGameTime();
      if (!(owner instanceof net.minecraft.server.level.ServerPlayer)
         && net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.chanting(owner)
         && now - this.entityData.get(CHANT_START) > 0 && (now - this.entityData.get(CHANT_START)) % 40 == 0) {
         // Overlap the short charge loops to cover the full voice window without empty visual gaps.
         net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects.spawn(level, "servant_oda_three_thousand_charge", owner, 72.0);
      }
      if (owner instanceof net.minecraft.server.level.ServerPlayer) {
         if (!net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.chanting(owner)) {
            this.entityData.set(CHANT_LIFT, false);
            playerChantAutoLift = false;
         }
         tickMount(level, owner);
         return;
      }
      if (!net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.chanting(owner)
         && !OdaAbilityService.hasThreeThousandArray(owner)) {
         if (!temporaryChantMount) {
            this.entityData.set(CHANT_LIFT, false);
            setPos(owner.getX(), owner.getY() - .18, owner.getZ());
            setDeltaMovement(Vec3.ZERO);
            return;
         }
         if (chantReturnAt < 0) { chantReturnAt = now; chantReturnY = getY(); }
      }
      double targetY;
      if (chantReturnAt >= 0) {
         double t = Math.clamp((now - chantReturnAt) / (double)OdaRules.THREE_THOUSAND_LAND_TICKS, 0, 1);
         targetY = Mth.lerp(t * t * (3 - 2 * t), chantReturnY, chantOrigin.y);
      } else {
         targetY = chantOrigin.y + OdaRules.THREE_THOUSAND_LIFT_HEIGHT
            * OdaRules.threeThousandLiftProgress(now - this.entityData.get(CHANT_START));
      }
      this.noPhysics = false;
      setNoGravity(true);
      setBoundingBox(new AABB(getX() - .9, getY(), getZ() - .9,
         getX() + .9, getY() + owner.getBbHeight() + .18, getZ() + .9));
      Vec3 motion = new Vec3(0, targetY - getY(), 0);
      move(net.minecraft.world.entity.MoverType.SELF, motion);
      setDeltaMovement(Vec3.ZERO);
      owner.fallDistance = 0;
      setFacing(owner.getLookAngle().multiply(1, 0, 1));
      if (now % 8 == 0 && motion.lengthSqr() > .0001) {
         level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 2, .15, .03, .15, .005);
      }
      if (chantReturnAt >= 0 && now - chantReturnAt >= OdaRules.THREE_THOUSAND_LAND_TICKS) {
         owner.stopRiding();
         owner.setDeltaMovement(Vec3.ZERO);
         owner.fallDistance = 0;
         dissolveAndDiscard(level);
      }
   }

   private void tickMount(ServerLevel level, LivingEntity owner) {
      Entity passenger = this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
      if (!(owner instanceof Player) || !owner.isAlive() || !(passenger instanceof Player rider) || !rider.isAlive()) {
         dissolveAndDiscard(level);
         return;
      }
      if (this.entityData.get(LIFE_TICKS) != 20 * 60) {
         this.entityData.set(LIFE_TICKS, 20 * 60);
      }
      this.fallDistance = 0.0F;
      // The mount must retain normal block collision while flying so descending
      // cannot tunnel through terrain into the void.
      this.noPhysics = false;
      this.setNoGravity(true);
      double yaw = Math.toRadians(rider.getYRot());
      Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 movement = forward.scale(this.entityData.get(MOVE_FORWARD) * .98).add(right.scale(this.entityData.get(MOVE_STRAFE) * .98));
      if (movement.lengthSqr() > 1.0) {
         movement = movement.normalize();
      }
      // Vanilla flying uses .05 speed, sprint x2, .91 horizontal drag and .6 vertical drag.
      boolean sprinting = rider.getPersistentData().getBoolean("OdaFlightSprint")
         || rider.getPersistentData().getBoolean("ServantCardFlightSprint");
      double flyingSpeed = 0.05 * (sprinting ? 2 : 1);
      Vec3 velocity = this.getDeltaMovement().add(movement.scale(flyingSpeed))
         .add(0, this.entityData.get(MOVE_VERTICAL) * .05 * 3 * (sprinting ? 1.35 : 1.0), 0).add(consumeRiderDash());
      if (playerChantAutoLift && isChantLift()) {
         long age = level.getGameTime() - this.entityData.get(CHANT_START);
         if (Math.abs(this.entityData.get(MOVE_VERTICAL)) > .001 || age > OdaRules.THREE_THOUSAND_LIFT_TICKS) {
            playerChantAutoLift = false;
         } else {
            double ascent = OdaRules.THREE_THOUSAND_LIFT_HEIGHT * (OdaRules.threeThousandLiftProgress(age)
               - OdaRules.threeThousandLiftProgress(Math.max(0, age - 1)));
            velocity = new Vec3(velocity.x, ascent, velocity.z);
         }
      }
      if (Math.abs(velocity.x) < .003) velocity = new Vec3(0, velocity.y, velocity.z);
      if (Math.abs(velocity.y) < .003) velocity = new Vec3(velocity.x, 0, velocity.z);
      if (Math.abs(velocity.z) < .003) velocity = new Vec3(velocity.x, velocity.y, 0);
      this.setDeltaMovement(velocity);
      // Include the passenger's head in block collision, with the entity's origin at the box floor.
      this.setBoundingBox(new AABB(getX() - .9, getY(), getZ() - .9, getX() + .9, getY() + rider.getBbHeight() + .18, getZ() + .9));
      this.move(net.minecraft.world.entity.MoverType.SELF, velocity);
      this.setDeltaMovement(this.getDeltaMovement().multiply(.91, .6, .91));
      setFacing(forward);
      rider.fallDistance = 0.0F;
      if (this.tickCount % MOUNT_TRAIL_PARTICLE_INTERVAL == 0 && velocity.lengthSqr() >= MOUNT_TRAIL_MIN_SPEED_SQR) {
         level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.08, this.getZ(), 1, 0.16, 0.04, 0.16, 0.008);
         if (this.tickCount % (MOUNT_TRAIL_PARTICLE_INTERVAL * 2) == 0) {
            level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.08, this.getZ(), 1, 0.08, 0.03, 0.08, 0.004);
         }
      }
   }

   private Vec3 consumeRiderDash() {
      if (this.riderDashTicks <= 0 || this.riderDashDuration <= 0) return Vec3.ZERO;
      double remaining = this.riderDashTicks / (double)this.riderDashDuration;
      Vec3 impulse = this.riderDashDirection.scale(this.riderDashPower * Math.max(0.15, remaining));
      this.riderDashTicks--;
      if (this.riderDashTicks <= 0) {
         this.riderDashDirection = Vec3.ZERO;
         this.riderDashPower = 0.0;
         this.riderDashDuration = 0;
      }
      return impulse;
   }

   @Override
   protected boolean canAddPassenger(Entity passenger) {
      return this.entityData.get(MOUNT_MODE) && this.getPassengers().isEmpty()
         && (passenger instanceof Player || isChantLift() && passenger instanceof LivingEntity && isOwnedBy(passenger.getUUID()));
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction callback) {
      if (this.entityData.get(MOUNT_MODE)) {
         callback.accept(passenger, this.getX(), this.getY() + .18, this.getZ());
      } else {
         super.positionRider(passenger, callback);
      }
   }

   @Override
   public boolean isPickable() {
      return !this.entityData.get(FOOT_SUPPORT_MODE) && !this.isRemoved();
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (this.level().isClientSide || this.entityData.get(FOOT_SUPPORT_MODE) || amount <= 0.0F) {
         return false;
      }
      Entity attacker = source.getEntity();
      if (attacker != null && this.ownerUuid != null && attacker.getUUID().equals(this.ownerUuid)) {
         return false;
      }
      float health = this.entityData.get(HEALTH) - amount;
      this.entityData.set(HEALTH, health);
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ASH, this.getX(), this.getY() + 0.25, this.getZ(), 6, 0.3, 0.16, 0.3, 0.008);
         level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.25, this.getZ(), 3, 0.18, 0.1, 0.18, 0.01);
         level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.35F, 1.6F);
         if (health <= 0.0F) {
            this.ejectPassengers();
            dissolveAndDiscard(level);
         }
      }
      return true;
   }

   private void updateFollowPosition(LivingEntity owner) {
      int count = Math.max(1, this.entityData.get(ORBIT_COUNT));
      int index = Mth.clamp(this.entityData.get(ORBIT_INDEX), 0, count - 1);
      Vec3 look = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = new Vec3(0.0, 0.0, 1.0);
      }
      look = look.normalize();
      Vec3 side = new Vec3(-look.z, 0.0, look.x);
      if (ability() == Ability.THREE_THOUSAND && count == OdaRules.THREE_THOUSAND_TOTAL_GUNS) {
         int wave = OdaRules.threeThousandWave(index);
         int gunIndex = OdaRules.threeThousandGunIndexInWave(index);
         int tier = OdaRules.threeThousandTier(gunIndex);
         int slot = gunIndex % OdaRules.THREE_THOUSAND_GUNS_PER_TIER;
         double angle = OdaRules.threeThousandAngle(tier, slot, wave) + owner.tickCount * 0.028;
         double radius = OdaRules.threeThousandRadius(tier, slot, wave);
         Vec3 next = owner.position()
            .add(side.scale(Math.cos(angle) * radius))
            .add(look.scale(Math.sin(angle) * radius))
            .add(0.0, owner.getBbHeight() * 0.55 + OdaRules.threeThousandHeight(tier, slot, wave), 0.0);
         this.setPos(next.x, next.y, next.z);
         setFacing(owner.getLookAngle());
         return;
      }
      double base = owner.tickCount * 0.075 + index * (Math.PI * 2.0 / count);
      double radius = count <= 2 ? 1.35 : 1.8;
      double behind = count == 1 ? -0.85 : -0.45;
      double bob = Math.sin(owner.tickCount * 0.17 + index) * 0.22;
      Vec3 offset = side.scale(Math.cos(base) * radius).add(look.scale(behind + Math.sin(base) * 0.55)).add(0.0, owner.getBbHeight() * 0.72 + bob, 0.0);
      Vec3 next = owner.position().add(offset);
      this.setPos(next.x, next.y, next.z);
      LivingEntity ownerTarget = owner instanceof Mob mob ? mob.getTarget() : null;
      Vec3 aim = ownerTarget != null && ownerTarget.isAlive()
         ? ownerTarget.position().add(0.0, ownerTarget.getBbHeight() * 0.55, 0.0).subtract(next)
         : owner.getLookAngle();
      setFacing(aim);
   }

   private void updateFootSupportPosition(LivingEntity owner) {
      Vec3 look = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = new Vec3(0.0, 0.0, 1.0);
      }
      look = look.normalize();
      Vec3 next = owner.position().add(look.scale(0.08)).add(0.0, 0.14, 0.0);
      this.setPos(next.x, next.y, next.z);
      setFacing(look);
   }

   private LivingEntity findTarget(ServerLevel level, LivingEntity owner) {
      // The high NP tiers must still be able to acquire enemies fighting on the ground below.
      AABB area = this.getBoundingBox().inflate(ability() == Ability.THREE_THOUSAND ? 48.0 : 24.0);
      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, entity -> canTarget(owner, entity))) {
         double distance = living.distanceToSqr(this);
         if (distance < bestDistance && hasRoughLineOfSight(living)) {
            best = living;
            bestDistance = distance;
         }
      }
      return best;
   }

   private LivingEntity findFieldTarget(ServerLevel level, LivingEntity owner) {
      return level.getEntitiesOfClass(LivingEntity.class, new AABB(fieldCenter, fieldCenter).inflate(5, 2.5, 5),
         target -> canTarget(owner, target) && target.position().subtract(fieldCenter).multiply(1, 0, 1).lengthSqr() <= 25 && hasRoughLineOfSight(target))
         .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
   }

   private LivingEntity getVolleyTarget(ServerLevel level, LivingEntity owner) {
      Entity target = level.getEntity(this.entityData.get(VOLLEY_TARGET_ID));
      if (target instanceof LivingEntity living && canTarget(owner, living)
         && (ability() != Ability.THREE_THOUSAND || hasRoughLineOfSight(living))) {
         return living;
      }
      return ability() == Ability.PURSUIT ? null : findTarget(level, owner);
   }

   private boolean canTarget(LivingEntity owner, LivingEntity target) {
      return target != null && target.isAlive() && target != owner && !EntityUtils.isImmunePlayerTarget(target)
         && (owner == null || (!owner.isAlliedTo(target) && !ServantMasterTargeting.isContractMaster(owner, target)));
   }

   private boolean hasRoughLineOfSight(LivingEntity target) {
      return this.level().clip(new net.minecraft.world.level.ClipContext(
         this.position(),
         target.position().add(0.0, target.getBbHeight() * 0.55, 0.0),
         net.minecraft.world.level.ClipContext.Block.COLLIDER,
         net.minecraft.world.level.ClipContext.Fluid.NONE,
         this
      )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
   }

   private void fireAt(ServerLevel level, LivingEntity owner, LivingEntity target) {
      Vec3 start = this.position();
      Vec3 shotDirection = fixedDirection != null ? fixedDirection : target == null ? owner.getLookAngle() : target.position().add(0.0, target.getBbHeight() * .55, 0).subtract(start);
      setFacing(shotDirection);
      OdaMatchlockBulletEntity bullet = new OdaMatchlockBulletEntity(level, owner, this, 10.0F)
         .source(ability()).tracking(ability() == Ability.PURSUIT ? target : null).pursuit(pursuitId, pursuitIndex);
      shotReported = true;
      bullet.setPos(start.x, start.y, start.z);
      bullet.shoot(shotDirection.x, shotDirection.y, shotDirection.z, 3.25F, 0.35F);
      level.addFreshEntity(bullet);
      level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.45F, 1.75F);
      level.sendParticles(ParticleTypes.SMOKE, start.x, start.y, start.z, 4, 0.08, 0.06, 0.08, 0.02);
      if (ability() == Ability.THREE_THOUSAND) {
         // A lingering grey black-powder cloud at every muzzle, including the high spiral tiers.
         level.sendParticles(ParticleTypes.LARGE_SMOKE, start.x, start.y + .08, start.z, 3, .24, .16, .24, .012);
         level.sendParticles(ParticleTypes.SMOKE, start.x, start.y + .12, start.z, 3, .32, .22, .32, .016);
      }
      level.sendParticles(ParticleTypes.FLAME, start.x, start.y, start.z, 1, 0.03, 0.02, 0.03, 0.008);
   }

   private void dissolveAndDiscard(ServerLevel level) {
      if (!this.dissolving) {
         this.dissolving = true;
         spawnDissolveEffect(level);
      }
      this.discard();
   }

   private void spawnDissolveEffect(ServerLevel level) {
      Vec3 center = this.position();
      level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 3, 0.12, 0.1, 0.12, 0.015);
      level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 7, 0.16, 0.12, 0.16, 0.01);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.35F, 1.8F);
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!shotReported && pursuitId != null && level() instanceof ServerLevel server) {
         LivingEntity owner = getOwnerLiving(server);
         if (owner != null) OdaAbilityService.report(owner, pursuitId, pursuitIndex, false);
         shotReported = true;
      }
      if (!this.level().isClientSide() && reason == RemovalReason.DISCARDED && !this.dissolving && this.level() instanceof ServerLevel level) {
         spawnDissolveEffect(level);
         this.dissolving = true;
      }
      super.remove(reason);
   }

   private LivingEntity getOwnerLiving(ServerLevel level) {
      if (this.ownerUuid == null) {
         return null;
      }
      Entity owner = level.getEntity(this.ownerUuid);
      return owner instanceof LivingEntity living ? living : null;
   }

   private void setFacing(Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() > 1.0E-4) {
         float yaw = (float)(Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG);
         this.setYRot(yaw);
         this.yRotO = yaw;
         setSynchedAngleIfChanged(AIM_YAW, yaw);
      }
      double horizontalLength = Math.max(1.0E-4, horizontal.length());
      float pitch = (float)(Mth.atan2(direction.y, horizontalLength) * Mth.RAD_TO_DEG);
      this.setXRot(pitch);
      this.xRotO = pitch;
      setSynchedAngleIfChanged(AIM_PITCH, pitch);
   }

   private void setSynchedFloatIfChanged(EntityDataAccessor<Float> accessor, float value, float epsilon) {
      if (Math.abs(this.entityData.get(accessor) - value) > epsilon) {
         this.entityData.set(accessor, value);
      }
   }

   private void setSynchedAngleIfChanged(EntityDataAccessor<Float> accessor, float value) {
      if (Math.abs(Mth.degreesDifference(this.entityData.get(accessor), value)) > AIM_SYNC_EPSILON_DEGREES) {
         this.entityData.set(accessor, value);
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }
      this.entityData.set(SHOTS_LEFT, tag.contains("ShotsLeft") ? tag.getInt("ShotsLeft") : 3);
      this.entityData.set(LIFE_TICKS, tag.contains("LifeTicks") ? tag.getInt("LifeTicks") : 20 * 8);
      this.entityData.set(FOLLOW_MODE, tag.getBoolean("FollowMode"));
      this.entityData.set(ORBIT_INDEX, tag.getInt("OrbitIndex"));
      this.entityData.set(ORBIT_COUNT, tag.contains("OrbitCount") ? Math.max(1, tag.getInt("OrbitCount")) : 1);
      this.entityData.set(AIM_YAW, tag.getFloat("AimYaw"));
      this.entityData.set(AIM_PITCH, tag.getFloat("AimPitch"));
      this.entityData.set(VOLLEY_MODE, tag.getBoolean("VolleyMode"));
      this.entityData.set(VOLLEY_TARGET_ID, tag.contains("VolleyTargetId") ? tag.getInt("VolleyTargetId") : -1);
      this.entityData.set(FOOT_SUPPORT_MODE, tag.getBoolean("FootSupportMode"));
      this.entityData.set(MOUNT_MODE, tag.getBoolean("MountMode"));
      this.entityData.set(HEALTH, tag.contains("Health") ? tag.getFloat("Health") : (isMountMode() ? MOUNT_MAX_HEALTH : 30.0F));
      legacy = !tag.contains("Ability") || !isMountMode() || tag.getBoolean("ChantLift");
      this.entityData.set(ABILITY, tag.getString("Ability"));
      shotInterval = tag.contains("ShotInterval") ? tag.getInt("ShotInterval") : 30;
      this.entityData.set(MOVE_FORWARD, tag.getFloat("MoveForward"));
      this.entityData.set(MOVE_STRAFE, tag.getFloat("MoveStrafe"));
      this.entityData.set(MOVE_VERTICAL, tag.getFloat("MoveVertical"));
      this.entityData.set(STATIC_VOLLEY_MODE, tag.getBoolean("StaticVolleyMode"));
      this.shootDelay = tag.getInt("ShootDelay");
      this.volleyNoTargetTicks = tag.getInt("VolleyNoTargetTicks");
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
      tag.putInt("ShotsLeft", this.entityData.get(SHOTS_LEFT));
      tag.putInt("LifeTicks", this.entityData.get(LIFE_TICKS));
      tag.putBoolean("FollowMode", this.entityData.get(FOLLOW_MODE));
      tag.putInt("OrbitIndex", this.entityData.get(ORBIT_INDEX));
      tag.putInt("OrbitCount", this.entityData.get(ORBIT_COUNT));
      tag.putFloat("AimYaw", this.entityData.get(AIM_YAW));
      tag.putFloat("AimPitch", this.entityData.get(AIM_PITCH));
      tag.putBoolean("VolleyMode", this.entityData.get(VOLLEY_MODE));
      tag.putInt("VolleyTargetId", this.entityData.get(VOLLEY_TARGET_ID));
      tag.putBoolean("FootSupportMode", this.entityData.get(FOOT_SUPPORT_MODE));
      tag.putBoolean("MountMode", this.entityData.get(MOUNT_MODE));
      tag.putBoolean("ChantLift", isChantLift());
      tag.putFloat("Health", this.entityData.get(HEALTH));
      tag.putFloat("MoveForward", this.entityData.get(MOVE_FORWARD));
      tag.putFloat("MoveStrafe", this.entityData.get(MOVE_STRAFE));
      tag.putFloat("MoveVertical", this.entityData.get(MOVE_VERTICAL));
      tag.putBoolean("StaticVolleyMode", this.entityData.get(STATIC_VOLLEY_MODE));
      tag.putString("Ability", this.entityData.get(ABILITY));
      tag.putInt("ShotInterval", shotInterval);
      tag.putInt("ShootDelay", this.shootDelay);
      tag.putInt("VolleyNoTargetTicks", this.volleyNoTargetTicks);
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event ->
         event.setAndContinue(IDLE_ANIMATION)
      ));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

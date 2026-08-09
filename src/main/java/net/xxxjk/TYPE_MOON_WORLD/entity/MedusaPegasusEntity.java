package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MedusaPegasusEntity extends PathfinderMob implements GeoEntity {
   private static final EntityDataAccessor<Boolean> FLYING_MODE = SynchedEntityData.defineId(MedusaPegasusEntity.class, EntityDataSerializers.BOOLEAN);
   private static final String TAG_SUMMONER_UUID = "MedusaPegasusSummoner";
   private static final String TAG_FLIGHT_CONTROL_TICK = "MedusaPegasusFlightControlTick";
   private static final DustParticleOptions TRAIL_LIGHT_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.98F, 0.9F), 1.35F);
   private static final DustParticleOptions TRAIL_GOLD_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.24F), 1.1F);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   @Nullable
   private UUID summonerUuid;

   public MedusaPegasusEntity(EntityType<? extends MedusaPegasusEntity> entityType, Level level) {
      super(entityType, level);
      this.setPersistenceRequired();
      this.setNoGravity(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 3000.0)
         .add(Attributes.MOVEMENT_SPEED, 0.45)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.FLYING_SPEED, 0.55)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FLYING_MODE, false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      this.setNoGravity(this.isFlyingMode());
      this.fallDistance = 0.0F;
      LivingEntity summoner = this.getSummoner();
      if (this.summonerUuid != null && (summoner == null || !summoner.isAlive())) {
         this.discard();
      }
      if (!this.isVehicle() && this.tickCount > 40 && !this.level().isClientSide()) {
         this.discard();
      } else if (this.isFlyingMode() && this.isVehicle()) {
         this.tickFallbackFlight(summoner);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         this.snapToNearbyGround();
         this.pullEnemiesTowardHead();
         this.spawnPegasusTrail();
      }
   }

   private void pullEnemiesTowardHead() {
      if (!(this.level() instanceof ServerLevel level) || (!this.isFlyingMode() && !this.isVehicle())) return;
      LivingEntity summoner = this.getSummoner();
      if (summoner == null) return;
      Vec3 motion = this.getDeltaMovement();
      Vec3 forward = motion.horizontalDistanceSqr() > 1.0E-4
         ? new Vec3(motion.x, 0.0, motion.z).normalize()
         : this.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
      if (forward.lengthSqr() < 1.0E-4) forward = new Vec3(0.0, 0.0, 1.0);
      Vec3 head = this.position().add(forward.scale(1.9)).add(0.0, this.getBbHeight() * 0.62, 0.0);
      AABB area = new AABB(head, head).inflate(4.5, 2.8, 4.5);
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area,
         target -> target != this && target != summoner && target.isAlive()
            && !summoner.isAlliedTo(target) && !target.isAlliedTo(summoner)
            && !EntityUtils.isImmunePlayerTarget(target))) {
         Vec3 pull = head.subtract(victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0));
         double distance = pull.length();
         if (distance < 0.15 || distance > 4.8) continue;
         double strength = 0.12 + (1.0 - Math.min(1.0, distance / 4.8)) * 0.24;
         Vec3 velocity = victim.getDeltaMovement().scale(0.68).add(pull.normalize().scale(strength));
         victim.setDeltaMovement(velocity);
         victim.hurtMarked = true;
      }
      if (this.tickCount % 4 == 0) {
         level.sendParticles(ParticleTypes.END_ROD, head.x, head.y, head.z, 5, 0.34, 0.28, 0.34, 0.018);
      }
   }

   private void spawnPegasusTrail() {
      if (!(this.level() instanceof ServerLevel level) || this.tickCount < 3) {
         return;
      }

      Vec3 motion = this.getDeltaMovement();
      Vec3 back = motion.horizontalDistanceSqr() > 1.0E-4
         ? new Vec3(motion.x, 0.0, motion.z).normalize().scale(-0.85)
         : this.getLookAngle().multiply(-0.65, 0.0, -0.65);
      double speedFactor = Math.min(1.0, Math.max(0.35, motion.length() * 0.85));
      double baseX = this.getX() + back.x;
      double baseY = this.getY() + 0.65;
      double baseZ = this.getZ() + back.z;

      level.sendParticles(TRAIL_LIGHT_PARTICLE, baseX, baseY, baseZ, 18, 0.42, 0.26, 0.42, 0.015 * speedFactor);
      level.sendParticles(ParticleTypes.END_ROD, baseX, baseY + 0.08, baseZ, 8, 0.32, 0.2, 0.32, 0.012 * speedFactor);
      level.sendParticles(TRAIL_GOLD_PARTICLE, baseX, baseY + 0.04, baseZ, 5, 0.3, 0.18, 0.3, 0.01 * speedFactor);
      if (this.tickCount % 2 == 0) {
         level.sendParticles(ParticleTypes.GLOW, baseX, baseY + 0.12, baseZ, 4, 0.24, 0.16, 0.24, 0.01 * speedFactor);
      }
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> {
         if (this.isFlyingMode() || !this.onGround()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.medusa_pegasus.flying"));
         }
         if (event.isMoving()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.medusa_pegasus.walk"));
         }
         return event.setAndContinue(RawAnimation.begin().thenLoop("animation.medusa_pegasus.idle"));
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      LivingEntity summoner = this.getSummoner();
      return summoner != null && (other == summoner || summoner.isAlliedTo(other));
   }

   @Override
   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
      return new Vec3(0.0, 1.25, -0.05);
   }

   public boolean isFlyingMode() {
      return this.entityData.get(FLYING_MODE);
   }

   public void setFlyingMode(boolean flyingMode) {
      this.entityData.set(FLYING_MODE, flyingMode);
      this.setNoGravity(flyingMode);
   }

   public void markFlightControlled(long now) {
      this.getPersistentData().putLong(TAG_FLIGHT_CONTROL_TICK, now);
   }

   public void setSummoner(LivingEntity summoner) {
      this.summonerUuid = summoner.getUUID();
      this.getPersistentData().putUUID(TAG_SUMMONER_UUID, this.summonerUuid);
   }

   @Nullable
   public LivingEntity getSummoner() {
      if (this.summonerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
         return null;
      }
      Entity entity = serverLevel.getEntity(this.summonerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.summonerUuid != null) {
         tag.putUUID(TAG_SUMMONER_UUID, this.summonerUuid);
      }
      tag.putBoolean("PegasusFlyingMode", this.isFlyingMode());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID(TAG_SUMMONER_UUID)) {
         this.summonerUuid = tag.getUUID(TAG_SUMMONER_UUID);
         this.getPersistentData().putUUID(TAG_SUMMONER_UUID, this.summonerUuid);
      }
      this.entityData.set(FLYING_MODE, tag.getBoolean("PegasusFlyingMode"));
      this.setNoGravity(this.isFlyingMode());
   }

   private void snapToNearbyGround() {
      if (this.isFlyingMode() || this.onGround() || this.getDeltaMovement().y > 0.0) {
         return;
      }

      double maxSnapDistance = 1.25;
      Vec3 snap = Entity.collideBoundingBox(this, new Vec3(0.0, -maxSnapDistance, 0.0), this.getBoundingBox(), this.level(), List.of());
      if (snap.y >= -1.0E-3 || snap.y <= -maxSnapDistance + 1.0E-3) {
         return;
      }

      this.setPos(this.getX(), this.getY() + snap.y, this.getZ());
      Vec3 motion = this.getDeltaMovement();
      this.setDeltaMovement(motion.x, 0.0, motion.z);
      this.setOnGround(true);
   }

   private void tickFallbackFlight(@Nullable LivingEntity summoner) {
      long now = this.level().getGameTime();
      if (now - this.getPersistentData().getLong(TAG_FLIGHT_CONTROL_TICK) <= 2L) return;
      LivingEntity target = summoner instanceof Mob mob ? mob.getTarget() : null;
      Vec3 motion = this.getDeltaMovement();
      if (target == null || !target.isAlive() || target.level() != this.level()) {
         this.setDeltaMovement(motion.x * 0.72, Math.max(-0.24, Math.min(-0.08, motion.y)), motion.z * 0.72);
         this.hasImpulse = true;
         return;
      }

      Vec3 horizontal = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
      Vec3 forward = horizontal.lengthSqr() > 1.0E-4 ? horizontal.normalize() : Vec3.ZERO;
      double desiredY = Math.max(this.level().getMinBuildHeight() + 1.0,
         Math.min(this.level().getMaxBuildHeight() - this.getBbHeight() - 1.0, target.getY() + 0.9));
      double vertical = Math.max(-0.28, Math.min(0.22, (desiredY - this.getY()) * 0.14));
      Vec3 wanted = new Vec3(motion.x * 0.2 + forward.x * 0.72, vertical,
         motion.z * 0.2 + forward.z * 0.72);
      Vec3 limited = Entity.collideBoundingBox(this, wanted, this.getBoundingBox(), this.level(), List.of());
      this.setDeltaMovement(limited);
      this.hasImpulse = true;
      if (horizontal.lengthSqr() > 1.0E-4) {
         float yaw = (float)(Math.atan2(forward.z, forward.x) * 180.0 / Math.PI) - 90.0F;
         this.setYRot(yaw);
         this.yBodyRot = yaw;
      }
   }
}

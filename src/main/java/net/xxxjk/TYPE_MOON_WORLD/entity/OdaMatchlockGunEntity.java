package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
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
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
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
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID ownerUuid;
   private int shootDelay;
   private int volleyNoTargetTicks;
   private boolean dissolving;

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
      return gun;
   }

   public static OdaMatchlockGunEntity threeThousandWorlds(Level level, LivingEntity owner, LivingEntity target, Vec3 pos, Vec3 facing, int initialDelay) {
      OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, owner, pos, facing, initialDelay);
      gun.entityData.set(VOLLEY_MODE, true);
      gun.entityData.set(VOLLEY_TARGET_ID, target == null ? -1 : target.getId());
      gun.entityData.set(SHOTS_LEFT, 3);
      gun.entityData.set(LIFE_TICKS, 20 * 16);
      return gun;
   }

   public static OdaMatchlockGunEntity footSupport(Level level, LivingEntity owner) {
      OdaMatchlockGunEntity gun = new OdaMatchlockGunEntity(level, owner, owner.position(), owner.getLookAngle(), 0);
      gun.entityData.set(FOOT_SUPPORT_MODE, true);
      gun.entityData.set(SHOTS_LEFT, 9999);
      gun.entityData.set(LIFE_TICKS, 20 * 4);
      return gun;
   }

   public boolean isFloatingFor(UUID ownerId) {
      return ownerId != null && this.entityData.get(FOLLOW_MODE) && ownerId.equals(this.ownerUuid);
   }

   public boolean isFootSupportFor(UUID ownerId) {
      return ownerId != null && this.entityData.get(FOOT_SUPPORT_MODE) && ownerId.equals(this.ownerUuid);
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
      if (owner == null && this.ownerUuid != null) {
         dissolveAndDiscard(level);
         return;
      }
      if (this.entityData.get(FOOT_SUPPORT_MODE)) {
         if (owner == null || !owner.isAlive() || !owner.isNoGravity()) {
            dissolveAndDiscard(level);
            return;
         }
         this.entityData.set(LIFE_TICKS, 20 * 4);
         updateFootSupportPosition(owner);
         if (this.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.08, this.getZ(), 2, 0.12, 0.04, 0.12, 0.01);
            level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.08, this.getZ(), 1, 0.08, 0.03, 0.08, 0.005);
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

      LivingEntity target = this.entityData.get(VOLLEY_MODE) ? getVolleyTarget(level, owner) : findTarget(level, owner);
      if (this.entityData.get(VOLLEY_MODE)) {
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
      if (target != null) {
         setFacing(target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(this.position()));
      } else if (owner != null) {
         setFacing(owner.getLookAngle());
      }

      if (this.shootDelay > 0) {
         this.shootDelay--;
         return;
      }

      if (target == null) {
         this.shootDelay = 8;
         return;
      }
      fireAt(level, owner, target);
      this.entityData.set(SHOTS_LEFT, this.entityData.get(SHOTS_LEFT) - 1);
      this.shootDelay = this.entityData.get(VOLLEY_MODE) ? 20 : this.entityData.get(FOLLOW_MODE) ? 18 + this.random.nextInt(12) : 16 + this.random.nextInt(10);
   }

   private void updateFollowPosition(LivingEntity owner) {
      int count = Math.max(1, this.entityData.get(ORBIT_COUNT));
      int index = Mth.clamp(this.entityData.get(ORBIT_INDEX), 0, count - 1);
      double base = owner.tickCount * 0.075 + index * (Math.PI * 2.0 / count);
      Vec3 look = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = new Vec3(0.0, 0.0, 1.0);
      }
      look = look.normalize();
      Vec3 side = new Vec3(-look.z, 0.0, look.x);
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
      AABB area = this.getBoundingBox().inflate(24.0);
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

   private LivingEntity getVolleyTarget(ServerLevel level, LivingEntity owner) {
      Entity target = level.getEntity(this.entityData.get(VOLLEY_TARGET_ID));
      if (target instanceof LivingEntity living && canTarget(owner, living)) {
         return living;
      }
      return findTarget(level, owner);
   }

   private boolean canTarget(LivingEntity owner, LivingEntity target) {
      return target != null && target.isAlive() && target != owner && !EntityUtils.isImmunePlayerTarget(target)
         && (owner == null || !owner.isAlliedTo(target));
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
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 shotDirection = end.subtract(start);
      setFacing(shotDirection);
      float damage = OdaNobunagaCombatHelper.scaleThreeThousandWorldsDamage(owner, target, 10.0F);
      OdaMatchlockBulletEntity bullet = new OdaMatchlockBulletEntity(level, owner, this, damage);
      bullet.setPos(start.x, start.y, start.z);
      bullet.shoot(shotDirection.x, shotDirection.y, shotDirection.z, 3.25F, 0.35F);
      level.addFreshEntity(bullet);
      level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.45F, 1.75F);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, start.x, start.y, start.z, 8, 0.08, 0.08, 0.08, 0.025);
      level.sendParticles(ParticleTypes.SMOKE, start.x, start.y, start.z, 12, 0.12, 0.1, 0.12, 0.04);
      level.sendParticles(ParticleTypes.FLAME, start.x, start.y, start.z, 4, 0.05, 0.04, 0.05, 0.015);
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
      level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 10, 0.18, 0.18, 0.18, 0.08);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 6, 0.12, 0.12, 0.12, 0.035);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 5, 0.1, 0.1, 0.1, 0.02);
      level.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z, 12, 0.16, 0.16, 0.16, 0.025);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.35F, 1.8F);
   }

   @Override
   public void remove(RemovalReason reason) {
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
         this.entityData.set(AIM_YAW, yaw);
      }
      double horizontalLength = Math.max(1.0E-4, horizontal.length());
      float pitch = (float)(Mth.atan2(direction.y, horizontalLength) * Mth.RAD_TO_DEG);
      this.setXRot(pitch);
      this.xRotO = pitch;
      this.entityData.set(AIM_PITCH, pitch);
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
      tag.putInt("ShootDelay", this.shootDelay);
      tag.putInt("VolleyNoTargetTicks", this.volleyNoTargetTicks);
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event ->
         event.setAndContinue(RawAnimation.begin().thenLoop("1"))
      ));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}

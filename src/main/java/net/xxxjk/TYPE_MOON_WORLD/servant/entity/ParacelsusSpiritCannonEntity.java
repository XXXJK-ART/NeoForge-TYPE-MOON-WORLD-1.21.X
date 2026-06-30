package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ParacelsusSpiritCannonEntity extends Entity implements GeoEntity {
   private static final EntityDataAccessor<Integer> LIFE_TICKS = SynchedEntityData.defineId(ParacelsusSpiritCannonEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> SHOTS_LEFT = SynchedEntityData.defineId(ParacelsusSpiritCannonEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(ParacelsusSpiritCannonEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(ParacelsusSpiritCannonEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(ParacelsusSpiritCannonEntity.class, EntityDataSerializers.FLOAT);
   private static final int NO_TARGET_TIMEOUT = 40;
   private static final int TARGET_RESCAN_INTERVAL = 10;
   private static final DustParticleOptions FIRE = new DustParticleOptions(new Vector3f(1.0F, 0.28F, 0.22F), 1.05F);
   private static final DustParticleOptions WATER = new DustParticleOptions(new Vector3f(0.25F, 0.55F, 1.0F), 1.05F);
   private static final DustParticleOptions EARTH = new DustParticleOptions(new Vector3f(0.35F, 0.9F, 0.35F), 1.05F);
   private static final DustParticleOptions WIND = new DustParticleOptions(new Vector3f(0.92F, 0.95F, 1.0F), 1.05F);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID ownerUuid;
   private int shootDelay;
   private int noTargetTicks;
   private int targetRescanTicks;
   private boolean dissolving;
   private boolean orbitAroundTarget;

   public ParacelsusSpiritCannonEntity(EntityType<? extends ParacelsusSpiritCannonEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.noCulling = true;
   }

   public static ParacelsusSpiritCannonEntity summon(ServerLevel level, ParacelsusEntity owner, LivingEntity target, int lifeTicks) {
      return summon(level, owner, target, lifeTicks, false);
   }

   public static ParacelsusSpiritCannonEntity summonAroundTarget(ServerLevel level, ParacelsusEntity owner, LivingEntity target, int lifeTicks) {
      return summon(level, owner, target, lifeTicks, true);
   }

   private static ParacelsusSpiritCannonEntity summon(ServerLevel level, ParacelsusEntity owner, LivingEntity target, int lifeTicks, boolean orbitAroundTarget) {
      ParacelsusSpiritCannonEntity cannon = new ParacelsusSpiritCannonEntity(ModEntities.PARACELSUS_SPIRIT_CANNON.get(), level);
      cannon.ownerUuid = owner.getUUID();
      cannon.orbitAroundTarget = orbitAroundTarget;
      cannon.entityData.set(LIFE_TICKS, Math.max(40, lifeTicks));
      cannon.entityData.set(SHOTS_LEFT, Math.max(2, lifeTicks / 45));
      cannon.entityData.set(TARGET_ID, target != null && target.isAlive() ? target.getId() : -1);
      cannon.shootDelay = 4;
      cannon.setPos(initialPosition(owner, target, orbitAroundTarget));
      cannon.setFacing(orbitAroundTarget && target != null ? target.position().subtract(owner.position()) : owner.getLookAngle());
      return cannon;
   }

   public void refresh(LivingEntity target, int lifeTicks) {
      this.entityData.set(LIFE_TICKS, Math.max(40, lifeTicks));
      if (target != null && target.isAlive()) {
         this.entityData.set(TARGET_ID, target.getId());
      }
      this.shootDelay = Math.min(this.shootDelay, 4);
   }

   public boolean isOwnedBy(UUID ownerId) {
      return ownerId != null && this.ownerUuid != null && ownerId.equals(this.ownerUuid);
   }

   public float getAimYaw() {
      return this.entityData.get(AIM_YAW);
   }

   public float getAimPitch() {
      return this.entityData.get(AIM_PITCH);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(LIFE_TICKS, 20 * 8);
      builder.define(SHOTS_LEFT, 3);
      builder.define(TARGET_ID, -1);
      builder.define(AIM_YAW, 0.0F);
      builder.define(AIM_PITCH, 0.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }

      ParacelsusEntity owner = this.getOwnerLiving(level);
      if (owner == null || !owner.isAlive()) {
         this.dissolveAndDiscard(level);
         return;
      }
      if (owner.getTarget() == null || !EntityUtils.isValidCombatTarget(owner, owner.getTarget())) {
         this.dissolveAndDiscard(level);
         return;
      }

      int life = this.entityData.get(LIFE_TICKS) - 1;
      this.entityData.set(LIFE_TICKS, life);
      if (life <= 0 || this.entityData.get(SHOTS_LEFT) <= 0) {
         this.dissolveAndDiscard(level);
         return;
      }

      LivingEntity target = this.resolveTarget(level, owner);
      if (target == null) {
         this.noTargetTicks++;
         if (this.noTargetTicks >= NO_TARGET_TIMEOUT) {
            this.dissolveAndDiscard(level);
            return;
         }
      } else {
         this.noTargetTicks = 0;
      }

      this.updateOrbitPosition(owner, target);
      this.setFacing(target != null ? target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(this.position()) : owner.getLookAngle());
      this.spawnElementalBody(level);

      if (this.shootDelay > 0) {
         this.shootDelay--;
         return;
      }

      if (target == null) {
         this.shootDelay = 8;
         return;
      }

      this.fireShot(level, owner, target);
      this.entityData.set(SHOTS_LEFT, this.entityData.get(SHOTS_LEFT) - 1);
      this.shootDelay = 18;
   }

   private void updateOrbitPosition(LivingEntity owner, LivingEntity target) {
      Vec3 anchor = this.orbitAroundTarget && target != null ? target.position() : owner.position();
      Vec3 look = (this.orbitAroundTarget && target != null ? target.position().subtract(owner.position()) : owner.getLookAngle()).multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = new Vec3(0.0, 0.0, 1.0);
      }
      look = look.normalize();
      Vec3 side = new Vec3(-look.z, 0.0, look.x);
      double orbit = Math.sin(this.tickCount * 0.16) * 1.35;
      double forward = 3.35 + Math.cos(this.tickCount * 0.09) * 0.4;
      double height = (this.orbitAroundTarget && target != null ? target.getBbHeight() * 0.95 : owner.getBbHeight() * 1.1) + Math.sin(this.tickCount * 0.18) * 0.16;
      Vec3 next = anchor.add(look.scale(forward)).add(side.scale(orbit)).add(0.0, height, 0.0);
      if (target != null) {
         Vec3 targetDir = target.position().subtract(anchor).multiply(1.0, 0.0, 1.0);
         if (targetDir.lengthSqr() > 1.0E-4) {
            next = next.add(targetDir.normalize().scale(0.9));
         }
      }
      this.setPos(next.x, next.y, next.z);
   }

   private LivingEntity resolveTarget(ServerLevel level, LivingEntity owner) {
      Entity stored = level.getEntity(this.entityData.get(TARGET_ID));
      if (stored instanceof LivingEntity living && canTarget(owner, living)) {
         return living;
      }
      if (this.targetRescanTicks++ % TARGET_RESCAN_INTERVAL != 0) {
         return null;
      }
      AABB search = this.getBoundingBox().inflate(26.0);
      LivingEntity best = null;
      double bestDistance = Double.MAX_VALUE;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, search, candidate -> canTarget(owner, candidate))) {
         double distance = living.distanceToSqr(this);
         if (distance < bestDistance && hasRoughLineOfSight(living)) {
            best = living;
            bestDistance = distance;
         }
      }
      if (best != null) {
         this.entityData.set(TARGET_ID, best.getId());
      }
      return best;
   }

   private boolean canTarget(LivingEntity owner, LivingEntity target) {
      return target != null
         && target.isAlive()
         && target != owner
         && target.getType() != ModEntities.PARACELSUS_SPIRIT_CANNON.get()
         && EntityUtils.isValidCombatTarget(owner, target);
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

   private void fireShot(ServerLevel level, LivingEntity owner, LivingEntity target) {
      Vec3 start = this.position();
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 dir = end.subtract(start);
      this.setFacing(dir);
      target.invulnerableTime = 0;
      float damage = 8.0F + (owner instanceof ParacelsusEntity paracelsus ? (float)(paracelsus.getCurrentMp() * 0.03) : 0.0F);
      target.hurt(owner.damageSources().magic(), damage);
      target.invulnerableTime = 0;
      level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 12, 0.14, 0.14, 0.14, 0.02);
      level.sendParticles(ParticleTypes.ENCHANT, start.x, start.y, start.z, 14, 0.24, 0.24, 0.24, 0.03);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, start.x, start.y, start.z, 5, 0.08, 0.08, 0.08, 0.01);
      level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.45F, 1.25F);
   }

   private void spawnElementalBody(ServerLevel level) {
      if (this.tickCount % 2 != 0) {
         return;
      }
      double phase = this.tickCount * 0.22;
      double radius = 0.32;
      level.sendParticles(FIRE, this.getX() + Math.cos(phase) * radius, this.getY() + 0.12, this.getZ() + Math.sin(phase) * radius, 2, 0.025, 0.025, 0.025, 0.0);
      level.sendParticles(WATER, this.getX() + Math.cos(phase + Math.PI * 0.5) * radius, this.getY() + 0.02, this.getZ() + Math.sin(phase + Math.PI * 0.5) * radius, 2, 0.025, 0.025, 0.025, 0.0);
      level.sendParticles(EARTH, this.getX() + Math.cos(phase + Math.PI) * radius, this.getY() - 0.08, this.getZ() + Math.sin(phase + Math.PI) * radius, 2, 0.025, 0.025, 0.025, 0.0);
      level.sendParticles(WIND, this.getX() + Math.cos(phase + Math.PI * 1.5) * radius, this.getY() + 0.2, this.getZ() + Math.sin(phase + Math.PI * 1.5) * radius, 2, 0.025, 0.025, 0.025, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.05, this.getZ(), 1, 0.04, 0.04, 0.04, 0.0);
   }

   private void dissolveAndDiscard(ServerLevel level) {
      if (!this.dissolving) {
         this.dissolving = true;
         level.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 10, 0.18, 0.18, 0.18, 0.08);
         level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 6, 0.12, 0.12, 0.12, 0.04);
         level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.35F, 1.8F);
      }
      this.discard();
   }

   private ParacelsusEntity getOwnerLiving(ServerLevel level) {
      if (this.ownerUuid == null) {
         return null;
      }
      Entity owner = level.getEntity(this.ownerUuid);
      return owner instanceof ParacelsusEntity paracelsus ? paracelsus : null;
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

   private static Vec3 initialPosition(LivingEntity owner, LivingEntity target, boolean orbitAroundTarget) {
      Vec3 look = (orbitAroundTarget && target != null ? target.position().subtract(owner.position()) : owner.getLookAngle()).multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() < 1.0E-4) {
         look = new Vec3(0.0, 0.0, 1.0);
      }
      look = look.normalize();
      Vec3 side = new Vec3(-look.z, 0.0, look.x);
      Vec3 base = orbitAroundTarget && target != null ? target.position() : owner.position();
      double height = orbitAroundTarget && target != null ? target.getBbHeight() * 0.95 : owner.getBbHeight() * 1.15;
      return base.add(look.scale(3.2)).add(side.scale(1.8)).add(0.0, height, 0.0);
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

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.ownerUuid = tag.getUUID("Owner");
      }
      this.entityData.set(LIFE_TICKS, tag.contains("LifeTicks") ? tag.getInt("LifeTicks") : 20 * 8);
      this.entityData.set(SHOTS_LEFT, tag.contains("ShotsLeft") ? tag.getInt("ShotsLeft") : 3);
      this.entityData.set(TARGET_ID, tag.contains("TargetId") ? tag.getInt("TargetId") : -1);
      this.entityData.set(AIM_YAW, tag.getFloat("AimYaw"));
      this.entityData.set(AIM_PITCH, tag.getFloat("AimPitch"));
      this.shootDelay = tag.getInt("ShootDelay");
      this.noTargetTicks = tag.getInt("NoTargetTicks");
      this.targetRescanTicks = tag.getInt("TargetRescanTicks");
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) {
         tag.putUUID("Owner", this.ownerUuid);
      }
      tag.putInt("LifeTicks", this.entityData.get(LIFE_TICKS));
      tag.putInt("ShotsLeft", this.entityData.get(SHOTS_LEFT));
      tag.putInt("TargetId", this.entityData.get(TARGET_ID));
      tag.putFloat("AimYaw", this.entityData.get(AIM_YAW));
      tag.putFloat("AimPitch", this.entityData.get(AIM_PITCH));
      tag.putInt("ShootDelay", this.shootDelay);
      tag.putInt("NoTargetTicks", this.noTargetTicks);
      tag.putInt("TargetRescanTicks", this.targetRescanTicks);
   }
}

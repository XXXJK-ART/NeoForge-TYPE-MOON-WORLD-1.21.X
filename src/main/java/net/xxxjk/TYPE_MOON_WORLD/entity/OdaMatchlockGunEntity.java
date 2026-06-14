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
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID ownerUuid;
   private int shootDelay;
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

   public boolean isFloatingFor(UUID ownerId) {
      return ownerId != null && this.entityData.get(FOLLOW_MODE) && ownerId.equals(this.ownerUuid);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(SHOTS_LEFT, 3);
      builder.define(LIFE_TICKS, 20 * 8);
      builder.define(FOLLOW_MODE, false);
      builder.define(ORBIT_INDEX, 0);
      builder.define(ORBIT_COUNT, 1);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      int life = this.entityData.get(LIFE_TICKS) - 1;
      this.entityData.set(LIFE_TICKS, life);
      if (life <= 0 || this.entityData.get(SHOTS_LEFT) <= 0) {
         dissolveAndDiscard(level);
         return;
      }
      LivingEntity owner = getOwnerLiving(level);
      if (this.entityData.get(FOLLOW_MODE)) {
         if (owner == null || !owner.isAlive()) {
            dissolveAndDiscard(level);
            return;
         }
         updateFollowPosition(owner);
      }
      if (this.shootDelay > 0) {
         this.shootDelay--;
         return;
      }
      LivingEntity target = findTarget(level, owner);
      if (target == null) {
         this.shootDelay = 8;
         return;
      }
      fireAt(level, owner, target);
      this.entityData.set(SHOTS_LEFT, this.entityData.get(SHOTS_LEFT) - 1);
      this.shootDelay = this.entityData.get(FOLLOW_MODE) ? 18 + this.random.nextInt(12) : 16 + this.random.nextInt(10);
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
      setFacing(end.subtract(start));
      spawnTracer(level, start, end);
      float damage = OdaNobunagaCombatHelper.scaleThreeThousandWorldsDamage(owner, target, 10.0F);
      target.invulnerableTime = 0;
      target.hurt(owner != null ? owner.damageSources().mobAttack(owner) : this.damageSources().magic(), damage);
      target.invulnerableTime = 0;
      if (owner != null) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(owner, target);
      }
      level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.45F, 1.75F);
      level.sendParticles(ParticleTypes.SMOKE, start.x, start.y, start.z, 6, 0.08, 0.08, 0.08, 0.02);
      level.sendParticles(ParticleTypes.FLAME, start.x, start.y, start.z, 2, 0.04, 0.04, 0.04, 0.01);
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

   private static void spawnTracer(ServerLevel level, Vec3 start, Vec3 end) {
      Vec3 diff = end.subtract(start);
      int steps = Math.max(2, Mth.ceil(diff.length() * 2.0));
      Vec3 step = diff.scale(1.0 / steps);
      for (int i = 0; i <= steps; i++) {
         Vec3 p = start.add(step.scale(i));
         level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.01, 0.01, 0.01, 0.0);
      }
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
         float yaw = (float)(Mth.atan2(horizontal.z, horizontal.x) * Mth.RAD_TO_DEG) - 90.0F;
         this.setYRot(yaw);
         this.yRotO = yaw;
      }
      double horizontalLength = Math.max(1.0E-4, horizontal.length());
      float pitch = (float)(-Mth.atan2(direction.y, horizontalLength) * Mth.RAD_TO_DEG);
      this.setXRot(pitch);
      this.xRotO = pitch;
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
      this.shootDelay = tag.getInt("ShootDelay");
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
      tag.putInt("ShootDelay", this.shootDelay);
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

package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import org.joml.Vector3f;

public class GaeBulgProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(GaeBulgProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(GaeBulgProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> ARMY_DAMAGE = SynchedEntityData.defineId(GaeBulgProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> SPLIT_INDEX = SynchedEntityData.defineId(GaeBulgProjectileEntity.class, EntityDataSerializers.INT);
   private static final DustParticleOptions DEATH_THORN_TRAIL = new DustParticleOptions(new Vector3f(0.45F, 0.0F, 0.02F), 1.25F);
   private static final int SINGLE_HOMING_TICKS = 60;
   private static final double ARMY_EXPLOSION_DISTANCE_SQR = 4.0D;
   private static final double SPLIT_TRIGGER_DISTANCE = 5.0D;
   private static final double SPLIT_IMPACT_DISTANCE_SQR = 9.0D;
   private static final int SPLIT_COUNT = 10;
   private static final double SPLIT_BURST_RADIUS = 4.25D;
   private int lifeTime = 0;
   public final List<Vec3> tracePos = new ArrayList<>();

   public GaeBulgProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public GaeBulgProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, LivingEntity shooter, Level level) {
      super(type, shooter, level);
      this.setItem(new ItemStack(ModItems.GAE_BULG.get()));
   }

   public GaeBulgProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.GAE_BULG_PROJECTILE.get(), shooter, level);
      this.setItem(new ItemStack(ModItems.GAE_BULG.get()));
   }

   public enum Mode {
      SINGLE(0),
      ARMY(1),
      SPLIT(2);

      private final int id;

      Mode(int id) {
         this.id = id;
      }

      public int id() {
         return this.id;
      }

      public static Mode fromId(int id) {
         return switch (id) {
            case 1 -> ARMY;
            case 2 -> SPLIT;
            default -> SINGLE;
         };
      }
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(MODE, 0);
      builder.define(TARGET_ID, -1);
      builder.define(ARMY_DAMAGE, 200.0F);
      builder.define(SPLIT_INDEX, 0);
   }

   public void setMode(Mode mode) {
      this.entityData.set(MODE, mode.id());
   }

   public Mode getMode() {
      return Mode.fromId(this.entityData.get(MODE));
   }

   public void setArmyDamage(float damage) {
      this.entityData.set(ARMY_DAMAGE, Mth.clamp(damage, 200.0F, 100000.0F));
   }

   public float getArmyDamage() {
      return this.entityData.get(ARMY_DAMAGE);
   }

   public void setSplitIndex(int index) {
      this.entityData.set(SPLIT_INDEX, Math.max(0, index));
   }

   public int getSplitIndex() {
      return this.entityData.get(SPLIT_INDEX);
   }

   public void setTrackedTarget(LivingEntity target) {
      this.entityData.set(TARGET_ID, target == null || EntityUtils.isImmunePlayerTarget(target) ? -1 : target.getId());
   }

   private LivingEntity getTrackedTarget() {
      Entity entity = this.level().getEntity(this.entityData.get(TARGET_ID));
      return entity instanceof LivingEntity living ? living : null;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      if (this.getMode() == Mode.ARMY || this.getMode() == Mode.SPLIT) {
         return entity != null && entity == this.getTrackedTarget() && super.canHitEntity(entity);
      }
      return entity != null && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity) && super.canHitEntity(entity);
   }

   @Override
   protected Item getDefaultItem() {
      return ModItems.GAE_BULG.get();
   }

   @Override
   public void tick() {
      this.noPhysics = true;
      this.setNoGravity(true);
      super.tick();
      this.recordTrailPoint();
      if (this.level().isClientSide()) {
         if (this.tickCount % 2 == 0) {
            this.level().addParticle(DEATH_THORN_TRAIL, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
         return;
      }
      if (!(this.level() instanceof ServerLevel level)) return;

      this.lifeTime++;
      LivingEntity target = this.resolveTrackedTarget(level);
      if (this.getMode() == Mode.SINGLE) {
         Vec3 motion = this.getDeltaMovement();
         Vec3 back = motion.lengthSqr() > 1.0E-4 ? motion.normalize().scale(-0.42) : Vec3.ZERO;
         for (int i = 0; i < 3; i++) {
            Vec3 pos = this.position().add(back.scale(i));
            level.sendParticles(DEATH_THORN_TRAIL, pos.x, pos.y, pos.z, 1, 0.025, 0.025, 0.025, 0.0);
         }
      }
      boolean armyMode = this.getMode() == Mode.ARMY;
      boolean splitMode = this.getMode() == Mode.SPLIT;
      boolean canHome = armyMode || splitMode || this.lifeTime <= SINGLE_HOMING_TICKS;
      if (!canHome) {
         this.setTrackedTarget(null);
         target = null;
      } else if (!isUsableTarget(target)) {
         this.setTrackedTarget(null);
         target = acquireNearbyTarget(level);
      }

      if (target != null && target.isAlive()) {
         Vec3 targetPoint = this.targetPoint(target);
         double distanceToTarget = this.distanceTo(target);
         if (splitMode) {
            this.steerToward(targetPoint, 5.2, 0.82);
            this.syncRotationToMotion();
            if (this.position().distanceToSqr(targetPoint) <= SPLIT_IMPACT_DISTANCE_SQR || this.lifeTime > 90) {
               this.resolveSplitBurst(level, target, targetPoint);
               return;
            }
         } else if (armyMode) {
            this.steerToward(targetPoint, 4.0, 0.72);
            this.syncRotationToMotion();
            if (distanceToTarget <= SPLIT_TRIGGER_DISTANCE) {
               this.spawnSplitSpears(level, target);
               this.discard();
               return;
            }
         } else {
            this.steerToward(targetPoint, 2.45, 0.48);
            this.syncRotationToMotion();
            if (distanceToTarget <= 2.0D) {
               this.resolveSingleTargetHit(target);
               return;
            }
            if (this.lifeTime > 120) {
               this.resolveSingleTargetHit(target);
               return;
            }
         }
      } else if (this.getMode() == Mode.SPLIT || this.getMode() == Mode.ARMY) {
         if (this.lifeTime > 40) {
            this.discard();
         }
      } else if (this.lifeTime > 120) {
         this.discard();
      }

      if (this.getMode() == Mode.SINGLE || this.getMode() == Mode.SPLIT || this.getMode() == Mode.ARMY) {
         this.syncRotationToMotion();
      }
   }

   private LivingEntity resolveTrackedTarget(ServerLevel level) {
      LivingEntity target = this.getTrackedTarget();
      if (isUsableTarget(target)) {
         return target;
      }
      return acquireNearbyTarget(level);
   }

   private Vec3 targetPoint(LivingEntity target) {
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0);
      if (this.getMode() == Mode.SPLIT) {
         return center.add(splitOffset(this.getSplitIndex()));
      }
      return center;
   }

   private static Vec3 splitOffset(int index) {
      Vec3 unit = switch (Math.floorMod(index, SPLIT_COUNT)) {
         case 0 -> new Vec3(0.0, 1.0, 0.0);
         case 1 -> new Vec3(0.0, -0.35, 1.0);
         case 2 -> new Vec3(1.0, 0.2, 0.0);
         case 3 -> new Vec3(-1.0, 0.2, 0.0);
         case 4 -> new Vec3(0.0, 0.15, -1.0);
         case 5 -> new Vec3(0.75, 0.55, 0.75);
         case 6 -> new Vec3(-0.75, 0.55, 0.75);
         case 7 -> new Vec3(0.75, -0.15, -0.75);
         case 8 -> new Vec3(-0.75, -0.15, -0.75);
         default -> new Vec3(0.45, 0.9, -0.45);
      };
      return unit.normalize().scale(SPLIT_BURST_RADIUS * 0.78);
   }

   private void spawnSplitSpears(ServerLevel level, LivingEntity target) {
      LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
      float splitDamage = this.getArmyDamage() / SPLIT_COUNT;
      Vec3 origin = this.position();
      for (int i = 0; i < SPLIT_COUNT; i++) {
         GaeBulgProjectileEntity spear = owner != null
            ? new GaeBulgProjectileEntity(level, owner)
            : new GaeBulgProjectileEntity(ModEntities.GAE_BULG_PROJECTILE.get(), level);
         spear.setItem(new ItemStack(ModItems.GAE_BULG.get()));
         spear.setMode(Mode.SPLIT);
         spear.setSplitIndex(i);
         spear.setArmyDamage(splitDamage);
         spear.setTrackedTarget(target);
         Vec3 side = splitOffset(i).normalize().scale(0.85);
         spear.setPos(origin.x + side.x, origin.y + side.y, origin.z + side.z);
         spear.noPhysics = true;
         spear.setNoGravity(true);
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0).add(splitOffset(i));
         Vec3 dir = aim.subtract(spear.position());
         if (dir.lengthSqr() < 1.0E-4) {
            dir = this.getDeltaMovement().lengthSqr() > 1.0E-4 ? this.getDeltaMovement() : this.getLookAngle();
         }
         dir = dir.normalize();
         spear.shoot(dir.x, dir.y, dir.z, 5.8F, 0.0F);
         level.addFreshEntity(spear);
      }
      level.sendParticles(DEATH_THORN_TRAIL, origin.x, origin.y, origin.z, 46, 1.2, 1.0, 1.2, 0.06);
      level.sendParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 3, 0.15, 0.15, 0.15, 0.0);
      level.playSound(null, BlockPos.containing(origin), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.8F, 0.52F);
   }

   private void resolveSplitBurst(ServerLevel level, LivingEntity trackedTarget, Vec3 center) {
      Vec3 burstCenter = this.position();
      LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
      DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().magic();
      float damage = this.getArmyDamage();
      Set<Integer> damaged = new HashSet<>();
      AABB damageBox = new AABB(burstCenter, burstCenter).inflate(SPLIT_BURST_RADIUS);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         damageBox,
         e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         if (living.distanceToSqr(burstCenter) > SPLIT_BURST_RADIUS * SPLIT_BURST_RADIUS || !damaged.add(living.getId())) {
            continue;
         }
         float finalDamage = MagicResistanceHelper.applyNoblePhantasmMagicResistance(living, damage);
         finalDamage = HeraclesGodHandHelper.applyAntiHeraclesNoblePhantasmSpecialAttack(living, finalDamage);
         if (!this.tryConsumeGodHandLife(living, finalDamage, false)) {
            this.applyGuaranteedDamage(living, source, finalDamage);
         }
         Vec3 push = living.position().subtract(burstCenter).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() > 1.0E-4) {
            push = push.normalize();
            living.push(push.x * 1.0, 0.36, push.z * 1.0);
            living.hurtMarked = true;
         }
      }

      VFXServerEffects.spawn(level, "gae_bolg_army_impact", burstCenter, 96.0);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, burstCenter.x, burstCenter.y, burstCenter.z, 1, 0.12, 0.12, 0.12, 0.0);
      level.sendParticles(ParticleTypes.FLASH, burstCenter.x, burstCenter.y, burstCenter.z, 2, 0.1, 0.1, 0.1, 0.0);
      level.sendParticles(ParticleTypes.CRIT, burstCenter.x, burstCenter.y, burstCenter.z, 32, SPLIT_BURST_RADIUS * 0.35, SPLIT_BURST_RADIUS * 0.35, SPLIT_BURST_RADIUS * 0.35, 0.12);
      level.sendParticles(ParticleTypes.CLOUD, burstCenter.x, burstCenter.y, burstCenter.z, 24, SPLIT_BURST_RADIUS * 0.25, 0.35, SPLIT_BURST_RADIUS * 0.25, 0.04);
      level.playSound(null, BlockPos.containing(burstCenter), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.65F, 0.72F);
      this.breakLowHardnessTerrain(level, burstCenter, SPLIT_BURST_RADIUS, 0.0);
      this.discard();
   }

   private boolean isUsableTarget(LivingEntity target) {
      Entity ownerEntity = this.getOwner();
      return target != null && target.isAlive() && target != ownerEntity
         && (ownerEntity == null || !target.isAlliedTo(ownerEntity))
         && (!(ownerEntity instanceof LivingEntity owner) || !ServantMasterTargeting.isContractMaster(owner, target))
         && !EntityUtils.isImmunePlayerTarget(target);
   }

   private LivingEntity acquireNearbyTarget(ServerLevel level) {
      Entity ownerEntity = this.getOwner();
      if (!(ownerEntity instanceof LivingEntity owner)) return null;
      double radius = this.getMode() == Mode.SINGLE ? 32.0 : 24.0;
      AABB search = this.getBoundingBox().inflate(radius);
      LivingEntity nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search, this::isUsableTarget)) {
         double distance = this.distanceToSqr(candidate);
         if (distance < nearestDistance) {
            nearest = candidate;
            nearestDistance = distance;
         }
      }
      if (nearest != null) this.setTrackedTarget(nearest);
      return nearest;
   }

   private void recordTrailPoint() {
      if (this.getMode() != Mode.SINGLE) {
         return;
      }
      Vec3 current = this.position();
      if (this.tracePos.isEmpty() || this.tracePos.get(this.tracePos.size() - 1).distanceToSqr(current) > 0.04) {
         this.tracePos.add(current);
      }
      while (this.tracePos.size() > 18) {
         this.tracePos.remove(0);
      }
   }

   private void steerToward(Vec3 targetPos, double strength, double blend) {
      Vec3 toTarget = targetPos.subtract(this.position());
      if (toTarget.lengthSqr() < 1.0E-4) {
         return;
      }

      Vec3 current = this.getDeltaMovement();
      double speed = Math.max(strength, current.length());
      Vec3 desired = toTarget.normalize().scale(speed);
      Vec3 next = current.scale(1.0 - blend).add(desired.scale(blend));
      this.setDeltaMovement(next);
      this.hasImpulse = true;
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (this.level().isClientSide()) {
         return;
      }

      if (result.getEntity() instanceof LivingEntity living) {
         if (this.getMode() == Mode.SINGLE) {
            this.resolveSingleTargetHit(living);
         } else if (this.getMode() == Mode.ARMY) {
            this.spawnSplitSpears((ServerLevel)this.level(), living);
            this.discard();
         } else if (this.getMode() == Mode.SPLIT) {
            this.resolveSplitBurst((ServerLevel)this.level(), living, this.targetPoint(living));
         }
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (this.level().isClientSide()) {
         return;
      }

      if (result.getType() == HitResult.Type.BLOCK) {
         if (this.getMode() == Mode.SINGLE || this.getMode() == Mode.ARMY || this.getMode() == Mode.SPLIT) {
            return;
         }
      }
   }

   private void syncRotationToMotion() {
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-4) {
         return;
      }

      double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 180.0F / (float)Math.PI));
      this.setXRot((float)(Mth.atan2(motion.y, horizontal) * 180.0F / (float)Math.PI));
   }

   private void clearPathObstacles(double distance) {
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-4) {
         return;
      }

      Vec3 direction = motion.normalize();
      for (double step = 0.35; step <= distance; step += 0.35) {
         BlockPos pos = BlockPos.containing(this.position().add(direction.scale(step)));
         if (this.tryDestroyBlock(pos)) {
            return;
         }
      }
   }

   private boolean tryDestroyBlock(BlockPos pos) {
      BlockState state = this.level().getBlockState(pos);
      float hardness = state.getDestroySpeed(this.level(), pos);
      if (state.isAir() || hardness < 0.0F || hardness > 50.0F || state.is(Blocks.BEDROCK)) {
         return false;
      }

      this.level().removeBlock(pos, false);
      return true;
   }

   private void nudgeAroundObstacle(LivingEntity target) {
      Vec3 motion = this.getDeltaMovement();
      Vec3 toTarget = target.position().subtract(this.position());
      Vec3 side = new Vec3(-motion.z, 0.0, motion.x);
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(-toTarget.z, 0.0, toTarget.x);
      }
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(this.random.nextBoolean() ? 1.0 : -1.0, 0.0, 0.0);
      }

      double speed = Math.max(1.2, motion.length());
      Vec3 adjusted = motion.scale(0.35).add(side.normalize().scale(speed)).add(0.0, toTarget.y > 1.0 ? 0.3 : 0.1, 0.0);
      this.setDeltaMovement(adjusted.normalize().scale(speed));
      this.hasImpulse = true;
      this.syncRotationToMotion();
   }

   private void resolveSingleTargetHit(LivingEntity target) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         this.discard();
         return;
      }

      LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
      DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().generic();
      if (ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(target, "gae_bolg_projectile")) {
         this.spawnSingleTargetImpact(target);
         this.discard();
         return;
      }
      boolean deathThorn = target.isAlive()
         && !(target instanceof EmiyaArcherEntity)
         && !(target instanceof EnkiduEntity)
         && this.random.nextFloat() < CuChulainnCombatHelper.getDeathThornChance(target);
      if (this.tryConsumeGodHandLife(target, 250.0F, deathThorn)) {
         this.spawnSingleTargetImpact(target);
         this.discard();
         return;
      }

      this.applyGuaranteedDamage(target, source, 250.0F);
      if (target.isAlive() && deathThorn) {
         this.applyDeathThorn(target, source);
      }

      this.spawnSingleTargetImpact(target);
      this.discard();
   }

   private void resolveArmyExplosion(Vec3 center) {
      LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
      LivingEntity trackedTarget = this.getTrackedTarget();
      DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().magic();
      double radius = 24.0;
      int waveCount = 24;
      double waveStep = radius / waveCount;
      Set<Integer> damagedEntities = new HashSet<>();
      float armyDamage = this.getArmyDamage();

      if (owner instanceof ServantEntity servant) {
         CuChulainnCombatHelper.applyExhaustion(servant, 200);
      }

      if (this.level() instanceof ServerLevel sl) {
         VFXServerEffects.spawn(sl, "gae_bolg_army_impact", center, 128.0);
         this.spawnArmyExplosionShellEffects(sl, center, radius);
         sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 5, 0.3, 0.3, 0.3, 0.0);
         sl.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 6, 0.15, 0.15, 0.15, 0.0);
         sl.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2, center.z, 40, 1.2, 0.4, 1.2, 0.04);
         sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.4, center.z, 24, 0.9, 0.6, 0.9, 0.03);
         sl.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.2F, 0.62F);
         for (int wave = 1; wave <= waveCount; wave++) {
            final int waveIndex = wave;
            TYPE_MOON_WORLD.queueServerWork(waveIndex * 2, () -> {
               double previousRadius = Math.max(0.0, (waveIndex - 1) * waveStep);
               double currentRadius = waveIndex * waveStep;
               this.processArmyExplosionWave(sl, center, currentRadius, previousRadius, source, owner, trackedTarget, damagedEntities, armyDamage);
            });
         }
      }
      this.discard();
   }

   private void spawnArmyExplosionShellEffects(ServerLevel level, Vec3 center, double radius) {
      float outerRadius = (float)radius;
      float midRadius = (float)(radius * 0.78);
      float innerRadius = (float)(radius * 0.58);
      int primaryColor = 0xF4F1E6;
      int accentColor = 0xCC4638;

      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.08, center.z, 0.35F, outerRadius, 0.42F, 26, primaryColor, 0.95F, 0.0F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.18, center.z, 0.25F, midRadius, 0.28F, 22, accentColor, 0.8F, 0.01F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.28, center.z, 0.18F, innerRadius, 0.2F, 18, primaryColor, 0.65F, 0.015F));

      // Crossed tilted rings to fake a visible spherical blast shell from most camera angles.
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.1, center.z, 0.22F, outerRadius, 0.2F, 24, accentColor, 0.56F, 0.0F, 90.0F, 0.0F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.1, center.z, 0.22F, outerRadius, 0.2F, 24, accentColor, 0.56F, 0.0F, 90.0F, 90.0F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.1, center.z, 0.2F, outerRadius, 0.18F, 24, primaryColor, 0.48F, 0.0F, 45.0F, 0.0F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, center.x, center.y + 0.1, center.z, 0.2F, outerRadius, 0.18F, 24, primaryColor, 0.48F, 0.0F, 45.0F, 90.0F));
   }

   private void processArmyExplosionWave(
      ServerLevel level,
      Vec3 center,
      double currentRadius,
      double previousRadius,
      DamageSource source,
      LivingEntity owner,
      LivingEntity trackedTarget,
      Set<Integer> damagedEntities,
      float armyDamage
   ) {
      int burstCount = Math.max(96, (int)(currentRadius * 18.0));
      for (int i = 0; i < burstCount; i++) {
         double theta = this.random.nextDouble() * Math.PI * 2.0;
         double phi = this.random.nextDouble() * Math.PI;
         double x = currentRadius * Math.sin(phi) * Math.cos(theta);
         double y = currentRadius * Math.cos(phi);
         double z = currentRadius * Math.sin(phi) * Math.sin(theta);
         level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x + x, center.y + y, center.z + z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.FLASH, center.x + x, center.y + y, center.z + z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.CRIT, center.x + x, center.y + y, center.z + z, 3, 0.2, 0.2, 0.2, 0.03);
         if (i % 2 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x + x, center.y + y, center.z + z, 4, 0.22, 0.22, 0.22, 0.04);
         }
         if (i % 3 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, center.x + x, center.y + y, center.z + z, 3, 0.22, 0.12, 0.22, 0.02);
         }
         if (i % 4 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x + x, center.y + y, center.z + z, 2, 0.18, 0.18, 0.18, 0.02);
         }
      }

      int ringCount = Math.max(24, (int)(currentRadius * 10.0));
      for (int i = 0; i < ringCount; i++) {
         double angle = (Math.PI * 2.0) * i / ringCount;
         double px = center.x + Math.cos(angle) * currentRadius;
         double pz = center.z + Math.sin(angle) * currentRadius;
         level.sendParticles(ParticleTypes.EXPLOSION, px, center.y + 0.35, pz, 1, 0.15, 0.15, 0.15, 0.0);
         level.sendParticles(ParticleTypes.CLOUD, px, center.y + 0.15, pz, 2, 0.1, 0.1, 0.1, 0.01);
      }

      if (Math.ceil(currentRadius) % 3 == 0) {
         level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.35F, 0.6F);
      }

      this.breakLowHardnessTerrain(level, center, currentRadius, previousRadius);

      AABB damageBox = new AABB(center, center).inflate(currentRadius);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         damageBox,
         e -> e.isAlive() && e != owner && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         double dist = Math.sqrt(living.distanceToSqr(center.x, center.y, center.z));
         if (dist > currentRadius || dist <= previousRadius || !damagedEntities.add(living.getId())) {
            continue;
         }

         float finalDamage = MagicResistanceHelper.applyNoblePhantasmMagicResistance(living, armyDamage);
         finalDamage = HeraclesGodHandHelper.applyAntiHeraclesNoblePhantasmSpecialAttack(living, finalDamage);
         if (this.tryConsumeGodHandLife(living, finalDamage, false)) {
            continue;
         }

         this.applyGuaranteedDamage(living, source, finalDamage);
         Vec3 push = living.position().subtract(center);
         double horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
         if (horizontal > 1.0E-4) {
            living.push(push.x / horizontal * 1.15, 0.4, push.z / horizontal * 1.15);
            living.hurtMarked = true;
         }
      }
   }

   private void breakLowHardnessTerrain(ServerLevel level, Vec3 center, double currentRadius, double previousRadius) {
      DeferredTerrainDestruction.queueShell(level, center, currentRadius, previousRadius, 36, (serverLevel, pos, distanceSqr, radius, origin) -> {
         BlockState state = serverLevel.getBlockState(pos);
         float hardness = state.getDestroySpeed(serverLevel, pos);
         return !state.isAir()
            && hardness >= 0.0F
            && hardness <= 35.0F
            && !state.is(Blocks.BEDROCK)
            && state.getExplosionResistance(serverLevel, pos, null) < 1200.0F;
      }, (serverLevel, pos, removed) -> {
         if ((removed & 127) == 0) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
         }
      });
   }

   private void applyGuaranteedDamage(LivingEntity target, DamageSource source, float damage) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(source, damage);
      target.invulnerableTime = 0;
      float desiredHealth = Math.max(0.0F, before - damage);
      if (target.getHealth() > desiredHealth && target.getHealth() <= before) {
         target.setHealth(desiredHealth);
      }
   }

   private boolean tryConsumeGodHandLife(LivingEntity target, float incomingDamage, boolean deathThorn) {
      CompoundTag targetData = target.getPersistentData();
      if (targetData.getBoolean("CausalSevered") || !targetData.getBoolean("GodHandActive")) {
         return false;
      }

      int livesLeft = targetData.getInt("GodHandLives");
      if (livesLeft <= 0) {
         return false;
      }

      boolean lethalByDamage = target.getHealth() <= incomingDamage;
      if (!lethalByDamage && !deathThorn) {
         return false;
      }

      target.setHealth(target.getMaxHealth());
      targetData.putInt("GodHandLives", livesLeft - 1);
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            target.getX(), target.getY() + 1.0, target.getZ(),
            30, 0.6, 0.6, 0.6, 0.15);
         sl.sendParticles(ParticleTypes.POOF,
            target.getX(), target.getY() + 0.5, target.getZ(),
            20, 0.5, 0.5, 0.5, 0.1);
         sl.playSound(null, target.blockPosition(),
            SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 0.8F);
      }
      return true;
   }

   private void applyDeathThorn(LivingEntity target, DamageSource source) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      if (ArtoriaPendragonCombatHelper.tryProtectWithAvalon(target)) {
         return;
      }
      float lethalDamage = Math.max(target.getMaxHealth() * 2.0F, 500.0F);
      target.invulnerableTime = 0;
      target.hurt(source, lethalDamage);
      target.invulnerableTime = 0;
      if (target.isAlive()) {
         target.setHealth(0.0F);
         target.die(this.damageSources().genericKill());
      }
   }

   private void spawnSingleTargetImpact(LivingEntity target) {
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.CRIT,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            18, 0.25, 0.25, 0.25, 0.15);
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
            4, 0.0, 0.0, 0.0, 0.0);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(MODE, tag.getInt("Mode"));
      this.entityData.set(TARGET_ID, tag.getInt("TargetId"));
      this.entityData.set(ARMY_DAMAGE, tag.getFloat("ArmyDamage"));
      this.entityData.set(SPLIT_INDEX, tag.getInt("SplitIndex"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("Mode", this.entityData.get(MODE));
      tag.putInt("TargetId", this.entityData.get(TARGET_ID));
      tag.putFloat("ArmyDamage", this.entityData.get(ARMY_DAMAGE));
      tag.putInt("SplitIndex", this.entityData.get(SPLIT_INDEX));
   }
}

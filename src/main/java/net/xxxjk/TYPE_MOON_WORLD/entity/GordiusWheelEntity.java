package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class GordiusWheelEntity extends IskandarMountEntity {
   private static final double REAR_BODY_DISTANCE = 2.7;
   private static final double CHARIOT_RIDER_SIDE = 0.0;
   private static final double CHARIOT_PASSENGER_SIDE = 1.0;
   private static final double CHARIOT_RIDER_FORWARD = 0.05;
   private static final double CHARIOT_PASSENGER_FORWARD = 0.35;
   private static final double CHARIOT_RIDER_HEIGHT = 0.65;
   private static final double CHARIOT_PASSENGER_HEIGHT = 0.65;
   private static final double THUNDER_STRIKE_RADIUS = 18.0;
   private static final int THUNDER_STRIKE_MIN_DELAY = 24;
   private static final int THUNDER_STRIKE_RANDOM_DELAY = 36;
   private static final int DIVE_COOLDOWN_TICKS = 9 * 20;
   private static final int DIVE_ASCENT_TICKS = 18;
   private static final int DIVE_ATTACK_TICKS = 16;
   private static final float LIGHTNING_AURA_DAMAGE = 4.0F;
   private static final float THUNDER_STRIKE_DAMAGE = 18.0F;
   private static final float DIVE_DIRECT_DAMAGE = 60.0F;
   private static final float DIVE_IMPACT_DAMAGE = 45.0F;
   private static final double LIGHTNING_AURA_RADIUS = 3.4;
   private static final int THUNDER_ROAR_COOLDOWN_TICKS = 16 * 20;
   private double rearX;
   private double rearY;
   private double rearZ;
   private double prevRearX;
   private double prevRearY;
   private double prevRearZ;
   private long nextThunderStrikeTick;
   private long nextDiveTick;
   private long nextThunderRoarTick;
   private int diveTicks;
   private boolean diveAttackPhase;
   private Vec3 diveTarget = Vec3.ZERO;
   private final Set<UUID> diveHitTargets = new HashSet<>();

   public GordiusWheelEntity(EntityType<? extends GordiusWheelEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(false);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMountAttributes(3000.0, 0.62);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         tickFlightAndDive(level);
         if (!isFlyingMode()) {
            snapToNearbyGround();
         }
         tickRearBodyPhysics();
         tickLightningAura(level);
         tickThunderRoar(level);
      }
   }

   @Override
   public boolean isNoGravity() {
      return isFlyingMode();
   }

   public boolean isFlyingMode() {
      return this.getPersistentData().getBoolean("GordiusWheelFlyingMode");
   }

   private void setFlyingMode(boolean flying) {
      this.getPersistentData().putBoolean("GordiusWheelFlyingMode", flying);
      this.setNoGravity(flying);
      if (flying) {
         this.fallDistance = 0.0F;
      }
   }

   public boolean isDiving() {
      return this.diveTicks > 0;
   }

   @Override
   protected void followIskandarCombatIntent(ServerLevel level, IskandarEntity iskandar) {
      LivingEntity target = resolveCombatTarget(level, iskandar);
      if (target != null && target.isAlive()) {
         super.followIskandarCombatIntent(level, iskandar);
         return;
      }
      LivingEntity master = iskandar.getEntityMaster();
      if (master != null && master.isAlive() && this.distanceToSqr(master) > 36.0) {
         moveTowardGround(master.position().subtract(this.position()), getFollowSpeed());
         return;
      }
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
   }

   private void tickFlightAndDive(ServerLevel level) {
      LivingEntity target = getCombatTarget(level);
      if (target != null && target.isAlive() && this.diveTicks <= 0 && shouldStartDive(level, target)) {
         startDive(level, target);
      }
      if (this.diveTicks > 0) {
         tickDive(level, target);
         return;
      }
      if (target != null && target.isAlive() && shouldFlyForTarget(target)) {
         setFlyingMode(true);
         double targetY = target.getY() + 1.0;
         double dy = Mth.clamp(targetY - this.getY(), -0.35, 0.45);
         Vec3 motion = this.getDeltaMovement();
         this.setDeltaMovement(motion.x, dy, motion.z);
         this.fallDistance = 0.0F;
      } else {
         setFlyingMode(false);
      }
   }

   private LivingEntity getCombatTarget(ServerLevel level) {
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity iskandar = getIskandar(level);
      return iskandar != null ? iskandar.getTarget() : null;
   }

   private boolean shouldFlyForTarget(LivingEntity target) {
      return !target.onGround() || target.getY() - this.getY() > 2.5;
   }

   private boolean shouldStartDive(ServerLevel level, LivingEntity target) {
      long now = level.getGameTime();
      if (now < this.nextDiveTick) {
         return false;
      }
      double distanceSqr = this.distanceToSqr(target);
      boolean airborne = shouldFlyForTarget(target);
      boolean strongOrClustered = target.getMaxHealth() >= 120.0F || countDiveTargets(level, target.position(), 4.5) >= 3;
      return distanceSqr >= 8.0 * 8.0 && distanceSqr <= 34.0 * 34.0 && (airborne || strongOrClustered || this.getRandom().nextInt(4) == 0);
   }

   private int countDiveTargets(ServerLevel level, Vec3 center, double radius) {
      return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), this::canThunderStrike).size();
   }

   private void startDive(ServerLevel level, LivingEntity target) {
      this.nextDiveTick = level.getGameTime() + DIVE_COOLDOWN_TICKS;
      this.diveTicks = DIVE_ASCENT_TICKS + DIVE_ATTACK_TICKS;
      this.diveAttackPhase = false;
      this.diveTarget = target.position();
      this.diveHitTargets.clear();
      setFlyingMode(true);
      level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.8F, 0.62F);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2, this.getZ(), 100, 3.4, 1.6, 3.4, 0.28);
   }

   private void tickDive(ServerLevel level, LivingEntity target) {
      this.diveTicks--;
      if (target != null && target.isAlive()) {
         this.diveTarget = target.position();
      }
      if (this.diveTicks > DIVE_ATTACK_TICKS) {
         Vec3 ascent = this.diveTarget.add(0.0, 6.0 + this.getRandom().nextDouble() * 2.0, 0.0).subtract(this.position());
         moveDive(ascent, 0.62, 0.42);
         if (this.diveTicks == DIVE_ATTACK_TICKS + 1) {
            this.diveAttackPhase = true;
         }
         return;
      }
      this.diveAttackPhase = true;
      Vec3 targetPos = findDiveImpactPosition(level, this.diveTarget);
      Vec3 direction = targetPos.subtract(this.position());
      moveDive(direction, 1.35, -0.95);
      damageDiveSweep(level);
      if (this.diveTicks <= 0 || this.onGround() || this.position().distanceToSqr(targetPos) < 3.0) {
         finishDive(level, targetPos);
      }
   }

   private Vec3 findDiveImpactPosition(ServerLevel level, Vec3 targetPos) {
      int x = Mth.floor(targetPos.x);
      int z = Mth.floor(targetPos.z);
      int y = Mth.floor(targetPos.y);
      for (int cursor = y + 5; cursor >= level.getMinBuildHeight() + 1; cursor--) {
         if (!level.getBlockState(new net.minecraft.core.BlockPos(x, cursor - 1, z)).isAir()) {
            return new Vec3(targetPos.x, cursor, targetPos.z);
         }
      }
      return targetPos;
   }

   private void moveDive(Vec3 direction, double speed, double fallbackY) {
      if (direction.lengthSqr() < 1.0E-4) {
         direction = this.getLookAngle();
      }
      Vec3 unit = direction.normalize();
      float yaw = (float)(Math.atan2(-unit.x, unit.z) * 180.0 / Math.PI);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      Vec3 motion = new Vec3(unit.x * speed, Math.abs(unit.y) < 0.05 ? fallbackY : unit.y * speed, unit.z * speed);
      this.move(MoverType.SELF, motion);
      this.setDeltaMovement(motion.scale(0.35));
      this.fallDistance = 0.0F;
   }

   private void damageDiveSweep(ServerLevel level) {
      AABB area = this.getBoundingBox().inflate(2.8, 1.8, 2.8);
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::canThunderStrike)) {
         if (!this.diveHitTargets.add(victim.getUUID())) {
            continue;
         }
         victim.invulnerableTime = 0;
         victim.hurt(zeusDamageSource(level), DIVE_DIRECT_DAMAGE);
         Vec3 push = victim.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-4) {
            push = horizontalForward();
         }
         push = push.normalize();
         victim.push(push.x * 2.7, 0.55, push.z * 2.7);
         victim.hurtMarked = true;
      }
   }

   private void finishDive(ServerLevel level, Vec3 impactPos) {
      this.diveTicks = 0;
      this.diveAttackPhase = false;
      this.diveHitTargets.clear();
      setFlyingMode(false);
      this.setDeltaMovement(0.0, 0.0, 0.0);
      emitDiveImpact(level, impactPos);
      snapToNearbyGround();
   }

   private void emitDiveImpact(ServerLevel level, Vec3 impactPos) {
      level.sendParticles(ParticleTypes.FLASH, impactPos.x, impactPos.y + 1.0, impactPos.z, 6, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impactPos.x, impactPos.y + 0.8, impactPos.z, 130, 4.6, 1.8, 4.6, 0.34);
      level.sendParticles(ParticleTypes.CLOUD, impactPos.x, impactPos.y + 0.2, impactPos.z, 46, 3.8, 0.3, 3.8, 0.12);
      for (int i = 0; i < 2 + this.getRandom().nextInt(3); i++) {
         double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
         double radius = this.getRandom().nextDouble() * 5.0;
         spawnVisualThunderbolt(level, impactPos.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
      }
      level.playSound(null, net.minecraft.core.BlockPos.containing(impactPos), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.0F, 0.58F);
      level.playSound(null, net.minecraft.core.BlockPos.containing(impactPos), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.85F, 0.92F);
      AABB area = new AABB(impactPos, impactPos).inflate(5.0, 2.6, 5.0);
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::canThunderStrike)) {
         victim.invulnerableTime = 0;
         victim.hurt(zeusDamageSource(level), DIVE_IMPACT_DAMAGE);
         Vec3 push = victim.position().subtract(impactPos).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-4) {
            push = horizontalForward();
         }
         push = push.normalize();
         victim.push(push.x * 2.4, 0.6, push.z * 2.4);
         victim.hurtMarked = true;
      }
   }

   private void moveTowardGround(Vec3 direction, double speed) {
      Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
      if (flat.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 unit = flat.normalize();
      float yaw = (float)(Math.atan2(-unit.x, unit.z) * 180.0 / Math.PI);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      Vec3 motion = this.getDeltaMovement();
      double y = this.onGround() ? 0.0 : Math.max(-0.42, motion.y);
      this.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(unit.x * speed, y, unit.z * speed));
      this.setDeltaMovement(0.0, y, 0.0);
   }

   private void snapToNearbyGround() {
      if (this.onGround() || this.getDeltaMovement().y > 0.0) {
         return;
      }
      double maxSnapDistance = 1.6;
      Vec3 snap = Entity.collideBoundingBox(this, new Vec3(0.0, -maxSnapDistance, 0.0), this.getBoundingBox(), this.level(), java.util.List.of());
      if (snap.y >= -1.0E-3 || snap.y <= -maxSnapDistance + 1.0E-3) {
         return;
      }
      this.setPos(this.getX(), this.getY() + snap.y, this.getZ());
      Vec3 motion = this.getDeltaMovement();
      this.setDeltaMovement(motion.x, 0.0, motion.z);
      this.setOnGround(true);
   }

   private void tickRearBodyPhysics() {
      this.prevRearX = this.rearX;
      this.prevRearY = this.rearY;
      this.prevRearZ = this.rearZ;
      Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      forward = forward.normalize();
      Vec3 desired = this.position().subtract(forward.scale(REAR_BODY_DISTANCE));
      double lateralLag = this.onGround() ? 0.18 : 0.10;
      double verticalLag = this.onGround() ? 0.28 : 1.0;
      this.rearX += (desired.x - this.rearX) * lateralLag;
      this.rearY += (desired.y - this.rearY) * verticalLag;
      this.rearZ += (desired.z - this.rearZ) * lateralLag;
   }

   private void tickLightningAura(ServerLevel level) {
      if (this.tickCount % 4 == 0) {
         emitZeusLightningAura(level);
      }
      if (this.tickCount % 8 == 0) {
         level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.35, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      }
      if (this.tickCount % 50 == 0) {
         level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.45F, 1.45F + this.getRandom().nextFloat() * 0.22F);
         level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.55F, 1.65F + this.getRandom().nextFloat() * 0.25F);
      }
      tickThunderStrike(level);
      if (this.tickCount % 10 != 0) {
         return;
      }
      AABB area = this.getBoundingBox().inflate(LIGHTNING_AURA_RADIUS, 1.7, LIGHTNING_AURA_RADIUS);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, this::canThunderStrike)) {
         target.hurt(zeusDamageSource(level), LIGHTNING_AURA_DAMAGE);
      }
   }

   private void tickThunderRoar(ServerLevel level) {
      LivingEntity target = getCombatTarget(level);
      if (target == null || !target.isAlive()) {
         return;
      }
      long now = level.getGameTime();
      if (now < this.nextThunderRoarTick) {
         return;
      }
      this.nextThunderRoarTick = now + THUNDER_ROAR_COOLDOWN_TICKS;
      Vec3 center = this.position().add(0.0, 1.0, 0.0);
      level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.25F, 0.95F);
      level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.72F, 0.64F);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.2, center.z, 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 72, 3.2, 1.2, 3.2, 0.22);
      AABB area = this.getBoundingBox().inflate(8.0, 2.2, 8.0);
      for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, area, this::canThunderStrike)) {
         enemy.invulnerableTime = 0;
         enemy.hurt(zeusDamageSource(level), 5.0F);
         enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
         Vec3 push = enemy.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-4) {
            push = horizontalForward();
         }
         push = push.normalize();
         enemy.push(push.x * 1.8, 0.35, push.z * 1.8);
         enemy.hurtMarked = true;
      }
   }

   private void emitZeusLightningAura(ServerLevel level) {
      Vec3 forward = horizontalForward();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 rear = getRearSeatAnchor();
      Vec3 front = this.position().add(forward.scale(0.8));
      Vec3 leftBull = front.add(right.scale(-1.0));
      Vec3 rightBull = front.add(right.scale(1.0));
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, front.x, front.y + 1.05, front.z, 10, 2.2, 0.95, 2.2, 0.10);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, rear.x, rear.y + 1.35, rear.z, 10, 2.4, 1.0, 2.4, 0.11);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, leftBull.x, leftBull.y + 0.8, leftBull.z, 5, 0.7, 0.6, 0.7, 0.07);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, rightBull.x, rightBull.y + 0.8, rightBull.z, 5, 0.7, 0.6, 0.7, 0.07);
      if (this.isCharging()) {
         level.sendParticles(ParticleTypes.CLOUD, rear.x, rear.y + 0.15, rear.z, 10, 1.8, 0.15, 1.8, 0.04);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2, this.getZ(), 18, 3.0, 1.2, 3.0, 0.18);
      }
   }

   private void tickThunderStrike(ServerLevel level) {
      long now = level.getGameTime();
      if (this.nextThunderStrikeTick <= 0L) {
         scheduleNextThunderStrike(level, now);
      }
      if (now < this.nextThunderStrikeTick) {
         return;
      }
      scheduleNextThunderStrike(level, now);
      LivingEntity target = selectThunderStrikeTarget(level);
      int visualStrikeCount = target == null ? 1 + this.getRandom().nextInt(2) : 2 + this.getRandom().nextInt(2);
      for (int i = 0; i < visualStrikeCount; i++) {
         Vec3 base = i == 0 && target != null ? target.position() : randomThunderStrikePosition(target);
         emitZeusThunderStrike(level, base);
         if (i > 0) {
            continue;
         }
         AABB area = new AABB(base, base).inflate(2.2, 2.8, 2.2);
         for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::canThunderStrike)) {
            victim.invulnerableTime = 0;
            victim.hurt(zeusDamageSource(level), THUNDER_STRIKE_DAMAGE);
         }
      }
   }

   private void emitZeusThunderStrike(ServerLevel level, Vec3 strikePos) {
      spawnVisualThunderbolt(level, strikePos);
      level.sendParticles(ParticleTypes.FLASH, strikePos.x, strikePos.y + 1.0, strikePos.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strikePos.x, strikePos.y + 0.55, strikePos.z, 54, 1.7, 0.65, 1.7, 0.22);
      level.sendParticles(ParticleTypes.CLOUD, strikePos.x, strikePos.y + 0.15, strikePos.z, 10, 1.4, 0.18, 1.4, 0.04);
      level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.95F, 0.86F + this.getRandom().nextFloat() * 0.16F);
      level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.8F, 1.10F + this.getRandom().nextFloat() * 0.16F);
      int arcs = 1 + this.getRandom().nextInt(3);
      for (int i = 0; i < arcs; i++) {
         double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
         double radius = 1.4 + this.getRandom().nextDouble() * 3.2;
         Vec3 arcPos = strikePos.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
         spawnVisualThunderbolt(level, arcPos);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, arcPos.x, arcPos.y + 0.35, arcPos.z, 18, 0.8, 0.38, 0.8, 0.16);
      }
   }

   private void scheduleNextThunderStrike(ServerLevel level, long now) {
      this.nextThunderStrikeTick = now + THUNDER_STRIKE_MIN_DELAY + level.random.nextInt(THUNDER_STRIKE_RANDOM_DELAY + 1);
   }

   private LivingEntity selectThunderStrikeTarget(ServerLevel level) {
      AABB area = this.getBoundingBox().inflate(THUNDER_STRIKE_RADIUS, 6.0, THUNDER_STRIKE_RADIUS);
      LivingEntity selected = null;
      double bestDistance = Double.MAX_VALUE;
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area, this::canThunderStrike)) {
         double distance = candidate.distanceToSqr(this);
         if (distance < bestDistance) {
            bestDistance = distance;
            selected = candidate;
         }
      }
      return selected;
   }

   private boolean canThunderStrike(LivingEntity entity) {
      return entity != this
         && entity.isAlive()
         && !EntityUtils.isImmunePlayerTarget(entity)
         && !this.getPassengers().contains(entity)
         && !this.isAlliedTo(entity);
   }

   private DamageSource zeusDamageSource(ServerLevel level) {
      IskandarEntity iskandar = getIskandar(level);
      return iskandar != null ? this.damageSources().mobAttack(iskandar) : this.damageSources().lightningBolt();
   }

   private Vec3 randomThunderStrikePosition(LivingEntity target) {
      if (target != null && this.getRandom().nextBoolean()) {
         double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
         double radius = 1.0 + this.getRandom().nextDouble() * 4.5;
         return target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
      }
      double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
      double radius = 3.0 + this.getRandom().nextDouble() * (THUNDER_STRIKE_RADIUS - 3.0);
      return new Vec3(this.getX() + Math.cos(angle) * radius, this.getY(), this.getZ() + Math.sin(angle) * radius);
   }

   private static void spawnVisualThunderbolt(ServerLevel level, Vec3 pos) {
      LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
      if (lightning == null) {
         return;
      }
      lightning.moveTo(pos.x, pos.y, pos.z);
      lightning.setVisualOnly(true);
      level.addFreshEntity(lightning);
   }

   @Override
   public void performCharge(ServerLevel level, LivingEntity source, float damage, double width) {
      if (!canStartCharge()) {
         return;
      }
      super.performCharge(level, source, damage, width);
      Vec3 burst = this.position().add(0.0, 1.0, 0.0);
      level.sendParticles(ParticleTypes.FLASH, burst.x, burst.y + 0.5, burst.z, 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, burst.x, burst.y, burst.z, 140, 3.4, 1.35, 3.4, 0.34);
      level.sendParticles(ParticleTypes.CLOUD, burst.x, this.getY() + 0.12, burst.z, 28, 2.8, 0.18, 2.8, 0.08);
      spawnVisualThunderbolt(level, this.position());
      level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.65F, 0.72F);
      level.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.35F, 1.15F);
      level.playSound(null, this.blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.HOSTILE, 1.35F, 0.68F);
   }

   public Vec3 getRearBodyAnchor(float partialTick) {
      return new Vec3(
         Mth.lerp(partialTick, this.prevRearX, this.rearX),
         Mth.lerp(partialTick, this.prevRearY, this.rearY),
         Mth.lerp(partialTick, this.prevRearZ, this.rearZ)
      );
   }

   public Vec3 getIdealRearBodyAnchor() {
      return this.position().subtract(horizontalForward().scale(REAR_BODY_DISTANCE));
   }

   public double getRearBodyDistance() {
      return REAR_BODY_DISTANCE;
   }

   public void snapRearBodyToCurrentPosition() {
      Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = new Vec3(0.0, 0.0, 1.0);
      }
      Vec3 rear = this.position().subtract(forward.normalize().scale(REAR_BODY_DISTANCE));
      this.rearX = rear.x;
      this.rearY = rear.y;
      this.rearZ = rear.z;
      this.prevRearX = rear.x;
      this.prevRearY = rear.y;
      this.prevRearZ = rear.z;
   }

   @Override
   protected double getSeatSideOffset(boolean passengerSeat) {
      return passengerSeat ? CHARIOT_PASSENGER_SIDE : CHARIOT_RIDER_SIDE;
   }

   @Override
   protected double getSeatForwardOffset(boolean passengerSeat) {
      // Matches the exported "骑乘点" deck marker on the chariot model.
      return passengerSeat ? CHARIOT_PASSENGER_FORWARD : CHARIOT_RIDER_FORWARD;
   }

   @Override
   protected double getSeatHeight(boolean passengerSeat) {
      return passengerSeat ? CHARIOT_PASSENGER_HEIGHT : CHARIOT_RIDER_HEIGHT;
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction callback) {
      positionChariotPassenger(passenger, callback);
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
      boolean passengerSeat = this.masterUuid != null && this.masterUuid.equals(passenger.getUUID());
      return new Vec3(
         getSeatSideOffset(passengerSeat),
         getSeatHeight(passengerSeat),
         -REAR_BODY_DISTANCE + getSeatForwardOffset(passengerSeat)
      );
   }

   private void positionChariotPassenger(Entity passenger, MoveFunction callback) {
      Vec3 attachment = getChariotSeatPosition(passenger);
      callback.accept(passenger, attachment.x, attachment.y, attachment.z);
      passenger.setYRot(this.getYRot());
      passenger.setYHeadRot(this.getYRot());
      if (passenger instanceof LivingEntity living) {
         living.setYBodyRot(this.getYRot());
         living.setXRot(0.0F);
      }
   }

   private Vec3 getChariotSeatPosition(Entity passenger) {
      boolean passengerSeat = this.masterUuid != null && this.masterUuid.equals(passenger.getUUID());
      Vec3 forward = horizontalForward();
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 rear = getRearSeatAnchor();
      return rear
         .add(right.scale(getSeatSideOffset(passengerSeat)))
         .add(forward.scale(getSeatForwardOffset(passengerSeat)))
         .add(0.0, getSeatHeight(passengerSeat), 0.0);
   }

   private Vec3 getRearSeatAnchor() {
      if (Math.abs(this.rearX) > 1.0E-4 || Math.abs(this.rearY) > 1.0E-4 || Math.abs(this.rearZ) > 1.0E-4) {
         return new Vec3(this.rearX, this.rearY, this.rearZ);
      }
      return this.position().subtract(horizontalForward().scale(REAR_BODY_DISTANCE));
   }

   private Vec3 horizontalForward() {
      Vec3 forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         return new Vec3(0.0, 0.0, 1.0);
      }
      return forward.normalize();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putDouble("GordiusRearX", this.rearX);
      tag.putDouble("GordiusRearY", this.rearY);
      tag.putDouble("GordiusRearZ", this.rearZ);
      tag.putBoolean("GordiusWheelFlyingMode", isFlyingMode());
      tag.putLong("GordiusNextDiveTick", this.nextDiveTick);
      tag.putLong("GordiusNextThunderRoarTick", this.nextThunderRoarTick);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.rearX = tag.contains("GordiusRearX") ? tag.getDouble("GordiusRearX") : tag.contains("GordiusFrontX") ? tag.getDouble("GordiusFrontX") : this.getX();
      this.rearY = tag.contains("GordiusRearY") ? tag.getDouble("GordiusRearY") : tag.contains("GordiusFrontY") ? tag.getDouble("GordiusFrontY") : this.getY();
      this.rearZ = tag.contains("GordiusRearZ") ? tag.getDouble("GordiusRearZ") : tag.contains("GordiusFrontZ") ? tag.getDouble("GordiusFrontZ") : this.getZ();
      this.prevRearX = this.rearX;
      this.prevRearY = this.rearY;
      this.prevRearZ = this.rearZ;
      setFlyingMode(tag.getBoolean("GordiusWheelFlyingMode"));
      this.nextDiveTick = tag.getLong("GordiusNextDiveTick");
      this.nextThunderRoarTick = tag.getLong("GordiusNextThunderRoarTick");
   }

   @Override
   protected String getLoopAnimation() {
      return "animation";
   }

   @Override
   protected String getMovingAnimation() {
      return "move";
   }

   @Override
   protected String getChargeAnimation() {
      return "charge";
   }

   @Override
   protected double getCombatOrbitRadius() {
      return 10.0;
   }

   @Override
   protected double getChargeSpeed() {
      return Math.max(1.05, getCombatSpeed() * 1.75);
   }

   @Override
   protected double getChargeKnockback() {
      return 2.2;
   }

   @Override
   protected int getChargeDurationTicks() {
      return 16;
   }
}

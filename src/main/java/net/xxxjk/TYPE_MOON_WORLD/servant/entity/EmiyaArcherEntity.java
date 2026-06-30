package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EmiyaArcherEntity extends ServantEntity {
   public static final String SERVANT_KEY = "emiya_archer";
   public static final String EMIYA_CONTINUATION_ACTIVE = "EmiyaStyleBattleContinuationActive";
   private static final String CONTINUATION_PHASE = "EmiyaContinuationPhase";
   private static final String CONTINUATION_DEADLINE = "EmiyaContinuationDeadline";
   private static final String CONTINUATION_LOCK_UNTIL = "EmiyaContinuationLockUntil";
   private static final String CONTINUATION_FORCED_DEATH = "EmiyaContinuationForcedDeath";
   private static final int SECOND_WIND_TICKS = 3 * 60 * 20;
   private static final int FINAL_LOCK_TICKS = 30 * 20;

   public EmiyaArcherEntity(EntityType<EmiyaArcherEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (!this.level().isClientSide()) {
         tickEmiyaContinuation();
         if (!this.isAlive() || this.isSpiritualDissolving()) {
            return;
         }
         EmiyaArcherCombatHelper.tick(this);
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (amount <= 0.0F) {
         return false;
      }
      if (!this.level().isClientSide() && !this.getPersistentData().getBoolean(CONTINUATION_FORCED_DEATH)) {
         if (tryHandleEmiyaContinuationDamage(source, amount)) {
            return false;
         }
      }
      return super.hurt(source, amount);
   }

   @Override
   public void die(DamageSource cause) {
      if (!this.level().isClientSide()) {
         EmiyaArcherCombatHelper.cleanupUbw(this);
      }
      super.die(cause);
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide() && reason != RemovalReason.CHANGED_DIMENSION) {
         EmiyaArcherCombatHelper.cleanupUbw(this);
      }
      super.remove(reason);
   }

   private boolean tryHandleEmiyaContinuationDamage(DamageSource source, float amount) {
      CompoundTag data = this.getPersistentData();
      if (!data.getBoolean(EMIYA_CONTINUATION_ACTIVE) || this.isSpiritualDissolving()) {
         return false;
      }

      long now = this.level().getGameTime();
      long lockUntil = data.getLong(CONTINUATION_LOCK_UNTIL);
      if (lockUntil > now && this.getHealth() - amount <= 1.0F) {
         this.setHealth(1.0F);
         this.invulnerableTime = 10;
         continuationParticles(ParticleTypes.CRIT, 10, 0.12);
         return true;
      }

      if (this.getHealth() - amount > 0.0F) {
         return false;
      }

      int phase = data.getInt(CONTINUATION_PHASE);
      if (phase <= 0) {
         if (this.getRandom().nextFloat() >= 0.50F) {
            data.putBoolean(CONTINUATION_FORCED_DEATH, true);
            return false;
         }
         triggerFirstContinuation(source, now);
      } else if (phase == 1) {
         triggerSecondContinuation(now);
      } else {
         triggerFinalLock(now);
      }
      return true;
   }

   private void tickEmiyaContinuation() {
      CompoundTag data = this.getPersistentData();
      if (!data.getBoolean(EMIYA_CONTINUATION_ACTIVE)) {
         return;
      }
      long now = this.level().getGameTime();
      long lockUntil = data.getLong(CONTINUATION_LOCK_UNTIL);
      if (lockUntil > now && this.getHealth() < 1.0F) {
         this.setHealth(1.0F);
      }
      long deadline = data.getLong(CONTINUATION_DEADLINE);
      if (deadline > 0L && now >= deadline) {
         forceContinuationDissolve();
         return;
      }
      if (this.tickCount % 20 == 0 && (data.getInt(CONTINUATION_PHASE) >= 2 || lockUntil > now)) {
         continuationParticles(lockUntil > now ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ASH, lockUntil > now ? 8 : 4, 0.02);
      }
   }

   private void triggerFirstContinuation(DamageSource source, long now) {
      CompoundTag data = this.getPersistentData();
      data.putInt(CONTINUATION_PHASE, 1);
      data.remove(CONTINUATION_DEADLINE);
      data.remove(CONTINUATION_LOCK_UNTIL);
      this.setHealth(Math.max(1.0F, this.getMaxHealth() * 0.5F));
      this.invulnerableTime = 40;
      this.clearFire();
      this.setDeltaMovement(Vec3.ZERO);
      this.getNavigation().stop();
      if (this.level() instanceof ServerLevel level) {
         findSafeContinuationPos(level, source).ifPresent(pos -> {
            this.teleportTo(pos.x, pos.y, pos.z);
            this.fallDistance = 0.0F;
         });
         clearEnemyTargets(level);
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 0.9, this.getZ(), 42, 0.45, 0.65, 0.45, 0.08);
         level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.4, this.getZ(), 22, 0.5, 0.25, 0.5, 0.05);
         level.playSound(null, this.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.9F, 0.82F);
      }
   }

   private void triggerSecondContinuation(long now) {
      CompoundTag data = this.getPersistentData();
      data.putInt(CONTINUATION_PHASE, 2);
      data.putLong(CONTINUATION_DEADLINE, now + SECOND_WIND_TICKS);
      data.remove(CONTINUATION_LOCK_UNTIL);
      this.setHealth(Math.max(1.0F, this.getMaxHealth() * 0.3F));
      this.invulnerableTime = 60;
      this.clearFire();
      this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 80, 1, false, false, true));
      continuationParticles(ParticleTypes.SOUL, 36, 0.07);
      if (this.level() instanceof ServerLevel level) {
         level.playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.9F, 0.62F);
      }
   }

   private void triggerFinalLock(long now) {
      CompoundTag data = this.getPersistentData();
      data.putInt(CONTINUATION_PHASE, 3);
      data.putLong(CONTINUATION_LOCK_UNTIL, now + FINAL_LOCK_TICKS);
      data.putLong(CONTINUATION_DEADLINE, now + FINAL_LOCK_TICKS);
      this.setHealth(Math.max(1.0F, this.getMaxHealth() * 0.2F));
      this.invulnerableTime = 80;
      this.clearFire();
      this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, FINAL_LOCK_TICKS, 4, false, false, true));
      continuationParticles(ParticleTypes.FLASH, 3, 0.0);
      continuationParticles(ParticleTypes.SOUL_FIRE_FLAME, 48, 0.04);
      if (this.level() instanceof ServerLevel level) {
         level.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 1.0F, 0.55F);
      }
   }

   private void forceContinuationDissolve() {
      CompoundTag data = this.getPersistentData();
      if (data.getBoolean(CONTINUATION_FORCED_DEATH) || !this.isAlive()) {
         return;
      }
      data.putBoolean(CONTINUATION_FORCED_DEATH, true);
      this.setInvulnerable(false);
      this.setHealth(1.0F);
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.8, this.getZ(), 32, 0.35, 0.65, 0.35, 0.05);
         level.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.6F, 1.35F);
      }
      super.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
   }

   private java.util.Optional<Vec3> findSafeContinuationPos(ServerLevel level, DamageSource source) {
      Vec3 threat = source.getEntity() instanceof LivingEntity living ? living.position() : this.position().subtract(this.getLookAngle());
      Vec3 away = this.position().subtract(threat).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-4) {
         away = this.getLookAngle().scale(-1.0).multiply(1.0, 0.0, 1.0);
      }
      away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x);
      for (int ring = 14; ring >= 6; ring -= 2) {
         for (int attempt = 0; attempt < 10; attempt++) {
            double offset = (this.getRandom().nextDouble() - 0.5) * 8.0;
            Vec3 candidate = this.position().add(away.scale(ring)).add(side.scale(offset));
            BlockPos feet = findSafeFeet(level, BlockPos.containing(candidate.x, this.getY(), candidate.z));
            if (feet != null) {
               Vec3 destination = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
               if (level.getWorldBorder().isWithinBounds(feet) && level.noCollision(this, this.getBoundingBox().move(destination.subtract(this.position())))) {
                  return java.util.Optional.of(destination);
               }
            }
         }
      }
      return java.util.Optional.empty();
   }

   private BlockPos findSafeFeet(ServerLevel level, BlockPos anchor) {
      int baseY = Mth.clamp(anchor.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
      for (int dy = -4; dy <= 5; dy++) {
         BlockPos feet = new BlockPos(anchor.getX(), baseY + dy, anchor.getZ());
         BlockPos below = feet.below();
         BlockState belowState = level.getBlockState(below);
         if (belowState.isSolidRender(level, below)
            && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
            return feet;
         }
      }
      return null;
   }

   private void clearEnemyTargets(ServerLevel level) {
      AABB area = this.getBoundingBox().inflate(40.0);
      for (Mob mob : level.getEntitiesOfClass(Mob.class, area, mob -> mob != this && mob.getTarget() == this)) {
         mob.setTarget(null);
         mob.getNavigation().stop();
      }
      this.getNavigation().stop();
   }

   private void continuationParticles(net.minecraft.core.particles.ParticleOptions particle, int count, double speed) {
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(particle, this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(), count, 0.35, 0.45, 0.35, speed);
      }
   }
}

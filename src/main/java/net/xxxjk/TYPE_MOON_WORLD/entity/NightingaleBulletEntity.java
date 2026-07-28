package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleSupportService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class NightingaleBulletEntity extends ThrowableItemProjectile {
   public static final float BASE_DAMAGE = 15.0F;
   private static final int MAX_LIFE = 45;
   public final List<Vec3> tracePos = new LinkedList<>();

   public NightingaleBulletEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public NightingaleBulletEntity(Level level, LivingEntity owner) {
      super(ModEntities.NIGHTINGALE_BULLET.get(), owner, level);
      this.setNoGravity(true);
   }

   @Override
   protected Item getDefaultItem() {
      return ModItems.BULLET.get();
   }

   @Override
   public boolean isNoGravity() {
      return true;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      if (entity == null || entity == this.getOwner() || EntityUtils.isImmunePlayerTarget(entity)) return false;
      if (this.getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity target
         && (owner.isAlliedTo(target) || target.isAlliedTo(owner)
            || NightingaleSupportService.isNightingaleSource(owner) && NightingaleSupportService.isAlly(owner, target))) return false;
      return super.canHitEntity(entity);
   }

   public boolean canHitTarget(Entity entity) {
      return canHitEntity(entity);
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      super.tick();
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() > 1.0E-4) {
         this.setYRot((float)(Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
         this.setXRot((float)(Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
      }
      if (this.level().isClientSide) {
         captureTrace();
         this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      } else if (this.tickCount > MAX_LIFE) {
         this.discard();
      } else if (this.level() instanceof ServerLevel level && this.tickCount % 2 == 0) {
         level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 1, 0.01, 0.01, 0.01, 0.0);
         level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().thrown(this, this.getOwner()), BASE_DAMAGE);
         target.invulnerableTime = 0;
         impact(result.getLocation(), true);
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide && result.getType() != HitResult.Type.ENTITY) impact(result.getLocation(), false);
   }

   private void impact(Vec3 position, boolean entityHit) {
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, position.x, position.y, position.z, 9, 0.1, 0.1, 0.1, 0.03);
         level.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z, 5, 0.1, 0.1, 0.1, 0.02);
         level.playSound(null, position.x, position.y, position.z,
            entityHit ? SoundEvents.ARROW_HIT_PLAYER : SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.7F, 1.2F);
      }
      this.discard();
   }

   private void captureTrace() {
      Vec3 position = this.position();
      if (this.tracePos.isEmpty() || this.tracePos.get(this.tracePos.size() - 1).distanceToSqr(position) >= 0.01) {
         this.tracePos.add(position);
         while (this.tracePos.size() > 32) this.tracePos.remove(0);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
   }
}

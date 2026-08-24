package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class CrimsonHoundProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(CrimsonHoundProjectileEntity.class, EntityDataSerializers.INT);
   private static final float DIRECT_HIT_DAMAGE = 1000.0F;
   private static final float BROKEN_PHANTASM_SCALE = 0.55F;

   public CrimsonHoundProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public CrimsonHoundProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.CRIMSON_HOUND_PROJECTILE.get(), shooter, level);
      this.setItem(new ItemStack((net.minecraft.world.level.ItemLike)ModItems.CRIMSON_HOUND.get()));
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(TARGET_ID, -1);
   }

   public void setTrackedTarget(LivingEntity target) {
      this.entityData.set(TARGET_ID, target == null || EntityUtils.isImmunePlayerTarget(target) ? -1 : target.getId());
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity) && super.canHitEntity(entity);
   }

   @Override
   protected Item getDefaultItem() {
      return ModItems.CRIMSON_HOUND.get();
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      super.tick();
      if (this.level().isClientSide()) {
         this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         return;
      }

      Entity targetEntity = this.level().getEntity(this.entityData.get(TARGET_ID));
      boolean tracking = targetEntity instanceof LivingEntity livingTarget
         && livingTarget.isAlive()
         && !EntityUtils.isImmunePlayerTarget(livingTarget);
      if (!tracking && targetEntity != null) {
         this.entityData.set(TARGET_ID, -1);
      }
      if (tracking) {
         LivingEntity target = (LivingEntity)targetEntity;
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0).subtract(this.position());
         if (aim.lengthSqr() > 1.0E-4) {
            Vec3 desired = aim.normalize().scale(Math.max(1.6, this.getDeltaMovement().length()));
            Vec3 next = this.getDeltaMovement().scale(0.75).add(desired.scale(0.25));
            this.setDeltaMovement(next);
         }
         if (this.distanceToSqr(target) <= 1.2 * 1.2) {
            hitTarget(target);
            return;
         }
      }

      if (!tracking && this.tickCount > 80) {
         triggerBrokenPhantasm(this.position());
         this.discard();
      }
   }

   @Override
   public boolean isNoGravity() {
      return true;
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (result.getEntity() instanceof LivingEntity living && !this.level().isClientSide()) {
         hitTarget(living);
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.level().isClientSide()) {
         triggerBrokenPhantasm(result.getLocation());
         this.discard();
      }
   }

   private void hitTarget(LivingEntity target) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         this.entityData.set(TARGET_ID, -1);
         return;
      }
      target.invulnerableTime = 0;
      target.hurt(this.damageSources().thrown(this, this.getOwner()), DIRECT_HIT_DAMAGE);
      target.invulnerableTime = 0;
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 32, 0.55, 0.45, 0.55, 0.12);
         level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 24, 0.45, 0.45, 0.45, 0.12);
      }
      triggerBrokenPhantasm(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
      this.discard();
   }

   private void triggerBrokenPhantasm(Vec3 pos) {
      if (this.level() instanceof ServerLevel level) {
         Entity owner = this.getOwner();
         UBWBrokenPhantasmExplosion.explode(level, this, owner, this.getItem(), pos, BROKEN_PHANTASM_SCALE);
      }
   }
}

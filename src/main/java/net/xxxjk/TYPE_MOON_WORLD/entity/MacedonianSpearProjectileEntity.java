package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Lightweight, non-terrain-breaking spear thrown by Ionioi Hetairoi soldiers. */
public final class MacedonianSpearProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> DAMAGE =
      SynchedEntityData.defineId(MacedonianSpearProjectileEntity.class, EntityDataSerializers.FLOAT);

   public MacedonianSpearProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setItem(new ItemStack(ModItems.MACEDONIAN_SPEAR.get()));
   }

   public MacedonianSpearProjectileEntity(Level level, LivingEntity shooter, float damage) {
      super(ModEntities.MACEDONIAN_SPEAR_PROJECTILE.get(), shooter, level);
      this.setItem(new ItemStack(ModItems.MACEDONIAN_SPEAR.get()));
      this.setDamage(damage);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DAMAGE, 16.0F);
   }

   public void setDamage(float damage) {
      this.entityData.set(DAMAGE, Math.max(0.0F, damage));
   }

   public float getDamage() {
      return this.entityData.get(DAMAGE);
   }

   @Override
   protected Item getDefaultItem() {
      return ModItems.MACEDONIAN_SPEAR.get();
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      Entity owner = this.getOwner();
      return entity != null
         && entity != owner
         && entity instanceof LivingEntity living
         && living.isAlive()
         && !EntityUtils.isImmunePlayerTarget(living)
         && !(owner instanceof LivingEntity livingOwner && livingOwner.isAlliedTo(living))
         && !(owner instanceof LivingEntity livingOwner && ServantMasterProtection.isProtectedMaster(livingOwner, living))
         && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.tickCount > 45) {
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (this.level().isClientSide() || !(result.getEntity() instanceof LivingEntity target) || !this.canHitEntity(target)) {
         return;
      }
      LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
      DamageSource source = owner != null
         ? this.damageSources().mobProjectile(this, owner)
         : this.damageSources().thrown(this, this);
      target.invulnerableTime = 0;
      target.hurt(source, this.getDamage());
      target.invulnerableTime = 0;
      this.discard();
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide() && result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult) {
         this.discard();
      }
   }
}

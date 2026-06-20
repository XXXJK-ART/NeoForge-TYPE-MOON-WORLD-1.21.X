package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class EnkiduEarthWeaponProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> FIXED_DAMAGE = SynchedEntityData.defineId(EnkiduEarthWeaponProjectileEntity.class, EntityDataSerializers.FLOAT);
   private final Set<Integer> hitEntities = new HashSet<>();

   public EnkiduEarthWeaponProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public EnkiduEarthWeaponProjectileEntity(Level level, LivingEntity shooter, ItemStack stack, float damage) {
      super(ModEntities.ENKIDU_EARTH_WEAPON.get(), shooter, level);
      this.setItem(stack);
      this.entityData.set(FIXED_DAMAGE, damage);
      this.setNoGravity(true);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FIXED_DAMAGE, 16.0F);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.IRON_SWORD;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      if (entity == null || entity == this.getOwner() || EntityUtils.isImmunePlayerTarget(entity)) {
         return false;
      }
      if (entity instanceof Projectile) {
         return super.canHitEntity(entity);
      }
      return entity instanceof LivingEntity living && (this.getOwner() == null || !living.isAlliedTo(this.getOwner())) && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      super.tick();
      updatePoseFromMotion();
      if (this.level() instanceof ServerLevel level) {
         if (this.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(), this.getZ(), 2, 0.04, 0.04, 0.04, 0.01);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(), this.getZ(), 1, 0.03, 0.03, 0.03, 0.01);
         }
         interceptNearbyProjectile(level);
      }
      if (this.tickCount > 100) {
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      if (this.level().isClientSide()) {
         return;
      }
      Entity hit = result.getEntity();
      if (hit instanceof Projectile projectile) {
         cancelProjectile(projectile);
         this.discard();
         return;
      }
      if (hit instanceof LivingEntity living && this.hitEntities.add(living.getId())) {
         DamageSource source = this.getOwner() instanceof LivingEntity owner ? this.damageSources().mobProjectile(this, owner) : this.damageSources().thrown(this, this);
         living.invulnerableTime = 0;
         living.hurt(source, this.entityData.get(FIXED_DAMAGE));
         living.invulnerableTime = 0;
      }
      this.discard();
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide() && result.getType() != HitResult.Type.ENTITY) {
         this.discard();
      }
   }

   private void interceptNearbyProjectile(ServerLevel level) {
      Entity owner = this.getOwner();
      Projectile hostile = level.getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(0.6),
         p -> p != this && p.isAlive() && (owner == null || p.getOwner() == null || !owner.isAlliedTo(p.getOwner()))).stream().findFirst().orElse(null);
      if (hostile != null) {
         cancelProjectile(hostile);
         this.discard();
      }
   }

   private void cancelProjectile(Entity projectile) {
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 12, 0.25, 0.25, 0.25, 0.08);
         level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.55F, 1.8F);
      }
      projectile.discard();
   }

   private void updatePoseFromMotion() {
      var motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6) {
         return;
      }
      double horizontal = motion.horizontalDistance();
      this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 180.0F / Math.PI));
      this.setXRot((float)(Mth.atan2(motion.y, horizontal) * 180.0F / Math.PI));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public void alignToMotion() {
      updatePoseFromMotion();
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(FIXED_DAMAGE, tag.getFloat("FixedDamage"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("FixedDamage", this.entityData.get(FIXED_DAMAGE));
   }
}

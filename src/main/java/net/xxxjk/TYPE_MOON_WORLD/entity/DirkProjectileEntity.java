package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.EntityHitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class DirkProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(DirkProjectileEntity.class, EntityDataSerializers.FLOAT);
   private int lifeTime;

   public DirkProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public DirkProjectileEntity(Level level, LivingEntity owner) {
      super(ModEntities.DIRK_PROJECTILE.get(), owner, level);
      this.setItem(new ItemStack(ModItems.DIRK_SMALL_KNIFE.get()));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DAMAGE, 20.0F);
   }

   public void setDamage(float damage) {
      this.entityData.set(DAMAGE, Math.max(0.0F, damage));
   }

   public float getDamage() {
      return this.entityData.get(DAMAGE);
   }

   @Override
   protected Item getDefaultItem() {
      return ModItems.DIRK_SMALL_KNIFE.get();
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      if (entity instanceof LivingEntity living && this.getOwner() instanceof ServantEntity servant && CursedArmHassanCombatHelper.refusesToHarm(servant, living)) {
         return false;
      }
      if (entity instanceof LivingEntity living && this.getOwner() instanceof LivingEntity owner
         && HundredFacesHassanCombatHelper.areSameHundredFacesSide(owner, living)) {
         return false;
      }
      return entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity) && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      super.tick();
      this.lifeTime++;
      this.syncRotationToMotion();
      if (this.level().isClientSide()) {
         if (this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
         return;
      }
      if (this.lifeTime > 100) {
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (this.level().isClientSide()) {
         return;
      }
      if (result.getEntity() instanceof LivingEntity target) {
         LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
         if (owner instanceof ServantEntity servant && CursedArmHassanCombatHelper.refusesToHarm(servant, target)) {
            this.discard();
            return;
         }
         if (HundredFacesHassanCombatHelper.areSameHundredFacesSide(owner, target)) {
            this.discard();
            return;
         }
         DamageSource source = owner != null ? this.damageSources().mobProjectile(this, owner) : this.damageSources().generic();
         target.invulnerableTime = 0;
         target.hurt(source, this.getDamage());
         target.invulnerableTime = 0;
         if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 8, 0.2, 0.2, 0.2, 0.08);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.6F, 1.4F);
         }
      }
      this.discard();
   }

   private void syncRotationToMotion() {
      var motion = this.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-4) {
         return;
      }
      double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
      this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 180.0F / Math.PI));
      this.setXRot((float)(Mth.atan2(motion.y, horizontal) * 180.0F / Math.PI));
   }
}

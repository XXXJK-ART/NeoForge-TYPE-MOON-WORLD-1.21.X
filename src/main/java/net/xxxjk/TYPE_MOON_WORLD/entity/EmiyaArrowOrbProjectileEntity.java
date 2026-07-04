package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class EmiyaArrowOrbProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> DIRECT_DAMAGE = SynchedEntityData.defineId(EmiyaArrowOrbProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> BROKEN_PHANTASM = SynchedEntityData.defineId(EmiyaArrowOrbProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private int maxLifeTicks = 80;

   public EmiyaArrowOrbProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public EmiyaArrowOrbProjectileEntity(Level level, LivingEntity shooter, ItemStack payload) {
      super(ModEntities.EMIYA_ARROW_ORB.get(), shooter, level);
      this.setItem(payload.isEmpty() ? new ItemStack(Items.ARROW) : payload.copy());
      this.setNoGravity(true);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DIRECT_DAMAGE, 18.0F);
      builder.define(BROKEN_PHANTASM, false);
   }

   public void setDirectDamage(float damage) {
      this.entityData.set(DIRECT_DAMAGE, Math.max(0.0F, damage));
   }

   public void setBrokenPhantasm(boolean brokenPhantasm) {
      this.entityData.set(BROKEN_PHANTASM, brokenPhantasm);
   }

   public void setMaxLifeTicks(int maxLifeTicks) {
      this.maxLifeTicks = Math.max(20, maxLifeTicks);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.ARROW;
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity) && super.canHitEntity(entity);
   }

   @Override
   public boolean isNoGravity() {
      return true;
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      super.tick();
      Vec3 motion = this.getDeltaMovement();
      if (motion.lengthSqr() > 1.0E-4) {
         this.setYRot((float)(Mth.atan2(motion.x, motion.z) * 180.0F / (float)Math.PI));
         this.setXRot((float)(Mth.atan2(motion.y, motion.horizontalDistance()) * 180.0F / (float)Math.PI));
      }

      if (this.level().isClientSide) {
         this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      } else if (this.tickCount > this.maxLifeTicks) {
         this.discard();
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().thrown(this, this.getOwner()), this.entityData.get(DIRECT_DAMAGE));
         target.invulnerableTime = 0;
         impact(result.getLocation());
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.level().isClientSide) {
         impact(result.getLocation());
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
   }

   private void impact(Vec3 pos) {
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 20, 0.22, 0.22, 0.22, 0.08);
         level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.65F, 1.65F);
         if (this.entityData.get(BROKEN_PHANTASM)) {
            UBWBrokenPhantasmExplosion.explode(level, this, this.getOwner(), this.getItem(), pos, isCrimsonHoundPayload() ? 0.55F : 1.0F);
         }
      }

      this.discard();
   }

   private boolean isCrimsonHoundPayload() {
      return this.getItem().is(ModItems.CRIMSON_HOUND.get());
   }
}

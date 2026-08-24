package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class PseudoSpiralSwordProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(PseudoSpiralSwordProjectileEntity.class, EntityDataSerializers.INT);
   private static final float DIRECT_HIT_DAMAGE = 1500.0F;
   private final Set<Integer> hitIds = new HashSet<>();

   public PseudoSpiralSwordProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public PseudoSpiralSwordProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.PSEUDO_SPIRAL_SWORD_PROJECTILE.get(), shooter, level);
      this.setItem(new ItemStack((net.minecraft.world.level.ItemLike)ModItems.PSEUDO_SPIRAL_SWORD.get()));
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
      return ModItems.PSEUDO_SPIRAL_SWORD.get();
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      Vec3 previous = this.position();
      super.tick();
      if (this.level().isClientSide()) {
         this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      } else {
         Entity targetEntity = this.level().getEntity(this.entityData.get(TARGET_ID));
         if (targetEntity instanceof LivingEntity target && target.isAlive() && !EntityUtils.isImmunePlayerTarget(target)) {
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.35, 0.0).subtract(this.position());
            if (aim.lengthSqr() > 1.0E-4) {
               Vec3 desired = aim.normalize().scale(Math.max(2.2, this.getDeltaMovement().length()));
               this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(desired.scale(0.2)));
            }
            if (this.distanceToSqr(target) <= 1.35 * 1.35) {
               applyDirectHit(target);
               triggerBrokenPhantasm(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
               this.discard();
               return;
            }
         } else if (targetEntity != null) {
            this.entityData.set(TARGET_ID, -1);
         }
         damageAlongPath(previous, this.position());
         if (this.tickCount > 60) {
            triggerBrokenPhantasm(this.position());
            this.discard();
         }
      }
   }

   @Override
   public boolean isNoGravity() {
      return true;
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide()) {
         if (result.getEntity() instanceof LivingEntity living) {
            applyDirectHit(living);
         }
         triggerBrokenPhantasm(result.getLocation());
         this.discard();
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

   private void damageAlongPath(Vec3 start, Vec3 end) {
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      AABB box = new AABB(start, end).inflate(0.75);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
         entity -> entity.isAlive() && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity))) {
         if (this.hitIds.add(living.getId())) {
            applyDirectHit(living);
            triggerBrokenPhantasm(living.position().add(0.0, living.getBbHeight() * 0.45, 0.0));
            this.discard();
            return;
         }
      }
   }

   private void applyDirectHit(LivingEntity target) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         this.entityData.set(TARGET_ID, -1);
         return;
      }
      Entity owner = this.getOwner();
      target.invulnerableTime = 0;
      target.hurt(this.damageSources().thrown(this, owner), DIRECT_HIT_DAMAGE);
      target.invulnerableTime = 0;
      Vec3 push = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
      if (push.lengthSqr() > 1.0E-4) {
         push = push.normalize();
         target.push(push.x * 1.2, 0.42, push.z * 1.2);
         target.hurtMarked = true;
      }
      if (this.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 42, 0.45, 0.55, 0.45, 0.18);
         level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 28, 0.35, 0.45, 0.35, 0.1);
      }
   }

   private void triggerBrokenPhantasm(Vec3 pos) {
      if (this.level() instanceof ServerLevel level) {
         Entity owner = this.getOwner();
         UBWBrokenPhantasmExplosion.explode(level, this, owner, this.getItem(), pos, 2.0F);
      }
   }
}

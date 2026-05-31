package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class BrokenPhantasmProjectileEntity extends ThrowableItemProjectile {
   private static final EntityDataAccessor<Float> EXPLOSION_POWER = SynchedEntityData.defineId(
      BrokenPhantasmProjectileEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Boolean> IS_EXPLODING = SynchedEntityData.defineId(
      BrokenPhantasmProjectileEntity.class, EntityDataSerializers.BOOLEAN
   );
   private int lifeTime = 0;
   private int explosionTick = 0;
   private double currentRadius = 0.0;
   private BlockPos explosionCenter;
   private float maxRadius;
   private List<Entity> damagedEntities = new ArrayList<>();

   public BrokenPhantasmProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public BrokenPhantasmProjectileEntity(Level level, LivingEntity shooter, ItemStack stack) {
      super(ModEntities.BROKEN_PHANTASM_PROJECTILE.get(), shooter, level);
      this.setItem(stack);
      this.setPos(EntityUtils.getRightHandCastAnchor(shooter));
   }

   public BrokenPhantasmProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.BROKEN_PHANTASM_PROJECTILE.get(), x, y, z, level);
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(EXPLOSION_POWER, 2.0F);
      builder.define(IS_EXPLODING, false);
   }

   public void setExplosionPower(float power) {
      this.entityData.set(EXPLOSION_POWER, power);
   }

   public float getExplosionPower() {
      return (Float)this.entityData.get(EXPLOSION_POWER);
   }

   public boolean isExploding() {
      return (Boolean)this.entityData.get(IS_EXPLODING);
   }

   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity == this.getOwner() ? false : super.canHitEntity(entity);
   }

   protected Item getDefaultItem() {
      return Items.IRON_SWORD;
   }

   public void tick() {
      if (this.isExploding()) {
         if (!this.level().isClientSide) {
            this.processExplosion();
         } else {
            this.spawnExplosionParticles();
         }
      } else {
         super.tick();
         if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         } else {
            this.lifeTime++;
            if (this.lifeTime >= 200) {
               this.startExplosion();
            }
         }
      }
   }

   private float calculateDamage() {
      ItemStack stack = this.getItem();
      if (stack.isEmpty()) {
         return 4.0F;
      } else {
         ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
         double damage = modifiers.compute(1.0, EquipmentSlot.MAINHAND);
         return (float)Math.max(4.0, damage);
      }
   }

   protected void onHit(HitResult result) {
      if (!this.isExploding()) {
         if (!this.level().isClientSide) {
            this.startExplosion();
         }
      }
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return true;
   }

   private void startExplosion() {
      if (!this.isExploding()) {
         this.entityData.set(IS_EXPLODING, true);
         this.setNoGravity(true);
         this.setDeltaMovement(Vec3.ZERO);
         this.explosionCenter = this.blockPosition();
         this.maxRadius = Math.min(this.getExplosionPower(), 50.0F);
         this.currentRadius = 0.0;
         this.explosionTick = 0;
         this.level()
            .playSound(
               null,
               this.getX(),
               this.getY(),
               this.getZ(),
               SoundEvents.GENERIC_EXPLODE,
               SoundSource.HOSTILE,
               4.0F,
               (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F
            );
         if (this.maxRadius <= 5.0F) {
            double damageRadius = 2.0 + this.maxRadius * 1.5;
            AABB damageBox = new AABB(this.explosionCenter).inflate(damageRadius);
            List<Entity> entities = this.level().getEntities(this, damageBox);
            DamageSource explosionSource = this.damageSources().explosion(this, this.getOwner());

            for (Entity e : entities) {
               if (e instanceof LivingEntity && e != this.getOwner() && !EntityUtils.isImmunePlayerTarget(e)) {
                  float totalDamage = this.getExplosionPower() * 10.0F;
                  e.invulnerableTime = 0;
                  e.hurt(explosionSource, totalDamage);
                  this.damagedEntities.add(e);
                  if (this.getOwner() instanceof LivingEntity owner) {
                     EntityUtils.triggerSwarmAnger(this.level(), owner, (LivingEntity)e);
                  }
               }
            }
         }
      }
   }

   private void processExplosion() {
      if (this.currentRadius > this.maxRadius * 1.2) {
         this.discard();
      } else {
         double step = Math.max(0.5, this.maxRadius / 20.0);
         double nextRadius = this.currentRadius + step;
         if (this.maxRadius > 0.0F && this.currentRadius < this.maxRadius) {
            int rInt = (int)Math.ceil(nextRadius);
            if (rInt > this.maxRadius) {
               rInt = (int)Math.ceil(this.maxRadius);
            }

            for (int x = -rInt; x <= rInt; x++) {
               for (int y = -rInt; y <= rInt; y++) {
                  for (int z = -rInt; z <= rInt; z++) {
                     double distSqr = x * x + y * y + z * z;
                     if (distSqr <= nextRadius * nextRadius && distSqr > this.currentRadius * this.currentRadius && distSqr <= this.maxRadius * this.maxRadius) {
                        BlockPos pos = this.explosionCenter.offset(x, y, z);
                        BlockState state = this.level().getBlockState(pos);
                        if (!state.isAir() && state.getExplosionResistance(this.level(), pos, null) < 1200.0F) {
                           this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                           if (this.level().random.nextInt(10) == 0) {
                              ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, pos.getX(), pos.getY(), pos.getZ(), 1, 0.5, 0.5, 0.5, 0.0);
                           }
                        }
                     }
                  }
               }
            }
         }

         if (this.getExplosionPower() > 5.0F) {
            double damageRadius = nextRadius * 1.2;
            AABB damageBox = new AABB(this.explosionCenter).inflate(damageRadius);
            List<Entity> entities = this.level().getEntities(this, damageBox);
            DamageSource explosionSource = this.damageSources().explosion(this, this.getOwner());

            for (Entity e : entities) {
               if (!this.damagedEntities.contains(e) && e instanceof LivingEntity && e != this.getOwner() && !EntityUtils.isImmunePlayerTarget(e)) {
                  double dist = e.distanceToSqr(this.getX(), this.getY(), this.getZ());
                  if (dist <= damageRadius * damageRadius) {
                     float totalDamage = this.getExplosionPower() * 10.0F;
                     e.invulnerableTime = 0;
                     e.hurt(explosionSource, totalDamage);
                     this.damagedEntities.add(e);
                     if (this.getOwner() instanceof LivingEntity owner) {
                        EntityUtils.triggerSwarmAnger(this.level(), owner, (LivingEntity)e);
                     }
                  }
               }
            }
         }

         if (this.explosionTick % 5 == 0) {
            this.level()
               .playSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.GENERIC_EXPLODE,
                  SoundSource.HOSTILE,
                  4.0F,
                  (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F
               );
         }

         if (this.level() instanceof ServerLevel serverLevel) {
            int particleCount = (int)(nextRadius * 2.0);

            for (int i = 0; i < particleCount; i++) {
               double theta = this.level().random.nextDouble() * Math.PI * 2.0;
               double phi = this.level().random.nextDouble() * Math.PI;
               double x = nextRadius * Math.sin(phi) * Math.cos(theta);
               double y = nextRadius * Math.sin(phi) * Math.sin(theta);
               double zx = nextRadius * Math.cos(phi);
               serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX() + x, this.getY() + y, this.getZ() + zx, 1, 0.0, 0.0, 0.0, 0.0);
            }
         }

         this.currentRadius = nextRadius;
         this.explosionTick++;
      }
   }

   private void spawnExplosionParticles() {
      if (this.level().random.nextInt(3) == 0) {
         double r = this.currentRadius;
         double theta = this.level().random.nextDouble() * Math.PI * 2.0;
         double phi = this.level().random.nextDouble() * Math.PI;
         double x = r * Math.sin(phi) * Math.cos(theta);
         double y = r * Math.sin(phi) * Math.sin(theta);
         double z = r * Math.cos(phi);
         this.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + x, this.getY() + y, this.getZ() + z, 0.0, 0.0, 0.0);
      }
   }
}

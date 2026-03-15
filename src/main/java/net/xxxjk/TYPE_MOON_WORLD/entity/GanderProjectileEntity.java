package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public class GanderProjectileEntity extends ThrowableItemProjectile {
   public final List<Vec3> tracePos = new LinkedList<>();
   private static final float BASE_HIT_DAMAGE = 1.5F;
   private static final int MAX_CHARGE_SECONDS = 5;
   private static final int BREAK_BLOCK_COUNT = 5;
   private static final int PROJECTILE_MAX_LIFETIME_TICKS = 160;
   private static final int PREVIEW_MAX_LIFETIME_TICKS = 600;
   private static final double PROJECTILE_MAX_OWNER_DISTANCE_SQR = 36864.0;
   private static final double PREVIEW_MAX_ANCHOR_DISTANCE_SQR = 256.0;
   private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
   private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.12F), 1.1F);
   private static final EntityDataAccessor<Boolean> CHARGING_PREVIEW = SynchedEntityData.defineId(GanderProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(GanderProjectileEntity.class, EntityDataSerializers.FLOAT);
   private int chargeSeconds = 1;

   public GanderProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public GanderProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.GANDER_PROJECTILE.get(), shooter, level);
   }

   public GanderProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.GANDER_PROJECTILE.get(), x, y, z, level);
   }

   protected Item getDefaultItem() {
      return ModItems.GANDER.get();
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(CHARGING_PREVIEW, false);
      builder.define(VISUAL_SCALE, 1.0F);
   }

   public void setChargeSeconds(int value) {
      this.chargeSeconds = Math.max(1, Math.min(MAX_CHARGE_SECONDS, value));
   }

   public void setChargingPreview(boolean chargingPreview) {
      this.entityData.set(CHARGING_PREVIEW, chargingPreview);
   }

   public boolean isChargingPreview() {
      return (Boolean)this.entityData.get(CHARGING_PREVIEW);
   }

   public void setVisualScale(float scale) {
      this.entityData.set(VISUAL_SCALE, Math.max(0.01F, scale));
   }

   public float getVisualScale() {
      return (Float)this.entityData.get(VISUAL_SCALE);
   }

   public void tick() {
      if (!this.level().isClientSide && this.shouldDiscardForCleanup()) {
         this.discard();
      } else {
         if (this.isChargingPreview()) {
            this.setNoGravity(true);
            this.noPhysics = true;
            this.setDeltaMovement(Vec3.ZERO);
         }

         super.tick();
         if (this.isChargingPreview() && this.getOwner() instanceof LivingEntity owner) {
            Vec3 anchor = MagicGander.getChargeAnchor(owner);
            this.setPos(anchor);
            this.xo = anchor.x;
            this.yo = anchor.y;
            this.zo = anchor.z;
            if (this.level().isClientSide) {
               this.tracePos.clear();
            }
         } else if (!this.isChargingPreview()) {
            this.spawnTrailParticles();
            if (this.level().isClientSide) {
               Vec3 pos = this.position();
               boolean addPos = true;
               if (!this.tracePos.isEmpty()) {
                  Vec3 lastPos = this.tracePos.get(this.tracePos.size() - 1);
                  addPos = pos.distanceToSqr(lastPos) >= 0.01;
               }

               if (addPos) {
                  this.tracePos.add(pos);
                  if (this.tracePos.size() > 560) {
                     this.tracePos.remove(0);
                  }
               }
            }
         }
      }
   }

   protected boolean canHitEntity(Entity entity) {
      if (this.isChargingPreview()) {
         return false;
      } else {
         return entity != null && entity == this.getOwner() ? false : super.canHitEntity(entity);
      }
   }

   public boolean isOwnedBy(Entity entity) {
      return entity != null && this.ownedBy(entity);
   }

   protected void onHitEntity(EntityHitResult result) {
      if (!this.isChargingPreview()) {
         super.onHitEntity(result);
         if (!this.level().isClientSide) {
            Entity target = result.getEntity();
            if (target != this.getOwner()) {
               if (EntityUtils.isImmunePlayerTarget(target)) {
                  this.discard();
               } else {
                  if (target instanceof LivingEntity livingTarget) {
                     int amplifier = Math.max(0, this.chargeSeconds - 1);
                     int duration = 80 + this.chargeSeconds * 40;
                     float curseDamage = 2.0F + this.chargeSeconds;
                     livingTarget.hurt(this.damageSources().thrown(this, this.getOwner()), BASE_HIT_DAMAGE);
                     livingTarget.hurt(this.damageSources().magic(), curseDamage);
                     livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, true, true));
                     livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier, false, true, true));
                     livingTarget.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.max(40, duration / 2), amplifier, false, true, true));
                  }

                  this.spawnImpactParticles(result.getLocation());
                  this.discard();
               }
            }
         }
      }
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.isChargingPreview()) {
         if (!this.isRemoved()) {
            if (!this.level().isClientSide) {
               if (result instanceof BlockHitResult blockHitResult && this.canBreakImpactBlocks()) {
                  this.breakBlocksFromImpact(blockHitResult);
               }

               this.spawnImpactParticles(result.getLocation());
               this.discard();
            }
         }
      }
   }

   private void spawnTrailParticles() {
      Vec3 motion = this.getDeltaMovement();
      double speed = motion.length();
      if (!(speed < 1.0E-4)) {
         Vec3 back = motion.normalize().scale(-0.15);
         Vec3 base = this.position().add(back);
         boolean engravedGanderVisual = "gander".equals(GemEngravingService.getEngravedMagicId(this.getItem()));
         Vec3 center = this.position();
         double angle = this.tickCount * 0.42 + this.random.nextDouble() * Math.PI * 2.0;
         Vec3 blackOrbit = Vec3.ZERO;
         Vec3 redOrbit = Vec3.ZERO;
         if (engravedGanderVisual) {
            double outerRadius = 0.14;
            double innerRadius = 0.09;
            blackOrbit = new Vec3(Math.cos(angle) * outerRadius, (this.random.nextDouble() - 0.5) * 0.08, Math.sin(angle) * outerRadius);
            redOrbit = new Vec3(Math.cos(angle + Math.PI) * innerRadius, (this.random.nextDouble() - 0.5) * 0.06, Math.sin(angle + Math.PI) * innerRadius);
         }

         if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(BLACK_DUST, base.x, base.y, base.z, 2, 0.04, 0.04, 0.04, 0.0);
            serverLevel.sendParticles(RED_DUST, base.x, base.y, base.z, 2, 0.03, 0.03, 0.03, 0.0);
            if (engravedGanderVisual) {
               serverLevel.sendParticles(BLACK_DUST, center.x + blackOrbit.x, center.y + blackOrbit.y, center.z + blackOrbit.z, 1, 0.0, 0.0, 0.0, 0.0);
               serverLevel.sendParticles(RED_DUST, center.x + redOrbit.x, center.y + redOrbit.y, center.z + redOrbit.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
         } else {
            this.level().addParticle(BLACK_DUST, base.x, base.y, base.z, 0.0, 0.0, 0.0);
            this.level().addParticle(RED_DUST, base.x, base.y, base.z, 0.0, 0.0, 0.0);
            if (engravedGanderVisual) {
               this.level().addParticle(BLACK_DUST, center.x + blackOrbit.x, center.y + blackOrbit.y, center.z + blackOrbit.z, 0.0, 0.0, 0.0);
               this.level().addParticle(RED_DUST, center.x + redOrbit.x, center.y + redOrbit.y, center.z + redOrbit.z, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private void spawnImpactParticles(Vec3 impactPos) {
      double x = impactPos.x;
      double y = impactPos.y;
      double z = impactPos.z;
      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(BLACK_DUST, x, y, z, 14, 0.22, 0.16, 0.22, 0.0);
         serverLevel.sendParticles(RED_DUST, x, y, z, 10, 0.18, 0.14, 0.18, 0.0);
         serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 8, 0.16, 0.12, 0.16, 0.01);
      } else {
         for (int i = 0; i < 12; i++) {
            this.level()
               .addParticle(
                  BLACK_DUST,
                  x + (this.random.nextDouble() - 0.5) * 0.32,
                  y + (this.random.nextDouble() - 0.5) * 0.24,
                  z + (this.random.nextDouble() - 0.5) * 0.32,
                  0.0,
                  0.01,
                  0.0
               );
         }

         for (int i = 0; i < 8; i++) {
            this.level()
               .addParticle(
                  RED_DUST,
                  x + (this.random.nextDouble() - 0.5) * 0.24,
                  y + (this.random.nextDouble() - 0.5) * 0.2,
                  z + (this.random.nextDouble() - 0.5) * 0.24,
                  0.0,
                  0.01,
                  0.0
               );
         }
      }
   }

   private boolean canBreakImpactBlocks() {
      return this.chargeSeconds >= MAX_CHARGE_SECONDS;
   }

   private void breakBlocksFromImpact(BlockHitResult hitResult) {
      Vec3 motion = this.getDeltaMovement();
      Direction forward = motion.lengthSqr() > 1.0E-6 ? Direction.getNearest(motion.x, motion.y, motion.z) : hitResult.getDirection().getOpposite();
      Entity breaker = (Entity)(this.getOwner() != null ? this.getOwner() : this);
      BlockPos start = hitResult.getBlockPos();

      for (int i = 0; i < BREAK_BLOCK_COUNT; i++) {
         BlockPos target = start.relative(forward, i);
         BlockState state = this.level().getBlockState(target);
         if (!state.isAir() && !(state.getDestroySpeed(this.level(), target) < 0.0F)) {
            this.level().destroyBlock(target, false, breaker);
         }
      }
   }

   private boolean shouldDiscardForCleanup() {
      Entity owner = this.getOwner();
      if (this.isChargingPreview()) {
         if (!(owner instanceof LivingEntity livingOwner && livingOwner.isAlive() && !livingOwner.isRemoved() && livingOwner.level() == this.level())) {
            return true;
         } else if (this.tickCount > PREVIEW_MAX_LIFETIME_TICKS) {
            return true;
         } else {
            Vec3 anchor = MagicGander.getChargeAnchor(livingOwner);
            return this.position().distanceToSqr(anchor) > PREVIEW_MAX_ANCHOR_DISTANCE_SQR;
         }
      } else if (this.tickCount > PROJECTILE_MAX_LIFETIME_TICKS) {
         return true;
      } else {
         if (owner != null) {
            if (!owner.isAlive() || owner.isRemoved() || owner.level() != this.level()) {
               return true;
            }

            if (this.distanceToSqr(owner) > PROJECTILE_MAX_OWNER_DISTANCE_SQR) {
               return true;
            }
         }

         return !(this.getY() < this.level().getMinBuildHeight() - 16.0) && !(this.getY() > this.level().getMaxBuildHeight() + 32.0)
            ? !this.level().getWorldBorder().isWithinBounds(this.blockPosition())
            : true;
      }
   }
}

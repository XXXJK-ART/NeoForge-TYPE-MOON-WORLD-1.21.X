package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;

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
   private final Set<UUID> damagedEntities = new HashSet<>();

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
         Vec3 previous = this.position();
         super.tick();
         if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         } else {
            AABB swept = new AABB(previous, this.position()).inflate(0.65);
            List<LivingEntity> sweptTargets = this.level().getEntitiesOfClass(
               LivingEntity.class,
               swept,
               entity -> entity.isAlive() && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity)
            );
            if (!sweptTargets.isEmpty()) {
               LivingEntity hit = sweptTargets.get(0);
               this.setPos(hit.getX(), hit.getY() + hit.getBbHeight() * 0.5, hit.getZ());
               this.startExplosion();
               return;
            }
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
         if (this.level() instanceof ServerLevel serverLevel) {
            VFXServerEffects.spawnScaled(serverLevel, "broken_phantasm_explosion", this.position(), 128.0,
               (float)Mth.clamp(this.maxRadius * 1.2F / 25.0F, 0.35F, 2.4F));
            queueBrokenPhantasmTerrain(serverLevel, Vec3.atCenterOf(this.explosionCenter), this.maxRadius, 42.0F, this.maxRadius >= 28.0F ? 80 : 45);
         }
         this.level()
            .playSound(
               null,
               this.getX(),
               this.getY(),
               this.getZ(),
               SoundEvents.GENERIC_EXPLODE.value(),
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
                  float finalDamage = MagicResistanceHelper.applyNoblePhantasmMagicResistance((LivingEntity)e, totalDamage);
                  finalDamage = HeraclesGodHandHelper.applyAntiHeraclesNoblePhantasmSpecialAttack((LivingEntity)e, finalDamage);
                  e.hurt(explosionSource, finalDamage);
                  this.damagedEntities.add(e.getUUID());
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

         if (this.getExplosionPower() > 5.0F) {
            double damageRadius = nextRadius * 1.2;
            AABB damageBox = new AABB(this.explosionCenter).inflate(damageRadius);
            List<Entity> entities = this.level().getEntities(this, damageBox);
            DamageSource explosionSource = this.damageSources().explosion(this, this.getOwner());

            for (Entity e : entities) {
               if (e instanceof LivingEntity living && !this.damagedEntities.contains(e.getUUID()) && e != this.getOwner() && !EntityUtils.isImmunePlayerTarget(e)) {
                  double dist = e.distanceToSqr(this.getX(), this.getY(), this.getZ());
                  if (dist <= damageRadius * damageRadius) {
                     float totalDamage = this.getExplosionPower() * 10.0F;
                     e.invulnerableTime = 0;
                     float finalDamage = MagicResistanceHelper.applyNoblePhantasmMagicResistance((LivingEntity)e, totalDamage);
                     finalDamage = HeraclesGodHandHelper.applyAntiHeraclesNoblePhantasmSpecialAttack((LivingEntity)e, finalDamage);
                     e.hurt(explosionSource, finalDamage);
                     this.damagedEntities.add(e.getUUID());
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
                  SoundEvents.GENERIC_EXPLODE.value(),
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

   private static void queueBrokenPhantasmTerrain(ServerLevel level, Vec3 center, double radius, float maxHardness, int targetTicks) {
      int waveCount = Math.max(6, Math.min(28, (int)Math.ceil(radius)));
      double waveStep = radius / waveCount;
      for (int wave = 1; wave <= waveCount; wave++) {
         final int waveIndex = wave;
         TYPE_MOON_WORLD.queueServerWork(waveIndex, () -> {
            double previousRadius = Math.max(0.0, (waveIndex - 1) * waveStep);
            double currentRadius = waveIndex * waveStep;
            DeferredTerrainDestruction.queueShell(level, center, currentRadius, previousRadius, Math.max(8, targetTicks / waveCount), (serverLevel, pos, distanceSqr, shellRadius, origin) -> {
               BlockState state = serverLevel.getBlockState(pos);
               float hardness = state.getDestroySpeed(serverLevel, pos);
               return !state.isAir()
                  && hardness >= 0.0F
                  && hardness <= maxHardness
                  && !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                  && state.getExplosionResistance(serverLevel, pos, null) < 1200.0F;
            }, null);
         });
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

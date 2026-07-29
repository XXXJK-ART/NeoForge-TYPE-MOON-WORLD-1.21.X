package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import org.joml.Vector3f;

public final class ArashParticleArrowEntity extends ThrowableItemProjectile {
   public static final int NORMAL = 0;
   public static final int RAIN = 1;
   public static final int SMALL_ENERGY = 2;
   public static final int LARGE_ENERGY = 3;
   public static final double MAX_VISIBLE_FLIGHT_DISTANCE = 128.0;
   private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(ArashParticleArrowEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(ArashParticleArrowEntity.class, EntityDataSerializers.INT);
   private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.42F), 1.15F);
   private static final DustParticleOptions CHARGED_GREEN = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.42F), 2.4F);
   private static final DustParticleOptions HEAVY_RED = new DustParticleOptions(new Vector3f(1.0F, 0.04F, 0.015F), 4.0F);
   private static final DustParticleOptions DARK_RED = new DustParticleOptions(new Vector3f(0.45F, 0.0F, 0.0F), 2.6F);
   private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.08F), 2.2F);
   private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.8F);
   private double distanceTraveled;

   public ArashParticleArrowEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public ArashParticleArrowEntity(Level level, LivingEntity owner, int variant, float damage) {
      super(ModEntities.ARASH_PARTICLE_ARROW.get(), owner, level);
      this.entityData.set(VARIANT, variant);
      this.entityData.set(DAMAGE, damage);
      this.setNoGravity(variant != RAIN);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DAMAGE, 10.0F);
      builder.define(VARIANT, NORMAL);
   }

   public int getVariant() { return this.entityData.get(VARIANT); }
   public float getDamageForGameTest() { return this.entityData.get(DAMAGE); }

   @Override
   protected Item getDefaultItem() { return Items.ARROW; }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity)
         && (!(this.getOwner() instanceof LivingEntity owner) || EntityUtils.isValidCombatTarget(owner, entity instanceof LivingEntity living ? living : null))
         && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      this.setNoGravity(this.getVariant() != RAIN);
      Vec3 previousPosition = this.position();
      if (this.level() instanceof ServerLevel level) {
         Vec3 nextPosition = previousPosition.add(this.getDeltaMovement());
         if (this.distanceTraveled + this.getDeltaMovement().length() > MAX_VISIBLE_FLIGHT_DISTANCE
            || !level.hasChunkAt(BlockPos.containing(nextPosition))) {
            this.discard();
            return;
         }
      }
      super.tick();
      if (!this.level().isClientSide && !this.isRemoved()) {
         this.distanceTraveled += previousPosition.distanceTo(this.position());
      }
      if (this.level().isClientSide) {
         spawnFlightTrail();
      }
   }

   private void spawnFlightTrail() {
      int variant = this.getVariant();
      if (variant != SMALL_ENERGY && variant != LARGE_ENERGY) {
         addFlightParticle(GREEN, this.position());
         addFlightParticle(ParticleTypes.END_ROD, this.position());
         return;
      }
      Vec3 forward = this.getDeltaMovement().normalize();
      if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0.0, 0.0, 1.0);
      Vec3 side = forward.cross(new Vec3(0.0, 1.0, 0.0));
      if (side.lengthSqr() < 1.0E-6) side = new Vec3(1.0, 0.0, 0.0);
      side = side.normalize();
      Vec3 up = side.cross(forward).normalize();
      boolean heavy = variant == LARGE_ENERGY;
      int ringPoints = heavy ? 14 : 9;
      int trailLayers = heavy ? 7 : 4;
      double radius = heavy ? 0.42 : 0.23;
      Vec3 position = this.position();
      for (int layer = 0; layer < trailLayers; layer++) {
         Vec3 center = position.subtract(forward.scale(layer * (heavy ? 0.26 : 0.2)));
         double layerRadius = radius * (1.0 - layer / (double)(trailLayers + 2));
         addFlightParticle(heavy ? HEAVY_RED : CHARGED_GREEN, center);
         addFlightParticle(heavy ? DARK_RED : layer % 2 == 0 ? WHITE : GOLD, center);
         for (int i = 0; i < ringPoints; i++) {
            double angle = Math.PI * 2.0 * i / ringPoints + this.tickCount * 0.22 + layer * 0.35;
            Vec3 point = center.add(side.scale(Math.cos(angle) * layerRadius)).add(up.scale(Math.sin(angle) * layerRadius));
            addFlightParticle(heavy ? (i % 3 == 0 ? DARK_RED : HEAVY_RED)
               : i % 3 == 0 ? GOLD : CHARGED_GREEN, point);
         }
      }
      addFlightParticle(heavy ? ParticleTypes.FLAME : ParticleTypes.END_ROD, position);
      if (heavy) addFlightParticle(ParticleTypes.ELECTRIC_SPARK, position);
   }

   private void addFlightParticle(net.minecraft.core.particles.ParticleOptions particle, Vec3 position) {
      this.level().addParticle(particle, false, position.x, position.y, position.z, 0.0, 0.0, 0.0);
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
         target.invulnerableTime = 0;
         target.hurt(this.damageSources().thrown(this, this.getOwner()), this.entityData.get(DAMAGE));
         impact(this.position());
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.level().isClientSide) impact(result.getLocation());
   }

   private void impact(Vec3 impactPosition) {
      if (this.level() instanceof ServerLevel level) {
         int terrainRadius = ArashCombatRules.terrainDestructionRadius(this.getVariant());
         if (terrainRadius > 0) {
            var terrain = DeferredTerrainDestruction.queueExpandingSphere(level, impactPosition, terrainRadius, null);
            terrain.advanceTo(terrainRadius);
            terrain.seal();
         }
         int count = this.getVariant() == LARGE_ENERGY ? 120 : this.getVariant() == SMALL_ENERGY ? 70 : 14;
         double spread = this.getVariant() == LARGE_ENERGY ? 1.1 : this.getVariant() == SMALL_ENERGY ? 0.65 : 0.35;
         level.sendParticles(this.getVariant() == LARGE_ENERGY ? HEAVY_RED : GREEN,
            this.getX(), this.getY(), this.getZ(), count, spread, spread, spread, 0.12);
         if (this.getVariant() == SMALL_ENERGY || this.getVariant() == LARGE_ENERGY) {
            level.sendParticles(this.getVariant() == LARGE_ENERGY ? DARK_RED : GOLD,
               this.getX(), this.getY(), this.getZ(), count / 2, spread, spread, spread, 0.1);
            level.sendParticles(this.getVariant() == LARGE_ENERGY ? ParticleTypes.FLAME : ParticleTypes.END_ROD,
               this.getX(), this.getY(), this.getZ(), count / 2, spread, spread, spread, 0.15);
         }
         level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(),
            this.getVariant() == LARGE_ENERGY ? 3 : 1, 0, 0, 0, 0);
      }
      this.discard();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("Damage", this.entityData.get(DAMAGE));
      tag.putInt("Variant", this.getVariant());
      tag.putDouble("DistanceTraveled", this.distanceTraveled);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(DAMAGE, tag.getFloat("Damage"));
      this.entityData.set(VARIANT, tag.getInt("Variant"));
      this.distanceTraveled = tag.getDouble("DistanceTraveled");
   }
}

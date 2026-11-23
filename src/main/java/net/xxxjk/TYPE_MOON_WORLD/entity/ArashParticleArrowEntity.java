package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import org.joml.Vector3f;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ArashParticleArrowEntity extends ThrowableItemProjectile {
   public static final int NORMAL = 0;
   public static final int RAIN = 1;
   public static final int SMALL_ENERGY = 2;
   public static final int LARGE_ENERGY = 3;
   public static final double MAX_VISIBLE_FLIGHT_DISTANCE = 1024.0;
   private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(ArashParticleArrowEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(ArashParticleArrowEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> CHARGE_TICKS = SynchedEntityData.defineId(ArashParticleArrowEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> FLIGHT_TICKS = SynchedEntityData.defineId(ArashParticleArrowEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> ARASH_SPECIAL = SynchedEntityData.defineId(ArashParticleArrowEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> BALLISTIC = SynchedEntityData.defineId(ArashParticleArrowEntity.class, EntityDataSerializers.BOOLEAN);
   private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.42F), 1.15F);
   private static final DustParticleOptions CHARGED_GREEN = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.42F), 2.4F);
   private static final DustParticleOptions HEAVY_RED = new DustParticleOptions(new Vector3f(1.0F, 0.04F, 0.015F), 4.0F);
   private static final DustParticleOptions ARROW_RED = new DustParticleOptions(new Vector3f(0.92F, 0.03F, 0.02F), 1.45F);
   private static final DustParticleOptions DARK_RED = new DustParticleOptions(new Vector3f(0.45F, 0.0F, 0.0F), 2.6F);
   private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.08F), 2.2F);
   private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.8F);
   private double distanceTraveled;
   private final Set<UUID> hitTargets = new HashSet<>();
   private final java.util.List<Vec3> flightTrace = new java.util.ArrayList<>();

   public ArashParticleArrowEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   public ArashParticleArrowEntity(Level level, LivingEntity owner, int variant, float damage) {
      this(level, owner, variant, damage, 0, true);
   }

   public ArashParticleArrowEntity(Level level, LivingEntity owner, int variant, float damage, int chargeTicks) {
      this(level, owner, variant, damage, chargeTicks, true);
   }

   public ArashParticleArrowEntity(Level level, LivingEntity owner, int variant, float damage, int chargeTicks, boolean arashSpecial) {
      super(ModEntities.ARASH_PARTICLE_ARROW.get(), owner, level);
      this.entityData.set(VARIANT, variant);
      this.entityData.set(CHARGE_TICKS, Math.max(0, Math.min(ArashCombatRules.ARROW_MAX_CHARGE_TICKS, chargeTicks)));
      this.entityData.set(ARASH_SPECIAL, arashSpecial);
      this.entityData.set(DAMAGE, arashSpecial
         ? scaledArrowDamage() : damage);
      this.setNoGravity(variant != RAIN && !(arashSpecial && variant == NORMAL));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DAMAGE, 10.0F);
      builder.define(VARIANT, NORMAL);
      builder.define(CHARGE_TICKS, 0);
      builder.define(FLIGHT_TICKS, 0);
      builder.define(ARASH_SPECIAL, true);
      builder.define(BALLISTIC, false);
   }

   public int getVariant() { return this.entityData.get(VARIANT); }
   public float getDamageForGameTest() { return this.entityData.get(DAMAGE); }
   public int getChargeTicks() { return this.entityData.get(CHARGE_TICKS); }
   public int getFlightTicks() { return this.entityData.get(FLIGHT_TICKS); }
   public boolean isArashSpecial() { return this.entityData.get(ARASH_SPECIAL); }
   public java.util.List<Vec3> getFlightTrace() { return java.util.Collections.unmodifiableList(this.flightTrace); }
   public void setBallistic(boolean ballistic) { this.entityData.set(BALLISTIC, ballistic); }

   public void setChargeTicks(int ticks) {
      this.entityData.set(CHARGE_TICKS, Math.max(0, Math.min(ArashCombatRules.ARROW_MAX_CHARGE_TICKS, ticks)));
      if (this.entityData.get(ARASH_SPECIAL)) {
         this.entityData.set(DAMAGE, scaledArrowDamage());
      }
   }

   private float scaledArrowDamage() {
      return this.getVariant() == RAIN
         ? ArashCombatRules.rainArrowDamage(this.getChargeTicks(), this.getFlightTicks())
         : ArashCombatRules.arrowDamage(this.getChargeTicks(), this.getFlightTicks());
   }

   @Override
   protected Item getDefaultItem() { return Items.ARROW; }

   @Override
   protected double getDefaultGravity() {
      return this.isArashSpecial() && this.getVariant() == NORMAL
         ? ArashCombatRules.NORMAL_ARROW_GRAVITY : super.getDefaultGravity();
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return entity != null && entity != this.getOwner() && !EntityUtils.isImmunePlayerTarget(entity)
         && (!(this.getOwner() instanceof LivingEntity owner) || !(entity instanceof LivingEntity living) || !ServantMasterTargeting.isContractMaster(owner, living))
         && (!(this.getOwner() instanceof LivingEntity owner) || EntityUtils.isValidCombatTarget(owner, entity instanceof LivingEntity living ? living : null))
         && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      this.setNoGravity(this.getVariant() != RAIN && !(this.isArashSpecial() && this.getVariant() == NORMAL)
         && !this.entityData.get(BALLISTIC));
      Vec3 previousPosition = this.position();
      if (this.level() instanceof ServerLevel level) {
         int lifetime = this.getVariant() == RAIN ? ArashCombatRules.RAIN_MAX_LIFETIME_TICKS
            : ArashCombatRules.ARROW_MAX_LIFETIME_TICKS;
         if (this.getFlightTicks() >= lifetime) {
            this.discard();
            return;
         }
         this.entityData.set(FLIGHT_TICKS, this.getFlightTicks() + 1);
         if (this.entityData.get(ARASH_SPECIAL)) {
            this.entityData.set(DAMAGE, scaledArrowDamage());
         }
         if (this.getVariant() == RAIN) {
            applyHoming(level);
         }
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
         if ((this.getVariant() == SMALL_ENERGY || this.getVariant() == LARGE_ENERGY)
            && tryWideEnergyHit((ServerLevel)this.level(), previousPosition, this.position())) {
            return;
         }
      }
      if (this.level().isClientSide) {
         this.flightTrace.add(previousPosition);
         if (this.flightTrace.size() > 10) this.flightTrace.remove(0);
         spawnFlightTrail();
      }
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < MAX_VISIBLE_FLIGHT_DISTANCE * MAX_VISIBLE_FLIGHT_DISTANCE;
   }

   /**
    * Charged arrows use a deliberately generous 3x3x3 collision volume. The
    * swept box also catches targets between ticks instead of requiring the
    * projectile's tiny visual core to overlap the entity exactly.
    */
   private boolean tryWideEnergyHit(ServerLevel level, Vec3 previousPosition, Vec3 currentPosition) {
      AABB sweep = new AABB(previousPosition, currentPosition).inflate(1.5);
      LivingEntity closest = null;
      double closestDistance = Double.MAX_VALUE;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep,
         entity -> canHitEntity(entity) && entity.isAlive())) {
         double distance = target.distanceToSqr(currentPosition);
         if (distance < closestDistance) {
            closestDistance = distance;
            closest = target;
         }
      }
      if (closest == null) return false;
      clearTargetIFrames(closest);
      impact(currentPosition, closest);
      return true;
   }

   /** Arrow Rain retains its own horizontal steering; basic arrows fly unassisted. */
   private void applyHoming(ServerLevel level) {
      Vec3 motion = this.getDeltaMovement();
      Vec3 horizontalMotion = new Vec3(motion.x, 0.0, motion.z);
      if (horizontalMotion.lengthSqr() < 1.0E-6) return;
      LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
      if (owner == null) return;
      LivingEntity target = null;
      double nearest = Double.MAX_VALUE;
      double searchRadius = this.getVariant() == RAIN ? 8.0 : 10.0;
      double verticalRadius = this.getVariant() == RAIN ? 4.0 : 3.0;
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
         this.getBoundingBox().inflate(searchRadius, verticalRadius, searchRadius),
         entity -> entity != owner && entity.isAlive() && canHitEntity(entity)
            && EntityUtils.isValidCombatTarget(owner, entity))) {
         double distance = candidate.distanceToSqr(this);
         if (distance < nearest) {
            nearest = distance;
            target = candidate;
         }
      }
      if (target == null) return;
      Vec3 desired = target.position().subtract(this.position());
      Vec3 desiredHorizontal = new Vec3(desired.x, 0.0, desired.z);
      if (desiredHorizontal.lengthSqr() < 1.0E-6) return;
      desiredHorizontal = desiredHorizontal.normalize().scale(horizontalMotion.length());
      double steering = ArashCombatRules.ARROW_STEERING_STRENGTH;
      Vec3 corrected = horizontalMotion.scale(1.0 - steering).add(desiredHorizontal.scale(steering));
      if (corrected.lengthSqr() > 1.0E-6) {
         this.setDeltaMovement(corrected.x, motion.y, corrected.z);
      }
   }

   private void spawnFlightTrail() {
      // A stopped projectile has no trail; accumulated particles would hide its 3D core.
      if (this.getDeltaMovement().lengthSqr() < 1.0E-8) return;
      int variant = this.getVariant();
      if (variant != SMALL_ENERGY && variant != LARGE_ENERGY) {
         double progress = Math.min(1.0, this.getFlightTicks() / (double)ArashCombatRules.ARROW_MAX_FLIGHT_TICKS);
         if (!this.entityData.get(ARASH_SPECIAL)) {
            addFlightParticle(variant == NORMAL ? ARROW_RED : GREEN, this.position());
            addFlightParticle(ParticleTypes.END_ROD, this.position());
            return;
         }
         spawnNormalTrail(progress);
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
      double progress = Math.min(1.0, this.getFlightTicks() / (double)ArashCombatRules.ARROW_MAX_FLIGHT_TICKS);
      double radius = (heavy ? 0.62 : 0.38) * (0.5 + progress * 1.2);
      Vec3 position = this.position();
      for (int layer = 0; layer < trailLayers; layer++) {
         Vec3 center = position.subtract(forward.scale(layer * (heavy ? 0.26 : 0.2)));
         double layerRadius = radius * (1.0 - layer / (double)(trailLayers + 2));
         addFlightParticle(arrowTrailParticle(progress, heavy), center);
         addFlightParticle(layer % 2 == 0 ? WHITE : arrowTrailParticle(progress, heavy), center);
         for (int i = 0; i < ringPoints; i++) {
            double angle = Math.PI * 2.0 * i / ringPoints + this.tickCount * 0.22 + layer * 0.35;
            Vec3 point = center.add(side.scale(Math.cos(angle) * layerRadius)).add(up.scale(Math.sin(angle) * layerRadius));
            addFlightParticle(arrowTrailParticle(progress, heavy), point);
         }
      }
      addFlightParticle(ParticleTypes.END_ROD, position);
      if (heavy) addFlightParticle(ParticleTypes.ELECTRIC_SPARK, position);
   }

   private void addFlightParticle(net.minecraft.core.particles.ParticleOptions particle, Vec3 position) {
      this.level().addParticle(particle, true, position.x, position.y, position.z, 0.0, 0.0, 0.0);
   }

   private net.minecraft.core.particles.ParticleOptions arrowTrailParticle(double progress, boolean heavy) {
      float scale = ArashCombatRules.arrowTrailScale(this.getFlightTicks(), heavy);
      return stageParticle(scale);
   }

   private DustParticleOptions stageParticle(float scale) {
      int color = ArashCombatRules.arrowColor(this.getFlightTicks());
      return new DustParticleOptions(new Vector3f(((color >> 16) & 255) / 255.0F,
         ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F), scale);
   }

   private void spawnNormalTrail(double progress) {
      Vec3 forward = this.getDeltaMovement().normalize();
      if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0.0, 0.0, 1.0);
      Vec3 side = forward.cross(new Vec3(0.0, 1.0, 0.0));
      if (side.lengthSqr() < 1.0E-6) side = new Vec3(1.0, 0.0, 0.0);
      side = side.normalize();
      Vec3 up = side.cross(forward).normalize();
      Vec3 position = this.position();
      double radius = 0.12 + progress * 0.5;
      int ringPoints = this.getVariant() == RAIN ? 3 : 10;
      for (int i = 0; i < ringPoints; i++) {
         double angle = Math.PI * 2.0 * i / ringPoints + this.tickCount * 0.24;
         Vec3 point = position.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
         addFlightParticle(arrowTrailParticle(progress, false), point);
      }
      addFlightParticle(ParticleTypes.END_ROD, position);
      addFlightParticle(ParticleTypes.ELECTRIC_SPARK, position);
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity target) {
         impact(this.position(), target);
      }
   }

   private static void clearTargetIFrames(LivingEntity target) {
      target.invulnerableTime = 0;
      target.hurtTime = 0;
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (!this.level().isClientSide) impact(result.getLocation(), null);
   }

   private void impact(Vec3 impactPosition, LivingEntity directTarget) {
      if (this.level() instanceof ServerLevel level) {
         float damage = this.entityData.get(DAMAGE);
         // Rain's lower damage cap must not shrink its blast or terrain footprint.
         float blastPower = ArashCombatRules.arrowDamage(this.getChargeTicks(), this.getFlightTicks());
         double radius = this.entityData.get(ARASH_SPECIAL) ? ArashCombatRules.arrowBlastRadius(blastPower) : 0.0;
         if (directTarget != null && canHitEntity(directTarget)) applyArrowDamage(directTarget, damage);
         if (radius > 0.0) {
            AABB blast = new AABB(impactPosition, impactPosition).inflate(radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, blast,
               entity -> entity.isAlive() && entity != directTarget && canHitEntity(entity))) {
               Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
               Vec3 offset = center.subtract(impactPosition);
               if (offset.y >= 0.0 && offset.lengthSqr() <= radius * radius) applyArrowDamage(target, damage);
            }
         }
         if (this.entityData.get(ARASH_SPECIAL) && blastPower > ArashCombatRules.ARROW_MIN_DAMAGE) {
            destroyShallowImpactTerrain(level, impactPosition, blastPower);
         }
         int count = this.getVariant() == LARGE_ENERGY ? 120 : this.getVariant() == SMALL_ENERGY ? 70 : 14;
         double spread = this.getVariant() == LARGE_ENERGY ? 1.1 : this.getVariant() == SMALL_ENERGY ? 0.65 : 0.35;
         level.sendParticles(this.isArashSpecial() ? stageParticle(2.2F) : this.getVariant() == LARGE_ENERGY ? HEAVY_RED : GREEN,
            this.getX(), this.getY(), this.getZ(), count, spread, spread, spread, 0.12);
         if (this.getVariant() == SMALL_ENERGY || this.getVariant() == LARGE_ENERGY) {
            level.sendParticles(this.isArashSpecial() ? stageParticle(2.6F) : this.getVariant() == LARGE_ENERGY ? DARK_RED : GOLD,
               this.getX(), this.getY(), this.getZ(), count / 2, spread, spread, spread, 0.1);
            level.sendParticles(this.isArashSpecial() ? ParticleTypes.END_ROD
                  : this.getVariant() == LARGE_ENERGY ? ParticleTypes.FLAME : ParticleTypes.END_ROD,
               this.getX(), this.getY(), this.getZ(), count / 2, spread, spread, spread, 0.15);
         }
         level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(),
            this.getVariant() == LARGE_ENERGY ? 3 : 1, 0, 0, 0, 0);
         if (this.entityData.get(ARASH_SPECIAL)) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impactPosition.x, impactPosition.y,
               impactPosition.z, 1, 0.0, 0.0, 0.0, 0.0);
            float volume = this.getVariant() == LARGE_ENERGY ? 2.4F
               : this.getVariant() == RAIN ? 0.38F : 1.25F;
            level.playSound(null, impactPosition.x, impactPosition.y, impactPosition.z,
               SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
               volume, this.getVariant() == LARGE_ENERGY ? 0.72F : 1.2F);
         }
      }
      this.discard();
   }

   private void destroyShallowImpactTerrain(ServerLevel level, Vec3 center, float damage) {
      int depth = ArashCombatRules.arrowTerrainDepth(damage);
      double radius = ArashCombatRules.arrowTerrainRadius(damage);
      if (!Config.terrainDestructionEnabled || depth <= 0 || radius <= 0.0) return;
      // Use the shared budget and protected-block rules, especially for 100/300-arrow rain.
      DeferredTerrainDestruction.queueRadialShallowImpact(level, center, radius, depth, 80.0F);
   }

   private void applyArrowDamage(LivingEntity target, float damage) {
      if (!this.hitTargets.add(target.getUUID())) return;
      clearTargetIFrames(target);
      target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("Damage", this.entityData.get(DAMAGE));
      tag.putInt("Variant", this.getVariant());
      tag.putDouble("DistanceTraveled", this.distanceTraveled);
      tag.putInt("ChargeTicks", this.getChargeTicks());
      tag.putInt("FlightTicks", this.getFlightTicks());
      tag.putBoolean("ArashSpecial", this.entityData.get(ARASH_SPECIAL));
      tag.putBoolean("Ballistic", this.entityData.get(BALLISTIC));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(DAMAGE, tag.getFloat("Damage"));
      this.entityData.set(VARIANT, tag.getInt("Variant"));
      this.distanceTraveled = tag.getDouble("DistanceTraveled");
      this.entityData.set(CHARGE_TICKS, tag.getInt("ChargeTicks"));
      this.entityData.set(FLIGHT_TICKS, tag.getInt("FlightTicks"));
      this.entityData.set(ARASH_SPECIAL, !tag.contains("ArashSpecial") || tag.getBoolean("ArashSpecial"));
      this.entityData.set(BALLISTIC, tag.getBoolean("Ballistic"));
   }
}

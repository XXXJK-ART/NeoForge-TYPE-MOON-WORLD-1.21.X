package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;
import org.joml.Vector3f;

/** Fast, single-hit Royal Cannon shot with a fixed launch direction. */
public final class RoyalCannonProjectileEntity extends Entity {
   private static final EntityDataAccessor<Boolean> EXPLOSIVE_VISUAL =
      SynchedEntityData.defineId(RoyalCannonProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> VISUAL_SCALE =
      SynchedEntityData.defineId(RoyalCannonProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final DustParticleOptions GOLD =
      new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 1.05F);
   private static final DustParticleOptions PALE_GOLD =
      new DustParticleOptions(new Vector3f(1.0F, 0.92F, 0.48F), 0.78F);
   private UUID ownerUuid;
   private UUID homingTargetUuid;
   private float damage = 20.0F;
   private float damageMultiplier = 1.0F;
   private boolean explosive;
   private float explosionRadius = 0.0F;
   private final Set<Integer> hit = new HashSet<>();
   public final List<Vec3> tracePos = new ArrayList<>();

   public RoyalCannonProjectileEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public RoyalCannonProjectileEntity(Level level, LivingEntity owner, Vec3 start, Vec3 direction, float damage) {
      this(ModEntities.ROYAL_CANNON_PROJECTILE.get(), level);
      this.ownerUuid = owner.getUUID();
      this.damage = damage;
      this.setPos(start);
      this.setDeltaMovement(direction.normalize().scale(2.15));
   }

   /** Used by the non-explosive slate volley; Royal Cannon never assigns a target. */
   public void setHomingTarget(LivingEntity target) {
      this.homingTargetUuid = target == null || EntityUtils.isImmunePlayerTarget(target) ? null : target.getUUID();
   }

   public void setExplosive(float radius) {
      this.explosive = radius > 0.0F;
      this.explosionRadius = Math.max(0.0F, radius);
      this.entityData.set(EXPLOSIVE_VISUAL, this.explosive);
      this.entityData.set(VISUAL_SCALE, this.explosive ? 1.42F : 0.86F);
   }

   public void configureRoyalCannon(float radius) {
      setExplosive(radius);
   }

   public void setDamageMultiplier(float damageMultiplier) {
      this.damageMultiplier = Math.max(0.0F, damageMultiplier);
   }

   public boolean isExplosiveVisual() {
      return this.entityData.get(EXPLOSIVE_VISUAL);
   }

   public float getVisualScale() {
      return this.entityData.get(VISUAL_SCALE);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(EXPLOSIVE_VISUAL, false);
      builder.define(VISUAL_SCALE, 0.86F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) {
         if (this.level().isClientSide()) {
            recordTrace();
            this.level().addParticle(GOLD, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
         }
         return;
      }
      LivingEntity owner = getOwner(level);
      if (owner == null || !owner.isAlive() || tickCount > 80) {
         discard();
         return;
      }
      if (!explosive) {
         LivingEntity target = getTarget(level);
         if (target != null) steer(target);
      }
      Vec3 old = position();
      Vec3 next = old.add(getDeltaMovement());
      HitResult blockHit = level.clip(new ClipContext(old, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
      if (blockHit.getType() == HitResult.Type.BLOCK) {
         impact(level, owner, blockHit.getLocation());
         discard();
         return;
      }
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(old, next).inflate(0.35),
         e -> isValidVictim(owner, e) && !hit.contains(e.getId()))) {
         impact(level, owner, victim.position().add(0.0, victim.getBbHeight() * 0.45, 0.0));
         discard();
         return;
      }
      setPos(next);
      if (tickCount % 2 == 0) {
         level.sendParticles(GOLD, getX(), getY(), getZ(), explosive ? 7 : 2, 0.08, 0.08, 0.08, 0.018);
         level.sendParticles(PALE_GOLD, getX(), getY(), getZ(), explosive ? 3 : 1, 0.05, 0.05, 0.05, 0.006);
         if (explosive) {
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 2, 0.04, 0.04, 0.04, 0.012);
         }
      }
   }

   private void recordTrace() {
      Vec3 pos = position();
      if (tracePos.isEmpty() || pos.distanceToSqr(tracePos.get(tracePos.size() - 1)) >= 0.01) {
         tracePos.add(pos);
         if (tracePos.size() > 24) tracePos.remove(0);
      }
   }

   private LivingEntity getOwner(ServerLevel level) {
      Entity entity = ownerUuid == null ? null : level.getEntity(ownerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   private LivingEntity getTarget(ServerLevel level) {
      LivingEntity owner = getOwner(level);
      if (owner == null || homingTargetUuid == null) return null;
      Entity entity = level.getEntity(homingTargetUuid);
      return entity instanceof LivingEntity living && isValidVictim(owner, living) ? living : null;
   }

   private void steer(LivingEntity target) {
      Vec3 desired = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(position());
      if (desired.lengthSqr() < 1.0E-6) return;
      Vec3 current = getDeltaMovement().normalize();
      Vec3 steered = current.scale(0.88).add(desired.normalize().scale(0.12));
      setDeltaMovement(steered.normalize().scale(2.15));
   }

   private void impact(ServerLevel level, LivingEntity owner, Vec3 pos) {
      if (explosive) {
         explode(level, owner, pos);
         return;
      }

      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(0.5),
         e -> isValidVictim(owner, e) && hit.add(e.getId()))) {
         hurtVictim(owner, victim, damage);
         break;
      }
      level.sendParticles(GOLD, pos.x, pos.y, pos.z, 18, 0.2, 0.2, 0.2, 0.05);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 8, 0.14, 0.14, 0.14, 0.035);
   }

   private void explode(ServerLevel level, LivingEntity owner, Vec3 pos) {
      float radius = Math.max(0.1F, explosionRadius);
      double radiusSqr = radius * radius;
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius),
         e -> isValidVictim(owner, e) && e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).distanceToSqr(pos) <= radiusSqr)) {
         if (hit.add(victim.getId())) {
            hurtVictim(owner, victim, damage);
         }
      }

      level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.85F, 1.32F);
      level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      TerrainImpactService.impact(level, owner, pos,
         new TerrainImpactProfile(TerrainImpactProfile.Tier.SMALL, radius, 3.0F, 18, 42),
         TerrainImpactService.Shape.AIR_SPHERE);
      spawnGoldSphere(level, pos, radius);
      level.sendParticles(GOLD, pos.x, pos.y, pos.z, 118, radius * 0.38, radius * 0.38, radius * 0.38, 0.10);
      level.sendParticles(PALE_GOLD, pos.x, pos.y, pos.z, 74, radius * 0.66, radius * 0.66, radius * 0.66, 0.055);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 42, radius * 0.34, radius * 0.34, radius * 0.34, 0.052);
      level.sendParticles(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z, 14, radius * 0.35, radius * 0.35, radius * 0.35, 0.045);
      level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
   }

   private void spawnGoldSphere(ServerLevel level, Vec3 center, float radius) {
      int rings = 5;
      for (int ring = 0; ring <= rings; ring++) {
         double polar = Math.PI * ring / rings;
         double y = Math.cos(polar) * radius;
         double ringRadius = Math.sin(polar) * radius;
         int points = Math.max(8, (int)Math.round(18 * Math.sin(polar)));
         for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            double x = Math.cos(angle) * ringRadius;
            double z = Math.sin(angle) * ringRadius;
            level.sendParticles(GOLD, center.x + x, center.y + y, center.z + z, 1, 0.025, 0.025, 0.025, 0.018);
         }
      }
   }

   private void hurtVictim(LivingEntity owner, LivingEntity victim, float amount) {
      if (!isValidVictim(owner, victim)) return;
      DamageSource source = owner.damageSources().mobProjectile(this, owner);
      victim.invulnerableTime = 0;
      float finalDamage = owner instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshEntity caster
         ? CasterGilgameshCombatHelper.magicDamage(caster, amount)
         : amount * damageMultiplier;
      victim.hurt(source, finalDamage);
      victim.invulnerableTime = 0;
   }

   private static boolean isValidVictim(LivingEntity owner, LivingEntity victim) {
      return owner != null && victim != null && victim.isAlive() && victim != owner
         && !victim.isAlliedTo(owner) && !owner.isAlliedTo(victim)
         && !EntityUtils.isImmunePlayerTarget(victim)
         && !CasterGilgameshCombatHelper.isProtectedMasterTarget(owner, victim);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
      if (tag.hasUUID("HomingTarget")) homingTargetUuid = tag.getUUID("HomingTarget");
      damage = tag.contains("Damage") ? tag.getFloat("Damage") : 20.0F;
      damageMultiplier = tag.contains("DamageMultiplier") ? tag.getFloat("DamageMultiplier") : 1.0F;
      explosive = tag.getBoolean("Explosive");
      explosionRadius = tag.contains("ExplosionRadius") ? tag.getFloat("ExplosionRadius") : 0.0F;
      if (explosive) {
         this.entityData.set(EXPLOSIVE_VISUAL, true);
         this.entityData.set(VISUAL_SCALE, 1.42F);
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
      if (homingTargetUuid != null) tag.putUUID("HomingTarget", homingTargetUuid);
      tag.putFloat("Damage", damage);
      tag.putFloat("DamageMultiplier", damageMultiplier);
      tag.putBoolean("Explosive", explosive);
      tag.putFloat("ExplosionRadius", explosionRadius);
   }
}

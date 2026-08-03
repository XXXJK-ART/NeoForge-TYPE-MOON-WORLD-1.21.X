package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshCombatHelper;
import org.joml.Vector3f;

/** Fast, single-hit, lightly homing Royal Cannon shot. */
public final class RoyalCannonProjectileEntity extends Entity {
   private static final DustParticleOptions GOLD =
      new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 0.85F);
   private UUID ownerUuid;
   private UUID homingTargetUuid;
   private float damage = 20.0F;
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

   public void setHomingTarget(LivingEntity target) {
      this.homingTargetUuid = target == null ? null : target.getUUID();
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {}

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
      LivingEntity target = getTarget(level);
      if (target != null) steer(target);
      Vec3 old = position();
      Vec3 next = old.add(getDeltaMovement());
      HitResult blockHit = level.clip(new ClipContext(old, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
      if (blockHit.getType() == HitResult.Type.BLOCK) {
         impact(level, blockHit.getLocation());
         discard();
         return;
      }
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(old, next).inflate(0.35),
         e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && hit.add(e.getId()))) {
         DamageSource source = owner.damageSources().mobProjectile(this, owner);
         victim.invulnerableTime = 0;
         victim.hurt(source, CasterGilgameshCombatHelper.magicDamage(
            owner instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshEntity caster ? caster : null,
            damage));
         victim.invulnerableTime = 0;
         impact(level, victim.position());
         discard();
         return;
      }
      setPos(next);
      if (tickCount % 2 == 0) {
         level.sendParticles(GOLD, getX(), getY(), getZ(), 2, 0.04, 0.04, 0.04, 0.02);
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
      LivingEntity target = null;
      if (homingTargetUuid != null) {
         Entity entity = level.getEntity(homingTargetUuid);
         if (entity instanceof LivingEntity living && living.isAlive()) target = living;
      }
      LivingEntity owner = getOwner(level);
      if (target == null && owner != null) {
         target = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(32.0),
            e -> e.isAlive() && e != owner && !e.isAlliedTo(owner))
            .stream().min(java.util.Comparator.comparingDouble(this::distanceTo)).orElse(null);
      }
      return target;
   }

   private void steer(LivingEntity target) {
      Vec3 desired = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0)
         .subtract(position());
      if (desired.lengthSqr() < 1.0E-6) return;
      Vec3 current = getDeltaMovement().normalize();
      Vec3 steered = current.scale(0.88).add(desired.normalize().scale(0.12));
      setDeltaMovement(steered.normalize().scale(2.15));
   }

   private void impact(ServerLevel level, Vec3 pos) {
      level.sendParticles(GOLD, pos.x, pos.y, pos.z, 14, 0.18, 0.18, 0.18, 0.04);
      level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 6, 0.12, 0.12, 0.12, 0.03);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
      if (tag.hasUUID("HomingTarget")) homingTargetUuid = tag.getUUID("HomingTarget");
      damage = tag.contains("Damage") ? tag.getFloat("Damage") : 20.0F;
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
      if (homingTargetUuid != null) tag.putUUID("HomingTarget", homingTargetUuid);
      tag.putFloat("Damage", damage);
   }
}

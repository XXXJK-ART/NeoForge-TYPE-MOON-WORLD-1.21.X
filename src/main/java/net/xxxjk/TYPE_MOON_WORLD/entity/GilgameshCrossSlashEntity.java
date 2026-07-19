package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Two enormous crossing blades. Damage is sampled against the oriented slash, not an AABB. */
public class GilgameshCrossSlashEntity extends Entity implements GeoEntity {
   private static final EntityDataAccessor<Boolean> FIRE_SLASH = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   public enum SlashType { IGALIMA, SULSAGANA }
   private UUID ownerUuid;
   private SlashType slashType = SlashType.IGALIMA;
   private Vec3 direction = new Vec3(0, 0, 1);
   private final Set<Integer> hit = new HashSet<>();
   private boolean terrainDone;
   private final Set<UUID> immuneEntities = new HashSet<>();

   public GilgameshCrossSlashEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   public GilgameshCrossSlashEntity(Level level, LivingEntity owner, SlashType type, Vec3 direction) {
      this(ModEntities.GILGAMESH_CROSS_SLASH.get(), level);
      this.ownerUuid = owner.getUUID();
      this.slashType = type;
      this.direction = direction.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : direction.normalize();
      syncVisualState();
      this.setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.6, owner.getZ());
   }

   public SlashType getSlashType() { return this.entityData.get(FIRE_SLASH) ? SlashType.SULSAGANA : SlashType.IGALIMA; }
   public Vec3 getSlashDirection() { return new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z)); }
   public void addImmuneEntity(Entity entity) { if (entity != null) immuneEntities.add(entity.getUUID()); }
   public boolean isOwnedBy(Entity entity) { return entity != null && entity.getUUID().equals(ownerUuid); }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
      LivingEntity owner = getOwner(level);
      if (owner == null || !owner.isAlive() || this.tickCount > 70) { this.discard(); return; }
      this.setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.6, owner.getZ());
      int impactTick = slashType == SlashType.IGALIMA ? 24 : 32;
      if (this.tickCount == impactTick) applyDamage(level, owner);
      if (!terrainDone && this.tickCount >= impactTick) queueTerrain(level);
      if (this.tickCount % 2 == 0) {
         level.sendParticles(slashType == SlashType.IGALIMA ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.FLAME,
            this.getX(), this.getY(), this.getZ(), 80, 16, 5, 16, 0.12);
         level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2, this.getZ(), 30, 8, 7, 8, 0.05);
      }
   }

   private void applyDamage(net.minecraft.server.level.ServerLevel level, LivingEntity owner) {
      Vec3 forward = new Vec3(direction.x, 0, direction.z);
      if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0, 0, 1);
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0, forward.x);
      Vec3 origin = this.position();
      Vec3 end = origin.add(forward.scale(400.0));
      AABB box = new AABB(origin, end).inflate(110.0, 80.0, 110.0);
      DamageSource source = slashType == SlashType.SULSAGANA ? owner.damageSources().inFire() : owner.damageSources().mobAttack(owner);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
         e -> e.isAlive() && e != owner && !immuneEntities.contains(e.getUUID()) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
         double along = rel.dot(forward);
         double sign = slashType == SlashType.IGALIMA ? 1.0 : -1.0;
         double planeDistance = Math.abs(rel.dot(side) - sign * rel.y) / Math.sqrt(2.0);
         if (along < 0 || along > 400 || planeDistance > 15 || Math.abs(rel.y) > 75 || !this.hit.add(target.getId())) continue;
         target.invulnerableTime = 0;
         target.hurt(source, 1500.0F);
         target.invulnerableTime = 0;
      }
   }

   private void queueTerrain(net.minecraft.server.level.ServerLevel level) {
      terrainDone = true;
      Vec3 origin = this.position(); Vec3 castDirection = this.direction;
      boolean mirrored = slashType == SlashType.SULSAGANA;
      if (slashType == SlashType.IGALIMA) {
         DeferredTerrainDestruction.queueDiagonalCut(level, origin, castDirection, 400, 30, 150, mirrored, null);
      } else {
         DeferredTerrainDestruction.queueDiagonalCut(level, origin, castDirection, 400, 30, 150, mirrored,
            () -> DeferredTerrainDestruction.queueDiagonalBurnShell(level, origin, castDirection, 400, 30, 150, mirrored, 4));
      }
   }

   private LivingEntity getOwner(net.minecraft.server.level.ServerLevel level) {
      if (ownerUuid == null) return null;
      Entity e = level.getEntity(ownerUuid);
      return e instanceof LivingEntity living ? living : null;
   }

   private void syncVisualState() {
      this.entityData.set(FIRE_SLASH, slashType == SlashType.SULSAGANA);
      this.entityData.set(DIR_X, (float)direction.x); this.entityData.set(DIR_Y, (float)direction.y); this.entityData.set(DIR_Z, (float)direction.z);
   }
   @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      builder.define(FIRE_SLASH, false); builder.define(DIR_X, 0.0F); builder.define(DIR_Y, 0.0F); builder.define(DIR_Z, 1.0F);
   }
   @Override protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
      slashType = tag.getBoolean("Fire") ? SlashType.SULSAGANA : SlashType.IGALIMA;
      direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
      syncVisualState();
   }
   @Override protected void addAdditionalSaveData(CompoundTag tag) {
      if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
      tag.putBoolean("Fire", slashType == SlashType.SULSAGANA);
      tag.putDouble("DirX", direction.x); tag.putDouble("DirY", direction.y); tag.putDouble("DirZ", direction.z);
   }
   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

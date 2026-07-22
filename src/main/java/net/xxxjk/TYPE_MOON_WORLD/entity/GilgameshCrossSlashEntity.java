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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaSlashHandler;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A moving, diagonal blade sweep. Each slash owns its own terrain and damage trace. */
public class GilgameshCrossSlashEntity extends Entity implements GeoEntity {
   public enum SlashType { IGALIMA, SULSAGANA }
   public record SlashPair(GilgameshCrossSlashEntity igalima, GilgameshCrossSlashEntity sulsagana) { }
   public record SlashPose(Vec3 center, Vec3 bladeAxis, Vec3 faceNormal, double progress) { }

   public static final int MANIFEST_TICKS = 4;
   public static final int IMPACT_TICK = 40;
   public static final int SULSAGANA_START_TICK = 7;
   public static final int SWING_FADE_TICKS = 8;
   public static final double MODEL_LENGTH = 100.0;
   public static final double SLASH_LENGTH = 400.0;
   public static final double AFTERSHOCK_LENGTH = 0.0;
   public static final double SWEEP_RADIUS = 4.0;
   private static final int SWING_TICKS = 24;
   private static final int VISUAL_SWING_TICKS = 24;
   private static final double START_SIDE_OFFSET = 64.0;
   private static final double START_HEIGHT_OFFSET = 52.0;
   private static final EntityDataAccessor<Boolean> FIRE_SLASH = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> FRONT_DISTANCE = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> IMPACTED = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> START_DELAY = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.INT);

   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final Set<Integer> hit = new HashSet<>();
   private final Set<Integer> aftershockHit = new HashSet<>();
   private final Set<UUID> immuneEntities = new HashSet<>();
   private UUID ownerUuid;
   private UUID castUuid;
   private SlashType slashType = SlashType.IGALIMA;
   private Vec3 direction = new Vec3(0, 0, 1);
   private Vec3 attackOrigin = Vec3.ZERO;
   private int startDelay;
   private boolean impactPlayed;
   private boolean terrainTraceQueued;

   public GilgameshCrossSlashEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   private GilgameshCrossSlashEntity(Level level, LivingEntity owner, SlashType type, Vec3 direction,
                                     UUID castUuid, Vec3 attackOrigin, int startDelay) {
      this(ModEntities.GILGAMESH_CROSS_SLASH.get(), level);
      this.ownerUuid = owner.getUUID();
      this.castUuid = castUuid;
      this.slashType = type;
      this.direction = normalizedFlat(direction);
      this.attackOrigin = attackOrigin;
      this.startDelay = startDelay;
      syncVisualState();
      this.setPos(getVisualCenter(0.0));
   }

   public static SlashPair spawnPair(ServerLevel level, LivingEntity owner, Vec3 direction, Entity... immune) {
      Vec3 forward = normalizedFlat(direction);
      double releaseY = Math.max(level.getMinBuildHeight() + 1.0, owner.getY() - 30.0);
      Vec3 origin = new Vec3(owner.getX(), releaseY + owner.getBbHeight() * 0.55, owner.getZ()).subtract(forward.scale(12.0));
      UUID castId = UUID.randomUUID();
      GilgameshCrossSlashEntity igalima = new GilgameshCrossSlashEntity(level, owner, SlashType.IGALIMA, forward, castId, origin, 0);
      GilgameshCrossSlashEntity sulsagana = new GilgameshCrossSlashEntity(level, owner, SlashType.SULSAGANA, forward, castId, origin, SULSAGANA_START_TICK);
      if (immune != null) for (Entity entity : immune) { igalima.addImmuneEntity(entity); sulsagana.addImmuneEntity(entity); }
      level.addFreshEntity(igalima);
      level.addFreshEntity(sulsagana);
      VFXServerEffects.spawnOriented(level, "gilgamesh_cross_slash", origin, forward, SLASH_LENGTH + AFTERSHOCK_LENGTH + 64.0);
      return new SlashPair(igalima, sulsagana);
   }

   public SlashType getSlashType() { return this.entityData.get(FIRE_SLASH) ? SlashType.SULSAGANA : SlashType.IGALIMA; }
   public Vec3 getSlashDirection() { return new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z)); }
   public float getFrontDistance() { return this.entityData.get(FRONT_DISTANCE); }
   public boolean hasImpacted() { return this.entityData.get(IMPACTED); }
   public UUID getCastUuid() { return castUuid; }
   public void addImmuneEntity(Entity entity) { if (entity != null) immuneEntities.add(entity.getUUID()); }
   public boolean isOwnedBy(Entity entity) { return entity != null && entity.getUUID().equals(ownerUuid); }

   public SlashPose getPose(double age) {
      double activeAge = age - getStartDelay();
      double progress = Mth.clamp(activeAge / (double)SWING_TICKS, 0.0, 1.0);
      double eased = progress * progress * (3.0 - 2.0 * progress);
      Vec3 forward = normalizedFlat(getSlashDirection());
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      double sign = this.getSlashType() == SlashType.IGALIMA ? 1.0 : -1.0;
      Vec3 path = forward.scale(2.0).add(right.scale(sign)).add(0.0, -0.75, 0.0).normalize();
      Vec3 start = attackOrigin.add(right.scale(-sign * START_SIDE_OFFSET)).add(0.0, START_HEIGHT_OFFSET, 0.0);
      Vec3 center = start.add(path.scale(eased * SLASH_LENGTH));
      Vec3 screenAxis = right.scale(sign).add(0.0, -1.0, 0.0).normalize();
      double swing = (this.getSlashType() == SlashType.IGALIMA ? 1.0 : -1.0) * Math.sin(progress * Math.PI) * 32.0;
      double radians = Math.toRadians(swing);
      screenAxis = screenAxis.scale(Math.cos(radians)).add(forward.cross(screenAxis).scale(Math.sin(radians))).normalize();
      double forwardTilt = Math.toRadians(18.0 + Math.sin(progress * Math.PI) * 14.0);
      Vec3 axis = screenAxis.scale(Math.cos(forwardTilt)).add(forward.scale(Math.sin(forwardTilt))).normalize();
      Vec3 faceNormal = forward.subtract(axis.scale(forward.dot(axis))).normalize();
      return new SlashPose(center, axis, faceNormal, progress);
   }

   private SlashPose getAftershockPose(double distance) {
      SlashPose end = getPose(getStartDelay() + SWING_TICKS);
      Vec3 forward = normalizedFlat(getSlashDirection());
      return new SlashPose(end.center().add(forward.scale(Mth.clamp(distance, 0.0, AFTERSHOCK_LENGTH))),
         end.bladeAxis(), end.faceNormal(), 1.0);
   }

   private Vec3 getVisualCenter(double age) {
      Vec3 forward = normalizedFlat(getSlashDirection());
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      double sign = this.getSlashType() == SlashType.IGALIMA ? 1.0 : -1.0;
      double progress = getVisualProgress(age);
      return attackOrigin.add(forward.scale(16.0)).add(right.scale(sign * 10.0)).add(0.0, 65.0 * (1.0 - progress), 0.0);
   }

   private double getVisualProgress(double age) {
      double progress = Mth.clamp((age - getStartDelay()) / VISUAL_SWING_TICKS, 0.0, 1.0);
      return progress * progress * (3.0 - 2.0 * progress);
   }

   public float getModelRollDegrees(float partialTick) {
      double progress = getVisualProgress(this.tickCount + partialTick);
      return this.getSlashType() == SlashType.IGALIMA
         ? (float)(55.0 - 110.0 * progress)
         : (float)(-55.0 + 110.0 * progress);
   }

   public float getModelPitchDegrees(float partialTick) {
      double progress = getVisualProgress(this.tickCount + partialTick);
      return (float)(-12.0 + 84.0 * progress);
   }

   public float getModelAlpha(float partialTick) {
      double age = this.tickCount + partialTick - getStartDelay();
      if (age < 0.0) return 0.0F;
      if (age < MANIFEST_TICKS) return Mth.clamp((float)(age / MANIFEST_TICKS), 0.0F, 1.0F);
      if (age > VISUAL_SWING_TICKS) return 0.0F;
      return 1.0F;
   }

   public boolean shouldRenderBladeModel(float partialTick) {
      double age = this.tickCount + partialTick - getStartDelay();
      return age >= 0.0 && age <= VISUAL_SWING_TICKS;
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) return;
      LivingEntity owner = getOwner(level);
      if (owner == null || !owner.isAlive() || owner.level() != level) { this.discard(); return; }
      this.setPos(getVisualCenter(this.tickCount));
      double activeAge = this.tickCount - startDelay;
      if (activeAge >= 0.0) {
         if (!terrainTraceQueued) {
            queueTerrainTrace(level, owner);
            terrainTraceQueued = true;
         }
         this.entityData.set(FRONT_DISTANCE, (float)(getVisualProgress(this.tickCount) * SLASH_LENGTH));
         if (activeAge >= VISUAL_SWING_TICKS && !impactPlayed) {
            impactPlayed = true;
            this.entityData.set(IMPACTED, true);
            spawnImpact(level, getVisualCenter(this.tickCount));
         }
      }
      if (activeAge > VISUAL_SWING_TICKS + 4) this.discard();
   }

   private void queueTerrainTrace(ServerLevel level, LivingEntity owner) {
      MuramasaSlashHandler.initiateGilgameshCross(level, owner, getSlashDirection(), this.slashType == SlashType.SULSAGANA);
   }

   private void applyDamage(ServerLevel level, LivingEntity owner, SlashPose previous, SlashPose current, Set<Integer> hitTargets) {
      Vec3 p0 = previous.center().subtract(previous.bladeAxis().scale(MODEL_LENGTH * 0.5));
      Vec3 p1 = previous.center().add(previous.bladeAxis().scale(MODEL_LENGTH * 0.5));
      Vec3 c0 = current.center().subtract(current.bladeAxis().scale(MODEL_LENGTH * 0.5));
      Vec3 c1 = current.center().add(current.bladeAxis().scale(MODEL_LENGTH * 0.5));
      AABB search = new AABB(p0, p1).minmax(new AABB(c0, c1)).inflate(SWEEP_RADIUS + 3.0);
      DamageSource source = this.slashType == SlashType.SULSAGANA ? owner.damageSources().inFire() : owner.damageSources().mobAttack(owner);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
         e -> e.isAlive() && !e.getUUID().equals(this.ownerUuid) && !this.immuneEntities.contains(e.getUUID()) && !EntityUtils.isImmunePlayerTarget(e))) {
         if (hitTargets.contains(target.getId())) continue;
         Vec3 point = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         double distance = Math.min(Math.min(distanceToSegmentSqr(point, p0, p1), distanceToSegmentSqr(point, c0, c1)),
            distanceToSegmentSqr(point, p0, c1));
         double reach = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5 + SWEEP_RADIUS;
         if (distance > reach * reach) continue;
         hitTargets.add(target.getId());
         target.invulnerableTime = 0;
         target.hurt(source, 1500.0F);
         target.invulnerableTime = 0;
      }
   }

   private void spawnImpact(ServerLevel level, Vec3 point) {
      level.sendParticles(this.slashType == SlashType.SULSAGANA ? ParticleTypes.FLAME : ParticleTypes.END_ROD,
         point.x, point.y, point.z, 45, 4.0, 4.0, 4.0, 0.08);
      level.playSound(null, BlockPos.containing(point), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.8F);
   }

   private void spawnTrailParticles(ServerLevel level, SlashPose pose) {
      Vec3 start = pose.center().subtract(pose.bladeAxis().scale(MODEL_LENGTH * 0.5));
      Vec3 end = pose.center().add(pose.bladeAxis().scale(MODEL_LENGTH * 0.5));
      for (int i = 0; i <= 12; i++) {
         Vec3 point = start.lerp(end, i / 12.0);
         level.sendParticles(this.slashType == SlashType.SULSAGANA ? ParticleTypes.FLAME : ParticleTypes.END_ROD,
            point.x, point.y, point.z, 2, 0.7, 0.7, 0.7, 0.025);
      }
   }

   private void spawnAftershockParticles(ServerLevel level, SlashPose pose) {
      Vec3 start = pose.center().subtract(pose.bladeAxis().scale(MODEL_LENGTH * 0.5));
      Vec3 end = pose.center().add(pose.bladeAxis().scale(MODEL_LENGTH * 0.5));
      for (int i = 0; i <= 16; i++) {
         Vec3 point = start.lerp(end, i / 16.0);
         level.sendParticles(this.slashType == SlashType.SULSAGANA ? ParticleTypes.LAVA : ParticleTypes.ELECTRIC_SPARK,
            point.x, point.y, point.z, 2, 0.45, 0.45, 0.45, 0.08);
      }
   }

   private LivingEntity getOwner(ServerLevel level) {
      if (this.ownerUuid == null) return null;
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
      Vec3 delta = end.subtract(start);
      double lengthSqr = delta.lengthSqr();
      if (lengthSqr < 1.0E-8) return point.distanceToSqr(start);
      double t = Mth.clamp(point.subtract(start).dot(delta) / lengthSqr, 0.0, 1.0);
      return point.distanceToSqr(start.add(delta.scale(t)));
   }

   private static Vec3 normalizedFlat(Vec3 value) {
      Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
      return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
   }

   private void syncVisualState() {
      this.entityData.set(FIRE_SLASH, this.slashType == SlashType.SULSAGANA);
      this.entityData.set(DIR_X, (float)this.direction.x);
      this.entityData.set(DIR_Y, (float)this.direction.y);
      this.entityData.set(DIR_Z, (float)this.direction.z);
      this.entityData.set(FRONT_DISTANCE, 0.0F);
      this.entityData.set(IMPACTED, false);
      this.entityData.set(START_DELAY, this.startDelay);
   }

   private int getStartDelay() { return this.entityData.get(START_DELAY); }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(FIRE_SLASH, false); builder.define(DIR_X, 0.0F); builder.define(DIR_Y, 0.0F); builder.define(DIR_Z, 1.0F);
      builder.define(FRONT_DISTANCE, 0.0F); builder.define(IMPACTED, false); builder.define(START_DELAY, 0);
   }

   @Override protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) this.ownerUuid = tag.getUUID("Owner");
      if (tag.hasUUID("Cast")) this.castUuid = tag.getUUID("Cast");
      this.slashType = tag.getBoolean("Fire") ? SlashType.SULSAGANA : SlashType.IGALIMA;
      this.direction = normalizedFlat(new Vec3(tag.getDouble("DirX"), 0.0, tag.getDouble("DirZ")));
      this.attackOrigin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
      this.startDelay = tag.getInt("StartDelay");
      this.tickCount = tag.getInt("LifeTicks");
      this.impactPlayed = tag.getBoolean("ImpactPlayed");
      this.terrainTraceQueued = false;
      this.immuneEntities.clear();
      int immuneCount = tag.getInt("ImmuneCount");
      for (int i = 0; i < immuneCount; i++) {
         String key = "Immune" + i;
         if (tag.hasUUID(key)) this.immuneEntities.add(tag.getUUID(key));
      }
      syncVisualState();
      this.entityData.set(IMPACTED, this.impactPlayed);
      this.setPos(getVisualCenter(this.tickCount));
   }

   @Override protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
      if (this.castUuid != null) tag.putUUID("Cast", this.castUuid);
      tag.putBoolean("Fire", this.slashType == SlashType.SULSAGANA);
      tag.putDouble("DirX", this.direction.x); tag.putDouble("DirZ", this.direction.z);
      tag.putDouble("OriginX", this.attackOrigin.x); tag.putDouble("OriginY", this.attackOrigin.y); tag.putDouble("OriginZ", this.attackOrigin.z);
      tag.putInt("StartDelay", this.startDelay); tag.putInt("LifeTicks", this.tickCount);
      tag.putBoolean("ImpactPlayed", this.impactPlayed);
      tag.putInt("ImmuneCount", this.immuneEntities.size());
      int immuneIndex = 0;
      for (UUID immune : this.immuneEntities) tag.putUUID("Immune" + immuneIndex++, immune);
   }

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}

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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A fixed giant blade manifestation followed by one half of the advancing X cut. */
public class GilgameshCrossSlashEntity extends Entity implements GeoEntity {
   public enum SlashType { IGALIMA, SULSAGANA }
   public record SlashPair(GilgameshCrossSlashEntity igalima, GilgameshCrossSlashEntity sulsagana) { }

   public static final int MANIFEST_TICKS = 12;
   public static final int IMPACT_TICK = 52;
   public static final int SULSAGANA_START_TICK = 60;
   public static final int FRONTS_MEET_TICK = 68;
   public static final int MODEL_HOLD_TICKS = 60;
   public static final int MODEL_FADE_TICKS = 10;
   public static final double SLASH_LENGTH = 400.0;
   public static final double SLASH_HEIGHT = 150.0;
   public static final double SLASH_WIDTH = 30.0;
   private static final double DESCENT_HEIGHT = 40.0;
   private static final int CONTROLLER_END_TICK = IMPACT_TICK + (int)(SLASH_LENGTH / 2.0) + 20;

   private static final EntityDataAccessor<Boolean> FIRE_SLASH = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> FRONT_DISTANCE = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> IMPACTED = SynchedEntityData.defineId(GilgameshCrossSlashEntity.class, EntityDataSerializers.BOOLEAN);

   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final Set<Integer> hit = new HashSet<>();
   private final Set<UUID> immuneEntities = new HashSet<>();
   private UUID ownerUuid;
   private UUID castUuid;
   private SlashType slashType = SlashType.IGALIMA;
   private Vec3 direction = new Vec3(0, 0, 1);
   private Vec3 attackOrigin = Vec3.ZERO;
   private Vec3 groundPosition = Vec3.ZERO;
   private double processedDamageDistance;
   private boolean terrainSealed;
   private DeferredTerrainDestruction.AdvancingDiagonalCut terrainCut;

   public GilgameshCrossSlashEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.setNoGravity(true);
   }

   private GilgameshCrossSlashEntity(Level level, LivingEntity owner, SlashType type, Vec3 direction,
                                     UUID castUuid, Vec3 attackOrigin, Vec3 groundPosition) {
      this(ModEntities.GILGAMESH_CROSS_SLASH.get(), level);
      this.ownerUuid = owner.getUUID();
      this.castUuid = castUuid;
      this.slashType = type;
      this.direction = normalizedFlat(direction);
      this.attackOrigin = attackOrigin;
      this.groundPosition = groundPosition;
      syncVisualState();
      this.setPos(groundPosition.add(0.0, DESCENT_HEIGHT, 0.0));
   }

   public static SlashPair spawnPair(ServerLevel level, LivingEntity owner, Vec3 direction, Entity... immune) {
      Vec3 forward = normalizedFlat(direction);
      Vec3 horizontalCenter = owner.position().add(forward.scale(10.0));
      int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
         Mth.floor(horizontalCenter.x), Mth.floor(horizontalCenter.z));
      if (groundY <= level.getMinBuildHeight() || Math.abs(groundY - owner.getY()) > 80.0) {
         groundY = Mth.floor(owner.getY());
      }
      Vec3 ground = new Vec3(horizontalCenter.x, groundY + 0.05, horizontalCenter.z);
      Vec3 origin = new Vec3(horizontalCenter.x, owner.getY() + owner.getBbHeight() * 0.55, horizontalCenter.z);
      UUID castId = UUID.randomUUID();
      GilgameshCrossSlashEntity igalima = new GilgameshCrossSlashEntity(level, owner, SlashType.IGALIMA, forward, castId, origin, ground);
      GilgameshCrossSlashEntity sulsagana = new GilgameshCrossSlashEntity(level, owner, SlashType.SULSAGANA, forward, castId, origin, ground);
      if (immune != null) {
         for (Entity entity : immune) {
            igalima.addImmuneEntity(entity);
            sulsagana.addImmuneEntity(entity);
         }
      }
      level.addFreshEntity(igalima);
      level.addFreshEntity(sulsagana);
      VFXServerEffects.spawnOriented(level, "gilgamesh_cross_slash", ground, forward, 512.0);
      return new SlashPair(igalima, sulsagana);
   }

   public SlashType getSlashType() { return this.entityData.get(FIRE_SLASH) ? SlashType.SULSAGANA : SlashType.IGALIMA; }
   public Vec3 getSlashDirection() { return new Vec3(this.entityData.get(DIR_X), this.entityData.get(DIR_Y), this.entityData.get(DIR_Z)); }
   public float getFrontDistance() { return this.entityData.get(FRONT_DISTANCE); }
   public boolean hasImpacted() { return this.entityData.get(IMPACTED); }
   public UUID getCastUuid() { return castUuid; }
   public void addImmuneEntity(Entity entity) { if (entity != null) immuneEntities.add(entity.getUUID()); }
   public boolean isOwnedBy(Entity entity) { return entity != null && entity.getUUID().equals(ownerUuid); }

   public float getModelAlpha(float partialTick) {
      float age = this.tickCount + partialTick;
      if (age < MANIFEST_TICKS) return Mth.clamp(age / MANIFEST_TICKS, 0.0F, 1.0F);
      float fadeStart = IMPACT_TICK + MODEL_HOLD_TICKS;
      if (age <= fadeStart) return 1.0F;
      return 1.0F - Mth.clamp((age - fadeStart) / MODEL_FADE_TICKS, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) return;
      LivingEntity owner = getOwner(level);
      if (this.tickCount < IMPACT_TICK && (owner == null || !owner.isAlive() || owner.level() != level)) {
         this.discard();
         return;
      }

      updateManifestPosition();
      if (this.tickCount == IMPACT_TICK) {
         this.entityData.set(IMPACTED, true);
         if (this.slashType == SlashType.IGALIMA) spawnGroundImpact(level);
      }

      double nextFront = calculateFrontDistance(this.tickCount);
      if (nextFront > 0.0 && this.terrainCut == null) {
         ensureTerrainCut(level);
         this.terrainCut.advanceTo(nextFront);
      }
      if (nextFront > this.processedDamageDistance) {
         applyDamage(level, owner, this.processedDamageDistance, nextFront);
         this.processedDamageDistance = nextFront;
         this.entityData.set(FRONT_DISTANCE, (float)nextFront);
         this.terrainCut.advanceTo(nextFront);
         spawnWaveFront(level, nextFront);
      }
      if (nextFront >= SLASH_LENGTH && !this.terrainSealed) {
         this.terrainSealed = true;
         if (this.terrainCut != null) this.terrainCut.seal();
      }
      if (this.hasImpacted() && this.tickCount <= IMPACT_TICK + MODEL_HOLD_TICKS && this.tickCount % 5 == 0) {
         spawnEmbeddedParticles(level);
      }
      if (this.tickCount > CONTROLLER_END_TICK) this.discard();
   }

   private void updateManifestPosition() {
      if (this.tickCount < MANIFEST_TICKS) {
         this.setPos(this.groundPosition.add(0.0, DESCENT_HEIGHT, 0.0));
         return;
      }
      if (this.tickCount < IMPACT_TICK) {
         double progress = Mth.clamp((this.tickCount - MANIFEST_TICKS) / (double)(IMPACT_TICK - MANIFEST_TICKS), 0.0, 1.0);
         double remaining = DESCENT_HEIGHT * (1.0 - progress * progress);
         this.setPos(this.groundPosition.add(0.0, remaining, 0.0));
         return;
      }
      this.setPos(this.groundPosition);
   }

   private double calculateFrontDistance(int age) {
      if (this.slashType == SlashType.IGALIMA) {
         return Mth.clamp((age - IMPACT_TICK) * 2.0, 0.0, SLASH_LENGTH);
      }
      if (age <= SULSAGANA_START_TICK) return 0.0;
      int elapsed = age - SULSAGANA_START_TICK;
      int catchUpTicks = FRONTS_MEET_TICK - SULSAGANA_START_TICK;
      return Mth.clamp(elapsed <= catchUpTicks ? elapsed * 4.0 : 32.0 + (elapsed - catchUpTicks) * 2.0, 0.0, SLASH_LENGTH);
   }

   private void ensureTerrainCut(ServerLevel level) {
      if (this.terrainCut != null) return;
      boolean mirrored = this.slashType == SlashType.SULSAGANA;
      Runnable completion = mirrored
         ? () -> DeferredTerrainDestruction.queueCrossBurnShell(level, this.attackOrigin, this.direction,
            SLASH_LENGTH, SLASH_WIDTH, SLASH_HEIGHT, true, 4.0)
         : null;
      this.terrainCut = DeferredTerrainDestruction.queueAdvancingDiagonalCut(level, this.attackOrigin, this.direction,
         SLASH_LENGTH, SLASH_WIDTH, SLASH_HEIGHT, mirrored, completion);
   }

   private void applyDamage(ServerLevel level, LivingEntity owner, double previousFront, double currentFront) {
      Vec3 forward = normalizedFlat(this.direction);
      Vec3 side = new Vec3(-forward.z, 0, forward.x);
      Vec3 from = this.attackOrigin.add(forward.scale(previousFront));
      Vec3 to = this.attackOrigin.add(forward.scale(currentFront));
      AABB search = new AABB(from, to).inflate(110.0, 80.0, 110.0);
      DamageSource source = owner == null
         ? level.damageSources().magic()
         : this.slashType == SlashType.SULSAGANA ? owner.damageSources().inFire() : owner.damageSources().mobAttack(owner);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
         e -> e.isAlive() && !e.getUUID().equals(this.ownerUuid) && !this.immuneEntities.contains(e.getUUID()) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 rel = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(this.attackOrigin);
         double along = rel.dot(forward);
         double reach = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5;
         if (along + reach < previousFront || along - reach > currentFront) continue;
         double sign = this.slashType == SlashType.IGALIMA ? 1.0 : -1.0;
         double planeDistance = Math.abs(rel.dot(side) - sign * rel.y) / Math.sqrt(2.0);
         if (planeDistance > SLASH_WIDTH * 0.5 + reach || Math.abs(rel.y) > SLASH_HEIGHT * 0.5 + reach || !this.hit.add(target.getId())) continue;
         target.invulnerableTime = 0;
         target.hurt(source, 1500.0F);
         target.invulnerableTime = 0;
      }
   }

   private void spawnGroundImpact(ServerLevel level) {
      Vec3 point = this.groundPosition.add(0.0, 0.5, 0.0);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, point.x, point.y, point.z, 8, 4.0, 1.0, 4.0, 0.0);
      level.sendParticles(ParticleTypes.POOF, point.x, point.y, point.z, 240, 18.0, 2.5, 18.0, 0.2);
      level.sendParticles(ParticleTypes.END_ROD, point.x, point.y + 2.0, point.z, 160, 12.0, 10.0, 12.0, 0.12);
      level.sendParticles(ParticleTypes.FLAME, point.x, point.y + 0.5, point.z, 120, 10.0, 1.0, 10.0, 0.08);
      level.playSound(null, BlockPos.containing(point), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 5.0F, 0.55F);
      level.playSound(null, BlockPos.containing(point), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 4.0F, 0.45F);
   }

   private void spawnEmbeddedParticles(ServerLevel level) {
      boolean fire = this.slashType == SlashType.SULSAGANA;
      level.sendParticles(fire ? ParticleTypes.FLAME : ParticleTypes.HAPPY_VILLAGER,
         this.getX(), this.getY() + 4.0, this.getZ(), 24, 6.0, 8.0, 6.0, 0.04);
      level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 3.0, this.getZ(), 10, 4.0, 5.0, 4.0, 0.02);
   }

   private void spawnWaveFront(ServerLevel level, double distance) {
      if (this.tickCount % 2 != 0) return;
      Vec3 forward = normalizedFlat(this.direction);
      Vec3 side = new Vec3(-forward.z, 0, forward.x);
      Vec3 center = this.attackOrigin.add(forward.scale(distance));
      double sign = this.slashType == SlashType.IGALIMA ? 1.0 : -1.0;
      for (int vertical = -75; vertical <= 75; vertical += 6) {
         Vec3 point = center.add(side.scale(sign * vertical)).add(0.0, vertical, 0.0);
         level.sendParticles(this.slashType == SlashType.IGALIMA ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.FLAME,
            point.x, point.y, point.z, 2, 1.2, 1.2, 1.2, 0.03);
         if (vertical % 18 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.5, 0.5, 0.5, 0.01);
         }
      }
   }

   private LivingEntity getOwner(ServerLevel level) {
      if (this.ownerUuid == null) return null;
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   private static Vec3 normalizedFlat(Vec3 direction) {
      Vec3 flat = direction == null ? Vec3.ZERO : new Vec3(direction.x, 0.0, direction.z);
      return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
   }

   private void syncVisualState() {
      this.entityData.set(FIRE_SLASH, this.slashType == SlashType.SULSAGANA);
      this.entityData.set(DIR_X, (float)this.direction.x);
      this.entityData.set(DIR_Y, (float)this.direction.y);
      this.entityData.set(DIR_Z, (float)this.direction.z);
      this.entityData.set(FRONT_DISTANCE, (float)this.processedDamageDistance);
      this.entityData.set(IMPACTED, this.tickCount >= IMPACT_TICK);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(FIRE_SLASH, false);
      builder.define(DIR_X, 0.0F);
      builder.define(DIR_Y, 0.0F);
      builder.define(DIR_Z, 1.0F);
      builder.define(FRONT_DISTANCE, 0.0F);
      builder.define(IMPACTED, false);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) this.ownerUuid = tag.getUUID("Owner");
      if (tag.hasUUID("Cast")) this.castUuid = tag.getUUID("Cast");
      this.slashType = tag.getBoolean("Fire") ? SlashType.SULSAGANA : SlashType.IGALIMA;
      this.direction = normalizedFlat(new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ")));
      this.attackOrigin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
      this.groundPosition = new Vec3(tag.getDouble("GroundX"), tag.getDouble("GroundY"), tag.getDouble("GroundZ"));
      this.tickCount = tag.getInt("LifeTicks");
      this.processedDamageDistance = tag.getDouble("ProcessedDistance");
      this.terrainSealed = false;
      this.immuneEntities.clear();
      int immuneCount = tag.getInt("ImmuneCount");
      for (int i = 0; i < immuneCount; i++) {
         String key = "Immune" + i;
         if (tag.hasUUID(key)) this.immuneEntities.add(tag.getUUID(key));
      }
      syncVisualState();
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerUuid != null) tag.putUUID("Owner", this.ownerUuid);
      if (this.castUuid != null) tag.putUUID("Cast", this.castUuid);
      tag.putBoolean("Fire", this.slashType == SlashType.SULSAGANA);
      tag.putDouble("DirX", this.direction.x); tag.putDouble("DirY", this.direction.y); tag.putDouble("DirZ", this.direction.z);
      tag.putDouble("OriginX", this.attackOrigin.x); tag.putDouble("OriginY", this.attackOrigin.y); tag.putDouble("OriginZ", this.attackOrigin.z);
      tag.putDouble("GroundX", this.groundPosition.x); tag.putDouble("GroundY", this.groundPosition.y); tag.putDouble("GroundZ", this.groundPosition.z);
      tag.putInt("LifeTicks", this.tickCount);
      tag.putDouble("ProcessedDistance", this.processedDamageDistance);
      tag.putInt("ImmuneCount", this.immuneEntities.size());
      int immuneIndex = 0;
      for (UUID immune : this.immuneEntities) tag.putUUID("Immune" + immuneIndex++, immune);
   }

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}

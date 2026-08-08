package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamClashManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamClashParticipant;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.BeamType;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshDuelState;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Invisible server controller for EA wind, orb charge, steerable beam and impact. */
public class GilgameshEaBeamEntity extends Entity implements BeamClashParticipant, GeoEntity {
   public enum Stage { WIND, ORB_CHARGE, BEAM, IMPACT, FINISHED }

   private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(GilgameshEaBeamEntity.class, EntityDataSerializers.FLOAT);
   public static final int WIND_TICKS = 40;
   public static final int ORB_CHARGE_TICKS = 100;
   public static final int BEAM_TICKS = 150;
   private static final int BEAM_DAMAGE_START = 58;
   private static final int BEAM_DAMAGE_INTERVAL = 5;
   private static final int BEAM_DAMAGE_PULSES = 18;
   private static final int EA_THUNDER_TICKS = 45 * 20;
   private static final double WIND_RADIUS = 50.0;
   private static final double BEAM_LENGTH = 150.0;
   private static final double BEAM_HALF_WIDTH = 12.0;
   private static final double BEAM_HALF_HEIGHT = 8.0;
   private static final double BEAM_SWEEP_STEP_RADIANS = Math.toRadians(2.0);
   private UUID ownerUuid;
   private UUID trackedTargetUuid;
   private Vec3 direction = new Vec3(0, 0, 1);
   private double windOriginY;
   private boolean releaseRequested;
   private boolean windOnly;
   private boolean beamStarted;
   private boolean impactQueued;
   private boolean clashing;
   private boolean autoReleaseAtFull;
   private boolean boundaryBroken;
   private boolean beamVisualStarted;
   private boolean duelFinale;
   private int stageTicks;
   private float clashDamageScale = 1.0F;
   private Vec3 lastDamageDirection;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public GilgameshEaBeamEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public GilgameshEaBeamEntity(Level level, LivingEntity owner, Vec3 initialDirection) {
      this(level, owner, initialDirection, null);
   }

   public GilgameshEaBeamEntity(Level level, LivingEntity owner, Vec3 initialDirection, LivingEntity trackedTarget) {
      this(ModEntities.GILGAMESH_EA_BEAM.get(), level);
      this.ownerUuid = owner.getUUID();
      this.trackedTargetUuid = trackedTarget == null ? null : trackedTarget.getUUID();
      this.duelFinale = trackedTarget != null && GilgameshDuelState.areDuelPartners(owner, trackedTarget);
      this.entityData.set(OWNER_ID, owner.getId());
      this.direction = normalized(initialDirection);
      this.windOriginY = owner.getY();
      this.autoReleaseAtFull = !(owner instanceof net.minecraft.server.level.ServerPlayer);
      this.setPos(owner.position().add(0, owner.getBbHeight() * 0.65, 0));
      this.updateEnd();
   }

   public Stage getStage() { return Stage.values()[Math.max(0, Math.min(Stage.values().length - 1, this.entityData.get(STAGE)))]; }
   public int getOwnerEntityId() { return this.entityData.get(OWNER_ID); }
   public float getPowerScale() { return this.entityData.get(POWER); }
   public Vec3 getEndPos() { return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z)); }
   public boolean isOwnedBy(Entity entity) { return entity != null && (entity.getId() == getOwnerEntityId() || entity.getUUID().equals(ownerUuid)); }
   public boolean shouldAnimateEa() { return getStage() != Stage.FINISHED && this.isAlive(); }

   public static boolean isEaActiveFor(LivingEntity living) {
      return !living.level().getEntitiesOfClass(GilgameshEaBeamEntity.class, living.getBoundingBox().inflate(3.0),
         e -> e.isAlive() && e.getOwnerEntityId() == living.getId() && e.shouldAnimateEa()).isEmpty();
   }

   public void requestRelease(int heldTicks) {
      this.releaseRequested = true;
      if (heldTicks < WIND_TICKS) this.windOnly = true;
   }

   @Override public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) return;
      LivingEntity owner = getOwner(level);
      if (owner == null) { setStage(Stage.FINISHED); discard(); return; }
      if (!owner.isAlive()) {
         if (owner instanceof GilgameshEntity) stopNpcEaVoice(level, owner);
         clearNpcEquipment(owner);
         setStage(Stage.FINISHED);
         discard();
         return;
      }
      if (owner instanceof GilgameshEntity && getStage() == Stage.WIND && !isTrackedTargetAlive(level)) {
         cancelNpcEa(level, owner);
         return;
      }
      this.setPos(owner.position().add(0, owner.getBbHeight() * 0.65, 0));
      if (getStage() != Stage.BEAM) owner.setDeltaMovement(Vec3.ZERO);
      owner.getPersistentData().putLong("GilgameshEaProtectedUntil", level.getGameTime() + 2);
      this.stageTicks++;

      switch (getStage()) {
         case WIND -> tickWind(level, owner);
         case ORB_CHARGE -> tickOrbCharge(level, owner);
         case BEAM -> tickBeam(level, owner);
         case IMPACT -> finishImpact(level, owner);
         case FINISHED -> discard();
      }
   }

   private void tickWind(ServerLevel level, LivingEntity owner) {
      if (!drainMana(owner, 2.5)) {
         clearNpcEquipment(owner); setStage(Stage.FINISHED); discard(); return;
      }
      if (stageTicks == 1) {
         level.setWeatherParameters(0, EA_THUNDER_TICKS, true, true);
         VFXServerEffects.spawn(level, "ea_wind", owner, 192.0);
         if (owner instanceof GilgameshEntity && !isDuelTarget(level, owner)) {
            level.playSound(null, owner.blockPosition(), ModSounds.GILGAMESH_VOICE_EA_NPC.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
         } else if (canPlayEaVoice(owner)) {
            level.playSound(null, owner.blockPosition(), ModSounds.GILGAMESH_VOICE_EA_DRAW.get(), SoundSource.HOSTILE, 2.0F, 0.9F);
         }
         DeferredTerrainDestruction.queueUpperHemisphere(level, owner.position(), WIND_RADIUS, this.windOriginY, 80.0F);
      }
      if (stageTicks % 4 == 0) applyWindDamage(level, owner);
      if (owner instanceof GilgameshEntity && !isTrackedTargetAlive(level)) {
         cancelNpcEa(level, owner);
         return;
      }
      if (stageTicks >= WIND_TICKS) {
         if (windOnly) { applyCooldown(owner, 600); setStage(Stage.FINISHED); discard(); return; }
         setStage(Stage.ORB_CHARGE);
         VFXServerEffects.spawn(level, "ea_charge", owner, 192.0);
         if (canPlayEaVoice(owner)) level.playSound(null, owner.blockPosition(), ModSounds.GILGAMESH_VOICE_EA.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
      }
   }

   private void tickOrbCharge(ServerLevel level, LivingEntity owner) {
      int charged = Math.min(ORB_CHARGE_TICKS, stageTicks);
      float scale = Math.max(0.35F, charged / (float)ORB_CHARGE_TICKS);
      this.entityData.set(POWER, scale);
      if (charged < ORB_CHARGE_TICKS && !releaseRequested && !drainMana(owner, 1.5)) releaseRequested = true;
      LivingEntity trackedTarget = getTrackedTarget(level);
      boolean duelFinale = trackedTarget != null && GilgameshDuelState.areDuelPartners(owner, trackedTarget);
      boolean synchronizedRush = duelFinale && GilgameshDuelState.isEnkiduRushStarted(owner, trackedTarget);
      if (releaseRequested || synchronizedRush || autoReleaseAtFull && charged >= ORB_CHARGE_TICKS && !duelFinale) {
         startBeam(level, owner, synchronizedRush ? 1.0F : scale);
      }
   }

   /** Forces the duel beam onto the exact server tick that Enkidu begins rushing. */
   public void synchronizeDuelRelease(ServerLevel level) {
      if (level == null || getStage() != Stage.ORB_CHARGE) return;
      LivingEntity owner = getOwner(level);
      LivingEntity target = getTrackedTarget(level);
      if (owner != null && target != null && GilgameshDuelState.areDuelPartners(owner, target)) {
         startBeam(level, owner, 1.0F);
      }
   }

   /** Ends the beam without generating EA's normal endpoint impact. */
   public void stopForDuelImpact() {
      this.impactQueued = true;
      this.clashing = false;
      setStage(Stage.FINISHED);
      discard();
   }

   private void startBeam(ServerLevel level, LivingEntity owner, float scale) {
      this.beamStarted = true;
      this.entityData.set(POWER, Math.max(0.35F, Math.min(1.0F, scale)));
      setStage(Stage.BEAM);
      updateDirectionFromOwner(owner);
      this.lastDamageDirection = this.direction;
      // EA is an anti-world attack: collapse UBW/Hajun before the first beam
      // frame, then move this invisible controller with its caster.
      if (collapseContainingBoundary(level, owner)) return;
   }

   private void tickBeam(ServerLevel level, LivingEntity owner) {
      updateDirectionFromOwner(owner);
      if (!beamVisualStarted) {
         beamVisualStarted = true;
         VFXServerEffects.spawn(level, "ea_beam", owner, 256.0);
      }
      BeamClashManager.tick(level, this);
      if (clashing) {
         // BeamClashManager applies symmetrical clash upkeep. Freeze EA's beam clock here.
         stageTicks--;
         return;
      }
      if (!drainMana(owner, 5.0)) {
         setStage(Stage.FINISHED);
         discard();
         return;
      }
      if (!clashing && stageTicks >= BEAM_DAMAGE_START) {
         if (stageTicks % BEAM_DAMAGE_INTERVAL == 0) applyBeamDamage(level, owner);
         destroyBeamBlocks(level, owner);
      }
      if (stageTicks >= BEAM_TICKS) setStage(Stage.IMPACT);
   }

   private void applyWindDamage(ServerLevel level, LivingEntity owner) {
      AABB area = owner.getBoundingBox().inflate(WIND_RADIUS);
      DamageSource source = owner.damageSources().mobProjectile(this, owner);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
         e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && !GilgameshDuelState.areDuelPartners(owner, e)
            && !EntityUtils.isImmunePlayerTarget(e) && e.distanceToSqr(owner) <= WIND_RADIUS * WIND_RADIUS)) {
         hurtWithoutIFrames(target, source, 50.0F);
         Vec3 push = target.position().subtract(owner.position());
         if (push.lengthSqr() > 1.0E-4) { push = push.normalize().scale(0.28); target.push(push.x, 0.12, push.z); }
      }
   }

   private void applyBeamDamage(ServerLevel level, LivingEntity owner) {
      Vec3 start = beamStart();
      List<Vec3> sweepDirections = sweepDirections(this.lastDamageDirection, this.direction);
      this.lastDamageDirection = this.direction;
      AABB search = new AABB(start, start);
      for (Vec3 forward : sweepDirections) {
         Vec3 end = start.add(forward.scale(BEAM_LENGTH));
         search = search.minmax(new AABB(end, end));
      }
      search = search.inflate(BEAM_HALF_WIDTH + 2, BEAM_HALF_HEIGHT + 2, BEAM_HALF_WIDTH + 2);
      float pulse = 5000.0F * getPowerScale() * clashDamageScale / BEAM_DAMAGE_PULSES;
      DamageSource source = owner.damageSources().mobProjectile(this, owner);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
         e -> e.isAlive() && e != owner && !e.isAlliedTo(owner) && !GilgameshDuelState.areDuelPartners(owner, e) && !EntityUtils.isImmunePlayerTarget(e))) {
         for (Vec3 forward : sweepDirections) {
            if (!intersectsBeam(target.getBoundingBox(), start, forward)) continue;
            hurtWithoutIFrames(target, source, pulse);
            target.push(forward.x * 0.15, forward.y * 0.08, forward.z * 0.15);
            break;
         }
      }
   }

   static List<Vec3> sweepDirections(Vec3 previous, Vec3 current) {
      Vec3 to = normalized(current);
      Vec3 from = previous == null ? to : normalized(previous);
      double angle = Math.acos(Math.max(-1.0, Math.min(1.0, from.dot(to))));
      int steps = Math.max(1, (int)Math.ceil(angle / BEAM_SWEEP_STEP_RADIANS));
      List<Vec3> directions = new ArrayList<>(steps + 1);
      for (int step = 0; step <= steps; step++) {
         double progress = step / (double)steps;
         Vec3 interpolated = from.scale(1.0 - progress).add(to.scale(progress));
         directions.add(interpolated.lengthSqr() < 1.0E-6 ? to : interpolated.normalize());
      }
      return directions;
   }

   static boolean intersectsBeam(AABB targetBounds, Vec3 start, Vec3 forward) {
      Vec3 direction = normalized(forward);
      Vec3 worldUp = Math.abs(direction.y) > 0.95 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
      Vec3 right = direction.cross(worldUp).normalize();
      Vec3 up = right.cross(direction).normalize();
      Vec3 center = targetBounds.getCenter();
      Vec3 rel = center.subtract(start);
      double halfX = targetBounds.getXsize() * 0.5;
      double halfY = targetBounds.getYsize() * 0.5;
      double halfZ = targetBounds.getZsize() * 0.5;
      double alongRadius = projectedRadius(direction, halfX, halfY, halfZ);
      double along = rel.dot(direction);
      if (along + alongRadius < -3.0 || along - alongRadius > BEAM_LENGTH) return false;
      double beamAlong = Math.max(0.0, Math.min(BEAM_LENGTH, along));
      double widthScale = Math.max(0.22, Math.sin(Math.PI * beamAlong / BEAM_LENGTH));
      double allowedWidth = along < 0.0 ? 2.8 : BEAM_HALF_WIDTH * Math.pow(widthScale, 0.35);
      return Math.abs(rel.dot(right)) <= allowedWidth + projectedRadius(right, halfX, halfY, halfZ)
         && Math.abs(rel.dot(up)) <= BEAM_HALF_HEIGHT + projectedRadius(up, halfX, halfY, halfZ);
   }

   private static double projectedRadius(Vec3 axis, double halfX, double halfY, double halfZ) {
      return Math.abs(axis.x) * halfX + Math.abs(axis.y) * halfY + Math.abs(axis.z) * halfZ;
   }

   private void destroyBeamBlocks(ServerLevel level, LivingEntity owner) {
      Vec3 start = beamStart();
      Vec3 forward = this.direction;
      Vec3 worldUp = Math.abs(forward.y) > 0.95 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
      Vec3 right = forward.cross(worldUp).normalize();
      Vec3 up = right.cross(forward).normalize();
      int phase = stageTicks % 5;
      int broken = 0;
      int maxBroken = Math.max(36, (int)(260 * getPowerScale()));
      for (double along = phase + 0.75; along <= BEAM_LENGTH && broken < maxBroken; along += 5.0) {
         double widthScale = Math.max(0.25, Math.sin(Math.PI * along / BEAM_LENGTH));
         double allowedWidth = BEAM_HALF_WIDTH * Math.pow(widthScale, 0.35);
         for (double side = -allowedWidth; side <= allowedWidth && broken < maxBroken; side += 0.85) {
            for (double vertical = -BEAM_HALF_HEIGHT * 0.34; vertical <= BEAM_HALF_HEIGHT * 0.6 && broken < maxBroken; vertical += 0.85) {
               BlockPos pos = BlockPos.containing(start.add(forward.scale(along)).add(right.scale(side)).add(up.scale(vertical)));
               if (destroyBlock(level, pos)) broken++;
            }
         }
      }
   }

   private boolean destroyBlock(ServerLevel level, BlockPos pos) {
      if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return false;
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0 || hardness > 100 || state.getExplosionResistance(level, pos, null) >= 1200) return false;
      return level.removeBlock(pos, false);
   }

   private void finishImpact(ServerLevel level, LivingEntity owner) {
      boolean duelImpact = duelFinale || isDuelTarget(level, owner);
      if (!impactQueued && !clashing && !duelImpact) {
         impactQueued = true;
         Vec3 center = getEndPos();
         double radius = 60.0;
         float damage = 200.0F * getPowerScale() * clashDamageScale;
         DamageSource source = level.damageSources().explosion(this, owner);
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
            e -> e.isAlive() && e != owner && !GilgameshDuelState.areDuelPartners(owner, e) && !EntityUtils.isImmunePlayerTarget(e) && e.distanceToSqr(center) <= radius * radius)) {
            hurtWithoutIFrames(target, source, damage);
         }
         VFXServerEffects.spawn(level, "ea_impact", center, 256.0);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 16, 2.5, 2.5, 2.5, 0.0);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, center.x, center.y, center.z, 6, 0.4, 0.4, 0.4, 0.0);
         queueImpactTerrainInWaves(level, center, radius);
         level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.45F);
      }
      int cooldown = 1200 + Math.round(1200 * Math.max(0, getPowerScale() - 0.35F) / 0.65F);
      applyCooldown(owner, cooldown);
      clearNpcEquipment(owner);
      setStage(Stage.FINISHED);
      discard();
   }

   /** One shared final impact for the Enkidu/Gilgamesh duel. */
   public static void spawnDuelFinalImpact(ServerLevel level, Vec3 center, double radius) {
      if (level == null || center == null) return;
      VFXServerEffects.spawn(level, "ea_impact", center, 280.0);
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_ground_impact", center, 280.0);
      VFXServerEffects.spawn(level, "servant_enkidu_enuma_elish_aftermath", center, 280.0);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
         center.x, center.y, center.z, 22, 2.8, 1.8, 2.8, 0.0);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
         center.x, center.y + 1.0, center.z, 120, radius * 0.35, radius * 0.2, radius * 0.35, 0.06);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
         center.x, center.y + 0.5, center.z, 8, 0.8, 0.35, 0.8, 0.0);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
         center.x, center.y + 1.5, center.z, 420, radius * 0.48, radius * 0.28, radius * 0.48, 0.2);
      VFXServerEffects.screenFlash(level, center, 128.0, 12, 0.75F);
      for (int delay : new int[]{14, 32, 54, 78}) {
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
               center.x, center.y + 1.0, center.z, 90, radius * 0.42, radius * 0.2, radius * 0.42, 0.08);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
               center.x, center.y + 2.0, center.z, 36, radius * 0.34, radius * 0.22, radius * 0.34, 0.05);
         });
      }
      queueImpactTerrainInWaves(level, center, radius);
      level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
         SoundSource.HOSTILE, 9.0F, 0.4F);
   }

   private boolean collapseContainingBoundary(ServerLevel source, LivingEntity owner) {
      if (boundaryBroken || (!net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager.isUbwDimension(source)
         && !net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions.isHajunDimension(source.dimension().location()))) {
         return false;
      }
      boundaryBroken = true;
      LivingEntity returned = EaWorldBoundaryBreaker.breakFor(owner, source);
      if (returned == null || !(returned.level() instanceof ServerLevel destination) || destination == source) {
         return false;
      }
      destination.setWeatherParameters(0, EA_THUNDER_TICKS, true, true);
      Entity moved = changeDimension(new DimensionTransition(destination,
         returned.position().add(0.0, returned.getBbHeight() * 0.65, 0.0), Vec3.ZERO,
         returned.getYRot(), returned.getXRot(), DimensionTransition.DO_NOTHING));
      if (moved instanceof GilgameshEaBeamEntity controller) {
         controller.ownerUuid = returned.getUUID();
         controller.entityData.set(OWNER_ID, returned.getId());
         controller.direction = this.direction;
         controller.windOriginY = this.windOriginY;
         controller.beamStarted = true;
         controller.boundaryBroken = true;
         controller.beamVisualStarted = false;
         controller.updateEnd();
         return true;
      }
      return false;
   }

   private void updateDirectionFromOwner(LivingEntity owner) {
      this.direction = normalized(owner.getLookAngle());
      this.updateEnd();
   }

   private static void queueImpactTerrainInWaves(ServerLevel level, Vec3 center, double radius) {
      int waves = Math.max(1, (int)Math.ceil(radius / 2.0));
      double step = radius / waves;
      for (int wave = 1; wave <= waves; wave++) {
         int index = wave;
         TYPE_MOON_WORLD.queueServerWork(index * 2, () -> {
            double previous = Math.max(0, (index - 1) * step);
            double current = index * step;
            DeferredTerrainDestruction.queueShell(level, center, current, previous, 48,
               (serverLevel, pos, distanceSqr, shellRadius, origin) -> {
                  if (!serverLevel.hasChunkAt(pos) || serverLevel.getBlockEntity(pos) != null) return false;
                  BlockState state = serverLevel.getBlockState(pos);
                  float hardness = state.getDestroySpeed(serverLevel, pos);
                  return !state.isAir() && !state.is(Blocks.BEDROCK) && hardness >= 0 && hardness <= 80
                     && state.getExplosionResistance(serverLevel, pos, null) < 1200;
               }, null);
         });
      }
   }

   private void updateEnd() {
      Vec3 end = this.position().add(this.direction.scale(BEAM_LENGTH));
      this.entityData.set(END_X, (float)end.x);
      this.entityData.set(END_Y, (float)end.y);
      this.entityData.set(END_Z, (float)end.z);
   }

   private void setStage(Stage stage) { this.entityData.set(STAGE, stage.ordinal()); this.stageTicks = 0; }
   private LivingEntity getOwner(ServerLevel level) { Entity e = ownerUuid == null ? null : level.getEntity(ownerUuid); return e instanceof LivingEntity living ? living : null; }

   private boolean isTrackedTargetAlive(ServerLevel level) {
      if (trackedTargetUuid == null) return true;
      LivingEntity target = getTrackedTarget(level);
      return target != null && target.isAlive() && target.level() == level;
   }

   private LivingEntity getTrackedTarget(ServerLevel level) {
      if (trackedTargetUuid == null) return null;
      Entity target = level.getEntity(trackedTargetUuid);
      return target instanceof LivingEntity living && !EntityUtils.isImmunePlayerTarget(living) ? living : null;
   }

   private boolean isDuelTarget(ServerLevel level, LivingEntity owner) {
      if (trackedTargetUuid == null) return false;
      Entity target = level.getEntity(trackedTargetUuid);
      return target instanceof LivingEntity living && GilgameshDuelState.areDuelPartners(owner, living);
   }

   private void cancelNpcEa(ServerLevel level, LivingEntity owner) {
      stopNpcEaVoice(level, owner);
      clearNpcEquipment(owner);
      setStage(Stage.FINISHED);
      discard();
   }

   private static void stopNpcEaVoice(ServerLevel level, LivingEntity owner) {
      ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(ModSounds.GILGAMESH_VOICE_EA_NPC.get().getLocation(), SoundSource.HOSTILE);
      for (net.minecraft.server.level.ServerPlayer listener : level.players()) {
         if (listener.distanceToSqr(owner) <= 256.0 * 256.0) listener.connection.send(packet);
      }
   }

   private static void clearNpcEquipment(LivingEntity owner) {
      if (owner instanceof GilgameshEntity gilgamesh) GilgameshCombatHelper.clearEaEquipment(gilgamesh);
   }

   private boolean drainMana(LivingEntity owner, double amount) {
      if (owner instanceof GilgameshEntity gil) { if (gil.getCurrentMp() < amount) return false; gil.setCurrentMp(gil.getCurrentMp() - amount); return true; }
      if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.servant_card_transformed) return ServantCardManaService.consumeNoblePhantasm(player, vars, amount);
         if (vars.player_mana < amount) return false; vars.player_mana -= amount; vars.syncMana(player); return true;
      }
      return true;
   }

   private static void applyCooldown(LivingEntity owner, int ticks) {
      if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
         player.getCooldowns().addCooldown(ModItems.GILGAMESH_EA.get(), ticks);
         player.getCooldowns().addCooldown(ModItems.GILGAMESH_BAB_ILU.get(), ticks);
      }
   }

   private static boolean canPlayEaVoice(LivingEntity owner) {
      if (owner instanceof net.minecraft.server.level.ServerPlayer player) {
         return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed;
      }
      return false;
   }

   private static Vec3 normalized(Vec3 direction) { return direction == null || direction.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : direction.normalize(); }
   private static void hurtWithoutIFrames(LivingEntity target, DamageSource source, float amount) { target.invulnerableTime = 0; target.hurtTime = 0; target.hurt(source, amount); target.invulnerableTime = 0; target.hurtTime = 0; }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(STAGE, Stage.WIND.ordinal()); builder.define(OWNER_ID, -1); builder.define(POWER, 0.35F);
      builder.define(END_X, 0.0F); builder.define(END_Y, 0.0F); builder.define(END_Z, 0.0F);
   }
   @Override protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
      if (tag.hasUUID("TrackedTarget")) trackedTargetUuid = tag.getUUID("TrackedTarget");
      entityData.set(STAGE, tag.getInt("Stage")); entityData.set(OWNER_ID, tag.getInt("OwnerId")); entityData.set(POWER, tag.getFloat("Power"));
      entityData.set(END_X, tag.getFloat("EndX")); entityData.set(END_Y, tag.getFloat("EndY")); entityData.set(END_Z, tag.getFloat("EndZ"));
      direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ")); windOriginY = tag.getDouble("WindOriginY");
      stageTicks = tag.getInt("StageTicks"); releaseRequested = tag.getBoolean("Release"); windOnly = tag.getBoolean("WindOnly");
      beamStarted = tag.getBoolean("BeamStarted"); impactQueued = tag.getBoolean("ImpactQueued"); autoReleaseAtFull = tag.getBoolean("AutoRelease");
      boundaryBroken = tag.getBoolean("BoundaryBroken"); beamVisualStarted = tag.getBoolean("BeamVisualStarted");
      duelFinale = tag.getBoolean("DuelFinale");
      clashDamageScale = Math.max(0.0F, Math.min(1.0F,
         tag.contains("ClashDamageScale") ? tag.getFloat("ClashDamageScale") : 1.0F));
   }
   @Override protected void addAdditionalSaveData(CompoundTag tag) {
      if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
      if (trackedTargetUuid != null) tag.putUUID("TrackedTarget", trackedTargetUuid);
      tag.putInt("Stage", entityData.get(STAGE)); tag.putInt("OwnerId", entityData.get(OWNER_ID)); tag.putFloat("Power", entityData.get(POWER));
      tag.putFloat("EndX", entityData.get(END_X)); tag.putFloat("EndY", entityData.get(END_Y)); tag.putFloat("EndZ", entityData.get(END_Z));
      tag.putDouble("DirX", direction.x); tag.putDouble("DirY", direction.y); tag.putDouble("DirZ", direction.z); tag.putDouble("WindOriginY", windOriginY);
      tag.putInt("StageTicks", stageTicks); tag.putBoolean("Release", releaseRequested); tag.putBoolean("WindOnly", windOnly);
      tag.putBoolean("BeamStarted", beamStarted); tag.putBoolean("ImpactQueued", impactQueued); tag.putBoolean("AutoRelease", autoReleaseAtFull);
      tag.putBoolean("BoundaryBroken", boundaryBroken); tag.putBoolean("BeamVisualStarted", beamVisualStarted);
      tag.putBoolean("DuelFinale", duelFinale);
      tag.putFloat("ClashDamageScale", clashDamageScale);
   }

   @Override public Entity clashEntity() { return this; }
   @Override public BeamType beamType() { return BeamType.EA; }
   @Override public LivingEntity beamOwner(ServerLevel level) { return getOwner(level); }
   @Override public Vec3 beamStart() { return this.position(); }
   @Override public Vec3 beamEnd() { return getEndPos(); }
   @Override public double beamHalfWidth() { return BEAM_HALF_WIDTH; }
   @Override public float clashPower() { return 1.18F * getPowerScale(); }
   @Override public boolean isBeamDamageActive() { return getStage() == Stage.BEAM && stageTicks >= BEAM_DAMAGE_START; }
   @Override public boolean isClashing() { return clashing; }
   @Override public void setClashing(boolean value) { clashing = value; }
   @Override public void setClashDamageScale(float value) { clashDamageScale = Math.max(0, Math.min(1.0F, value)); }
   @Override public void cancelClashBeam() { setStage(Stage.FINISHED); discard(); }
   @Override public void continueAfterClash() { clashing = false; }
   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardArashSkills;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class ArashStellaControllerEntity extends Entity {
   public static final TicketController CHUNK_TICKETS = new TicketController(
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "arash_stella"),
      ArashStellaControllerEntity::validateLoadedTickets);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CHUNK_TICKET_REFRESH_TICKS = 8;
   private static final double CHUNK_TICKET_BACK_DISTANCE = 128.0;
   private static final double CHUNK_TICKET_AHEAD_DISTANCE = 256.0;
   private static final int STAGE_CHANT = 0;
   private static final int STAGE_FLIGHT = 1;
   private static final int STAGE_EXPLOSION = 2;
   private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.18F, 1.0F, 0.38F), 1.6F);
   private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.08F), 1.5F);
   private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.8F);
   private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0F, 0.12F, 0.02F), 1.3F);
   private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.2F, 0.75F, 1.0F), 1.45F);

   private int stage = STAGE_CHANT;
   private int stageTicks;
   private int preloadIndex;
   private Vec3 origin = Vec3.ZERO;
   private Vec3 direction = new Vec3(0.0, 0.0, 1.0);
   private UUID casterId;
   private boolean playerCaster;
   private boolean playerReleaseRequested;
   private int releaseRequestTick = -1;
   private int playerChargeTicks = ArashCombatRules.PLAYER_STELLA_FULL_CHARGE_TICKS;
   private String teamName = "";
   private final List<Long> chunks = new ArrayList<>();
   private final Set<Long> forcedChunks = new HashSet<>();
   private final Set<UUID> allies = new HashSet<>();
   private final Set<UUID> lineHits = new HashSet<>();
   private transient DeferredTerrainDestruction.AdvancingCylinder cylinder;
   private transient DeferredTerrainDestruction.AdvancingSkyRift skyRift;
   private transient DeferredTerrainDestruction.ExpandingSphere sphere;
   private boolean finalDamageDone;
   private boolean released;
   private boolean ticketsReleased;
   private int lastTicketRefreshTick = -CHUNK_TICKET_REFRESH_TICKS;

   public ArashStellaControllerEntity(EntityType<? extends ArashStellaControllerEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public static boolean tryBegin(ArashEntity arash, LivingEntity target) {
      if (!(arash.level() instanceof ServerLevel level) || arash.getPersistentData().getBoolean(ArashEntity.TAG_STELLA_USED)
         || arash.getPersistentData().getBoolean(ArashEntity.TAG_STELLA_CHANTING)
         || arash.getCurrentMp() < 100.0 || !arash.hasMasterNoblePhantasmPermission()) return false;
      if (hasActiveController(level, arash.getUUID())) return false;
      List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, arash.getBoundingBox().inflate(64.0),
         living -> ArashCombatHelper.isTarget(arash, living));
      boolean surrounded = hostiles.size() >= 8;
      boolean lastStand = arash.getHealth() <= arash.getMaxHealth() * 0.25F && ArashCombatHelper.isHighThreat(target);
      if (!surrounded && !lastStand) return false;

      Vec3 aim = weightedAim(arash, hostiles.isEmpty() ? List.of(target) : hostiles);
      Vec3 flat = new Vec3(aim.x - arash.getX(), 0.0, aim.z - arash.getZ());
      if (flat.lengthSqr() < 1.0E-6) flat = arash.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (flat.lengthSqr() < 1.0E-6) return false;
      flat = flat.normalize();
      Vec3 origin = arash.getEyePosition().add(flat.scale(0.9));
      Vec3 end = origin.add(flat.scale(ArashCombatRules.STELLA_LENGTH));
      if (!insideWorldBorder(level, origin, end, flat) || hasFriendlyInBlastPath(level, arash, origin, end)) return false;

      ArashStellaControllerEntity controller = ModEntities.ARASH_STELLA_CONTROLLER.get().create(level);
      if (controller == null) return false;
      if (!arash.consumeCraftedArrows(ArashCombatRules.STELLA_ARROW_COST)) return false;
      controller.init(arash, origin, flat);
      arash.setCurrentMp(arash.getCurrentMp() - 100.0);
      arash.getPersistentData().putBoolean(ArashEntity.TAG_STELLA_USED, true);
      arash.getPersistentData().putBoolean(ArashEntity.TAG_STELLA_CHANTING, true);
      arash.faceVector(flat);
      arash.triggerNamedActionAnimation("stella_chant");
      level.addFreshEntity(controller);
      level.playSound(null, arash.blockPosition(), ModSounds.ARASH_VOICE_STELLA.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
      return true;
   }

   public static boolean beginPlayerStella(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level) || !ServantCardArashSkills.isArash(player)
         || ServantCardArashSkills.isPlayerChanting(player) || !player.getMainHandItem().is(ModItems.ARASH_BOW.get())) {
         return false;
      }
      if (hasActiveController(level, player.getUUID())) return false;
      Vec3 flat = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (flat.lengthSqr() < 1.0E-6) return false;
      flat = flat.normalize();
      Vec3 origin = player.getEyePosition().add(flat.scale(0.9));
      Vec3 end = origin.add(flat.scale(ArashCombatRules.STELLA_LENGTH));
      if (!insideWorldBorder(level, origin, end, flat) || hasFriendlyInBlastPath(level, player, origin, end)) return false;

      ArashStellaControllerEntity controller = ModEntities.ARASH_STELLA_CONTROLLER.get().create(level);
      if (controller == null) return false;
      controller.playerCaster = true;
      controller.init(player, origin, flat);
      ServantCardArashSkills.beginPlayerChant(player, controller.getUUID());
      level.addFreshEntity(controller);
      player.playNotifySound(ModSounds.ARASH_VOICE_STELLA.get(), SoundSource.VOICE, 1.6F, 1.0F);
      level.playSound(player, player.blockPosition(), ModSounds.ARASH_VOICE_STELLA.get(), SoundSource.VOICE, 1.6F, 1.0F);
      return true;
   }

   private void init(LivingEntity caster, Vec3 origin, Vec3 direction) {
      this.casterId = caster.getUUID();
      this.origin = origin;
      this.direction = direction.normalize();
      this.setPos(origin.x, origin.y, origin.z);
      this.teamName = caster.getTeam() == null ? "" : caster.getTeam().getName();
      AABB search = pathBounds(origin, origin.add(direction.scale(ArashCombatRules.STELLA_LENGTH)), ArashCombatRules.STELLA_END_RADIUS);
      for (LivingEntity living : caster.level().getEntitiesOfClass(LivingEntity.class, search,
         living -> living == caster || caster.isAlliedTo(living) || living.isAlliedTo(caster))) {
         allies.add(living.getUUID());
      }
      chunks.addAll(computeChunks(origin, direction, ArashCombatRules.fullStellaProfile()));
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level)) return;
      try {
         if (stage == STAGE_CHANT) tickChant(level);
         else if (stage == STAGE_FLIGHT) tickFlight(level);
         else tickExplosion(level);
      } catch (RuntimeException exception) {
         LOGGER.error("Arash Stella controller failed on the server; aborting safely (entity={}, stage={}, tick={})",
            getUUID(), stage, stageTicks, exception);
         abort(level, true);
      }
   }

   private void tickChant(ServerLevel level) {
      LivingEntity caster = getCaster(level);
      if (caster == null || !caster.isAlive()) { abort(level, false); return; }
      updateChunkTickets(level, 0.0, stellaProfile());
      if (playerCaster) {
         if (!(caster instanceof ServerPlayer player) || !ServantCardArashSkills.isPlayerChanting(player)) {
            abort(level, false);
            return;
         }
      } else if (caster instanceof ArashEntity arash) {
         arash.getNavigation().stop();
         arash.setDeltaMovement(Vec3.ZERO);
         arash.faceVector(direction);
      }
      spawnChantEffects(level, caster, stageTicks);
      preloadIndex = chunks.size();
      stageTicks++;

      if (playerCaster) {
         if (!playerReleaseRequested && stageTicks >= ArashCombatRules.PLAYER_STELLA_AUTO_RELEASE_TICKS) {
            playerReleaseRequested = true;
            releaseRequestTick = stageTicks;
            configurePlayerCharge(ArashCombatRules.PLAYER_STELLA_FULL_CHARGE_TICKS);
            release(level, caster);
            return;
         }
         if (!playerReleaseRequested) return;
         if (preloadIndex < chunks.size()) {
            if (stageTicks <= releaseRequestTick + ArashCombatRules.STELLA_PRELOAD_GRACE_TICKS) return;
            abort(level, true);
            return;
         }
      } else {
         if (stageTicks < ArashCombatRules.STELLA_CHANT_TICKS) return;
         if (preloadIndex < chunks.size()) {
            if (stageTicks <= ArashCombatRules.STELLA_CHANT_TICKS + ArashCombatRules.STELLA_PRELOAD_GRACE_TICKS) return;
            abort(level, true);
            return;
         }
      }
      release(level, caster);
   }

   public boolean requestPlayerRelease(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level) || !playerCaster || stage != STAGE_CHANT
         || casterId == null || !casterId.equals(player.getUUID())) return false;
      if (playerReleaseRequested) return true;
      if (!ArashCombatRules.canReleasePlayerStella(stageTicks)) {
         player.displayClientMessage(Component.translatable(
            "message.typemoonworld.servant_card.arash_stella_charge_too_short"), true);
         return false;
      }
      playerReleaseRequested = true;
      releaseRequestTick = stageTicks;
      if (ArashCombatRules.shouldFinishLongStellaVoice(stageTicks)) {
         playerChargeTicks = Math.min(stageTicks, ArashCombatRules.PLAYER_STELLA_FULL_CHARGE_TICKS);
         configurePlayerCharge(playerChargeTicks);
         // Keep the 38-second long voice playing after the 36-second shot.
         if (chunks.isEmpty()) release(level, player);
      } else {
         playerChargeTicks = stageTicks;
         configurePlayerCharge(playerChargeTicks);
         stopLongChantSound(level);
         playShortStella(level, player);
         if (chunks.isEmpty()) release(level, player);
      }
      return true;
   }

   private void configurePlayerCharge(int chargeTicks) {
      playerChargeTicks = Math.max(ArashCombatRules.PLAYER_STELLA_MIN_CHARGE_TICKS,
         Math.min(ArashCombatRules.PLAYER_STELLA_FULL_CHARGE_TICKS, chargeTicks));
      List<Long> required = computeChunks(origin, direction, stellaProfile());
      chunks.clear();
      chunks.addAll(required);
      preloadIndex = chunks.size();
   }

   private ArashCombatRules.StellaProfile stellaProfile() {
      return playerCaster ? ArashCombatRules.playerStellaProfile(playerChargeTicks)
         : ArashCombatRules.fullStellaProfile();
   }

   private void release(ServerLevel level, LivingEntity caster) {
      ArashCombatRules.StellaProfile profile = stellaProfile();
      released = true;
      stage = STAGE_FLIGHT;
      stageTicks = 0;
      lastTicketRefreshTick = -CHUNK_TICKET_REFRESH_TICKS;
      if (caster instanceof ArashEntity arash) {
         arash.getPersistentData().remove(ArashEntity.TAG_STELLA_CHANTING);
         arash.beginStellaSacrifice();
         arash.triggerNamedActionAnimation("stella_release");
      } else if (caster instanceof ServerPlayer player) {
         ServantCardArashSkills.finishPlayerChant(player);
         ServantCardArashSkills.beginPlayerStellaSacrifice(player);
      }
      level.sendParticles(WHITE, origin.x, origin.y, origin.z, 140, 1.2, 1.2, 1.2, 0.28);
      level.sendParticles(GREEN, origin.x, origin.y, origin.z, 220, 1.8, 1.8, 1.8, 0.34);
      level.sendParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 3, 0, 0, 0, 0);
      this.cylinder = DeferredTerrainDestruction.queueAdvancingCylinder(level, origin, direction,
         profile.length(), profile.terrainRadius(), profile.scarRadius(), null);
      this.skyRift = DeferredTerrainDestruction.queueAdvancingSkyRift(level, origin, direction,
         profile.length(), profile.terrainRadius(), null);
   }

   public void forceReleaseForGameTest() {
      if (this.level() instanceof ServerLevel level && stage == STAGE_CHANT) {
         LivingEntity caster = getCaster(level);
         if (caster != null && caster.isAlive()) {
            if (playerCaster) configurePlayerCharge(ArashCombatRules.PLAYER_STELLA_FULL_CHARGE_TICKS);
            release(level, caster);
         }
      }
   }

   public void abortTechnicalForGameTest() {
      if (this.level() instanceof ServerLevel level && stage == STAGE_CHANT) abort(level, true);
   }

   public Vec3 getDirectionForGameTest() { return direction; }

   public boolean isInFlight() { return stage == STAGE_FLIGHT; }

   private void tickFlight(ServerLevel level) {
      ArashCombatRules.StellaProfile profile = stellaProfile();
      double currentDistance = profile.distanceAtTick(stageTicks);
      updateChunkTickets(level, currentDistance, profile);
      if (cylinder == null) {
         cylinder = DeferredTerrainDestruction.queueAdvancingCylinder(level, origin, direction,
            profile.length(), profile.terrainRadius(), profile.scarRadius(), null);
      }
      if (skyRift == null) {
         skyRift = DeferredTerrainDestruction.queueAdvancingSkyRift(level, origin, direction,
            profile.length(), profile.terrainRadius(), null);
      }
      double previous = currentDistance;
      stageTicks++;
      double current = profile.distanceAtTick(stageTicks);
      cylinder.advanceTo(current);
      skyRift.advanceTo(current);
      damageSegment(level, previous, current);
      Vec3 point = origin.add(direction.scale(current));
      this.setPos(point.x, point.y, point.z);
      spawnMeteorTrail(level, current);
      if (stageTicks >= profile.flightTicks()) {
         cylinder.advanceTo(profile.length());
         cylinder.seal();
         skyRift.advanceTo(profile.length());
         skyRift.seal();
         stage = STAGE_EXPLOSION;
         stageTicks = 0;
         this.setPos(point.x, point.y, point.z);
      }
   }

   private void tickExplosion(ServerLevel level) {
      ArashCombatRules.StellaProfile profile = stellaProfile();
      Vec3 center = origin.add(direction.scale(profile.length()));
      // Keep the final flight window alive while the deferred cylinder finishes.
      if (forcedChunks.isEmpty()) updateChunkTickets(level, profile.length(), profile);
      if (!finalDamageDone) {
         finalDamageDone = true;
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(center, center).inflate(profile.endRadius()), this::canDamage)) {
            if (target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).distanceToSqr(center)
               <= profile.endRadius() * profile.endRadius()) {
               hurtStellaTarget(level, target, profile.coreDamage());
            }
         }
         level.sendParticles(WHITE, center.x, center.y, center.z, 420, 4.0, 4.0, 4.0, 0.6);
         level.sendParticles(GREEN, center.x, center.y, center.z, 520, 7.0, 7.0, 7.0, 0.75);
         level.sendParticles(GOLD, center.x, center.y, center.z, 280, 5.0, 5.0, 5.0, 0.5);
         level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 5, 2.0, 2.0, 2.0, 0.0);
      }
      if (sphere == null) {
         sphere = DeferredTerrainDestruction.queueExpandingSphere(level, center, (int)Math.round(profile.endRadius()),
            () -> { if (!this.isRemoved() && this.level() instanceof ServerLevel current) finish(current); });
      }
      stageTicks++;
      sphere.advanceTo(profile.explosionRadiusAtTick(stageTicks));
      if (stageTicks >= profile.explosionTicks()) sphere.seal();
      if (stageTicks % 4 == 0) {
         double radius = profile.explosionRadiusAtTick(stageTicks);
         spawnSphereShell(level, center, radius, 48);
      }
      if (sphere.isComplete()) finish(level);
   }

   private void damageSegment(ServerLevel level, double previous, double current) {
      ArashCombatRules.StellaProfile profile = stellaProfile();
      Vec3 start = origin.add(direction.scale(Math.max(0.0, previous - 1.0)));
      Vec3 end = origin.add(direction.scale(Math.min(profile.length(), current + 1.0)));
      double damageRadius = profile.outerRadius() + ArashCombatRules.STELLA_DAMAGE_RADIUS_PADDING;
      AABB area = pathBounds(start, end, damageRadius);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, this::canDamage)) {
         if (lineHits.contains(target.getUUID())) continue;
         double distance = distanceToSegment(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), start, end);
         float damage = distance <= profile.coreRadius() ? profile.coreDamage()
            : distance <= damageRadius ? profile.outerDamage() : 0.0F;
         if (damage > 0.0F) {
            lineHits.add(target.getUUID());
            hurtStellaTarget(level, target, damage);
         }
      }
   }

   private void hurtStellaTarget(ServerLevel level, LivingEntity target, float damage) {
      LivingEntity caster = getCaster(level);
      DamageSource source = caster != null
         ? caster.damageSources().source(ArashDamageTypes.STELLA, this, caster)
         : level.damageSources().generic();
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
      target.hurt(source, damage);
      target.invulnerableTime = 0;
      target.hurtTime = 0;
      target.hurtDuration = 0;
   }

   private boolean canDamage(LivingEntity target) {
      if (!target.isAlive() || EntityUtils.isImmunePlayerTarget(target) || allies.contains(target.getUUID())) return false;
      return teamName.isEmpty() || target.getTeam() == null || !teamName.equals(target.getTeam().getName());
   }

   private void spawnChantEffects(ServerLevel level, LivingEntity caster, int tick) {
      int fullChargeTicks = playerCaster ? ArashCombatRules.PLAYER_STELLA_FULL_CHARGE_TICKS
         : ArashCombatRules.STELLA_CHANT_TICKS;
      double progress = Math.min(1.0, tick / (double)fullChargeTicks);
      Vec3 feet = caster.position().add(0.0, 0.08, 0.0);
      Vec3 bow = caster.getEyePosition().add(direction.scale(0.9));
      DustParticleOptions lifeColor = progress < 0.42 ? GREEN : progress < 0.78 ? GOLD : WHITE;

      level.sendParticles(lifeColor, caster.getX(), caster.getY() + caster.getBbHeight() * 0.52, caster.getZ(),
         18 + (int)(progress * 28.0), 0.65, caster.getBbHeight() * 0.55, 0.65, 0.025 + progress * 0.025);
      level.sendParticles(ParticleTypes.ENCHANT, bow.x, bow.y, bow.z, 20, 2.8, 5.5, 2.8, 0.42);

      if (tick % 2 == 0) {
         for (int strand = 0; strand < 3; strand++) {
            double helixRadius = 1.0 + strand * 0.45 + progress * 0.9;
            for (int i = 0; i < 12; i++) {
               double angle = tick * (0.13 + strand * 0.025) + i * Math.PI / 6.0 + strand * 2.1;
               double y = caster.getY() + (i / 11.0) * (4.0 + progress * 5.0);
               level.sendParticles(strand == 0 ? GREEN : strand == 1 ? GOLD : WHITE,
                  caster.getX() + Math.cos(angle) * helixRadius, y,
                  caster.getZ() + Math.sin(angle) * helixRadius, 1, 0, 0, 0, 0);
            }
         }

         int arrowPoints = 18 + (int)(progress * 18.0);
         double arrowLength = 1.2 + progress * 3.8;
         Vec3 arrowStart = bow.subtract(direction.scale(arrowLength * 0.45));
         for (int i = 0; i <= arrowPoints; i++) {
            Vec3 point = arrowStart.add(direction.scale(arrowLength * i / arrowPoints));
            level.sendParticles(i % 5 == 0 ? WHITE : lifeColor, point.x, point.y, point.z,
               1, 0.025 + progress * 0.045, 0.025 + progress * 0.045, 0.025 + progress * 0.045, 0.0);
         }
      }

      if (tick % 3 == 0) {
         for (int ring = 0; ring < 3; ring++) {
            double radius = 1.0 + ring * 2.1 + progress * (4.0 + ring * 1.6);
            spawnGroundRing(level, feet, radius, tick * (0.055 + ring * 0.018),
               ring == 0 ? GREEN : ring == 1 ? GOLD : CYAN, 32);
         }
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, caster.getX(), caster.getY() + 0.2, caster.getZ(),
            32, 3.5 + progress * 4.0, 0.15, 3.5 + progress * 4.0, 0.035);
      }

      if (tick % 4 == 0) {
         double skyY = caster.getY() + 13.0 + progress * 12.0;
         for (int i = -12; i <= 12; i++) {
            double zigzag = Math.sin(i * 1.73 + tick * 0.09) * (1.0 + progress * 2.5);
            Vec3 crack = caster.position().add(direction.scale(i * 1.15)).add(-direction.z * zigzag, skyY - caster.getY(), direction.x * zigzag);
            level.sendParticles(i % 4 == 0 ? WHITE : CYAN, crack.x, crack.y, crack.z, 2, 0.16, 0.16, 0.16, 0.01);
         }
         for (int ray = 0; ray < 12; ray++) {
            double angle = ray * Math.PI / 6.0 + tick * 0.025;
            Vec3 sky = new Vec3(caster.getX() + Math.cos(angle) * (7.0 + progress * 7.0), skyY,
               caster.getZ() + Math.sin(angle) * (7.0 + progress * 7.0));
            for (int step = 1; step <= 4; step++) {
               Vec3 point = sky.lerp(bow, step / 5.0);
               level.sendParticles(step < 3 ? CYAN : WHITE, point.x, point.y, point.z, 1, 0.08, 0.08, 0.08, 0.0);
            }
         }
      }

      if (progress > 0.62 && tick % 5 == 0) {
         for (double radius : new double[]{1.2, 2.4, 3.8}) {
            spawnCrossSectionRing(level, bow, radius * (0.7 + progress * 0.3), tick * 0.12, WHITE, 28);
         }
         level.sendParticles(ParticleTypes.FLASH, bow.x, bow.y, bow.z, 1, 0, 0, 0, 0);
      }
   }

   private void spawnMeteorTrail(ServerLevel level, double distance) {
      if ((this.tickCount & 1) != 0) return;
      Vec3 head = origin.add(direction.scale(distance));
      level.sendParticles(WHITE, head.x, head.y, head.z, 28, 1.05, 1.05, 1.05, 0.08);
      level.sendParticles(GREEN, head.x, head.y, head.z, 52, 2.25, 2.25, 2.25, 0.12);
      level.sendParticles(GOLD, head.x, head.y, head.z, 24, 1.35, 1.35, 1.35, 0.09);
      level.sendParticles(ParticleTypes.FIREWORK, head.x, head.y, head.z, 20, 2.8, 2.8, 2.8, 0.16);
      for (int i = 6; i <= 180; i += 12) {
         Vec3 trail = head.subtract(direction.scale(i));
         level.sendParticles(i < 75 ? GREEN : i < 150 ? GOLD : WHITE,
            trail.x, trail.y, trail.z, 5, i * 0.024, i * 0.024, i * 0.024, 0.015);
      }
      if (this.tickCount % 4 == 0) {
         for (double radius : new double[]{3.5, 7.0}) {
            spawnCrossSectionRing(level, head, radius, this.tickCount * 0.14 + radius,
               radius < 5.0 ? WHITE : GREEN, 18);
         }
         level.sendParticles(RED, head.x, head.y - 6.0, head.z, 24, 7.0, 1.0, 7.0, 0.06);
         level.sendParticles(ParticleTypes.LARGE_SMOKE, head.x, head.y - 5.5, head.z, 18, 6.0, 0.9, 6.0, 0.045);
      }
   }

   private void spawnCrossSectionRing(ServerLevel level, Vec3 center, double radius, double rotation,
                                      DustParticleOptions particle, int points) {
      Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
      for (int i = 0; i < points; i++) {
         double angle = rotation + Math.PI * 2.0 * i / points;
         Vec3 point = center.add(side.scale(Math.cos(angle) * radius)).add(0.0, Math.sin(angle) * radius, 0.0);
         level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
      }
   }

   private static void spawnGroundRing(ServerLevel level, Vec3 center, double radius, double rotation,
                                       DustParticleOptions particle, int points) {
      for (int i = 0; i < points; i++) {
         double angle = rotation + Math.PI * 2.0 * i / points;
         level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y,
            center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
      }
   }

   private static void spawnSphereShell(ServerLevel level, Vec3 center, double radius, int points) {
      if (radius <= 0.0) return;
      for (int i = 0; i < points; i++) {
         double u = level.random.nextDouble() * 2.0 - 1.0;
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double horizontal = Math.sqrt(1.0 - u * u);
         Vec3 point = center.add(Math.cos(angle) * horizontal * radius, u * radius, Math.sin(angle) * horizontal * radius);
         level.sendParticles(i % 3 == 0 ? GOLD : GREEN, point.x, point.y, point.z, 1, 0, 0, 0, 0);
      }
   }

   private void abort(ServerLevel level, boolean refund) {
      try {
         LivingEntity caster = getCaster(level);
         if (caster instanceof ArashEntity arash) {
            arash.getPersistentData().remove(ArashEntity.TAG_STELLA_CHANTING);
            if (!released && refund) {
               arash.setCurrentMp(Math.min(arash.getMaxMp(), arash.getCurrentMp() + 100.0));
               arash.getPersistentData().remove(ArashEntity.TAG_STELLA_USED);
            }
         } else if (caster instanceof ServerPlayer player) {
            if (!released && refund) ServantCardArashSkills.abortPlayerChantTechnical(player);
            else ServantCardArashSkills.abortPlayerChantNoRefund(player);
         }
      } catch (RuntimeException exception) {
         LOGGER.warn("Failed to restore Arash Stella caster state during abort (entity={})", getUUID(), exception);
      } finally {
         try {
            stopAllStellaSounds(level);
         } catch (RuntimeException exception) {
            LOGGER.warn("Failed to stop Arash Stella sounds during abort (entity={})", getUUID(), exception);
         }
         releaseTickets(level);
         this.discard();
      }
   }

   private void finish(ServerLevel level) {
      releaseTickets(level);
      this.discard();
   }

   @Override
   public void remove(RemovalReason reason) {
      if (this.level() instanceof ServerLevel level && !ticketsReleased
         && reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.CHANGED_DIMENSION) {
         releaseTickets(level);
      }
      super.remove(reason);
   }

   private void releaseTickets(ServerLevel level) {
      if (ticketsReleased) return;
      ticketsReleased = true;
      for (long packed : new ArrayList<>(forcedChunks)) {
         safeForceChunk(level, packed, false);
      }
      forcedChunks.clear();
   }

   private void updateChunkTickets(ServerLevel level, double distance, ArashCombatRules.StellaProfile profile) {
      if (ticketsReleased || profile == null) return;
      if (stageTicks - lastTicketRefreshTick < CHUNK_TICKET_REFRESH_TICKS) return;
      lastTicketRefreshTick = stageTicks;

      LinkedHashSet<Long> desired = new LinkedHashSet<>();
      double from = Math.max(0.0, distance - CHUNK_TICKET_BACK_DISTANCE);
      double to = Math.min(profile.length(), distance + CHUNK_TICKET_AHEAD_DISTANCE);
      Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
      for (double sampleDistance = from; sampleDistance <= to + 1.0E-6; sampleDistance += 8.0) {
         Vec3 point = origin.add(direction.scale(sampleDistance));
         for (double offset : new double[]{-profile.scarRadius(), -profile.terrainRadius(), 0.0,
            profile.terrainRadius(), profile.scarRadius()}) {
            Vec3 sample = point.add(side.scale(offset));
            desired.add(ChunkPos.asLong((int)Math.floor(sample.x) >> 4, (int)Math.floor(sample.z) >> 4));
         }
      }
      if (to >= profile.length() - 1.0E-6) {
         Vec3 end = origin.add(direction.scale(profile.length()));
         int minX = (int)Math.floor(end.x - profile.endRadius()) >> 4;
         int maxX = (int)Math.floor(end.x + profile.endRadius()) >> 4;
         int minZ = (int)Math.floor(end.z - profile.endRadius()) >> 4;
         int maxZ = (int)Math.floor(end.z + profile.endRadius()) >> 4;
         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) desired.add(ChunkPos.asLong(x, z));
         }
      }

      for (long packed : new ArrayList<>(forcedChunks)) {
         if (!desired.contains(packed)) {
            safeForceChunk(level, packed, false);
            forcedChunks.remove(packed);
         }
      }
      for (long packed : desired) {
         if (!forcedChunks.contains(packed) && safeForceChunk(level, packed, true)) {
            forcedChunks.add(packed);
         }
      }
   }

   private boolean safeForceChunk(ServerLevel level, long packed, boolean add) {
      if (level == null || level.isClientSide()) return false;
      try {
         CHUNK_TICKETS.forceChunk(level, this, ChunkPos.getX(packed), ChunkPos.getZ(packed), add, false);
         return true;
      } catch (RuntimeException exception) {
         LOGGER.warn("Could not {} Arash Stella chunk ticket at {} (entity={})",
            add ? "add" : "remove", packed, getUUID(), exception);
         return false;
      }
   }

   private static boolean hasActiveController(ServerLevel level, UUID casterId) {
      if (level == null || casterId == null) return false;
      Entity caster = level.getEntity(casterId);
      if (caster == null) return false;
      for (ArashStellaControllerEntity controller : level.getEntitiesOfClass(
         ArashStellaControllerEntity.class,
         caster.getBoundingBox().inflate(8.0),
         candidate -> !candidate.isRemoved() && casterId.equals(candidate.casterId))) {
         return true;
      }
      return false;
   }

   private static void validateLoadedTickets(ServerLevel level, TicketHelper helper) {
      for (UUID owner : List.copyOf(helper.getEntityTickets().keySet())) {
         if (!(level.getEntity(owner) instanceof ArashStellaControllerEntity)) {
            helper.removeAllTickets(owner);
         }
      }
   }

   private void stopLongChantSound(ServerLevel level) {
      stopSound(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", "arash_voice_stella"));
   }

   private void stopAllStellaSounds(ServerLevel level) {
      stopLongChantSound(level);
      stopSound(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", "arash_voice_stella_short"));
   }

   private void stopSound(ServerLevel level, ResourceLocation sound) {
      SoundSource source = playerCaster ? SoundSource.VOICE : SoundSource.HOSTILE;
      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(this) <= 256.0 * 256.0) player.connection.send(new ClientboundStopSoundPacket(sound, source));
      }
   }

   private static void playShortStella(ServerLevel level, ServerPlayer player) {
      player.playNotifySound(ModSounds.ARASH_VOICE_STELLA_SHORT.get(), SoundSource.VOICE, 1.6F, 1.0F);
      level.playSound(player, player.blockPosition(), ModSounds.ARASH_VOICE_STELLA_SHORT.get(),
         SoundSource.VOICE, 1.6F, 1.0F);
   }

   private LivingEntity getCaster(ServerLevel level) {
      return casterId != null && level.getEntity(casterId) instanceof LivingEntity living ? living : null;
   }

   private static Vec3 weightedAim(ArashEntity arash, List<LivingEntity> targets) {
      Vec3 sum = Vec3.ZERO;
      double weights = 0.0;
      for (LivingEntity target : targets) {
         double weight = ArashCombatHelper.isHighThreat(target) ? 3.0 : 1.0;
         double travel = Math.min(80.0, arash.distanceTo(target)) / 3.0;
         sum = sum.add(target.position().add(target.getDeltaMovement().scale(travel)).scale(weight));
         weights += weight;
      }
      return weights > 0.0 ? sum.scale(1.0 / weights) : arash.position().add(arash.getLookAngle().scale(50.0));
   }

   private static boolean insideWorldBorder(ServerLevel level, Vec3 start, Vec3 end, Vec3 direction) {
      Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
      for (double sideOffset : new double[]{-ArashCombatRules.STELLA_SCAR_RADIUS, ArashCombatRules.STELLA_SCAR_RADIUS}) {
         Vec3 point = start.add(side.scale(sideOffset));
         if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(point))) return false;
      }
      for (double sideOffset : new double[]{-50.0, 50.0}) {
         Vec3 point = end.add(side.scale(sideOffset));
         if (!level.getWorldBorder().isWithinBounds(BlockPos.containing(point))) return false;
      }
      return true;
   }

   private static boolean hasFriendlyInBlastPath(ServerLevel level, LivingEntity caster, Vec3 start, Vec3 end) {
      AABB area = pathBounds(start, end, ArashCombatRules.STELLA_END_RADIUS);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
         living -> living != caster && living.isAlive() && (caster.isAlliedTo(living) || living.isAlliedTo(caster)))) {
         if (distanceToSegment(living.position().add(0.0, living.getBbHeight() * 0.5, 0.0), start, end)
            <= ArashCombatRules.STELLA_TERRAIN_RADIUS
            || living.position().distanceToSqr(end) <= ArashCombatRules.STELLA_END_RADIUS * ArashCombatRules.STELLA_END_RADIUS) return true;
      }
      return false;
   }

   private static List<Long> computeChunks(Vec3 origin, Vec3 direction, ArashCombatRules.StellaProfile profile) {
      LinkedHashSet<Long> result = new LinkedHashSet<>();
      Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
      for (double distance = 0.0; distance <= profile.length(); distance += 8.0) {
         Vec3 point = origin.add(direction.scale(distance));
         for (double offset : new double[]{-profile.scarRadius(), -profile.terrainRadius(), 0.0,
            profile.terrainRadius(), profile.scarRadius()}) {
            Vec3 sample = point.add(side.scale(offset));
            result.add(ChunkPos.asLong(((int)Math.floor(sample.x)) >> 4, ((int)Math.floor(sample.z)) >> 4));
         }
      }
      Vec3 end = origin.add(direction.scale(profile.length()));
      int minX = ((int)Math.floor(end.x - profile.endRadius())) >> 4;
      int maxX = ((int)Math.floor(end.x + profile.endRadius())) >> 4;
      int minZ = ((int)Math.floor(end.z - profile.endRadius())) >> 4;
      int maxZ = ((int)Math.floor(end.z + profile.endRadius())) >> 4;
      for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) result.add(ChunkPos.asLong(x, z));
      return new ArrayList<>(result);
   }

   private static AABB pathBounds(Vec3 start, Vec3 end, double inflation) {
      return new AABB(Math.min(start.x, end.x), Math.min(start.y, end.y), Math.min(start.z, end.z),
         Math.max(start.x, end.x), Math.max(start.y, end.y), Math.max(start.z, end.z)).inflate(inflation);
   }

   private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
      Vec3 segment = end.subtract(start);
      double lengthSqr = segment.lengthSqr();
      if (lengthSqr < 1.0E-8) return point.distanceTo(start);
      double t = Math.max(0.0, Math.min(1.0, point.subtract(start).dot(segment) / lengthSqr));
      return point.distanceTo(start.add(segment.scale(t)));
   }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("Stage", stage);
      tag.putInt("StageTicks", stageTicks);
      tag.putInt("PreloadIndex", preloadIndex);
      tag.putDouble("OriginX", origin.x); tag.putDouble("OriginY", origin.y); tag.putDouble("OriginZ", origin.z);
      tag.putDouble("DirectionX", direction.x); tag.putDouble("DirectionY", direction.y); tag.putDouble("DirectionZ", direction.z);
      if (casterId != null) tag.putUUID("Caster", casterId);
      tag.putString("Team", teamName);
      tag.putLongArray("Chunks", chunks);
      tag.putLongArray("ForcedChunks", new ArrayList<>(forcedChunks));
      tag.putBoolean("Released", released);
      tag.putBoolean("PlayerCaster", playerCaster);
      tag.putBoolean("PlayerReleaseRequested", playerReleaseRequested);
      tag.putInt("ReleaseRequestTick", releaseRequestTick);
      tag.putInt("PlayerChargeTicks", playerChargeTicks);
      tag.putBoolean("FinalDamage", finalDamageDone);
      tag.put("Allies", writeUuids(allies));
      tag.put("LineHits", writeUuids(lineHits));
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      stage = Math.max(STAGE_CHANT, Math.min(STAGE_EXPLOSION, tag.getInt("Stage")));
      stageTicks = Math.max(0, tag.getInt("StageTicks"));
      preloadIndex = Math.max(0, tag.getInt("PreloadIndex"));
      Vec3 loadedOrigin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
      Vec3 loadedDirection = new Vec3(tag.getDouble("DirectionX"), tag.getDouble("DirectionY"), tag.getDouble("DirectionZ"));
      origin = finite(loadedOrigin) ? loadedOrigin : position();
      Vec3 horizontalDirection = new Vec3(loadedDirection.x, 0.0, loadedDirection.z);
      direction = finite(horizontalDirection) && horizontalDirection.lengthSqr() > 1.0E-6
         ? horizontalDirection.normalize() : new Vec3(0.0, 0.0, 1.0);
      if (tag.hasUUID("Caster")) casterId = tag.getUUID("Caster");
      teamName = tag.getString("Team");
      chunks.clear(); for (long packed : tag.getLongArray("Chunks")) chunks.add(packed);
      forcedChunks.clear();
      for (long packed : tag.getLongArray("ForcedChunks")) forcedChunks.add(packed);
      if (!tag.contains("ForcedChunks", Tag.TAG_LONG_ARRAY)) {
         for (int i = 0; i < Math.min(preloadIndex, chunks.size()); i++) forcedChunks.add(chunks.get(i));
      }
      released = tag.getBoolean("Released"); finalDamageDone = tag.getBoolean("FinalDamage");
      playerCaster = tag.getBoolean("PlayerCaster");
      playerReleaseRequested = tag.getBoolean("PlayerReleaseRequested");
      releaseRequestTick = tag.contains("ReleaseRequestTick") ? tag.getInt("ReleaseRequestTick") : -1;
      playerChargeTicks = tag.contains("PlayerChargeTicks") ? tag.getInt("PlayerChargeTicks")
         : ArashCombatRules.PLAYER_STELLA_FULL_CHARGE_TICKS;
      readUuids(tag.getList("Allies", Tag.TAG_INT_ARRAY), allies);
      readUuids(tag.getList("LineHits", Tag.TAG_INT_ARRAY), lineHits);
      lastTicketRefreshTick = -CHUNK_TICKET_REFRESH_TICKS;
   }

   private static boolean finite(Vec3 value) {
      return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
   }

   private static ListTag writeUuids(Set<UUID> ids) {
      ListTag list = new ListTag();
      for (UUID id : ids) list.add(NbtUtils.createUUID(id));
      return list;
   }

   private static void readUuids(ListTag list, Set<UUID> output) {
      output.clear();
      for (Tag value : list) output.add(NbtUtils.loadUUID(value));
   }
}

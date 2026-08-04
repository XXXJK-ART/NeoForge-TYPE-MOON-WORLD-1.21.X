package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderPossessionInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorseEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorsemanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ConceptSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SoulEchoEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCorruptionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderEntityIndex;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.RatSwarmRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.SoulLibrary;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.SoulSnapshot;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ServantCardPaleRiderSkills {
   public static final String PROXY_TAG = "PaleRiderCardProxyUuid";
   public static final String HOST_TAG = "PaleRiderCardHostUuid";
   public static final String STEALTH_TAG = "PaleRiderCardPerfectStealth";
   private static final String STEALTH_UNTIL_TAG = "PaleRiderCardPerfectStealthUntil";
   public static final String SPAWN_MODE_TAG = "PaleRiderCardSpawnMode";
   public static final String COMMAND_TAG = "PaleRiderCardCommand";
   public static final int COMMAND_FREE = 0;
   public static final int COMMAND_HOLD = 1;
   public static final int COMMAND_ATTACK = 2;
   public static final int COMMAND_GATHER = 3;
   public static final int COMMAND_LETHAL = 4;
   public static final String UNDERWORLD_TAG = "PaleRiderCardUnderworldActive";
   public static final String CALAMITY_TAG = "PaleRiderCardCalamityActive";
   public static final String DOMAIN_PROXY_TAG = "PaleRiderCardDomainProxyUuid";
   private static final String SOUL_LIBRARY_TAG = "PaleRiderCardSoulLibrary";
   private static final String LAST_CALAMITY_TICK_TAG = "PaleRiderCardLastCalamityTick";
   private static final Map<ServerPlayer, Long> LAST_POSSESSION_INPUT_TICKS = new WeakHashMap<>();
   private static final Map<ServerPlayer, PaleRiderStateMessage> LAST_SENT_STATES = new WeakHashMap<>();

   private ServantCardPaleRiderSkills() {}

   public static void initialize(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      LAST_POSSESSION_INPUT_TICKS.remove(player);
      LAST_SENT_STATES.remove(player);
      removeLegacyProxy(player);
      PaleRiderInfectionService.cleanse(player, false);
      player.getPersistentData().putInt(SPAWN_MODE_TAG, 0);
      player.getPersistentData().putInt(COMMAND_TAG, 0);
      player.getPersistentData().remove(STEALTH_TAG);
      player.getPersistentData().remove(STEALTH_UNTIL_TAG);
      player.getPersistentData().remove(HOST_TAG);
      player.getPersistentData().remove(UNDERWORLD_TAG);
      player.getPersistentData().remove(CALAMITY_TAG);
      player.getPersistentData().remove(DOMAIN_PROXY_TAG);
      player.noPhysics = false;
   }

   public static void clear(ServerPlayer player) {
      LAST_POSSESSION_INPUT_TICKS.remove(player);
      LAST_SENT_STATES.remove(player);
      removeLegacyProxy(player);
      releasePossession(player);
      if (player.level() instanceof ServerLevel level) {
         returnAllLivingSouls(player, level);
         for (LivingEntity entity : controlled(level, player.getUUID())) {
            if (entity instanceof OwnedPaleRiderMob || entity instanceof PaleRiderCrowEntity) entity.discard();
            else PaleRiderInfectionService.cleanse(entity, false);
         }
         cleanupDomainEntities(player, level);
      }
      PaleRiderCorruptionService.end(player);
      player.getPersistentData().remove(STEALTH_TAG);
      player.getPersistentData().remove(STEALTH_UNTIL_TAG);
      player.getPersistentData().remove(UNDERWORLD_TAG);
      player.getPersistentData().remove(CALAMITY_TAG);
      player.getPersistentData().remove(DOMAIN_PROXY_TAG);
      player.getPersistentData().remove(LAST_CALAMITY_TICK_TAG);
      player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
      player.noPhysics = false;
      sendIfSupported(player, new PaleRiderOpenScreenMessage(5, List.of()));
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"pale_rider".equals(vars.servant_card_id)) return;
      if (player.getPersistentData().hasUUID(PROXY_TAG)) removeLegacyProxy(player);
      tickPerfectConcealment(player);
      Mob host = findStoredHost(player);
      if (host == null) {
         if (player.getPersistentData().hasUUID(HOST_TAG)) {
            host = possessNearestControlled(player);
            if (host == null) releasePossession(player);
         }
         player.noPhysics = false;
         ServantCardConcealmentHelper.maintain(player, 40);
      } else {
         if (player.getVehicle() != host && !player.startRiding(host, true)) {
            releasePossession(player);
            return;
         }
         player.noPhysics = false;
         ServantCardConcealmentHelper.maintain(player, 40);
      }

      if (player.level() instanceof ServerLevel level) {
         if (player.tickCount % 10 == Math.floorMod(player.getId(), 10)) tickDomainMounts(player, level);
         LivingEntity domainAnchor = getDomainAnchor(player);
         if (domainAnchor != null) {
            if (isUnderworldActive(player) && player.tickCount % 10 == 0) {
               // Refill vacant domain slots from the strongest remaining stored souls.
               manifestStoredSouls(player, level, domainAnchor);
            }
            if (isUnderworldActive(player) && player.tickCount % 80 == 0) {
               VFXServerEffects.spawn(level, "pale_rider_underworld_sustain", domainAnchor, 64.0);
            }
            if (isCalamityActive(player)) {
               tickCalamity(player, vars, level, domainAnchor);
               if (player.tickCount % 80 == 0) VFXServerEffects.spawn(level, "pale_rider_calamity_sustain", domainAnchor, 40.0);
            }
         }
          if (player.tickCount % 20 == 0) {
             PaleRiderStateMessage state = new PaleRiderStateMessage(countControlled(level, player.getUUID()), host != null,
                isUnderworldActive(player), isCalamityActive(player), player.getPersistentData().getBoolean(STEALTH_TAG));
             if (!state.equals(LAST_SENT_STATES.get(player)) || player.tickCount % 100 == 0) {
                sendIfSupported(player, state);
                LAST_SENT_STATES.put(player, state);
             }
          }
      }
   }

   public static boolean spawnMenu(ServerPlayer player, boolean crouching) {
      if (!isActiveCard(player)) return false;
      sendIfSupported(player, new PaleRiderOpenScreenMessage(crouching ? 1 : 0, List.of()));
      return true;
   }

   public static boolean spawn(ServerPlayer player, int mode) {
      if (!isActiveCard(player)) return false;
      if (!(player.level() instanceof ServerLevel level)) return false;
      int spawnCount = mode == 1 ? 5 : 1;
      if (!PaleRiderInfectionService.hasControlCapacity(player, spawnCount)) return false;
      if (mode == 0 || mode == 2) {
         RatSwarmEntity swarm = ModEntities.RAT_SWARM.get().create(level);
         if (swarm == null) return false;
         swarm.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
         swarm.setPaleRiderOwner(player);
         swarm.setSwarmHealth(mode == 0 ? RatSwarmRules.MAX_HEALTH : RatSwarmRules.HEALTH_PER_RAT);
         if (mode == 2) PaleRiderInfectionService.markStationaryAnchor(swarm);
         level.addFreshEntity(swarm);
         PaleRiderInfectionService.forceControl(swarm, player);
      } else if (mode == 1 || mode == 3) {
         List<PaleRiderCrowEntity> spawned = new java.util.ArrayList<>(spawnCount);
         for (int index = 0; index < spawnCount; index++) {
            PaleRiderCrowEntity crow = ModEntities.PALE_RIDER_CROW.get().create(level);
            if (crow == null) {
               spawned.forEach(Entity::discard);
               return false;
            }
            double angle = Math.PI * 2.0 * index / spawnCount;
            double radius = spawnCount == 1 ? 0.0 : 1.25;
            crow.moveTo(player.getX() + Math.cos(angle) * radius, player.getY() + 1.5,
               player.getZ() + Math.sin(angle) * radius, player.getYRot(), 0.0F);
            crow.setPaleRiderOwner(player);
            if (mode == 3) PaleRiderInfectionService.markStationaryAnchor(crow);
            if (!level.addFreshEntity(crow)) {
               crow.discard();
               spawned.forEach(Entity::discard);
               return false;
            }
            spawned.add(crow);
            PaleRiderInfectionService.forceControl(crow, player);
         }
      } else {
         return false;
      }
      return true;
   }

   public static boolean openPossession(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      if (findStoredHost(player) != null) {
         releasePossession(player);
         sendIfSupported(player, new PaleRiderOpenScreenMessage(5, List.of()));
         return true;
      }
      sendIfSupported(player, new PaleRiderOpenScreenMessage(2, controlledMobs(player).stream()
         .filter(mob -> !PaleRiderInfectionService.isForbiddenPossessionHost(mob))
         .map(mob -> new PaleRiderOpenScreenMessage.Target(mob.getId(), mob.blockPosition().getX(), mob.blockPosition().getZ(), mob.getDisplayName().getString())).toList()));
      return true;
   }

   public static boolean possess(ServerPlayer player, int entityId) {
      if (!isActiveCard(player)) return false;
      if (!(player.level() instanceof ServerLevel level)) return false;
      Entity entity = level.getEntity(entityId);
      if (!(entity instanceof Mob mob) || PaleRiderInfectionService.isForbiddenPossessionHost(mob)
         || !isControlledBy(mob, player.getUUID())) return false;
      releasePossession(player);
      if (!player.startRiding(mob, true)) return false;
      mob.getPersistentData().putBoolean("PaleRiderPossessed", true);
      mob.getPersistentData().putDouble("PaleRiderPossessionBaseY", mob.getY());
      player.getPersistentData().putUUID(HOST_TAG, mob.getUUID());
      return true;
   }

   public static boolean releasePossession(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      UUID hostUuid = player.getPersistentData().hasUUID(HOST_TAG) ? player.getPersistentData().getUUID(HOST_TAG) : null;
      Mob host = player.level() instanceof ServerLevel level && hostUuid != null && level.getEntity(hostUuid) instanceof Mob mob ? mob : null;
      if (host != null) host.getPersistentData().remove("PaleRiderPossessed");
      boolean hadHost = hostUuid != null;
      player.getPersistentData().remove(HOST_TAG);
      if (player.getVehicle() != null && hostUuid != null && hostUuid.equals(player.getVehicle().getUUID())) player.stopRiding();
      player.noPhysics = false;
      return hadHost;
   }

   public static boolean redirectPossessedDamage(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float amount) {
      Mob host = getPossessedHost(player);
      if (host == null || !host.isAlive()) return false;
      if (source.getEntity() != host && source.getDirectEntity() != host) {
         host.invulnerableTime = 0;
         host.hurt(source, amount);
      }
      return true;
   }

   public static void applyPossessionInput(ServerPlayer player, PaleRiderPossessionInputMessage input) {
      if (!isActiveCard(player) || input == null
         || !Float.isFinite(input.forward()) || !Float.isFinite(input.strafe())
         || !Float.isFinite(input.vertical()) || !Float.isFinite(input.yaw())
         || !Float.isFinite(input.pitch())) return;
      long now = player.level().getGameTime();
      Long previousTick = LAST_POSSESSION_INPUT_TICKS.put(player, now);
      if (previousTick != null && previousTick == now) return;
      Mob host = getPossessedHost(player);
      if (host == null || !host.isAlive()) return;
      if (PaleRiderInfectionService.isStationaryAnchor(host)) {
         PaleRiderInfectionService.holdStationaryAnchor(host);
         return;
      }
      float yaw = input.yaw();
      float pitch = Math.max(-90.0F, Math.min(90.0F, input.pitch()));
      host.setYRot(yaw);
      host.setYHeadRot(yaw);
      host.setXRot(pitch);
      double angle = Math.toRadians(yaw);
      Vec3 forward = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 move = forward.scale(clampInput(input.forward())).add(right.scale(clampInput(input.strafe())));
      if (move.lengthSqr() > 1.0) move = move.normalize();
      double y = host.getDeltaMovement().y;
      if (host instanceof PaleRiderCrowEntity) {
         double baseY = host.getPersistentData().getDouble("PaleRiderPossessionBaseY");
         float vertical = host.getY() >= baseY + 5.0 && input.vertical() > 0.0F ? 0.0F : input.vertical();
         y = Math.max(-0.35, Math.min(0.35, vertical * 0.35));
      } else if (input.vertical() > 0.5F && host.onGround()) y = 0.42;
      double speed = host instanceof PaleRiderCrowEntity ? 0.35 : 0.22;
      host.setDeltaMovement(move.x * speed, y, move.z * speed);
      host.hasImpulse = true;
   }

   public static boolean openCommand(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      sendIfSupported(player, new PaleRiderOpenScreenMessage(3, List.of()));
      return true;
   }

   public static void sendIfSupported(ServerPlayer player, CustomPacketPayload payload) {
      if (NetworkRegistry.hasChannel(player.connection, payload.type().id())) {
         PacketDistributor.sendToPlayer(player, payload);
      }
   }

   public static void setCommand(ServerPlayer player, int command) {
      if (!isActiveCard(player)) return;
      player.getPersistentData().putInt(COMMAND_TAG, Math.max(COMMAND_FREE, Math.min(COMMAND_LETHAL, command)));
   }

   public static void togglePerfectConcealment(ServerPlayer player) {
      if (!isActiveCard(player)) return;
      player.getPersistentData().putBoolean(STEALTH_TAG, true);
      player.getPersistentData().putLong(STEALTH_UNTIL_TAG, player.level().getGameTime() + 200L);
   }

   public static boolean transfer(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      HitResult hit = player.pick(64.0, 0.0F, false);
      if (!(hit instanceof BlockHitResult block)) return false;
      BlockPos target = block.getBlockPos().relative(block.getDirection());
      if (!player.level().noCollision(player, player.getBoundingBox().move(target.getX() + 0.5 - player.getX(), target.getY() - player.getY(), target.getZ() + 0.5 - player.getZ()))) return false;
      player.teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
      return true;
   }

   public static boolean plagueRush(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 0.01) return false;
      dir = dir.normalize();
      LivingEntity actor = getPossessedHost(player);
      if (actor == null) actor = player;
      final LivingEntity attackActor = actor;
      attackActor.setDeltaMovement(dir.x * 1.35, 0.2, dir.z * 1.35);
      attackActor.hasImpulse = true;
      if (player.level() instanceof ServerLevel level) {
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, attackActor.getBoundingBox().inflate(2.0),
            entity -> entity != attackActor && !PaleRiderInfectionService.arePaleRiderAllies(player, entity) && !EntityUtils.isImmunePlayerTarget(entity))) {
            target.hurt(player.damageSources().playerAttack(player), 12.0F);
            PaleRiderInfectionService.infect(target, player, 5);
         }
      }
      return true;
   }

   public static boolean ashStep(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      HitResult hit = player.pick(16.0, 0.0F, false);
      if (!(hit instanceof BlockHitResult block)) return false;
      BlockPos pos = block.getBlockPos().relative(block.getDirection());
      player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      return true;
   }

   public static boolean deathPulse(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      if (!(player.level() instanceof ServerLevel level)) return false;
      LivingEntity actor = getPossessedHost(player);
      if (actor == null) actor = player;
      final LivingEntity pulseActor = actor;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, pulseActor.getBoundingBox().inflate(5.0),
         entity -> entity != pulseActor && !PaleRiderInfectionService.arePaleRiderAllies(player, entity) && !EntityUtils.isImmunePlayerTarget(entity))) {
         target.hurt(player.damageSources().playerAttack(player), 10.0F);
         PaleRiderInfectionService.infect(target, player, 5);
         Vec3 push = target.position().subtract(pulseActor.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() > 0.01) target.push(push.normalize().scale(0.5).add(0.0, 0.15, 0.0));
      }
      return true;
   }

   public static boolean toggleUnderworld(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      LivingEntity previousAnchor = getDomainAnchor(player);
      Vec3 previousOrigin = previousAnchor == null ? player.position() : previousAnchor.position();
      boolean active = !isUnderworldActive(player);
      player.getPersistentData().putBoolean(UNDERWORLD_TAG, active);
      if (player.level() instanceof ServerLevel level) {
         if (!active) returnAllLivingSouls(player, level);
         tickDomainMounts(player, level);
         LivingEntity anchor = getDomainAnchor(player);
         if (active && anchor != null) {
            manifestStoredSouls(player, level, anchor);
            VFXServerEffects.spawn(level, "pale_rider_underworld_open", anchor, 64.0);
         }
         else VFXServerEffects.spawn(level, "pale_rider_underworld_end", previousOrigin, 64.0);
      }
      return true;
   }

   public static boolean toggleCalamity(ServerPlayer player) {
      if (!isActiveCard(player)) return false;
      LivingEntity previousAnchor = getDomainAnchor(player);
      Vec3 previousOrigin = previousAnchor == null ? player.position() : previousAnchor.position();
      boolean active = !isCalamityActive(player);
      player.getPersistentData().putBoolean(CALAMITY_TAG, active);
      if (player.level() instanceof ServerLevel level) {
         if (active) {
            PaleRiderCorruptionService.begin(player);
         } else {
            PaleRiderCorruptionService.end(player);
            cleanupCalamityHorsemen(player, level);
         }
         tickDomainMounts(player, level);
         LivingEntity anchor = getDomainAnchor(player);
         if (active && anchor != null) VFXServerEffects.spawn(level, "pale_rider_calamity_open", anchor, 40.0);
         else VFXServerEffects.spawn(level, "pale_rider_calamity_end", previousOrigin, 40.0);
      }
      return true;
   }

   public static boolean isPossessing(ServerPlayer player) { return getPossessedHost(player) != null; }
   public static boolean isUnderworldActive(ServerPlayer player) { return player.getPersistentData().getBoolean(UNDERWORLD_TAG); }
   public static boolean isCalamityActive(ServerPlayer player) { return player.getPersistentData().getBoolean(CALAMITY_TAG); }

   private static boolean isActiveCard(ServerPlayer player) {
      return player != null && PaleRiderInfectionService.isPaleRiderCardPlayer(player);
   }

   public static List<Mob> controlledMobs(ServerPlayer player) {
      return player.level() instanceof ServerLevel level ? controlled(level, player.getUUID()).stream().filter(Mob.class::isInstance).map(Mob.class::cast).toList() : List.of();
   }

   public static boolean captureSoul(ServerPlayer player, LivingEntity defeated) {
      if (!PaleRiderInfectionService.isPaleRiderCardPlayer(player) || defeated == null || defeated == player
         || defeated instanceof OwnedPaleRiderMob || defeated instanceof PaleRiderCrowEntity
         || defeated instanceof net.minecraft.world.entity.decoration.ArmorStand
         || ServantIdentityHelper.isServantLike(defeated)) return false;
      SoulLibrary library = loadSoulLibrary(player);
      boolean added = library.add(SoulSnapshot.capture(defeated));
      if (added) saveSoulLibrary(player, library);
      return added;
   }

   public static void returnManifestedSoul(ServerPlayer player, SoulEchoEntity echo) {
      if (echo == null || echo.getSnapshot() == null) return;
      SoulLibrary library = loadSoulLibrary(player);
      if (library.add(echo.getSnapshot())) saveSoulLibrary(player, library);
   }

   public static void returnAllLivingSouls(ServerPlayer player, ServerLevel level) {
      List<SoulEchoEntity> echoes = ownedSoulEchoes(player, level);
      if (echoes.isEmpty()) return;
      SoulLibrary library = loadSoulLibrary(player);
      boolean changed = false;
      for (SoulEchoEntity echo : echoes) {
         if (echo.isAlive() && echo.getSnapshot() != null) changed |= library.add(echo.getSnapshot());
         echo.discard();
      }
      if (changed) saveSoulLibrary(player, library);
   }

   public static void cleanupPreviousLevel(ServerPlayer player, ServerLevel level) {
      if (player == null || level == null) return;
      returnAllLivingSouls(player, level);
      for (LivingEntity entity : controlled(level, player.getUUID())) {
         if (entity instanceof OwnedPaleRiderMob || entity instanceof PaleRiderCrowEntity) entity.discard();
         else PaleRiderInfectionService.cleanse(entity, false);
      }
      cleanupDomainEntities(player, level);
      player.getPersistentData().remove(HOST_TAG);
      player.getPersistentData().remove(DOMAIN_PROXY_TAG);
      LAST_POSSESSION_INPUT_TICKS.remove(player);
      LAST_SENT_STATES.remove(player);
   }

   public static LivingEntity findSoulEchoTarget(ServerPlayer player, SoulEchoEntity echo) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      LivingEntity anchor = getDomainAnchor(player);
      if (anchor == null) return null;
      LivingEntity nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, anchor.getBoundingBox().inflate(50.0),
         candidate -> candidate != player && candidate != echo && candidate.isAlive()
            && !PaleRiderInfectionService.arePaleRiderAllies(player, candidate)
            && !EntityUtils.isImmunePlayerTarget(candidate))) {
         double distance = target.distanceToSqr(echo);
         if (distance < nearestDistance) {
            nearest = target;
            nearestDistance = distance;
         }
      }
      return nearest;
   }

   private static void tickPerfectConcealment(ServerPlayer player) {
      if (!player.getPersistentData().getBoolean(STEALTH_TAG)) return;
      if (player.getPersistentData().getLong(STEALTH_UNTIL_TAG) > player.level().getGameTime()) return;
      player.getPersistentData().remove(STEALTH_TAG);
      player.getPersistentData().remove(STEALTH_UNTIL_TAG);
   }

   private static void manifestStoredSouls(ServerPlayer player, ServerLevel level, LivingEntity anchor) {
      List<SoulEchoEntity> active = ownedSoulEchoes(player, level);
      if (active.size() == SoulLibrary.MAX_MANIFESTED_SOULS) return;
      SoulLibrary library = loadSoulLibrary(player);
      boolean changed = false;
      if (active.size() > SoulLibrary.MAX_MANIFESTED_SOULS) {
         for (SoulEchoEntity excess : active.subList(SoulLibrary.MAX_MANIFESTED_SOULS, active.size())) {
            if (excess.isAlive() && excess.getSnapshot() != null) changed |= library.add(excess.getSnapshot());
            excess.discard();
         }
         active = active.subList(0, SoulLibrary.MAX_MANIFESTED_SOULS);
      }
      int slots = Math.max(0, SoulLibrary.MAX_MANIFESTED_SOULS - active.size());
      List<SoulSnapshot> souls = library.takeStrongest(slots);
      if (souls.isEmpty()) {
         if (changed) saveSoulLibrary(player, library);
         return;
      }
      int index = 0;
      for (SoulSnapshot soul : souls) {
         SoulEchoEntity echo = ModEntities.SOUL_ECHO.get().create(level);
         if (echo == null) {
            library.add(soul);
            continue;
         }
         double angle = Math.PI * 2.0 * index++ / Math.max(1, souls.size());
         double radius = 4.0 + (index % 5) * 1.4;
         echo.moveTo(anchor.getX() + Math.cos(angle) * radius, anchor.getY(), anchor.getZ() + Math.sin(angle) * radius, anchor.getYRot(), 0.0F);
         echo.setPaleRiderOwner(player);
         echo.applySnapshot(soul);
         if (!level.addFreshEntity(echo)) library.add(soul);
      }
      saveSoulLibrary(player, library);
   }

   private static SoulLibrary loadSoulLibrary(ServerPlayer player) {
      CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
      SoulLibrary library = new SoulLibrary();
      if (persisted.contains(SOUL_LIBRARY_TAG, Tag.TAG_COMPOUND)) library.load(persisted.getCompound(SOUL_LIBRARY_TAG));
      return library;
   }

   private static void saveSoulLibrary(ServerPlayer player, SoulLibrary library) {
      CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
      persisted.put(SOUL_LIBRARY_TAG, library.save());
      player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
   }

   private static List<SoulEchoEntity> ownedSoulEchoes(ServerPlayer player, ServerLevel level) {
      return PaleRiderEntityIndex.owned(level, player.getUUID(), SoulEchoEntity.class,
         echo -> player.getUUID().equals(echo.getPaleRiderOwnerUuid()));
   }

   private static void tickCalamity(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServerLevel level, LivingEntity domainAnchor) {
      PaleRiderCorruptionService.tickDomain(player, domainAnchor, level);
      long now = level.getGameTime();
      if (now - player.getPersistentData().getLong(LAST_CALAMITY_TICK_TAG) < 20L) return;
      player.getPersistentData().putLong(LAST_CALAMITY_TICK_TAG, now);
      if (vars.servant_card_mana < 10.0) {
         toggleCalamity(player);
         return;
      }
      vars.servant_card_mana -= 10.0;
      List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, domainAnchor.getBoundingBox().inflate(25.0),
         target -> target != player && target != domainAnchor && target.isAlive()
            && !PaleRiderInfectionService.arePaleRiderAllies(player, target) && !EntityUtils.isImmunePlayerTarget(target));
      for (LivingEntity enemy : enemies) {
         enemy.hurt(player.damageSources().source(PaleRiderDamageTypes.FAMINE, player), 5.0F);
         PaleRiderInfectionService.infect(enemy, player, 1);
      }
      if (!enemies.isEmpty() && now % 40L == 0L) {
         LivingEntity target = enemies.get(player.getRandom().nextInt(enemies.size()));
         ConceptSwordEntity sword = new ConceptSwordEntity(level, player, target, 35.0F);
         sword.setPos(domainAnchor.getX(), domainAnchor.getY() + 2.5, domainAnchor.getZ());
         sword.shoot(target.getX() - sword.getX(), target.getY() + target.getBbHeight() * 0.5 - sword.getY(), target.getZ() - sword.getZ(), 1.6F, 1.5F);
         level.addFreshEntity(sword);
      }
   }

   private static void tickDomainMounts(ServerPlayer player, ServerLevel level) {
      if (!isUnderworldActive(player) && !isCalamityActive(player)) {
         cleanupDomainEntities(player, level);
         return;
      }

      boolean possessing = getPossessedHost(player) != null;
      List<ApocalypseHorseEntity> horses = ownedHorses(player, level);
      if (!possessing && !(player.getVehicle() instanceof ApocalypseHorseEntity)) {
         horses.stream().filter(horse -> horse.getPassengers().isEmpty() && horse.distanceToSqr(player) <= 16.0)
            .min((left, right) -> Double.compare(left.distanceToSqr(player), right.distanceToSqr(player)))
            .ifPresent(horse -> player.startRiding(horse, true));
      }
      horses.stream().filter(horse -> horse.getPassengers().isEmpty()).forEach(Entity::discard);
      if (possessing) {
         ApocalypseHorsemanEntity proxy = ensureDomainProxy(player, level);
         if (proxy != null) ensureHorse(player, proxy, level);
      } else {
         removeDomainProxy(player, level);
         ensureHorse(player, player, level);
      }

      if (isCalamityActive(player)) ensureCalamityHorsemen(player, level);
      else cleanupCalamityHorsemen(player, level);
      for (ApocalypseHorsemanEntity horseman : ownedHorsemen(player, level)) ensureHorse(player, horseman, level);
   }

   private static ApocalypseHorsemanEntity ensureDomainProxy(ServerPlayer player, ServerLevel level) {
      ApocalypseHorsemanEntity proxy = getDomainProxy(player, level);
      if (proxy != null) return proxy;
      proxy = ModEntities.APOCALYPSE_HORSEMAN.get().create(level);
      if (proxy == null) return null;
      proxy.setPaleRiderOwner(player);
      proxy.setPaleRiderProxy(true);
      proxy.moveTo(player.getX() + 2.0, player.getY(), player.getZ(), player.getYRot(), 0.0F);
      if (!level.addFreshEntity(proxy)) return null;
      player.getPersistentData().putUUID(DOMAIN_PROXY_TAG, proxy.getUUID());
      rebindDomainEffects(player, level, proxy);
      return proxy;
   }

   private static void ensureCalamityHorsemen(ServerPlayer player, ServerLevel level) {
      List<ApocalypseHorsemanEntity> existing = ownedHorsemen(player, level).stream().filter(entity -> !entity.isPaleRiderProxy()).toList();
      LivingEntity anchor = getDomainAnchor(player);
      if (anchor == null) return;
      for (ApocalypseHorsemanEntity.Calamity calamity : ApocalypseHorsemanEntity.Calamity.values()) {
         List<ApocalypseHorsemanEntity> matching = existing.stream().filter(entity -> entity.getCalamity() == calamity).toList();
         if (!matching.isEmpty()) {
            for (int index = 1; index < matching.size(); index++) discardHorsemanAndMount(matching.get(index));
            continue;
         }
         ApocalypseHorsemanEntity horseman = ModEntities.APOCALYPSE_HORSEMAN.get().create(level);
         if (horseman == null) continue;
         double angle = Math.PI * 2.0 * calamity.ordinal() / 3.0;
         double x = anchor.getX() + Math.cos(angle) * 5.0;
         double z = anchor.getZ() + Math.sin(angle) * 5.0;
         horseman.setPaleRiderOwner(player);
         horseman.setCalamity(calamity);
         horseman.moveTo(x, anchor.getY(), z, anchor.getYRot(), 0.0F);
         level.addFreshEntity(horseman);
      }
   }

   private static void ensureHorse(ServerPlayer player, LivingEntity passenger, ServerLevel level) {
      if (passenger.getVehicle() instanceof ApocalypseHorseEntity horse && player.getUUID().equals(horse.getPaleRiderOwnerUuid())) return;
      ApocalypseHorseEntity horse = ModEntities.APOCALYPSE_HORSE.get().create(level);
      if (horse == null) return;
      horse.setPaleRiderOwner(player);
      horse.moveTo(passenger.getX(), passenger.getY(), passenger.getZ(), passenger.getYRot(), 0.0F);
      horse.setHealth(horse.getMaxHealth());
      if (!level.addFreshEntity(horse) || !passenger.startRiding(horse, true)) horse.discard();
   }

   private static ApocalypseHorsemanEntity getDomainProxy(ServerPlayer player, ServerLevel level) {
      if (player.getPersistentData().hasUUID(DOMAIN_PROXY_TAG)
         && level.getEntity(player.getPersistentData().getUUID(DOMAIN_PROXY_TAG)) instanceof ApocalypseHorsemanEntity proxy
         && proxy.isAlive() && proxy.isPaleRiderProxy() && player.getUUID().equals(proxy.getPaleRiderOwnerUuid())) return proxy;
      ApocalypseHorsemanEntity proxy = ownedHorsemen(player, level).stream().filter(ApocalypseHorsemanEntity::isPaleRiderProxy).findFirst().orElse(null);
      if (proxy == null) player.getPersistentData().remove(DOMAIN_PROXY_TAG);
      else player.getPersistentData().putUUID(DOMAIN_PROXY_TAG, proxy.getUUID());
      return proxy;
   }

   public static LivingEntity getDomainAnchor(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      return getPossessedHost(player) == null ? player : getDomainProxy(player, level);
   }

   public static LivingEntity getDomainFormationLeader(ServerPlayer player) {
      return getDomainAnchor(player);
   }

   private static void removeDomainProxy(ServerPlayer player, ServerLevel level) {
      boolean removed = false;
      for (ApocalypseHorsemanEntity proxy : ownedHorsemen(player, level).stream().filter(ApocalypseHorsemanEntity::isPaleRiderProxy).toList()) {
         discardHorsemanAndMount(proxy);
         removed = true;
      }
      player.getPersistentData().remove(DOMAIN_PROXY_TAG);
      if (removed) rebindDomainEffects(player, level, player);
   }

   private static void rebindDomainEffects(ServerPlayer player, ServerLevel level, LivingEntity anchor) {
      if (isUnderworldActive(player)) VFXServerEffects.spawn(level, "pale_rider_underworld_sustain", anchor, 64.0);
      if (isCalamityActive(player)) VFXServerEffects.spawn(level, "pale_rider_calamity_sustain", anchor, 40.0);
   }

   private static void cleanupCalamityHorsemen(ServerPlayer player, ServerLevel level) {
      for (ApocalypseHorsemanEntity horseman : ownedHorsemen(player, level).stream().filter(entity -> !entity.isPaleRiderProxy()).toList()) {
         discardHorsemanAndMount(horseman);
      }
   }

   private static void cleanupDomainEntities(ServerPlayer player, ServerLevel level) {
      if (player.getVehicle() instanceof ApocalypseHorseEntity horse && player.getUUID().equals(horse.getPaleRiderOwnerUuid())) player.stopRiding();
      for (ApocalypseHorsemanEntity horseman : ownedHorsemen(player, level)) horseman.discard();
      for (ApocalypseHorseEntity horse : ownedHorses(player, level)) horse.discard();
      player.getPersistentData().remove(DOMAIN_PROXY_TAG);
   }

   private static void discardHorsemanAndMount(ApocalypseHorsemanEntity horseman) {
      Entity vehicle = horseman.getVehicle();
      horseman.discard();
      if (vehicle instanceof ApocalypseHorseEntity) vehicle.discard();
   }

   private static List<ApocalypseHorsemanEntity> ownedHorsemen(ServerPlayer player, ServerLevel level) {
      return PaleRiderEntityIndex.owned(level, player.getUUID(), ApocalypseHorsemanEntity.class, entity -> player.getUUID().equals(entity.getPaleRiderOwnerUuid()));
   }

   private static List<ApocalypseHorseEntity> ownedHorses(ServerPlayer player, ServerLevel level) {
      return PaleRiderEntityIndex.owned(level, player.getUUID(), ApocalypseHorseEntity.class, entity -> player.getUUID().equals(entity.getPaleRiderOwnerUuid()));
   }

   private static Mob findStoredHost(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level) || !player.getPersistentData().hasUUID(HOST_TAG)) return null;
      Entity entity = level.getEntity(player.getPersistentData().getUUID(HOST_TAG));
      return entity instanceof Mob mob && mob.isAlive() && !PaleRiderInfectionService.isForbiddenPossessionHost(mob)
         && isControlledBy(mob, player.getUUID()) ? mob : null;
   }

   private static Mob possessNearestControlled(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      Mob nearest = controlledMobs(player).stream()
         .filter(mob -> mob.isAlive() && !PaleRiderInfectionService.isForbiddenPossessionHost(mob))
         .min((left, right) -> Double.compare(left.distanceToSqr(player), right.distanceToSqr(player)))
         .orElse(null);
      if (nearest == null) return null;
      UUID old = player.getPersistentData().hasUUID(HOST_TAG) ? player.getPersistentData().getUUID(HOST_TAG) : null;
      if (old != null && level.getEntity(old) instanceof Mob oldMob) oldMob.getPersistentData().remove("PaleRiderPossessed");
      player.stopRiding();
      if (!player.startRiding(nearest, true)) return null;
      nearest.getPersistentData().putBoolean("PaleRiderPossessed", true);
      nearest.getPersistentData().putDouble("PaleRiderPossessionBaseY", nearest.getY());
      player.getPersistentData().putUUID(HOST_TAG, nearest.getUUID());
      return nearest;
   }

   private static Mob getPossessedHost(ServerPlayer player) {
      Mob host = findStoredHost(player);
      return host != null && player.getVehicle() == host ? host : null;
   }

   private static void removeLegacyProxy(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level && player.getPersistentData().hasUUID(PROXY_TAG)
         && level.getEntity(player.getPersistentData().getUUID(PROXY_TAG)) instanceof PaleRiderEntity proxy
         && proxy.getPersistentData().getBoolean("PaleRiderCardProxy")) proxy.discard();
      player.getPersistentData().remove(PROXY_TAG);
   }

   private static List<LivingEntity> controlled(ServerLevel level, UUID owner) {
      return PaleRiderEntityIndex.controlled(level, owner, entity -> isControlledBy(entity, owner));
   }

   private static int countControlled(ServerLevel level, UUID owner) {
      return PaleRiderEntityIndex.controlledCount(level, owner, entity -> isControlledBy(entity, owner)
         && (PaleRiderInfectionService.isControlled(entity) || entity instanceof RatSwarmEntity || entity instanceof PaleRiderCrowEntity));
   }
   private static float clampInput(float value) { return Math.max(-1.0F, Math.min(1.0F, value)); }

   private static boolean isControlledBy(LivingEntity entity, UUID owner) {
      return entity != null && entity.isAlive() && entity instanceof Mob && !(entity instanceof Player) && !(entity instanceof ServantEntity)
         && ((entity.getPersistentData().hasUUID(PaleRiderInfectionService.TAG_OWNER) && owner.equals(entity.getPersistentData().getUUID(PaleRiderInfectionService.TAG_OWNER)))
            || (entity instanceof OwnedPaleRiderMob owned && owner.equals(owned.getPaleRiderOwnerUuid()))
            || (entity instanceof PaleRiderCrowEntity crow && owner.equals(crow.getPaleRiderOwnerUuid())));
   }
}

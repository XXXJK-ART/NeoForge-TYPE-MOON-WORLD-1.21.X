package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCorruptionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.RatSwarmRules;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ServantCardPaleRiderSkills {
   public static final String PROXY_TAG = "PaleRiderCardProxyUuid";
   public static final String HOST_TAG = "PaleRiderCardHostUuid";
   public static final String STEALTH_TAG = "PaleRiderCardPerfectStealth";
   private static final String STEALTH_UNTIL_TAG = "PaleRiderCardPerfectStealthUntil";
   public static final String SPAWN_MODE_TAG = "PaleRiderCardSpawnMode";
   public static final String COMMAND_TAG = "PaleRiderCardCommand";
   public static final String UNDERWORLD_TAG = "PaleRiderCardUnderworldActive";
   public static final String CALAMITY_TAG = "PaleRiderCardCalamityActive";
   public static final String DOMAIN_PROXY_TAG = "PaleRiderCardDomainProxyUuid";
   private static final String LAST_CALAMITY_TICK_TAG = "PaleRiderCardLastCalamityTick";
   private static final int MAX_CONTROLLED = 128;

   private ServantCardPaleRiderSkills() {}

   public static void initialize(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
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
      removeLegacyProxy(player);
      releasePossession(player);
      if (player.level() instanceof ServerLevel level) {
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
      PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(5, List.of()), new CustomPacketPayload[0]);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"pale_rider".equals(vars.servant_card_id)) return;
      if (player.getPersistentData().hasUUID(PROXY_TAG)) removeLegacyProxy(player);
      tickPerfectConcealment(player);
      Mob host = findStoredHost(player);
      if (host == null) {
         if (player.getPersistentData().hasUUID(HOST_TAG)) releasePossession(player);
         player.noPhysics = false;
         ServantCardConcealmentHelper.apply(player, 40);
         if (!player.getPersistentData().getBoolean(STEALTH_TAG) && player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.8, player.getZ(), 5, 0.22, 0.7, 0.22, 0.01);
            level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 0.9, player.getZ(), 3, 0.25, 0.7, 0.25, 0.01);
         }
      } else {
         if (player.getVehicle() != host && !player.startRiding(host, true)) {
            releasePossession(player);
            return;
         }
         player.noPhysics = false;
         player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 40, 0, false, false, false));
      }

      if (player.level() instanceof ServerLevel level) {
         if (isUnderworldActive(player) && player.tickCount % 80 == 0) VFXServerEffects.spawn(level, "pale_rider_underworld_sustain", player, 64.0);
         if (isCalamityActive(player)) tickCalamity(player, vars, level);
         tickDomainMounts(player, level);
         if (player.tickCount % 20 == 0) {
            PacketDistributor.sendToPlayer(player, new PaleRiderStateMessage(countControlled(level, player.getUUID()), host != null,
               isUnderworldActive(player), isCalamityActive(player), player.getPersistentData().getBoolean(STEALTH_TAG)), new CustomPacketPayload[0]);
         }
      }
      if (player.tickCount % 5 == 0) vars.syncPlayerVariables(player);
   }

   public static boolean spawnMenu(ServerPlayer player, boolean crouching) {
      PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(crouching ? 1 : 0, List.of()), new CustomPacketPayload[0]);
      return true;
   }

   public static boolean spawn(ServerPlayer player, int mode) {
      if (!(player.level() instanceof ServerLevel level) || countControlled(level, player.getUUID()) >= MAX_CONTROLLED) return false;
      if (mode == 0 || mode == 2) {
         RatSwarmEntity swarm = ModEntities.RAT_SWARM.get().create(level);
         if (swarm == null) return false;
         swarm.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
         swarm.setPaleRiderOwner(player);
         swarm.setSwarmHealth(mode == 0 ? RatSwarmRules.MAX_HEALTH : RatSwarmRules.HEALTH_PER_RAT);
         if (mode == 2) PaleRiderInfectionService.markStationaryAnchor(swarm);
         level.addFreshEntity(swarm);
         PaleRiderInfectionService.forceControl(swarm, player);
      } else {
         PaleRiderCrowEntity crow = ModEntities.PALE_RIDER_CROW.get().create(level);
         if (crow == null) return false;
         crow.moveTo(player.getX(), player.getY() + 1.5, player.getZ(), player.getYRot(), 0.0F);
         crow.setPaleRiderOwner(player);
         if (mode == 3) PaleRiderInfectionService.markStationaryAnchor(crow);
         level.addFreshEntity(crow);
         PaleRiderInfectionService.forceControl(crow, player);
      }
      return true;
   }

   public static boolean openPossession(ServerPlayer player) {
      if (findStoredHost(player) != null) {
         releasePossession(player);
         PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(5, List.of()), new CustomPacketPayload[0]);
         return true;
      }
      PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(2, controlledMobs(player).stream()
         .map(mob -> new PaleRiderOpenScreenMessage.Target(mob.getId(), mob.blockPosition().getX(), mob.blockPosition().getZ(), mob.getDisplayName().getString())).toList()), new CustomPacketPayload[0]);
      return true;
   }

   public static boolean possess(ServerPlayer player, int entityId) {
      if (!(player.level() instanceof ServerLevel level)) return false;
      Entity entity = level.getEntity(entityId);
      if (!(entity instanceof Mob mob) || !isControlledBy(mob, player.getUUID())) return false;
      releasePossession(player);
      if (!player.startRiding(mob, true)) return false;
      mob.getPersistentData().putBoolean("PaleRiderPossessed", true);
      mob.getPersistentData().putDouble("PaleRiderPossessionBaseY", mob.getY());
      player.getPersistentData().putUUID(HOST_TAG, mob.getUUID());
      return true;
   }

   public static boolean releasePossession(ServerPlayer player) {
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
      Mob host = getPossessedHost(player);
      if (host == null || !host.isAlive()) return;
      if (PaleRiderInfectionService.isStationaryAnchor(host)) {
         PaleRiderInfectionService.holdStationaryAnchor(host);
         return;
      }
      float yaw = input.yaw();
      host.setYRot(yaw);
      host.setYHeadRot(yaw);
      host.setXRot(input.pitch());
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
      PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(3, List.of()), new CustomPacketPayload[0]);
      return true;
   }

   public static void setCommand(ServerPlayer player, int command) {
      player.getPersistentData().putInt(COMMAND_TAG, Math.max(0, Math.min(3, command)));
   }

   public static void togglePerfectConcealment(ServerPlayer player) {
      player.getPersistentData().putBoolean(STEALTH_TAG, true);
      player.getPersistentData().putLong(STEALTH_UNTIL_TAG, player.level().getGameTime() + 200L);
   }

   public static boolean transfer(ServerPlayer player) {
      HitResult hit = player.pick(64.0, 0.0F, false);
      if (!(hit instanceof BlockHitResult block)) return false;
      BlockPos target = block.getBlockPos().relative(block.getDirection());
      if (!player.level().noCollision(player, player.getBoundingBox().move(target.getX() + 0.5 - player.getX(), target.getY() - player.getY(), target.getZ() + 0.5 - player.getZ()))) return false;
      player.teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
      return true;
   }

   public static boolean plagueRush(ServerPlayer player) {
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
      HitResult hit = player.pick(16.0, 0.0F, false);
      if (!(hit instanceof BlockHitResult block)) return false;
      BlockPos pos = block.getBlockPos().relative(block.getDirection());
      player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
      return true;
   }

   public static boolean deathPulse(ServerPlayer player) {
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
      boolean active = !isUnderworldActive(player);
      player.getPersistentData().putBoolean(UNDERWORLD_TAG, active);
      if (player.level() instanceof ServerLevel level) {
         tickDomainMounts(player, level);
         VFXServerEffects.spawn(level, active ? "pale_rider_underworld_open" : "pale_rider_underworld_end", player, 64.0);
      }
      return true;
   }

   public static boolean toggleCalamity(ServerPlayer player) {
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
         VFXServerEffects.spawn(level, active ? "pale_rider_calamity_open" : "pale_rider_calamity_end", player, 40.0);
      }
      return true;
   }

   public static boolean isPossessing(ServerPlayer player) { return getPossessedHost(player) != null; }
   public static boolean isUnderworldActive(ServerPlayer player) { return player.getPersistentData().getBoolean(UNDERWORLD_TAG); }
   public static boolean isCalamityActive(ServerPlayer player) { return player.getPersistentData().getBoolean(CALAMITY_TAG); }

   public static List<Mob> controlledMobs(ServerPlayer player) {
      return player.level() instanceof ServerLevel level ? controlled(level, player.getUUID()).stream().filter(Mob.class::isInstance).map(Mob.class::cast).toList() : List.of();
   }

   private static void tickPerfectConcealment(ServerPlayer player) {
      if (!player.getPersistentData().getBoolean(STEALTH_TAG)) return;
      if (player.getPersistentData().getLong(STEALTH_UNTIL_TAG) > player.level().getGameTime()) return;
      player.getPersistentData().remove(STEALTH_TAG);
      player.getPersistentData().remove(STEALTH_UNTIL_TAG);
   }

   private static void tickCalamity(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServerLevel level) {
      PaleRiderCorruptionService.tickDomain(player, level);
      long now = level.getGameTime();
      if (now - player.getPersistentData().getLong(LAST_CALAMITY_TICK_TAG) < 20L) return;
      player.getPersistentData().putLong(LAST_CALAMITY_TICK_TAG, now);
      if (vars.servant_card_mana < 10.0) {
         toggleCalamity(player);
         return;
      }
      vars.servant_card_mana -= 10.0;
      List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(25.0),
         target -> target != player && target.isAlive() && !PaleRiderInfectionService.arePaleRiderAllies(player, target) && !EntityUtils.isImmunePlayerTarget(target));
      for (LivingEntity enemy : enemies) {
         enemy.hurt(player.damageSources().source(PaleRiderDamageTypes.FAMINE, player), 5.0F);
         PaleRiderInfectionService.infect(enemy, player, 1);
      }
      if (!enemies.isEmpty() && now % 40L == 0L) {
         LivingEntity target = enemies.get(player.getRandom().nextInt(enemies.size()));
         level.addFreshEntity(new ConceptSwordEntity(level, player, target, 35.0F));
      }
   }

   private static void tickDomainMounts(ServerPlayer player, ServerLevel level) {
      if (!isUnderworldActive(player) && !isCalamityActive(player)) {
         cleanupDomainEntities(player, level);
         return;
      }

      ownedHorses(player, level).stream().filter(horse -> horse.getPassengers().isEmpty()).forEach(Entity::discard);
      if (getPossessedHost(player) != null) {
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
      return proxy;
   }

   private static void ensureCalamityHorsemen(ServerPlayer player, ServerLevel level) {
      List<ApocalypseHorsemanEntity> existing = ownedHorsemen(player, level).stream().filter(entity -> !entity.isPaleRiderProxy()).toList();
      for (ApocalypseHorsemanEntity.Calamity calamity : ApocalypseHorsemanEntity.Calamity.values()) {
         List<ApocalypseHorsemanEntity> matching = existing.stream().filter(entity -> entity.getCalamity() == calamity).toList();
         if (!matching.isEmpty()) {
            for (int index = 1; index < matching.size(); index++) discardHorsemanAndMount(matching.get(index));
            continue;
         }
         ApocalypseHorsemanEntity horseman = ModEntities.APOCALYPSE_HORSEMAN.get().create(level);
         if (horseman == null) continue;
         double angle = Math.PI * 2.0 * calamity.ordinal() / 3.0;
         double x = player.getX() + Math.cos(angle) * 5.0;
         double z = player.getZ() + Math.sin(angle) * 5.0;
         horseman.setPaleRiderOwner(player);
         horseman.setCalamity(calamity);
         horseman.moveTo(x, player.getY(), z, player.getYRot(), 0.0F);
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

   public static LivingEntity getDomainFormationLeader(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      return getPossessedHost(player) == null ? player : getDomainProxy(player, level);
   }

   private static void removeDomainProxy(ServerPlayer player, ServerLevel level) {
      for (ApocalypseHorsemanEntity proxy : ownedHorsemen(player, level).stream().filter(ApocalypseHorsemanEntity::isPaleRiderProxy).toList()) {
         discardHorsemanAndMount(proxy);
      }
      player.getPersistentData().remove(DOMAIN_PROXY_TAG);
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
      return level.getEntitiesOfClass(ApocalypseHorsemanEntity.class, dimensionBounds(level), entity -> player.getUUID().equals(entity.getPaleRiderOwnerUuid()));
   }

   private static List<ApocalypseHorseEntity> ownedHorses(ServerPlayer player, ServerLevel level) {
      return level.getEntitiesOfClass(ApocalypseHorseEntity.class, dimensionBounds(level), entity -> player.getUUID().equals(entity.getPaleRiderOwnerUuid()));
   }

   private static Mob findStoredHost(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level) || !player.getPersistentData().hasUUID(HOST_TAG)) return null;
      Entity entity = level.getEntity(player.getPersistentData().getUUID(HOST_TAG));
      return entity instanceof Mob mob && mob.isAlive() && isControlledBy(mob, player.getUUID()) ? mob : null;
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
      return level.getEntitiesOfClass(LivingEntity.class, dimensionBounds(level), entity -> isControlledBy(entity, owner));
   }

   private static AABB dimensionBounds(ServerLevel level) {
      return new AABB(-3.0E7, level.getMinBuildHeight(), -3.0E7, 3.0E7, level.getMaxBuildHeight(), 3.0E7);
   }

   private static int countControlled(ServerLevel level, UUID owner) { return controlled(level, owner).size(); }
   private static float clampInput(float value) { return Math.max(-1.0F, Math.min(1.0F, value)); }

   private static boolean isControlledBy(LivingEntity entity, UUID owner) {
      return entity != null && entity.isAlive() && entity instanceof Mob && !(entity instanceof Player) && !(entity instanceof ServantEntity)
         && ((entity.getPersistentData().hasUUID(PaleRiderInfectionService.TAG_OWNER) && owner.equals(entity.getPersistentData().getUUID(PaleRiderInfectionService.TAG_OWNER)))
            || (entity instanceof OwnedPaleRiderMob owned && owner.equals(owned.getPaleRiderOwnerUuid()))
            || (entity instanceof PaleRiderCrowEntity crow && owner.equals(crow.getPaleRiderOwnerUuid())));
   }
}

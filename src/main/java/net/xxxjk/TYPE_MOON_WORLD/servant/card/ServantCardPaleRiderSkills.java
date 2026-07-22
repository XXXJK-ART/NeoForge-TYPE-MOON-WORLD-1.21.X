package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderPossessionInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.RatSwarmRules;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ServantCardPaleRiderSkills {
   public static final String PROXY_TAG = "PaleRiderCardProxyUuid";
   public static final String HOST_TAG = "PaleRiderCardHostUuid";
   public static final String STEALTH_TAG = "PaleRiderCardPerfectStealth";
   public static final String SPAWN_MODE_TAG = "PaleRiderCardSpawnMode";
   public static final String COMMAND_TAG = "PaleRiderCardCommand";
   private static final int MAX_CONTROLLED = 128;

   private ServantCardPaleRiderSkills() {}

   public static void initialize(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      player.getPersistentData().putInt(SPAWN_MODE_TAG, 0);
      player.getPersistentData().putInt(COMMAND_TAG, 0);
      player.getPersistentData().remove(STEALTH_TAG);
      player.noPhysics = false;
      ensureProxy(player);
   }

   public static void clear(ServerPlayer player) {
      PaleRiderEntity proxy = getProxy(player);
      if (proxy != null) {
         proxy.endPossession();
         if (proxy.level() instanceof ServerLevel level) {
            for (LivingEntity entity : controlled(level, proxy.getUUID())) {
               PaleRiderInfectionService.cleanse(entity, false);
               entity.getPersistentData().remove("PaleRiderCardCommand");
            }
         }
         proxy.discard();
      }
      player.getPersistentData().remove(PROXY_TAG);
      player.getPersistentData().remove(HOST_TAG);
      player.getPersistentData().remove(STEALTH_TAG);
      player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
      PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(5, List.of()), new CustomPacketPayload[0]);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"pale_rider".equals(vars.servant_card_id)) return;
      PaleRiderEntity proxy = ensureProxy(player);
      if (proxy == null) return;
      boolean possessed = proxy.hasPossessedHost();
      if (!possessed) {
         player.noPhysics = false;
         proxy.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
         ServantCardConcealmentHelper.apply(player, 40);
         if (!player.getPersistentData().getBoolean(STEALTH_TAG) && player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.8, player.getZ(), 5, 0.22, 0.7, 0.22, 0.01);
            level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 0.9, player.getZ(), 3, 0.25, 0.7, 0.25, 0.01);
         }
      } else if (proxy.getPossessedHost() != null) {
         Mob host = proxy.getPossessedHost();
         player.noPhysics = true;
         player.teleportTo(host.getX(), host.getY(), host.getZ());
         player.setYRot(host.getYRot());
         player.setXRot(host.getXRot());
         player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 40, 0, false, false, false));
      }
      double before = proxy.getCurrentMp();
      PaleRiderCombatHelper.tick(proxy);
      if (possessed && !proxy.hasPossessedHost()) {
         player.getPersistentData().remove(HOST_TAG);
         PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(5, List.of()), new CustomPacketPayload[0]);
      }
      double after = proxy.getCurrentMp();
      if (before != after) vars.servant_card_mana = Math.max(0.0, Math.min(vars.servant_card_max_mana, vars.servant_card_mana + after - before));
      if (player.tickCount % 20 == 0) {
         PacketDistributor.sendToPlayer(player, new PaleRiderStateMessage(countControlled((ServerLevel)player.level(), proxy.getUUID()),
            proxy.hasPossessedHost(), proxy.isUnderworldActive(), proxy.isCalamityActive(), player.getPersistentData().getBoolean(STEALTH_TAG)), new CustomPacketPayload[0]);
      }
      if (player.tickCount % 5 == 0) vars.syncPlayerVariables(player);
   }

   public static boolean spawnMenu(ServerPlayer player, boolean crouching) {
      PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(crouching ? 1 : 0, List.of()), new CustomPacketPayload[0]);
      return true;
   }

   public static boolean spawn(ServerPlayer player, int mode) {
      PaleRiderEntity proxy = ensureProxy(player);
      if (proxy == null || countControlled((ServerLevel)player.level(), proxy.getUUID()) >= MAX_CONTROLLED) return false;
      ServerLevel level = (ServerLevel)player.level();
      if (mode == 0 || mode == 2) {
         RatSwarmEntity swarm = ModEntities.RAT_SWARM.get().create(level);
         if (swarm == null) return false;
         swarm.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
         swarm.setPaleRiderOwner(proxy);
         swarm.setSwarmHealth(mode == 0 ? RatSwarmRules.MAX_HEALTH : RatSwarmRules.HEALTH_PER_RAT);
         level.addFreshEntity(swarm);
         PaleRiderInfectionService.forceControl(swarm, proxy);
      } else {
         PaleRiderCrowEntity crow = ModEntities.PALE_RIDER_CROW.get().create(level);
         if (crow == null) return false;
         crow.moveTo(player.getX(), player.getY() + 1.5, player.getZ(), player.getYRot(), 0.0F);
         crow.setPaleRiderOwner(proxy);
         level.addFreshEntity(crow);
         PaleRiderInfectionService.forceControl(crow, proxy);
      }
      return true;
   }

   public static boolean openPossession(ServerPlayer player) {
      PaleRiderEntity proxy = ensureProxy(player);
      if (proxy == null) return false;
      if (proxy.hasPossessedHost()) {
         releasePossession(player);
         PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(5, List.of()), new CustomPacketPayload[0]);
         return true;
      }
      PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(2,
         controlledMobs(player).stream().map(mob -> new PaleRiderOpenScreenMessage.Target(
            mob.getId(), mob.blockPosition().getX(), mob.blockPosition().getZ(), mob.getDisplayName().getString())).toList()), new CustomPacketPayload[0]);
      return true;
   }

   public static boolean possess(ServerPlayer player, int entityId) {
      PaleRiderEntity proxy = ensureProxy(player);
      if (proxy == null || !(player.level() instanceof ServerLevel level)) return false;
      Entity entity = level.getEntity(entityId);
      if (!(entity instanceof Mob mob) || !isControlledBy(mob, proxy.getUUID())) return false;
      if (!proxy.beginPossession(mob)) return false;
      mob.getPersistentData().putDouble("PaleRiderPossessionBaseY", mob.getY());
      player.getPersistentData().putUUID(HOST_TAG, mob.getUUID());
      return true;
   }

   public static boolean releasePossession(ServerPlayer player) {
      PaleRiderEntity proxy = getProxy(player);
      if (proxy == null) return false;
      Mob host = proxy.getPossessedHost();
      proxy.endPossession();
      player.getPersistentData().remove(HOST_TAG);
      player.noPhysics = false;
      if (host != null && host.isAlive()) player.teleportTo(host.getX(), host.getY(), host.getZ());
      return true;
   }

   public static boolean redirectPossessedDamage(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float amount) {
      PaleRiderEntity proxy = getProxy(player);
      Mob host = proxy == null ? null : proxy.getPossessedHost();
      if (host == null || !host.isAlive() || source.getEntity() == host || source.getDirectEntity() == host) return host != null;
      host.invulnerableTime = 0;
      host.hurt(source, amount);
      return true;
   }

   public static void applyPossessionInput(ServerPlayer player, PaleRiderPossessionInputMessage input) {
      PaleRiderEntity proxy = getProxy(player);
      Mob host = proxy == null ? null : proxy.getPossessedHost();
      if (host == null || !host.isAlive()) return;
      float yaw = input.yaw();
      host.setYRot(yaw);
      host.setYHeadRot(yaw);
      host.setXRot(input.pitch());
      double angle = Math.toRadians(yaw);
      Vec3 forward = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 move = forward.scale(Math.max(-1.0F, Math.min(1.0F, input.forward())))
         .add(right.scale(Math.max(-1.0F, Math.min(1.0F, input.strafe()))));
      if (move.lengthSqr() > 1.0) move = move.normalize();
      double speed = host instanceof PaleRiderCrowEntity ? 0.35 : 0.22;
      double y = host.getDeltaMovement().y;
      if (host instanceof PaleRiderCrowEntity) {
         double baseY = host.getPersistentData().getDouble("PaleRiderPossessionBaseY");
         float vertical = host.getY() >= baseY + 5.0 && input.vertical() > 0.0F ? 0.0F : input.vertical();
         y = Math.max(-0.35, Math.min(0.35, vertical * 0.35));
      }
      else if (input.vertical() > 0.5F && host.onGround()) y = 0.42;
      host.setDeltaMovement(move.x * speed, y, move.z * speed);
      host.hasImpulse = true;
      if (input.attack() && host.level().getGameTime() >= host.getPersistentData().getLong("PaleRiderCardNextAttack")) {
         LivingEntity target = host.level().getEntitiesOfClass(LivingEntity.class, host.getBoundingBox().inflate(3.0), e -> e != host && !proxy.isAlliedTo(e) && !EntityUtils.isImmunePlayerTarget(e)).stream().findFirst().orElse(null);
         if (target != null) {
            host.getPersistentData().putLong("PaleRiderCardNextAttack", host.level().getGameTime() + 10L);
            host.doHurtTarget(target);
         }
      }
   }

   public static boolean openCommand(ServerPlayer player) {
      if (ensureProxy(player) == null) return false;
      PacketDistributor.sendToPlayer(player, new PaleRiderOpenScreenMessage(3, List.of()), new CustomPacketPayload[0]);
      return true;
   }

   public static void setCommand(ServerPlayer player, int command) {
      player.getPersistentData().putInt(COMMAND_TAG, Math.max(0, Math.min(3, command)));
   }

   public static void togglePerfectConcealment(ServerPlayer player) {
      boolean active = !player.getPersistentData().getBoolean(STEALTH_TAG);
      player.getPersistentData().putBoolean(STEALTH_TAG, active);
      if (!active) player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
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
      PaleRiderEntity proxy = ensureProxy(player);
      if (proxy == null) return false;
      Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 0.01) return false;
      dir = dir.normalize();
      LivingEntity actor = proxy.getPossessedHost();
      if (actor == null) actor = player;
      final LivingEntity attackActor = actor;
      attackActor.setDeltaMovement(dir.x * 1.35, 0.2, dir.z * 1.35);
      attackActor.hasImpulse = true;
      if (player.level() instanceof ServerLevel level) {
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, attackActor.getBoundingBox().inflate(2.0), e -> e != attackActor && !EntityUtils.isImmunePlayerTarget(e) && !proxy.isAlliedTo(e))) {
            target.hurt(proxy.damageSources().mobAttack(proxy), 12.0F);
            PaleRiderInfectionService.infect(target, proxy, 5);
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
      PaleRiderEntity proxy = ensureProxy(player);
      if (proxy == null || !(player.level() instanceof ServerLevel level)) return false;
      LivingEntity actor = proxy.getPossessedHost() == null ? player : proxy.getPossessedHost();
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, actor.getBoundingBox().inflate(5.0), e -> e != actor && !proxy.isAlliedTo(e) && !EntityUtils.isImmunePlayerTarget(e))) {
         target.hurt(proxy.damageSources().mobAttack(proxy), 10.0F);
         PaleRiderInfectionService.infect(target, proxy, 5);
         Vec3 push = target.position().subtract(actor.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() > 0.01) target.push(push.normalize().scale(0.5).add(0.0, 0.15, 0.0));
      }
      return true;
   }

   public static boolean toggleUnderworld(ServerPlayer player) {
      PaleRiderEntity proxy = ensureProxy(player);
      if (proxy == null) return false;
      if (proxy.isUnderworldActive()) PaleRiderCombatHelper.stopUnderworldForCard(proxy);
      else PaleRiderCombatHelper.startUnderworldForCard(proxy);
      return true;
   }

   public static boolean toggleCalamity(ServerPlayer player) {
      PaleRiderEntity proxy = ensureProxy(player);
      if (proxy == null) return false;
      if (proxy.isCalamityActive()) PaleRiderCombatHelper.stopCalamityForCard(proxy);
      else PaleRiderCombatHelper.startCalamityForCard(proxy);
      return true;
   }

   public static boolean isPossessing(ServerPlayer player) {
      PaleRiderEntity proxy = getProxy(player);
      return proxy != null && proxy.hasPossessedHost();
   }

   public static boolean isUnderworldActive(ServerPlayer player) {
      PaleRiderEntity proxy = getProxy(player);
      return proxy != null && proxy.isUnderworldActive();
   }

   public static boolean isCalamityActive(ServerPlayer player) {
      PaleRiderEntity proxy = getProxy(player);
      return proxy != null && proxy.isCalamityActive();
   }

   public static List<Mob> controlledMobs(ServerPlayer player) {
      PaleRiderEntity proxy = getProxy(player);
      return proxy == null || !(player.level() instanceof ServerLevel level) ? List.of() : controlled(level, proxy.getUUID()).stream().filter(e -> e instanceof Mob).map(e -> (Mob)e).toList();
   }

   private static PaleRiderEntity ensureProxy(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      PaleRiderEntity existing = getProxy(player);
      if (existing != null && existing.isAlive()) return existing;
      PaleRiderEntity proxy = ModEntities.PALE_RIDER.get().create(level);
      if (proxy == null) return null;
      proxy.setCardOwner(player);
      proxy.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
      proxy.setCurrentMp(1000.0);
      level.addFreshEntity(proxy);
      player.getPersistentData().putUUID(PROXY_TAG, proxy.getUUID());
      return proxy;
   }

   private static PaleRiderEntity getProxy(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level) || !player.getPersistentData().hasUUID(PROXY_TAG)) return null;
      return level.getEntity(player.getPersistentData().getUUID(PROXY_TAG)) instanceof PaleRiderEntity proxy ? proxy : null;
   }

   private static List<LivingEntity> controlled(ServerLevel level, UUID owner) {
      AABB bounds = new AABB(-3.0E7, level.getMinBuildHeight(), -3.0E7, 3.0E7, level.getMaxBuildHeight(), 3.0E7);
      return level.getEntitiesOfClass(LivingEntity.class, bounds, e -> isControlledBy(e, owner));
   }

   private static int countControlled(ServerLevel level, UUID owner) { return controlled(level, owner).size(); }

   private static boolean isControlledBy(LivingEntity entity, UUID owner) {
      return entity != null && entity.isAlive() && entity instanceof Mob && !(entity instanceof Player) && !(entity instanceof ServantEntity)
         && ((entity.getPersistentData().hasUUID(PaleRiderInfectionService.TAG_OWNER) && owner.equals(entity.getPersistentData().getUUID(PaleRiderInfectionService.TAG_OWNER)))
         || (entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.palerider.OwnedPaleRiderMob owned && owner.equals(owned.getPaleRiderOwnerUuid()))
         || (entity instanceof PaleRiderCrowEntity crow && owner.equals(crow.getPaleRiderOwnerUuid())));
   }
}

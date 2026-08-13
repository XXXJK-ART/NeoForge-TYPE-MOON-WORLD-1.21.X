package net.xxxjk.TYPE_MOON_WORLD.vfx;

import java.util.Optional;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.VFXTriggerEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.DuelScreenFlashMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.vfx.network.VFXSpawnEffectMessage;

public final class VFXServerEffects {
   private static final double DEFAULT_RADIUS = 128.0;

   private VFXServerEffects() {
   }

   public static void spawn(ServerLevel level, String effectId, Vec3 origin) {
      spawn(level, effectId, origin, DEFAULT_RADIUS);
   }

   public static void spawn(ServerLevel level, String effectId, Vec3 origin, double radius) {
      VFXSpawnEffectMessage message = new VFXSpawnEffectMessage(
         effectId,
         origin.x,
         origin.y,
         origin.z,
         Optional.empty(),
         level.dimension().location().toString(),
         level.getRandom().nextLong()
      );
      sendToSupportedPlayersNear(level, origin, radius, message);
   }

   public static void spawnOriented(ServerLevel level, String effectId, Vec3 origin, Vec3 direction, double radius) {
      Vec3 dir = direction == null || direction.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : direction.normalize();
      VFXSpawnEffectMessage message = new VFXSpawnEffectMessage(effectId, origin.x, origin.y, origin.z, Optional.empty(),
         level.dimension().location().toString(), level.getRandom().nextLong(), Optional.of(dir));
      sendToSupportedPlayersNear(level, origin, radius, message);
   }

   public static void spawn(ServerLevel level, String effectId, Entity target) {
      spawn(level, effectId, target, DEFAULT_RADIUS);
   }

   public static void spawn(ServerLevel level, String effectId, Entity target, double radius) {
      VFXSpawnEffectMessage message = new VFXSpawnEffectMessage(
         effectId,
         target.getX(),
         target.getY(),
         target.getZ(),
         Optional.of(target.getUUID()),
         level.dimension().location().toString(),
         level.getRandom().nextLong()
      );
      sendToSupportedPlayersNear(level, target.position(), radius, message);
   }

   public static void spawnReplayable(ServerLevel level, String effectId, Vec3 origin, float durationSeconds) {
      long seed = level.getRandom().nextLong();
      VFXTriggerEntity trigger = new VFXTriggerEntity(level, effectId, origin.x, origin.y, origin.z, Mth.ceil((durationSeconds + 0.25F) * 20.0F), seed);
      level.addFreshEntity(trigger);
   }

   public static void spawnReplayable(ServerLevel level, String effectId, Entity target, float durationSeconds) {
      long seed = level.getRandom().nextLong();
      VFXTriggerEntity trigger = new VFXTriggerEntity(
         level,
         effectId,
         target.getX(),
         target.getY(),
         target.getZ(),
         Mth.ceil((durationSeconds + 0.25F) * 20.0F),
         seed,
         target.getId()
      );
      level.addFreshEntity(trigger);
   }

   public static void screenFlash(ServerLevel level, Vec3 origin, double radius, int ticks, float strength) {
      sendToSupportedPlayersNear(level, origin, radius, new DuelScreenFlashMessage(ticks, strength));
   }

   private static void sendToSupportedPlayersNear(ServerLevel level, Vec3 origin, double radius,
                                                   CustomPacketPayload payload) {
      double radiusSqr = radius * radius;
      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(origin) <= radiusSqr) {
            ModNetwork.sendToPlayer(player, payload);
         }
      }
   }
}

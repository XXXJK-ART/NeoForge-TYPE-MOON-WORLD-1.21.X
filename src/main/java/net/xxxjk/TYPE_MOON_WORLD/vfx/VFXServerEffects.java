package net.xxxjk.TYPE_MOON_WORLD.vfx;

import java.util.Optional;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
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
      PacketDistributor.sendToPlayersNear(level, null, origin.x, origin.y, origin.z, radius, message, new CustomPacketPayload[0]);
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
      PacketDistributor.sendToPlayersNear(level, null, target.getX(), target.getY(), target.getZ(), radius, message, new CustomPacketPayload[0]);
   }
}

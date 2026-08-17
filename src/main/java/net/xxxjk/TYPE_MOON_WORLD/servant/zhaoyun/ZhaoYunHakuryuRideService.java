package net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;

public final class ZhaoYunHakuryuRideService {
   private ZhaoYunHakuryuRideService() {
   }

   public static boolean tryToggle(ServerPlayer player, ZhaoYunHakuryuEntity mount) {
      if (player == null || mount == null || !player.isAlive() || !mount.isAlive()
         || player.level() != mount.level() || player.distanceToSqr(mount) > 36.0) {
         return false;
      }
      if (player.getVehicle() == mount) {
         dismountSafely(player, mount);
         return true;
      }
      if (player.isPassenger()) {
         return false;
      }
      mount.sanitizePassengers();
      if (!mount.canPlayerMount(player)) {
         return false;
      }
      if (!player.startRiding(mount, true)) {
         return false;
      }
      player.fallDistance = 0.0F;
      syncPassengers(mount);
      return true;
   }

   public static void dismountSafely(ServerPlayer player, ZhaoYunHakuryuEntity mount) {
      int seatIndex = mount.getPassengers().indexOf(player);
      player.stopRiding();
      if (player.level() == mount.level()) {
         double yaw = Math.toRadians(mount.getYRot());
         Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
         double side = seatIndex == 1 ? -1.35 : 1.35;
         Vec3 preferred = mount.position().add(right.scale(side));
         Vec3 safe = DismountHelper.findSafeDismountLocation(
            player.getType(), player.level(), BlockPos.containing(preferred), true);
         Vec3 destination = safe == null ? preferred : safe;
         player.teleportTo(destination.x, destination.y, destination.z);
      }
      player.fallDistance = 0.0F;
      syncPassengers(mount);
   }

   private static void syncPassengers(Entity vehicle) {
      if (!(vehicle.level() instanceof ServerLevel level)) {
         return;
      }
      ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(vehicle);
      for (ServerPlayer observer : level.players()) {
         if (observer.distanceToSqr(vehicle) <= 64.0 * 64.0) {
            observer.connection.send(packet);
         }
      }
   }
}

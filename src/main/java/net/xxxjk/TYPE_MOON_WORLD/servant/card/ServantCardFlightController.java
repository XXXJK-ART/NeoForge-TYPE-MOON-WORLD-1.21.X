package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardFlightController {
   private static final double MP_PER_TICK = 0.28;

   private ServantCardFlightController() {
   }

   public static boolean canFly(String servantId) {
      return "medea".equals(servantId) || "oda_nobunaga".equals(servantId) || "enkidu".equals(servantId);
   }

   public static void setInput(ServerPlayer player, boolean toggle, double forward, double strafe, double vertical) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return;
      }
      if (toggle) {
         toggleFlight(player, vars);
      }
      vars.servant_card_flight_forward = Mth.clamp(forward, -1.0, 1.0);
      vars.servant_card_flight_strafe = Mth.clamp(strafe, -1.0, 1.0);
      vars.servant_card_flight_vertical = Mth.clamp(vertical, -1.0, 1.0);
      vars.syncPlayerVariables(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_flight_toggle_cooldown > 0) {
         vars.servant_card_flight_toggle_cooldown--;
      }
      if (!vars.servant_card_transformed || !vars.servant_card_flying) {
         if (!vars.servant_card_transformed || !canFly(vars.servant_card_id)) {
            stop(player, vars, false);
         }
         return;
      }
      if (!canFly(vars.servant_card_id) || !ServantCardManaService.consume(player, vars, MP_PER_TICK)) {
         stop(player, vars, true);
         return;
      }
      player.setNoGravity(true);
      player.fallDistance = 0.0F;
      double yaw = Math.toRadians(player.getYRot());
      Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 right = new Vec3(forward.z, 0.0, -forward.x);
      Vec3 movement = forward.scale(vars.servant_card_flight_forward).add(right.scale(vars.servant_card_flight_strafe));
      if (movement.lengthSqr() > 1.0) {
         movement = movement.normalize();
      }
      double horizontalSpeed = switch (vars.servant_card_id) {
         case "enkidu" -> 0.58;
         case "oda_nobunaga" -> 0.54;
         default -> 0.48;
      };
      Vec3 velocity = movement.scale(horizontalSpeed).add(0.0, vars.servant_card_flight_vertical * 0.42, 0.0);
      if (velocity.lengthSqr() < 0.0001) {
         velocity = new Vec3(0.0, -0.015, 0.0);
      }
      player.setDeltaMovement(velocity);
      player.hurtMarked = true;
      if (player.level() instanceof ServerLevel level && player.tickCount % 10 == 0) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, player.getX(), player.getY() + 0.55, player.getZ(), 3, 0.25, 0.25, 0.25, 0.02);
      }
   }

   public static void stop(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, boolean sync) {
      if (vars.servant_card_flying || player.isNoGravity()) {
         player.setNoGravity(false);
         vars.servant_card_flying = false;
         vars.servant_card_flight_forward = 0.0;
         vars.servant_card_flight_strafe = 0.0;
         vars.servant_card_flight_vertical = 0.0;
         if (sync) {
            vars.syncPlayerVariables(player);
         }
      }
   }

   private static void toggleFlight(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_flight_toggle_cooldown > 0) {
         return;
      }
      if (!canFly(vars.servant_card_id)) {
         player.displayClientMessage(Component.literal("This servant cannot fly"), true);
         return;
      }
      vars.servant_card_flying = !vars.servant_card_flying;
      vars.servant_card_flight_toggle_cooldown = 8;
      if (!vars.servant_card_flying) {
         player.setNoGravity(false);
      }
      player.displayClientMessage(Component.literal(vars.servant_card_flying ? "Flight enabled" : "Flight disabled"), true);
   }
}

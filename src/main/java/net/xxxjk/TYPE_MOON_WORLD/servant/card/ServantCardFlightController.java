package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardFlightController {
   private static final double MP_PER_TICK = 0.28;
   private static final int MODE_OFF = 0;
   private static final int MODE_NORMAL = 1;
   private static final int MODE_HIGH = 2;
   private static final int HIGH_FLIGHT_MAX_TICKS = 5 * 20;
   private static final int HIGH_FLIGHT_EXHAUSTED_COOLDOWN = 20 * 20;
   private static final int HIGH_FLIGHT_RECHARGE_INTERVAL = 3 * 20;
   private static final int HIGH_FLIGHT_RECHARGE_AMOUNT = 20;

   private ServantCardFlightController() {
   }

   public static boolean canFly(String servantId) {
      return "medea".equals(servantId) || "oda_nobunaga".equals(servantId) || "enkidu".equals(servantId) || "gilgamesh".equals(servantId);
   }

   public static void setInput(ServerPlayer player, boolean toggle, double forward, double strafe, double vertical) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed) {
         return;
      }
      if (toggle) {
         toggleFlight(player, vars);
         vars.syncServantCardRuntime(player);
         return;
      }
      double clampedForward = Mth.clamp(forward, -1.0, 1.0);
      double clampedStrafe = Mth.clamp(strafe, -1.0, 1.0);
      double clampedVertical = Mth.clamp(vertical, -1.0, 1.0);
      if (Math.abs(vars.servant_card_flight_forward - clampedForward) > 1.0E-4
         || Math.abs(vars.servant_card_flight_strafe - clampedStrafe) > 1.0E-4
         || Math.abs(vars.servant_card_flight_vertical - clampedVertical) > 1.0E-4) {
         vars.servant_card_flight_forward = clampedForward;
         vars.servant_card_flight_strafe = clampedStrafe;
         vars.servant_card_flight_vertical = clampedVertical;
      }
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      boolean unlimited = ServantCardUnlimitedMode.isEnabled(player);
      if (unlimited) {
         vars.servant_card_flight_toggle_cooldown = 0;
      } else if (vars.servant_card_flight_toggle_cooldown > 0) {
         vars.servant_card_flight_toggle_cooldown--;
      }
      if (vars.servant_card_transformed && canFly(vars.servant_card_id)) {
         tickHighFlightRecharge(player, vars);
      }
      if (!vars.servant_card_transformed || !vars.servant_card_flying) {
         if (!vars.servant_card_transformed || !canFly(vars.servant_card_id)) {
            stop(player, vars, false);
         }
         return;
      }
      long now = player.level().getGameTime();
      if (vars.servant_card_flight_mode == MODE_HIGH) {
         vars.servant_card_high_flight_until = now + vars.servant_card_oda_flight_ticks;
         vars.servant_card_oda_flight_ticks = Math.max(0, vars.servant_card_oda_flight_ticks - 1);
      }
      if (vars.servant_card_flight_mode == MODE_HIGH && vars.servant_card_oda_flight_ticks <= 0) {
         vars.servant_card_flight_mode = MODE_NORMAL;
         vars.servant_card_high_flight_until = 0L;
         if (unlimited) {
            vars.servant_card_oda_flight_ticks = HIGH_FLIGHT_MAX_TICKS;
            vars.servant_card_oda_flight_cooldown_until = 0L;
            vars.servant_card_oda_flight_recharge_at = 0L;
         } else {
            vars.servant_card_oda_flight_cooldown_until = now + HIGH_FLIGHT_EXHAUSTED_COOLDOWN;
            vars.servant_card_oda_flight_recharge_at = vars.servant_card_oda_flight_cooldown_until + HIGH_FLIGHT_RECHARGE_INTERVAL;
         }
         vars.syncServantCardRuntime(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.high_flight_expired"), true);
      }
      if ("oda_nobunaga".equals(vars.servant_card_id) && ServantCardOdaNobunagaSkills.tickMountFlight(player, vars)) {
         return;
      }
      if (!canFly(vars.servant_card_id) || !ServantCardManaService.consumeSilently(player, vars, MP_PER_TICK)) {
         stop(player, vars, true);
         return;
      }
      if (player.tickCount % 10 == 0) {
         vars.syncMana(player);
      }
      player.setNoGravity(true);
      player.fallDistance = 0.0F;
      double yaw = Math.toRadians(player.getYRot());
      Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 movement = forward.scale(vars.servant_card_flight_forward).add(right.scale(vars.servant_card_flight_strafe));
      if (movement.lengthSqr() > 1.0) {
         movement = movement.normalize();
      }
      double horizontalSpeed = switch (vars.servant_card_id) {
         case "enkidu", "gilgamesh" -> 0.58;
         case "oda_nobunaga" -> 0.54;
         default -> 0.48;
      };
      double verticalInput = vars.servant_card_flight_vertical;
      if (vars.servant_card_flight_mode == MODE_NORMAL && !hasGroundWithin(player, 5)) {
         verticalInput = Math.min(verticalInput, -0.28);
      }
      Vec3 velocity = movement.scale(horizontalSpeed).add(0.0, verticalInput * 0.42, 0.0);
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
      if ("oda_nobunaga".equals(vars.servant_card_id)) {
         if (vars.servant_card_flight_mode == MODE_HIGH) {
            beginHighFlightRecharge(player, vars);
         }
         ServantCardOdaNobunagaSkills.stopMountFlight(player, vars, sync);
         return;
      }
      if (vars.servant_card_flying || player.isNoGravity()) {
         if (vars.servant_card_flight_mode == MODE_HIGH) {
            beginHighFlightRecharge(player, vars);
         }
         player.setNoGravity(false);
         vars.servant_card_flying = false;
         vars.servant_card_flight_mode = MODE_OFF;
         vars.servant_card_flight_forward = 0.0;
         vars.servant_card_flight_strafe = 0.0;
         vars.servant_card_flight_vertical = 0.0;
         if (sync) {
            vars.syncServantCardRuntime(player);
         }
      }
   }

   private static void toggleFlight(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      boolean unlimited = ServantCardUnlimitedMode.isEnabled(player);
      if (!unlimited && vars.servant_card_flight_toggle_cooldown > 0) {
         return;
      }
      if (!canFly(vars.servant_card_id)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.flight_unavailable"), true);
         return;
      }
      long now = player.level().getGameTime();
      if (!vars.servant_card_flying) {
         if ("oda_nobunaga".equals(vars.servant_card_id) && !ServantCardOdaNobunagaSkills.startMountFlight(player, vars)) {
            return;
         }
         vars.servant_card_flying = true;
         vars.servant_card_flight_mode = MODE_NORMAL;
      } else if (vars.servant_card_flight_mode == MODE_NORMAL) {
         if (now < vars.servant_card_oda_flight_cooldown_until || vars.servant_card_oda_flight_ticks <= 0) {
            vars.servant_card_flying = false;
            vars.servant_card_flight_mode = MODE_OFF;
         } else {
            vars.servant_card_flight_mode = MODE_HIGH;
            vars.servant_card_high_flight_until = now + vars.servant_card_oda_flight_ticks;
            vars.servant_card_oda_flight_recharge_at = 0L;
         }
      } else {
         beginHighFlightRecharge(player, vars);
         vars.servant_card_flying = false;
         vars.servant_card_flight_mode = MODE_OFF;
      }
      if (!vars.servant_card_flying && "oda_nobunaga".equals(vars.servant_card_id)) {
         ServantCardOdaNobunagaSkills.stopMountFlight(player, vars, false);
      }
      vars.servant_card_high_flight_cooldown_until = 0L;
      vars.servant_card_flight_toggle_cooldown = unlimited ? 0 : 8;
      if (!vars.servant_card_flying) {
         player.setNoGravity(false);
      }
      String message = !vars.servant_card_flying
         ? "message.typemoonworld.servant_card.flight_disabled"
         : vars.servant_card_flight_mode == MODE_HIGH
            ? "message.typemoonworld.servant_card.high_flight_enabled"
            : "message.typemoonworld.servant_card.normal_flight_enabled";
      player.displayClientMessage(Component.translatable(message), true);
   }

   static boolean hasGroundWithin(ServerPlayer player, int blocks) {
      net.minecraft.core.BlockPos.MutableBlockPos pos = player.blockPosition().mutable();
      for (int i = 1; i <= blocks; i++) {
         pos.set(player.getBlockX(), Mth.floor(player.getY()) - i, player.getBlockZ());
         if (!player.level().getBlockState(pos).getCollisionShape(player.level(), pos).isEmpty()) {
            return true;
         }
      }
      return false;
   }

   private static void tickHighFlightRecharge(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.servant_card_high_flight_cooldown_until = 0L;
      if (vars.servant_card_flight_mode == MODE_HIGH || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      if (vars.servant_card_oda_flight_ticks > HIGH_FLIGHT_MAX_TICKS) {
         vars.servant_card_oda_flight_ticks = HIGH_FLIGHT_MAX_TICKS;
         vars.syncServantCardRuntime(player);
      }
      if (vars.servant_card_oda_flight_ticks >= HIGH_FLIGHT_MAX_TICKS) {
         vars.servant_card_oda_flight_recharge_at = 0L;
         return;
      }
      if (now < vars.servant_card_oda_flight_cooldown_until) {
         return;
      }
      if (vars.servant_card_oda_flight_recharge_at <= 0L) {
         vars.servant_card_oda_flight_recharge_at = now + HIGH_FLIGHT_RECHARGE_INTERVAL;
         return;
      }
      if (now >= vars.servant_card_oda_flight_recharge_at) {
         vars.servant_card_oda_flight_ticks = Math.min(
            HIGH_FLIGHT_MAX_TICKS,
            vars.servant_card_oda_flight_ticks + HIGH_FLIGHT_RECHARGE_AMOUNT
         );
         vars.servant_card_oda_flight_recharge_at = vars.servant_card_oda_flight_ticks >= HIGH_FLIGHT_MAX_TICKS
            ? 0L
            : now + HIGH_FLIGHT_RECHARGE_INTERVAL;
         vars.syncServantCardRuntime(player);
      }
   }

   private static void beginHighFlightRecharge(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.servant_card_high_flight_until = 0L;
      if (ServantCardUnlimitedMode.isEnabled(player)) {
         vars.servant_card_oda_flight_ticks = HIGH_FLIGHT_MAX_TICKS;
         vars.servant_card_oda_flight_cooldown_until = 0L;
         vars.servant_card_oda_flight_recharge_at = 0L;
         return;
      }
      if (player.level() instanceof ServerLevel level
         && vars.servant_card_oda_flight_ticks < HIGH_FLIGHT_MAX_TICKS
         && vars.servant_card_oda_flight_recharge_at <= 0L) {
         vars.servant_card_oda_flight_recharge_at = level.getGameTime() + HIGH_FLIGHT_RECHARGE_INTERVAL;
      }
   }
}

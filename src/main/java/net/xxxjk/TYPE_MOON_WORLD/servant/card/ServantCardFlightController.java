package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public final class ServantCardFlightController {
   private static final String FLIGHT_WAS_AIRBORNE_TAG = "ServantCardFlightWasAirborne";
   private static final String FLIGHT_DASH_DIR_X_TAG = "ServantCardFlightDashDirX";
   private static final String FLIGHT_DASH_DIR_Z_TAG = "ServantCardFlightDashDirZ";
   private static final String FLIGHT_DASH_POWER_TAG = "ServantCardFlightDashPower";
   private static final String FLIGHT_DASH_TICKS_TAG = "ServantCardFlightDashTicks";
   private static final String FLIGHT_DASH_DURATION_TAG = "ServantCardFlightDashDuration";
   private static final double MP_PER_TICK = 0.28;
   private static final int MODE_OFF = 0;
   private static final int MODE_NORMAL = 1;
   private static final int MODE_HIGH = 2;
   private static final int HIGH_FLIGHT_MAX_TICKS = 5 * 20;
   private static final int HIGH_FLIGHT_EXHAUSTED_COOLDOWN = 20 * 20;
   private static final int HIGH_FLIGHT_RECHARGE_INTERVAL = 3 * 20;
   private static final int HIGH_FLIGHT_RECHARGE_AMOUNT = 20;
   private static final int FLIGHT_DASH_DURATION_TICKS = 6;

   private ServantCardFlightController() {
   }

   public static boolean canFly(String servantId) {
      return "medea".equals(servantId) || "oda_nobunaga".equals(servantId) || "enkidu".equals(servantId)
         || "gilgamesh".equals(servantId) || "gilgamesh_caster".equals(servantId);
   }

   public static boolean tryDash(ServerPlayer player, float forwardInput, float strafeInput) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var card = vars.servantCardState();
      if (!card.transformed() || !card.flying() || card.jumpCharges() <= 0) {
         return false;
      }
      if ("shadow_hassan".equals(card.id()) && !ServantCardShadowHassanSkills.canAttack(player)) return false;
      if (PlayerNoblePhantasmHelper.isChargingMovementLocked(player)) {
         return false;
      }
      ServantDefinition definition = ServantDataRegistry.get(card.id());
      double scale = definition == null ? 1.0 : Mth.clamp(definition.parameters().movementSpeed() / 0.28, 0.8, 1.8);
      Vec3 horizontal = buildHorizontalMotion(player, forwardInput, strafeInput);
      if (horizontal.lengthSqr() < 0.0001) {
         horizontal = buildHorizontalMotion(player, 1.0F, 0.0F);
      }
      startDashMotion(player, horizontal.normalize(), scale);
      player.fallDistance = 0.0F;
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, player.getX(), player.getY() + 0.15, player.getZ(), 14, 0.25, 0.10, 0.25, 0.06);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, player.getX(), player.getY() + 0.35, player.getZ(), 8, 0.20, 0.14, 0.20, 0.05);
         level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_1.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.65F, 1.35F);
      }
      card.setJumpCharges(card.jumpCharges() - 1);
      card.setJumpRecoveryTicks(ServantCardJumpRecoveryRules.recoveryTicksFor(card.jumpCharges()));
      card.setJumpRecoveryEnd(0L);
      vars.syncPlayerVariables(player);
      return true;
   }

   public static void setInput(ServerPlayer player, boolean toggle, double forward, double strafe, double vertical) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var card = vars.servantCardState();
      if (!card.transformed()) {
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
      if (Math.abs(card.flightForward() - clampedForward) > 1.0E-4
         || Math.abs(card.flightStrafe() - clampedStrafe) > 1.0E-4
         || Math.abs(card.flightVertical() - clampedVertical) > 1.0E-4) {
         card.setFlightInput(clampedForward, clampedStrafe, clampedVertical);
      }
   }

   private static Vec3 buildHorizontalMotion(ServerPlayer player, float forwardInput, float strafeInput) {
      double yaw = Math.toRadians(player.getYRot());
      Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
      Vec3 horizontal = forward.scale(Mth.clamp(forwardInput, -1.0F, 1.0F)).add(right.scale(Mth.clamp(strafeInput, -1.0F, 1.0F)));
      if (horizontal.lengthSqr() > 1.0) {
         horizontal = horizontal.normalize();
      }
      return horizontal;
   }

   private static void startDashMotion(ServerPlayer player, Vec3 horizontal, double scale) {
      Vec3 direction = horizontal.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
      double power = 1.08 * scale;
      var data = player.getPersistentData();
      data.putDouble(FLIGHT_DASH_DIR_X_TAG, direction.x);
      data.putDouble(FLIGHT_DASH_DIR_Z_TAG, direction.z);
      data.putDouble(FLIGHT_DASH_POWER_TAG, power);
      data.putInt(FLIGHT_DASH_TICKS_TAG, FLIGHT_DASH_DURATION_TICKS);
      data.putInt(FLIGHT_DASH_DURATION_TAG, FLIGHT_DASH_DURATION_TICKS);
   }

   private static Vec3 consumeDashMotion(ServerPlayer player) {
      var data = player.getPersistentData();
      int ticks = data.getInt(FLIGHT_DASH_TICKS_TAG);
      int duration = Math.max(1, data.getInt(FLIGHT_DASH_DURATION_TAG));
      if (ticks <= 0 || duration <= 0) {
         clearDashMotion(player);
         return Vec3.ZERO;
      }
      double dirX = data.getDouble(FLIGHT_DASH_DIR_X_TAG);
      double dirZ = data.getDouble(FLIGHT_DASH_DIR_Z_TAG);
      double power = data.getDouble(FLIGHT_DASH_POWER_TAG);
      double scale = ticks / (double)duration;
      data.putInt(FLIGHT_DASH_TICKS_TAG, ticks - 1);
      if (ticks - 1 <= 0) {
         clearDashMotion(player);
      }
      return new Vec3(dirX, 0.0, dirZ).scale(power * Math.max(0.15, scale));
   }

   private static void clearDashMotion(ServerPlayer player) {
      var data = player.getPersistentData();
      data.remove(FLIGHT_DASH_DIR_X_TAG);
      data.remove(FLIGHT_DASH_DIR_Z_TAG);
      data.remove(FLIGHT_DASH_POWER_TAG);
      data.remove(FLIGHT_DASH_TICKS_TAG);
      data.remove(FLIGHT_DASH_DURATION_TAG);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      var card = vars.servantCardState();
      boolean unlimited = ServantCardUnlimitedMode.isEnabled(player);
      if (unlimited) {
         card.setFlightToggleCooldown(0);
      } else if (card.flightToggleCooldown() > 0) {
         card.setFlightToggleCooldown(card.flightToggleCooldown() - 1);
      }
      if (card.transformed() && canFly(card.id())) {
         tickHighFlightRecharge(player, vars);
      }
      if (!card.transformed() || !card.flying()) {
         if (!card.transformed() || !canFly(card.id())) {
            stop(player, vars, false);
         }
         return;
      }
      boolean grounded = player.getVehicle() != null ? player.getVehicle().onGround() : player.onGround();
      if (!grounded) {
         player.getPersistentData().putBoolean(FLIGHT_WAS_AIRBORNE_TAG, true);
      } else if (player.getPersistentData().getBoolean(FLIGHT_WAS_AIRBORNE_TAG)) {
         stop(player, vars, true);
         return;
      }
      long now = player.level().getGameTime();
      if (card.flightMode() == MODE_HIGH) {
         card.setHighFlightUntil(now + card.odaFlightTicks());
         card.setOdaFlightTicks(Math.max(0, card.odaFlightTicks() - 1));
      }
      if (card.flightMode() == MODE_HIGH && card.odaFlightTicks() <= 0) {
         card.setFlightMode(MODE_NORMAL);
         card.setHighFlightUntil(0L);
         if (unlimited) {
            card.setOdaFlightTicks(HIGH_FLIGHT_MAX_TICKS);
            card.setOdaFlightCooldownUntil(0L);
            card.setOdaFlightRechargeAt(0L);
         } else {
            card.setOdaFlightCooldownUntil(now + HIGH_FLIGHT_EXHAUSTED_COOLDOWN);
            card.setOdaFlightRechargeAt(card.odaFlightCooldownUntil() + HIGH_FLIGHT_RECHARGE_INTERVAL);
         }
         vars.syncServantCardRuntime(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.high_flight_expired"), true);
      }
      if ("oda_nobunaga".equals(card.id()) && ServantCardOdaNobunagaSkills.tickMountFlight(player, vars)) {
         return;
      }
      if (!canFly(card.id()) || !ServantCardManaService.consumeSilently(player, vars, MP_PER_TICK)) {
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
      Vec3 movement = forward.scale(card.flightForward()).add(right.scale(card.flightStrafe()));
      if (movement.lengthSqr() > 1.0) {
         movement = movement.normalize();
      }
      double horizontalSpeed = switch (card.id()) {
         case "enkidu", "gilgamesh", "gilgamesh_caster" -> 0.58;
         case "oda_nobunaga" -> 0.54;
         default -> 0.48;
      };
      double verticalInput = card.flightVertical();
      if (card.flightMode() == MODE_NORMAL && !hasGroundWithin(player, 5)) {
         verticalInput = Math.min(verticalInput, -0.28);
      }
      Vec3 velocity = movement.scale(horizontalSpeed).add(0.0, verticalInput * 0.42, 0.0);
      velocity = velocity.add(consumeDashMotion(player));
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
      var card = vars.servantCardState();
      player.getPersistentData().remove(FLIGHT_WAS_AIRBORNE_TAG);
      clearDashMotion(player);
      if ("oda_nobunaga".equals(card.id())) {
         if (card.flightMode() == MODE_HIGH) {
            beginHighFlightRecharge(player, vars);
         }
         ServantCardOdaNobunagaSkills.stopMountFlight(player, vars, sync);
         return;
      }
      if (card.flying() || player.isNoGravity()) {
         if (card.flightMode() == MODE_HIGH) {
            beginHighFlightRecharge(player, vars);
         }
         player.setNoGravity(false);
         card.setFlying(false);
         card.setFlightMode(MODE_OFF);
         card.setFlightInput(0.0, 0.0, 0.0);
         if (sync) {
            vars.syncServantCardRuntime(player);
         }
      }
   }

   private static void toggleFlight(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      var card = vars.servantCardState();
      boolean unlimited = ServantCardUnlimitedMode.isEnabled(player);
      if (!unlimited && card.flightToggleCooldown() > 0) {
         return;
      }
      if (!canFly(card.id())) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.flight_unavailable"), true);
         return;
      }
      long now = player.level().getGameTime();
      if (!card.flying()) {
         player.getPersistentData().remove(FLIGHT_WAS_AIRBORNE_TAG);
         if ("oda_nobunaga".equals(card.id()) && !ServantCardOdaNobunagaSkills.startMountFlight(player, vars)) {
            return;
         }
         card.setFlying(true);
         card.setFlightMode(MODE_NORMAL);
      } else if (card.flightMode() == MODE_NORMAL) {
         if (now < card.odaFlightCooldownUntil() || card.odaFlightTicks() <= 0) {
            card.setFlying(false);
            card.setFlightMode(MODE_OFF);
         } else {
            card.setFlightMode(MODE_HIGH);
            card.setHighFlightUntil(now + card.odaFlightTicks());
            card.setOdaFlightRechargeAt(0L);
         }
      } else {
         beginHighFlightRecharge(player, vars);
         card.setFlying(false);
         card.setFlightMode(MODE_OFF);
      }
      if (!card.flying() && "oda_nobunaga".equals(card.id())) {
         ServantCardOdaNobunagaSkills.stopMountFlight(player, vars, false);
      }
      card.setHighFlightCooldownUntil(0L);
      card.setFlightToggleCooldown(unlimited ? 0 : 8);
      if (!card.flying()) {
         player.setNoGravity(false);
      }
      String message = !card.flying()
         ? "message.typemoonworld.servant_card.flight_disabled"
         : card.flightMode() == MODE_HIGH
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
      var card = vars.servantCardState();
      card.setHighFlightCooldownUntil(0L);
      if (card.flightMode() == MODE_HIGH || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      if (card.odaFlightTicks() > HIGH_FLIGHT_MAX_TICKS) {
         card.setOdaFlightTicks(HIGH_FLIGHT_MAX_TICKS);
         vars.syncServantCardRuntime(player);
      }
      if (card.odaFlightTicks() >= HIGH_FLIGHT_MAX_TICKS) {
         card.setOdaFlightRechargeAt(0L);
         return;
      }
      if (now < card.odaFlightCooldownUntil()) {
         return;
      }
      if (card.odaFlightRechargeAt() <= 0L) {
         card.setOdaFlightRechargeAt(now + HIGH_FLIGHT_RECHARGE_INTERVAL);
         return;
      }
      if (now >= card.odaFlightRechargeAt()) {
         card.setOdaFlightTicks(Math.min(
            HIGH_FLIGHT_MAX_TICKS,
            card.odaFlightTicks() + HIGH_FLIGHT_RECHARGE_AMOUNT
         ));
         card.setOdaFlightRechargeAt(card.odaFlightTicks() >= HIGH_FLIGHT_MAX_TICKS
            ? 0L
            : now + HIGH_FLIGHT_RECHARGE_INTERVAL);
         vars.syncServantCardRuntime(player);
      }
   }

   private static void beginHighFlightRecharge(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      var card = vars.servantCardState();
      card.setHighFlightUntil(0L);
      if (ServantCardUnlimitedMode.isEnabled(player)) {
         card.setOdaFlightTicks(HIGH_FLIGHT_MAX_TICKS);
         card.setOdaFlightCooldownUntil(0L);
         card.setOdaFlightRechargeAt(0L);
         return;
      }
      if (player.level() instanceof ServerLevel level
         && card.odaFlightTicks() < HIGH_FLIGHT_MAX_TICKS
         && card.odaFlightRechargeAt() <= 0L) {
         card.setOdaFlightRechargeAt(level.getGameTime() + HIGH_FLIGHT_RECHARGE_INTERVAL);
      }
   }
}

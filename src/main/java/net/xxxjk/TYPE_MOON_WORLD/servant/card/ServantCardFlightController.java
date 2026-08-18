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
      if (!vars.servant_card_transformed || !vars.servant_card_flying || vars.servant_card_jump_charges <= 0) {
         return false;
      }
      if ("shadow_hassan".equals(vars.servant_card_id) && !ServantCardShadowHassanSkills.canAttack(player)) return false;
      if (PlayerNoblePhantasmHelper.isChargingMovementLocked(player)) {
         return false;
      }
      ServantDefinition definition = ServantDataRegistry.get(vars.servant_card_id);
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
      vars.servant_card_jump_charges--;
      vars.servant_card_jump_recovery_ticks = ServantCardJumpRecoveryRules.recoveryTicksFor(vars.servant_card_jump_charges);
      vars.servant_card_jump_recovery_end = 0L;
      vars.syncPlayerVariables(player);
      return true;
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
      boolean grounded = player.getVehicle() != null ? player.getVehicle().onGround() : player.onGround();
      if (!grounded) {
         player.getPersistentData().putBoolean(FLIGHT_WAS_AIRBORNE_TAG, true);
      } else if (player.getPersistentData().getBoolean(FLIGHT_WAS_AIRBORNE_TAG)) {
         stop(player, vars, true);
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
         case "enkidu", "gilgamesh", "gilgamesh_caster" -> 0.58;
         case "oda_nobunaga" -> 0.54;
         default -> 0.48;
      };
      double verticalInput = vars.servant_card_flight_vertical;
      if (vars.servant_card_flight_mode == MODE_NORMAL && !hasGroundWithin(player, 5)) {
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
      player.getPersistentData().remove(FLIGHT_WAS_AIRBORNE_TAG);
      clearDashMotion(player);
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
         player.getPersistentData().remove(FLIGHT_WAS_AIRBORNE_TAG);
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

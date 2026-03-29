package net.xxxjk.TYPE_MOON_WORLD.magic.other;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.advancement.TypeMoonAdvancementHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicGravity {
   public static final int MODE_ULTRA_LIGHT = -2;
   public static final int MODE_LIGHT = -1;
   public static final int MODE_NORMAL = 0;
   public static final int MODE_HEAVY = 1;
   public static final int MODE_ULTRA_HEAVY = 2;
   private static final int MIN_DURATION_TICKS = 600;
   private static final int MAX_DURATION_TICKS = 3600;
   private static final double CAST_MANA_COST = 20.0;
   private static final double TARGET_RANGE = 10.0;
   private static final int RESULT_MESSAGE_DELAY_TICKS = 12;

   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         LivingEntity target = resolveTarget(player, vars.gravity_magic_target);
         if (target == null) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gravity.no_target"), true);
         } else if (!ManaHelper.consumeOneTimeMagicCost(player, CAST_MANA_COST)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         } else {
            int nextMode = Math.max(MODE_ULTRA_LIGHT, Math.min(MODE_ULTRA_HEAVY, vars.gravity_magic_mode));
            Component targetComp = (Component)(target == player ? Component.translatable("gui.typemoonworld.mode.self") : target.getDisplayName());
            player.displayClientMessage(getChantComponentForMode(nextMode), true);
            Component resultMessage;
            if (nextMode == MODE_NORMAL) {
               MagicGravityEffectHandler.clearGravityState(target);
               MagicGravityEffectHandler.playGravityNormalizeFx(player, target);
               resultMessage = Component.translatable("message.typemoonworld.magic.gravity.normalized", targetComp);
            } else {
               int duration = getDurationTicks(vars.proficiency_gravity_magic);
               MagicGravityEffectHandler.applyGravityState(target, nextMode, player.level().getGameTime() + duration, player);
               MagicGravityEffectHandler.playGravityCastFx(player, target, nextMode);
               TypeMoonAdvancementHelper.grantOutIfEligible(player, target, nextMode);

               String modeKey = switch (nextMode) {
                  case -2 -> "gui.typemoonworld.mode.gravity.ultra_light";
                  case -1 -> "gui.typemoonworld.mode.gravity.light";
                  default -> "gui.typemoonworld.mode.gravity.normal";
                  case 1 -> "gui.typemoonworld.mode.gravity.heavy";
                  case 2 -> "gui.typemoonworld.mode.gravity.ultra_heavy";
               };
               int durationSec = duration / 20;
               resultMessage = Component.translatable(
                  "message.typemoonworld.magic.gravity.applied", targetComp, Component.translatable(modeKey), durationSec
               );
            }

            queueActionbarResult(player, resultMessage);
            vars.proficiency_gravity_magic = Math.min(100.0, vars.proficiency_gravity_magic + 0.4);
            vars.syncPlayerVariables(player);
         }
      }
   }

   private static LivingEntity resolveTarget(ServerPlayer player, int targetMode) {
      if (targetMode == MODE_NORMAL) {
         return player;
      } else {
         HitResult hitResult = EntityUtils.getRayTraceTarget(player, TARGET_RANGE);
         return hitResult.getType() == Type.ENTITY
               && ((EntityHitResult)hitResult).getEntity() instanceof LivingEntity living
               && EntityUtils.isValidCombatTarget(player, living)
            ? living
            : EntityUtils.findAutoAimTarget(player, TARGET_RANGE, 65.0);
      }
   }

   private static int getDurationTicks(double proficiency) {
      double clamped = Math.max(0.0, Math.min(100.0, proficiency));
      double ratio = clamped / 100.0;
      return MIN_DURATION_TICKS + (int)Math.round((MAX_DURATION_TICKS - MIN_DURATION_TICKS) * ratio);
   }

   public static String getChantForMode(int mode) {
      return mode == 0 ? "vos Gott es Atlas." : "Es ist gros, es ist klein.";
   }

   public static Component getChantComponentForMode(int mode) {
      ChatFormatting chantColor;
      if (mode == 0) {
         chantColor = ChatFormatting.GOLD;
      } else if (mode < 0) {
         chantColor = ChatFormatting.AQUA;
      } else {
         chantColor = ChatFormatting.RED;
      }

      return Component.literal(getChantForMode(mode)).withStyle(new ChatFormatting[]{chantColor, ChatFormatting.ITALIC});
   }

   private static void queueActionbarResult(ServerPlayer player, Component message) {
      TYPE_MOON_WORLD.queueServerWork(RESULT_MESSAGE_DELAY_TICKS, () -> {
         if (!player.isRemoved()) {
            player.displayClientMessage(message, true);
         }
      });
   }
}

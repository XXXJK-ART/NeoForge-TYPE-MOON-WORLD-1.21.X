package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import org.joml.Vector3f;

public final class MagicTimeAlter {
   public static final String MAGIC_ID = "time_alter";
   public static final int MODE_ACCEL = 0;
   public static final int MODE_STAGNATE = 1;
   private static final DustParticleOptions TIME_DUST = new DustParticleOptions(new Vector3f(0.35F, 0.75F, 1.0F), 1.15F);

   private MagicTimeAlter() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean fromCrest = vars.isCurrentSelectionFromCrest(MAGIC_ID);
      double proficiency = fromCrest ? 100.0 : vars.proficiency_time_alter;
      CompoundTag preset = fromCrest ? vars.getCurrentCrestPresetPayload() : new CompoundTag();
      int configuredMode = fromCrest && preset.contains("time_alter_mode") ? preset.getInt("time_alter_mode") : vars.time_alter_mode;
      int mode = configuredMode == MODE_STAGNATE ? MODE_STAGNATE : MODE_ACCEL;
      double multiplier;
      if (fromCrest) {
         multiplier = mode == MODE_ACCEL && preset.contains("time_alter_multiplier")
            ? Math.max(1, preset.getInt("time_alter_multiplier"))
            : mode == MODE_ACCEL ? accelMultiplier(proficiency) : stagnateMultiplier(proficiency);
      } else {
         multiplier = mode == MODE_ACCEL && proficiency >= 80.0
            ? Math.max(1, vars.time_alter_multiplier)
            : mode == MODE_ACCEL ? accelMultiplier(proficiency) : stagnateMultiplier(proficiency);
      }
      int durationTicks = durationTicks(proficiency);
      double cost = mode == MODE_ACCEL ? accelCost(multiplier) : stagnateCost(multiplier);

      if (TimeAlterEventHandler.isActive(player)) {
         TimeAlterEventHandler.release(player);
         return true;
      }
      if (!TimeAlterEventHandler.canStart(player, mode, multiplier)) {
         return false;
      }
      if (!ManaHelper.consumeOneTimeMagicCost(player, cost)) {
         return false;
      }

      TimeAlterEventHandler.start(player, mode, multiplier, durationTicks);
      spawnCastParticles(player, mode);
      player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.75F, mode == MODE_ACCEL ? 1.45F : 0.75F);
      player.displayClientMessage(Component.translatable(mode == MODE_ACCEL
         ? "message.typemoonworld.magic.time_alter.accel"
         : "message.typemoonworld.magic.time_alter.stagnate"), true);
      if (!fromCrest) {
         vars.proficiency_time_alter = Math.min(100.0, vars.proficiency_time_alter + (mode == MODE_ACCEL ? 0.16 : 0.18));
         vars.syncProficiency(player);
      }
      return true;
   }

   public static double accelMultiplier(double proficiency) {
      double p = clamp(proficiency);
      if (p >= 75.0) {
         return 4.0;
      } else if (p >= 50.0) {
         return 3.0;
      } else if (p >= 25.0) {
         return 2.0;
      }
      return 1.5;
   }

   public static double stagnateMultiplier(double proficiency) {
      double p = clamp(proficiency);
      if (p >= 75.0) {
         return 0.25;
      } else if (p >= 50.0) {
         return 0.4;
      } else if (p >= 25.0) {
         return 0.6;
      }
      return 0.8;
   }

   public static int durationTicks(double proficiency) {
      double p = clamp(proficiency);
      if (p >= 75.0) {
         return 100;
      } else if (p >= 50.0) {
         return 80;
      } else if (p >= 25.0) {
         return 60;
      }
      return 40;
   }

   public static double accelCost(double multiplier) {
      return 10.0 + multiplier * 5.0;
   }

   public static double stagnateCost(double multiplier) {
      return 15.0 + (1.0 / Math.max(0.05, multiplier)) * 5.0;
   }

   private static double clamp(double proficiency) {
      return Math.max(0.0, Math.min(100.0, proficiency));
   }

   private static void spawnCastParticles(ServerPlayer player, int mode) {
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(TIME_DUST, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), mode == MODE_ACCEL ? 28 : 18, 0.45, 0.55, 0.45, 0.02);
      }
   }
}

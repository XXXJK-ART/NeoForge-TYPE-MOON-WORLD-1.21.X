package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class MagicLearningProgressService {
   private static final double DECAY_THRESHOLD_PERCENT = 10.0;
   private static final double DECAY_PER_SECOND_RATIO = 0.0025;
   private static final long DECAY_DELAY_TICKS = 20L;

   private MagicLearningProgressService() {
   }

   public static Map<String, Double> progressMap(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars == null ? Map.of() : vars.magic_learning_progress;
   }

   public static Map<String, Long> lastGainMap(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars == null ? Map.of() : vars.magic_learning_progress_last_gain_tick;
   }

   public static double maxProgress(String magicId) {
      return Math.max(1.0D, MagicLearningStrategy.complexity(magicId) * 100.0D);
   }

   public static double get(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (vars == null || magicId == null || magicId.isBlank()) {
         return 0.0D;
      }
      return Math.max(0.0D, progressMap(vars).getOrDefault(magicId, 0.0D));
   }

   public static double percent(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      return get(vars, magicId) / maxProgress(magicId) * 100.0D;
   }

   public static double add(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, double amount, long gameTick) {
      if (vars == null || magicId == null || magicId.isBlank() || amount <= 0.0D) {
         return get(vars, magicId);
      }
      double current = get(vars, magicId);
      double next = Math.min(maxProgress(magicId), current + effectiveGain(current, amount));
      vars.magic_learning_progress.put(magicId, next);
      vars.magic_learning_progress_last_gain_tick.put(magicId, gameTick);
      return next;
   }

   /** Adds raw learning work using the two late-stage diminishing-return bands. */
   public static double addWithLearningCheck(ServerPlayer player, String magicId, double amount) {
      if (player == null || magicId == null || magicId.isBlank() || amount <= 0.0D) {
         return player == null ? 0.0D : get(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), magicId);
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.learned_magics.contains(magicId)) {
         return get(vars, magicId);
      }
      double beforePercent = percent(vars, magicId);
      double next = add(vars, magicId, amount, player.level().getGameTime());
      double afterPercent = percent(vars, magicId);
      checkThresholds(player, vars, magicId, beforePercent, afterPercent);
      if (afterPercent >= 100.0D) {
         completeIfReady(player, vars, magicId);
      }
      return next;
   }

   public static boolean addFromSource(ServerPlayer player, String magicId, double fraction) {
      if (player == null || magicId == null || magicId.isBlank()) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.learned_magics.contains(magicId)) {
         return false;
      }
      addWithLearningCheck(player, magicId, maxProgress(magicId) * Math.max(0.0D, fraction));
      vars.syncPlayerVariables(player);
      return true;
   }

   public static double addFraction(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, double fraction, long gameTick) {
      return add(vars, magicId, maxProgress(magicId) * Math.max(0.0D, fraction), gameTick);
   }

   public static boolean completeIfReady(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (player == null || vars == null || magicId == null || magicId.isBlank()) {
         return false;
      }
      if (get(vars, magicId) + 1.0E-6 < maxProgress(magicId)) {
         return false;
      }
      return MagicLearningService.grantFromProgress(player, magicId);
   }

   public static void tick(ServerPlayer player) {
      if (player == null) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars == null || vars.magic_learning_progress.isEmpty()) {
         return;
      }
      long now = player.level().getGameTime();
      boolean changed = false;
      for (Map.Entry<String, Double> entry : new HashMap<>(vars.magic_learning_progress).entrySet()) {
         String magicId = entry.getKey();
         double current = Math.max(0.0D, entry.getValue());
         double max = maxProgress(magicId);
         double percent = current / max * 100.0D;
         long lastGain = vars.magic_learning_progress_last_gain_tick.getOrDefault(magicId, now);
         if (percent < DECAY_THRESHOLD_PERCENT && now - lastGain >= DECAY_DELAY_TICKS && current > 0.0D) {
            double decay = Math.max(1.0D, max * DECAY_PER_SECOND_RATIO);
            double next = Math.max(0.0D, current - decay);
            if (next != current) {
               vars.magic_learning_progress.put(magicId, next);
               changed = true;
            }
         }
      }
      if (changed) {
         vars.syncPlayerVariables(player);
      }
   }

   public static void reset(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (vars == null || magicId == null) {
         return;
      }
      vars.magic_learning_progress.remove(magicId);
      vars.magic_learning_progress_last_gain_tick.remove(magicId);
   }

   private static void checkThresholds(ServerPlayer player,
                                       TypeMoonWorldModVariables.PlayerVariables vars,
                                       String magicId,
                                       double beforePercent,
                                       double afterPercent) {
      if (afterPercent <= beforePercent || afterPercent < 55.0D) {
         return;
      }
      double nextThreshold = Math.max(55.0D, Math.floor(beforePercent / 5.0D) * 5.0D + 5.0D);
      while (nextThreshold <= afterPercent && nextThreshold <= 100.0D) {
         double chance = Math.max(0.0D, Math.min(1.0D, (nextThreshold - 50.0D) * 0.02D));
         if (player.getRandom().nextDouble() < chance) {
            completeIfReady(player, vars, magicId);
            return;
         }
         nextThreshold += 5.0D;
      }
   }

   private static double effectiveGain(double current, double rawAmount) {
      double remaining = rawAmount;
      double gain = 0.0D;
      if (current < 5000.0D) {
         double firstBand = Math.min(remaining, 5000.0D - current);
         gain += firstBand;
         remaining -= firstBand;
         current += firstBand;
      }
      if (remaining > 0.0D && current < 7500.0D) {
         double secondBand = Math.min(remaining, 7500.0D - current);
         gain += secondBand * 0.25D;
         remaining -= secondBand;
         current += secondBand;
      }
      if (remaining > 0.0D) {
         gain += remaining * 0.025D;
      }
      return gain;
   }

   public static void removeUnknownEntries(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return;
      }
      vars.magic_learning_progress.keySet().removeIf(key -> key == null || key.isBlank());
      vars.magic_learning_progress_last_gain_tick.keySet().removeIf(key -> key == null || key.isBlank());
   }
}

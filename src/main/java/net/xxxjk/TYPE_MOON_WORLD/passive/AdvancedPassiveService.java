package net.xxxjk.TYPE_MOON_WORLD.passive;

import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class AdvancedPassiveService {
   private static final double[] INCANTATION_CHANT = {0.80, 0.70, 0.60, 0.50, 0.40};
   private static final double[] INCANTATION_MANA = {0.90, 0.82, 0.74, 0.66, 0.58};
   private static final double[] DIVINE_WORDS_CHANT = {0.50, 0.40, 0.30, 0.20, 0.10};
   private static final double[] DIVINE_WORDS_MANA = {0.70, 0.60, 0.50, 0.40, 0.30};
   private static final double[] GOLD_CHANCE = {0.02, 0.04, 0.06, 0.08, 0.10};

   private AdvancedPassiveService() {
   }

   public static boolean active(TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      return vars != null && !PassiveService.effectsSuppressed(vars) && PassiveService.has(vars, id);
   }

   public static boolean ignoresOriginBulletSeal(Player player) {
      if (player == null) return false;
      return active(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), PassiveService.HIGH_SPEED_DIVINE_WORDS);
   }

   public static double chantMultiplier(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null || PassiveService.effectsSuppressed(vars)) return 1.0;
      double multiplier = 1.0;
      PassiveRank incantation = PassiveService.rank(vars, PassiveService.HIGH_SPEED_INCANTATION);
      if (incantation != null) {
         multiplier = Math.min(multiplier, enhancedIncantationChantMultiplier(incantation, effectiveMagicAnalysisProficiency(vars)));
      }
      PassiveRank divine = PassiveService.rank(vars, PassiveService.HIGH_SPEED_DIVINE_WORDS);
      if (divine != null) {
         multiplier = Math.min(multiplier, divineWordsChantMultiplier(divine));
      }
      return multiplier;
   }

   public static double manaMultiplier(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null || PassiveService.effectsSuppressed(vars)) return 1.0;
      double multiplier = 1.0;
      PassiveRank incantation = PassiveService.rank(vars, PassiveService.HIGH_SPEED_INCANTATION);
      if (incantation != null) {
         multiplier = Math.min(multiplier, enhancedIncantationManaMultiplier(incantation, effectiveMagicAnalysisProficiency(vars)));
      }
      PassiveRank divine = PassiveService.rank(vars, PassiveService.HIGH_SPEED_DIVINE_WORDS);
      if (divine != null) {
         multiplier = Math.min(multiplier, divineWordsManaMultiplier(divine));
      }
      return multiplier;
   }

   public static double enhancedIncantationChantMultiplier(PassiveRank rank, double effectiveAnalysisProficiency) {
      return enhancedIncantationMultiplier(rank, effectiveAnalysisProficiency, true);
   }

   public static double enhancedIncantationManaMultiplier(PassiveRank rank, double effectiveAnalysisProficiency) {
      return enhancedIncantationMultiplier(rank, effectiveAnalysisProficiency, false);
   }

   public static double divineWordsChantMultiplier(PassiveRank rank) {
      return rank == null ? 1.0 : DIVINE_WORDS_CHANT[rank.rankIndex()];
   }

   public static double divineWordsManaMultiplier(PassiveRank rank) {
      return rank == null ? 1.0 : DIVINE_WORDS_MANA[rank.rankIndex()];
   }

   private static double enhancedIncantationMultiplier(PassiveRank rank, double effectiveAnalysisProficiency, boolean chant) {
      if (rank == null) return 1.0;
      int idx = rank.rankIndex();
      double base = chant ? INCANTATION_CHANT[idx] : INCANTATION_MANA[idx];
      double target = chant ? DIVINE_WORDS_CHANT[idx] : DIVINE_WORDS_MANA[idx];
      double analysis = Math.max(0.0, Math.min(100.0, effectiveAnalysisProficiency));
      if (analysis < 50.0) return base;
      double t = (analysis - 50.0) / 50.0;
      return base + (target - base) * t;
   }

   public static int partitionN(TypeMoonWorldModVariables.PlayerVariables vars) {
      PassiveRank rank = active(vars, PassiveService.PARTITIONED_THOUGHT)
         ? PassiveService.rank(vars, PassiveService.PARTITIONED_THOUGHT) : null;
      return partitionN(rank);
   }

   public static int partitionN(PassiveRank rank) {
      return rank == null ? 0 : 3 + rank.rankIndex();
   }

   public static int analysisWorkPerTick(TypeMoonWorldModVariables.PlayerVariables vars) {
      PassiveRank rank = active(vars, PassiveService.PARTITIONED_THOUGHT)
         ? PassiveService.rank(vars, PassiveService.PARTITIONED_THOUGHT) : null;
      return analysisWorkPerTick(rank);
   }

   public static int analysisWorkPerTick(PassiveRank rank) {
      int n = partitionN(rank);
      if (n <= 0) return 1;
      int value = 1;
      for (int i = 0; i < n; i++) value *= n;
      return value;
   }

   public static double effectiveMagicAnalysisProficiency(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) return 0.0;
      double stored = MagicProficiencyService.getRaw(vars, "magic_analysis");
      PassiveRank rank = active(vars, PassiveService.PARTITIONED_THOUGHT)
         ? PassiveService.rank(vars, PassiveService.PARTITIONED_THOUGHT) : null;
      return effectiveMagicAnalysisProficiency(stored, rank);
   }

   public static double effectiveMagicAnalysisProficiency(double stored, PassiveRank partitionedThoughtRank) {
      int n = partitionN(partitionedThoughtRank);
      if (n <= 0 || stored >= 75.0) return stored;
      return Math.min(74.99, stored * (1.0 + n * 0.05));
   }

   public static int lootingBonus(TypeMoonWorldModVariables.PlayerVariables vars) {
      PassiveRank rank = active(vars, PassiveService.GOLDEN_RULE) ? PassiveService.rank(vars, PassiveService.GOLDEN_RULE) : null;
      return lootingBonus(rank);
   }

   public static int lootingBonus(PassiveRank rank) {
      return rank == null ? 0 : rank.rankIndex() + 1;
   }

   public static double goldenDropChance(TypeMoonWorldModVariables.PlayerVariables vars) {
      PassiveRank rank = active(vars, PassiveService.GOLDEN_RULE) ? PassiveService.rank(vars, PassiveService.GOLDEN_RULE) : null;
      return goldenDropChance(rank);
   }

   public static double goldenDropChance(PassiveRank rank) {
      return rank == null ? 0.0 : GOLD_CHANCE[rank.rankIndex()];
   }

   public static String formatPercent(double multiplier) {
      return Math.round(multiplier * 1000.0) / 10.0 + "%";
   }
}

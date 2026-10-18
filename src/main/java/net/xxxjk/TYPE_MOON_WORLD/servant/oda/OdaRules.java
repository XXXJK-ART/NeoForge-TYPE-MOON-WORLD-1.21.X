package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantEra;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.NpType;

/** The numerical contract shared by the servant card, NPC, projectiles and tests. */
public final class OdaRules {
   private OdaRules() {}
   public record Skill(String name, String effect, int mp, int cooldown) {}
   public static Skill skill(int slot) {
      return switch (slot) {
         case -1 -> new Skill("压切长谷部·一闪", "oda_hasebe_short_thrust", 8, 40);
         case 0 -> new Skill("火绳枪·自动布阵", "oda_floating_refill", 20, 200);
         case 1 -> new Skill("三段击·轮转齐射", "oda_three_line", 30, 200);
         case 2 -> new Skill("火绳枪·追击阵", "oda_encircle_matchlocks", 40, 160);
         case 3 -> new Skill("狙击·火绳枪", "oda_charged_matchlock", 50, 300);
         case 4 -> new Skill("交叉火网·封锁", "oda_crossfire_net", 35, 240);
         case 5 -> new Skill("焦土战术·烈焰焚城", "oda_scorched_earth", 40, 400);
         case 6 -> new Skill("压切长谷部·烈火突进", "oda_hasebe_breakthrough", 25, 150);
         case 7 -> new Skill("第六天魔王·显现", "maou", 45, 500);
         case 8 -> new Skill("三千世界", "three_thousand", 95, 3600);
         case 9 -> new Skill("第六天魔王波旬", "hajun", 130, 3600);
         default -> null;
      };
   }
   public static int divinity(String rank, boolean divine) {
      if (rank == null || rank.isBlank()) return divine ? 1 : 0;
      String key = rank.toUpperCase(java.util.Locale.ROOT).replace("DIVINITY_", "").replace("DIVINITY:", "");
      if (key.startsWith("EX") || key.startsWith("A")) return 5;
      return switch (key.charAt(0)) { case 'B' -> 4; case 'C' -> 3; case 'D' -> 2; case 'E' -> 1; default -> divine ? 1 : 0; };
   }
   public static int eraBonus(ServantEra era) {
      if (era == null) return 0;
      return switch (era) { case MYTHOLOGY -> 3; case ANCIENT -> 2; case AGE_OF_SAIL -> 1; default -> 0; };
   }
   public static double eraFinal(ServantEra era) {
      return era == ServantEra.MODERN ? 0.5 : era == ServantEra.FUTURE ? 0.25 : 1.0;
   }
   public static double special(int divinity, ServantEra era, boolean threeThousandRiding) {
      return 1 + Math.max(0, Math.min(5, divinity)) + eraBonus(era) + (threeThousandRiding ? 1 : 0);
   }
   public static double strategy(String rank) {
      return switch (rank == null ? "" : rank.toUpperCase(java.util.Locale.ROOT)) {
         case "E" -> .04; case "D" -> .08; case "C" -> .12; case "B" -> .16; case "A" -> .20; default -> 0;
      };
   }
   public static boolean armyOrCastle(NpType type) { return type == NpType.ARMY || type == NpType.CASTLE; }
   public static float chargeDamage(int ticks) { return 10 + 40 * Math.clamp(ticks / 40.0F, 0, 1); }
   public static float chargeSpeed(int ticks) { return 3 + Math.clamp(ticks / 40.0F, 0, 1); }
   public static boolean followMode(int heldTicks) { return heldTicks < 12; }
}

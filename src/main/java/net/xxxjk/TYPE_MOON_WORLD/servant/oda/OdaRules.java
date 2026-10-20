package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantEra;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.NpType;

/** The numerical contract shared by the servant card, NPC, projectiles and tests. */
public final class OdaRules {
   private OdaRules() {}
   /** Three tiers of eleven guns make the requested thirty-three-gun wave. */
   public static final int THREE_THOUSAND_TIER_COUNT = 3;
   public static final int THREE_THOUSAND_GUNS_PER_TIER = 11;
   public static final int THREE_THOUSAND_WAVE_COUNT = 3;
   public static final int THREE_THOUSAND_TIER_INTERVAL = 3;
   public static final int THREE_THOUSAND_GUNS_PER_WAVE = THREE_THOUSAND_TIER_COUNT * THREE_THOUSAND_GUNS_PER_TIER;
   public static final int THREE_THOUSAND_TOTAL_GUNS = THREE_THOUSAND_GUNS_PER_WAVE * THREE_THOUSAND_WAVE_COUNT;
   public static final int THREE_THOUSAND_SHOTS_PER_GUN = 3;
   /** Dedicated NP shot damage; the damage reduction comes from the smaller array. */
   public static final float THREE_THOUSAND_SHOT_DAMAGE = 10.0F;
   public static final int THREE_THOUSAND_SHOT_INTERVAL = 30;
   public static final int THREE_THOUSAND_MAX_LIFE = 20 * 30;
   public static final int THREE_THOUSAND_INITIAL_SHOT_DELAY = 2;
   public static final int THREE_THOUSAND_LIFT_TICKS = 40;
   public static final int THREE_THOUSAND_LAND_TICKS = 20;
   public static final double THREE_THOUSAND_LIFT_HEIGHT = 3.0;
   public static final int THREE_THOUSAND_TOTAL_SHOTS = THREE_THOUSAND_TOTAL_GUNS * THREE_THOUSAND_SHOTS_PER_GUN;
   private static final double[] THREE_THOUSAND_ANGLES = threeThousandAngles();
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
   public static int threeThousandTier(int gunIndex) {
      return Math.clamp(gunIndex / THREE_THOUSAND_GUNS_PER_TIER, 0, THREE_THOUSAND_TIER_COUNT - 1);
   }
   public static int threeThousandTierDelay(int tier) {
      return Math.clamp(tier, 0, THREE_THOUSAND_TIER_COUNT - 1) * THREE_THOUSAND_TIER_INTERVAL;
   }
   public static double threeThousandLiftProgress(double ticks) {
      double t = Math.clamp(ticks / THREE_THOUSAND_LIFT_TICKS, 0.0, 1.0);
      return t * t * (3.0 - 2.0 * t);
   }
   public static int threeThousandWave(int gunIndex) {
      return Math.clamp(gunIndex / THREE_THOUSAND_GUNS_PER_WAVE, 0, THREE_THOUSAND_WAVE_COUNT - 1);
   }
   public static int threeThousandGunIndexInWave(int gunIndex) {
      return Math.floorMod(gunIndex, THREE_THOUSAND_GUNS_PER_WAVE);
   }
   public static int threeThousandShotDelay(int slot) {
      return THREE_THOUSAND_INITIAL_SHOT_DELAY + Math.floorMod(slot, 3);
   }
   public static double threeThousandRadius(int tier) {
      return threeThousandRadius(tier, 0, 0);
   }
   public static double threeThousandRadius(int tier, int wave) {
      return threeThousandRadius(tier);
   }
   public static double threeThousandRadius(int tier, int slot, int wave) {
      int index = Math.clamp(tier, 0, THREE_THOUSAND_TIER_COUNT - 1) * THREE_THOUSAND_GUNS_PER_TIER
         + Math.floorMod(slot, THREE_THOUSAND_GUNS_PER_TIER);
      return 1.5 + 3.0 * index / (THREE_THOUSAND_GUNS_PER_WAVE - 1);
   }
   public static double threeThousandHeight(int tier) {
      int index = Math.clamp(tier, 0, THREE_THOUSAND_TIER_COUNT - 1) * THREE_THOUSAND_GUNS_PER_TIER;
      return -3.675 + 7.35 * index / (THREE_THOUSAND_GUNS_PER_WAVE - 1);
   }
   public static double threeThousandHeight(int tier, int wave) {
      return threeThousandHeight(tier);
   }
   public static double threeThousandHeight(int tier, int slot, int wave) {
      int index = Math.clamp(tier, 0, THREE_THOUSAND_TIER_COUNT - 1) * THREE_THOUSAND_GUNS_PER_TIER
         + Math.floorMod(slot, THREE_THOUSAND_GUNS_PER_TIER);
      return -3.675 + 7.35 * index / (THREE_THOUSAND_GUNS_PER_WAVE - 1);
   }
   public static double threeThousandAngle(int tier, int slot) {
      int normalizedTier = Math.clamp(tier, 0, THREE_THOUSAND_TIER_COUNT - 1);
      int normalizedSlot = Math.floorMod(slot, THREE_THOUSAND_GUNS_PER_TIER);
      return THREE_THOUSAND_ANGLES[normalizedTier * THREE_THOUSAND_GUNS_PER_TIER + normalizedSlot];
   }
   public static double threeThousandAngle(int tier, int slot, int wave) {
      return threeThousandAngle(tier, slot);
   }
   private static double[] threeThousandAngles() {
      double[] angles = new double[THREE_THOUSAND_GUNS_PER_WAVE];
      for (int i = 1; i < angles.length; i++) {
         double previousRadius = 1.5 + 3.0 * (i - 1) / (angles.length - 1);
         double radius = 1.5 + 3.0 * i / (angles.length - 1);
         angles[i] = angles[i - 1] + .9 / ((previousRadius + radius) * .5);
      }
      return angles;
   }
}

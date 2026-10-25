package net.xxxjk.TYPE_MOON_WORLD.servant.artoria;

/** FGO Saber reset: shared by the card, NPC and Excalibur controller. */
public final class ArtoriaRules {
   public static final String ID = "artoria_pendragon";
   public static final int BURST_START_MP = 60, BURST_DRAIN_MP = 30, BURST_REOPEN_TICKS = 120;
   /** Shared Wind King barrier recovery after the wind has been released. */
   public static final int WIND_REGATHER_TICKS = 40;
   public static final int WIND_HAMMER_WINDUP_TICKS = 10;
   public static final float NPC_EXCALIBUR_SELECTION_CHANCE = 0.95F;
   public static final float NPC_DECISIVE_WIND_HAMMER_CHANCE = 0.12F;
   public static final String WIND_BARRIER_TOGGLE = "artoria_invisible_air_toggle";
   public static final String WIND_DEFENSE = "artoria_wind_defense";
   public static final double WIND_DEFENSE_MP = 35.0;
   public static final int WIND_DEFENSE_MAX_TICKS = 100;
   /** All three Invisible Air actions share the same wind regather cooldown. */
   public static final int WIND_DEFENSE_COOLDOWN = WIND_REGATHER_TICKS;
   public static final double WIND_DEFENSE_RADIUS = 6.0;
   public static final float WIND_HAMMER_PLAYER_DAMAGE = 90.0F;
   public static final float WIND_HAMMER_PLAYER_BURST_DAMAGE = 125.0F;
   public static final float WIND_HAMMER_NPC_DAMAGE = 300.0F;
   public static final float WIND_HAMMER_NPC_BURST_DAMAGE = 400.0F;
   public static final double WIND_HAMMER_PLAYER_RANGE = 10.5;
   public static final double WIND_HAMMER_PLAYER_BURST_RANGE = 12.5;
   public static final double WIND_HAMMER_NPC_RANGE = 10.0;
   public static final double WIND_HAMMER_PLAYER_WIDTH_SCALE = 0.48;
   public static final double WIND_HAMMER_PLAYER_BURST_WIDTH_SCALE = 0.58;
   public static final double WIND_HAMMER_NPC_WIDTH_SCALE = 0.45;
   public static final double BURST_SWORD_MULTIPLIER = 10.0, BURST_DEFENSE_MULTIPLIER = .30, BURST_SPEED_BONUS = .15;
   public static final double BURST_SWORD_HIT_COST = 20.0;
   public static final int JET_TICKS = 40, INSTINCT_RECOVERY_TICKS = 300;
   public static final double EXCALIBUR_WEAPON_DAMAGE = 13;
   public static final int EXCALIBUR_MP = 150, EXCALIBUR_COOLDOWN = 3600, EXCALIBUR_TICKS = 150;
   public static final int EXCALIBUR_WINDUP_TICKS = 152;
   public static final double EXCALIBUR_SHORT_MP = EXCALIBUR_MP / 4.0;
   public static final int EXCALIBUR_SHORT_COOLDOWN = EXCALIBUR_COOLDOWN / 4;
   public static final int EXCALIBUR_SHORT_TICKS = Math.max(1, Math.round(EXCALIBUR_TICKS / 4.0F));
   public static final double EXCALIBUR_SHORT_RANGE = EXCALIBUR_TICKS / 4.0;
   public static final int EXCALIBUR_SHORT_CHARGE_TICKS = 60;
   public static final String EXCALIBUR_SHORT_SKILL = "artoria_excalibur_short";
   public static final int PULSE_INTERVAL = 10, PULSES = 15, PULSE_DAMAGE = 500, REVEAL_AFTER_NP = 100;
   public static final int EXCALIBUR_SHORT_PULSES = 4;
   public static final int EXCALIBUR_SHORT_PULSE_INTERVAL = EXCALIBUR_SHORT_TICKS / EXCALIBUR_SHORT_PULSES;
   public static final int EXCALIBUR_SHORT_PULSE_DAMAGE = 120;
   public static final String KNIGHT_TRIPLE_SLASH = "artoria_knight_triple_slash";
   public static final String INTERCEPT_THRUST = "artoria_intercept_thrust";
   public static final String SPINNING_SLASH = "artoria_spinning_slash";
   public static final String UPWARD_SLASH = "artoria_upward_slash";
   public static final String GUARD_COUNTER = "artoria_guard_counter";
   public static final String KNIGHT_HEAVY_SLASH = "artoria_knight_heavy_slash";
   public static final double TRIPLE_SLASH_MP = 18, INTERCEPT_THRUST_MP = 14, SPINNING_SLASH_MP = 16;
   public static final double UPWARD_SLASH_MP = 12, GUARD_COUNTER_MP = 22, KNIGHT_HEAVY_SLASH_MP = 32;
   public static final int TRIPLE_SLASH_COOLDOWN = 140, INTERCEPT_THRUST_COOLDOWN = 100;
   public static final int SPINNING_SLASH_COOLDOWN = 100, UPWARD_SLASH_COOLDOWN = 80;
   public static final int GUARD_COUNTER_COOLDOWN = 180, KNIGHT_HEAVY_SLASH_COOLDOWN = 300;
   public static final float TRIPLE_SLASH_FIRST_DAMAGE = 16, TRIPLE_SLASH_SECOND_DAMAGE = 16, TRIPLE_SLASH_FINAL_DAMAGE = 20;
   public static final float INTERCEPT_THRUST_DAMAGE = 34, SPINNING_SLASH_DAMAGE = 26;
   public static final float UPWARD_SLASH_DAMAGE = 24, GUARD_COUNTER_DAMAGE = 38, KNIGHT_HEAVY_SLASH_DAMAGE = 54;
   public static final float MANA_BURST_BEAM_DAMAGE = 100;
   public static final double MANA_BURST_BEAM_MP = 100;
   public static final int MANA_BURST_BEAM_COOLDOWN = 300;
   public static final int MANA_BURST_BEAM_MAX_BLOCKS = 6;
   private ArtoriaRules() {}
   public static double swordMultiplier(boolean burst) { return burst ? BURST_SWORD_MULTIPLIER : 1; }
   public static boolean npcMayUseExcalibur(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase phase, float roll) {
      return phase == net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase.DECISIVE
         && roll >= 0.0F && roll < NPC_EXCALIBUR_SELECTION_CHANCE;
   }
   public static boolean npcPrioritizesWindHammer(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase phase, float roll) {
      return phase != net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase.DECISIVE
         || roll >= 0.0F && roll < NPC_DECISIVE_WIND_HAMMER_CHANCE;
   }
   public static double revealedBonus(double weaponDamage, double charge, boolean burst) {
      return 0.0;
   }
   public static double basicDamage(double normal, double special, double weaponDamage, double charge, boolean revealed, boolean burst) {
      return normal * swordMultiplier(burst) * special;
   }
   public static double skillMpCost(String skill) {
      return switch (skill) {
         case KNIGHT_TRIPLE_SLASH -> TRIPLE_SLASH_MP;
         case INTERCEPT_THRUST -> INTERCEPT_THRUST_MP;
         case SPINNING_SLASH -> SPINNING_SLASH_MP;
         case UPWARD_SLASH -> UPWARD_SLASH_MP;
         case GUARD_COUNTER -> GUARD_COUNTER_MP;
         case KNIGHT_HEAVY_SLASH -> KNIGHT_HEAVY_SLASH_MP;
         case WIND_DEFENSE -> WIND_DEFENSE_MP;
         case "mana_burst_beam" -> MANA_BURST_BEAM_MP;
         case EXCALIBUR_SHORT_SKILL -> EXCALIBUR_SHORT_MP;
         default -> 0;
      };
   }
   public static int skillCooldown(String skill) {
      return switch (skill) {
         case KNIGHT_TRIPLE_SLASH -> TRIPLE_SLASH_COOLDOWN;
         case INTERCEPT_THRUST -> INTERCEPT_THRUST_COOLDOWN;
         case SPINNING_SLASH -> SPINNING_SLASH_COOLDOWN;
         case UPWARD_SLASH -> UPWARD_SLASH_COOLDOWN;
         case GUARD_COUNTER -> GUARD_COUNTER_COOLDOWN;
         case KNIGHT_HEAVY_SLASH -> KNIGHT_HEAVY_SLASH_COOLDOWN;
         case WIND_DEFENSE -> WIND_DEFENSE_COOLDOWN;
         case "mana_burst_beam" -> MANA_BURST_BEAM_COOLDOWN;
         case EXCALIBUR_SHORT_SKILL -> EXCALIBUR_SHORT_COOLDOWN;
         default -> 0;
      };
   }
}

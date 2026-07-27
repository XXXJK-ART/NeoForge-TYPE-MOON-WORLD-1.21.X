package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public final class ArashCombatRules {
   public static final double MAX_HEALTH = 600.0;
   public static final double MAX_MANA = 200.0;
   public static final float STOUT_DAMAGE_MULTIPLIER = 0.85F;
   public static final float MAGIC_RESISTANCE_MULTIPLIER = 0.80F;
   public static final float FUTURE_SIGHT_DODGE_CHANCE = 0.10F;
   public static final int TARGET_RANGE = 200;
   public static final int TARGET_SCAN_INTERVAL = 100;
   public static final int TACTICAL_REPATH_INTERVAL = 12;
   public static final double PREFERRED_COMBAT_RANGE = 32.0;
   public static final double APPROACH_THRESHOLD = 56.0;
   public static final double CROSSOVER_TRIGGER_RANGE = 7.0;
   public static final int CROSSOVER_COOLDOWN = 60;
   public static final int ARROW_CAPACITY = 10;
   public static final double ARROW_REFILL_MANA = 5.0;
   public static final float NORMAL_ARROW_DAMAGE = 10.0F;
   public static final int NORMAL_ARROW_INTERVAL = 5;
   public static final float RAIN_ARROW_DAMAGE = 13.2F;
   public static final int RAIN_ARROW_COUNT = 50;
   public static final double RAIN_MANA = 8.0;
   public static final int RAIN_COOLDOWN = 160;
   public static final float SMALL_ENERGY_DAMAGE = 30.0F;
   public static final double SMALL_ENERGY_MANA = 8.0;
   public static final int SMALL_ENERGY_COOLDOWN = 80;
   public static final float LARGE_ENERGY_DAMAGE = 60.0F;
   public static final double LARGE_ENERGY_MANA = 20.0;
   public static final int LARGE_ENERGY_COOLDOWN = 240;
   public static final int STELLA_CHANT_TICKS = 700;
   public static final int STELLA_PRELOAD_GRACE_TICKS = 200;
   public static final int STELLA_SACRIFICE_TICKS = 200;
   public static final double STELLA_LENGTH = 2500.0;
   public static final int STELLA_FLIGHT_TICKS = 2400;
   public static final int STELLA_EXPLOSION_TICKS = 100;
   public static final int STELLA_TERRAIN_RADIUS = 10;
   public static final int STELLA_SCAR_RADIUS = 18;
   public static final double STELLA_OUTER_RADIUS = 5.0;
   public static final double STELLA_CORE_RADIUS = 1.25;
   public static final double STELLA_END_RADIUS = 50.0;
   public static final float STELLA_CORE_DAMAGE = 2000.0F;
   public static final float STELLA_OUTER_DAMAGE = 500.0F;

   private ArashCombatRules() {
   }

   public static double stellaDistanceAtTick(int tick) {
      return STELLA_LENGTH * Math.max(0, Math.min(STELLA_FLIGHT_TICKS, tick)) / STELLA_FLIGHT_TICKS;
   }

   public static double stellaExplosionRadiusAtTick(int tick) {
      return STELLA_END_RADIUS * Math.max(0, Math.min(STELLA_EXPLOSION_TICKS, tick)) / STELLA_EXPLOSION_TICKS;
   }

   public static float stellaRemainingHealth(float initialHealth, int elapsedTicks) {
      int remaining = Math.max(0, STELLA_SACRIFICE_TICKS - Math.max(0, elapsedTicks));
      return Math.max(0.0F, initialHealth) * remaining / STELLA_SACRIFICE_TICKS;
   }
}

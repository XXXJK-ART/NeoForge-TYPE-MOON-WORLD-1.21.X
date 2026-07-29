package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public final class ArashCombatRules {
   public static final int INITIAL_ARROW_COUNT = 500;
   public static final int MAX_ARROW_COUNT = 5000;
   public static final int ARROW_CREATION_THRESHOLD = 1000;
   public static final int ARROW_CREATION_AMOUNT = 10;
   public static final double ARROW_CREATION_MANA_COST = 1.0;
   public static final int ARROW_CREATION_INTERVAL = 20;
   public static final int NORMAL_ARROW_COST = 1;
   public static final int ENERGY_ARROW_COST = 1;
   public static final int ARROW_RAIN_COST = 100;
   public static final int CROUCH_ARROW_RAIN_COST = 500;
   public static final int STELLA_ARROW_COST = 1;
   public static final double MAX_HEALTH = 600.0;
   public static final double MAX_MANA = 200.0;
   public static final float STOUT_DAMAGE_MULTIPLIER = 0.70F;
   public static final double DEFENSE_RECOVERY_MULTIPLIER = 2.0;
   public static final double POISE_RECOVERY_MULTIPLIER = 2.0;
   public static final float MAGIC_RESISTANCE_MULTIPLIER = 0.80F;
   public static final float FUTURE_SIGHT_DODGE_CHANCE = 0.10F;
   public static final int TARGET_RANGE = 200;
   public static final int TARGET_SCAN_INTERVAL = 100;
   public static final int TACTICAL_REPATH_INTERVAL = 12;
   public static final double PREFERRED_COMBAT_RANGE = 32.0;
   public static final double APPROACH_THRESHOLD = 56.0;
   public static final double CROSSOVER_TRIGGER_RANGE = 7.0;
   public static final int CROSSOVER_COOLDOWN = 60;
   public static final float NORMAL_ARROW_DAMAGE = 10.0F;
   public static final int NORMAL_ARROW_INTERVAL = 5;
   public static final float RAIN_ARROW_DAMAGE = 13.2F;
   public static final int RAIN_ARROW_COUNT = 100;
   public static final int CROUCH_RAIN_ARROW_COUNT = 500;
   public static final double RAIN_SPREAD_RADIUS = 6.0;
   public static final double CROUCH_RAIN_SPREAD_RADIUS = 30.0;
   public static final double CROUCH_RAIN_ASSIST_RADIUS = 36.0;
   public static final double RAIN_MANA = 8.0;
   public static final int RAIN_COOLDOWN = 160;
   public static final float SMALL_ENERGY_DAMAGE = 30.0F;
   public static final int SMALL_ENERGY_TERRAIN_RADIUS = 2;
   public static final double SMALL_ENERGY_MANA = 8.0;
   public static final int SMALL_ENERGY_COOLDOWN = 80;
   public static final float LARGE_ENERGY_DAMAGE = 60.0F;
   public static final int LARGE_ENERGY_TERRAIN_RADIUS = 4;
   public static final double LARGE_ENERGY_MANA = 20.0;
   public static final int LARGE_ENERGY_COOLDOWN = 240;
   public static final int STELLA_CHANT_TICKS = 700;
   public static final int STELLA_VOICE_TICKS = 777;
   public static final int PLAYER_STELLA_AUTO_RELEASE_TICKS = 720;
   public static final int PLAYER_STELLA_MIN_CHARGE_TICKS = 200;
   public static final int PLAYER_STELLA_FULL_CHARGE_TICKS = PLAYER_STELLA_AUTO_RELEASE_TICKS;
   public static final int PLAYER_STELLA_LONG_VOICE_CUTOFF_TICKS = 660;
   public static final double PLAYER_STELLA_MIN_LENGTH = 500.0;
   public static final float PLAYER_STELLA_MIN_DAMAGE_SCALE = 0.50F;
   public static final int STELLA_PRELOAD_GRACE_TICKS = 200;
   public static final int STELLA_SACRIFICE_TICKS = 200;
   public static final int STELLA_SACRIFICE_DAMAGE_INTERVAL = 20;
   public static final float STELLA_SACRIFICE_DAMAGE_FRACTION = 0.10F;
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

   public record StellaProfile(double length, int flightTicks, int explosionTicks, int terrainRadius,
                               int scarRadius, double outerRadius, double coreRadius, double endRadius,
                               float coreDamage, float outerDamage) {
      public double distanceAtTick(int tick) {
         return length * Math.max(0, Math.min(flightTicks, tick)) / flightTicks;
      }

      public double explosionRadiusAtTick(int tick) {
         return endRadius * Math.max(0, Math.min(explosionTicks, tick)) / explosionTicks;
      }
   }

   private ArashCombatRules() {
   }

   public static double boostedDefenseRecovery(double baseRecovery) {
      return Math.max(0.0, baseRecovery) * DEFENSE_RECOVERY_MULTIPLIER;
   }

   public static double boostedPoiseRecovery(double baseRecovery) {
      return Math.max(0.0, baseRecovery) * POISE_RECOVERY_MULTIPLIER;
   }

   public static int terrainDestructionRadius(int arrowVariant) {
      return switch (arrowVariant) {
         case 2 -> SMALL_ENERGY_TERRAIN_RADIUS;
         case 3 -> LARGE_ENERGY_TERRAIN_RADIUS;
         default -> 0;
      };
   }

   public static double stellaDistanceAtTick(int tick) {
      return STELLA_LENGTH * Math.max(0, Math.min(STELLA_FLIGHT_TICKS, tick)) / STELLA_FLIGHT_TICKS;
   }

   public static double stellaExplosionRadiusAtTick(int tick) {
      return STELLA_END_RADIUS * Math.max(0, Math.min(STELLA_EXPLOSION_TICKS, tick)) / STELLA_EXPLOSION_TICKS;
   }

   public static StellaProfile fullStellaProfile() {
      return new StellaProfile(STELLA_LENGTH, STELLA_FLIGHT_TICKS, STELLA_EXPLOSION_TICKS,
         STELLA_TERRAIN_RADIUS, STELLA_SCAR_RADIUS, STELLA_OUTER_RADIUS, STELLA_CORE_RADIUS,
         STELLA_END_RADIUS, STELLA_CORE_DAMAGE, STELLA_OUTER_DAMAGE);
   }

   public static StellaProfile playerStellaProfile(int chargeTicks) {
      double progress = playerStellaChargeProgress(chargeTicks);
      double length = lerp(PLAYER_STELLA_MIN_LENGTH, STELLA_LENGTH, progress);
      double sizeScale = length / STELLA_LENGTH;
      float damageScale = (float)lerp(PLAYER_STELLA_MIN_DAMAGE_SCALE, 1.0, progress);
      return new StellaProfile(length,
         Math.max(1, (int)Math.round(STELLA_FLIGHT_TICKS * sizeScale)),
         Math.max(20, (int)Math.round(STELLA_EXPLOSION_TICKS * sizeScale)),
         Math.max(2, (int)Math.round(lerp(2.0, STELLA_TERRAIN_RADIUS, progress))),
         Math.max(4, (int)Math.round(lerp(4.0, STELLA_SCAR_RADIUS, progress))),
         lerp(1.0, STELLA_OUTER_RADIUS, progress),
         lerp(0.25, STELLA_CORE_RADIUS, progress),
         lerp(10.0, STELLA_END_RADIUS, progress),
         STELLA_CORE_DAMAGE * damageScale, STELLA_OUTER_DAMAGE * damageScale);
   }

   public static double playerStellaChargeProgress(int chargeTicks) {
      return Math.max(0.0, Math.min(1.0,
         (chargeTicks - PLAYER_STELLA_MIN_CHARGE_TICKS)
            / (double)(PLAYER_STELLA_FULL_CHARGE_TICKS - PLAYER_STELLA_MIN_CHARGE_TICKS)));
   }

   public static boolean canReleasePlayerStella(int chargeTicks) {
      return chargeTicks >= PLAYER_STELLA_MIN_CHARGE_TICKS;
   }

   public static boolean shouldFinishLongStellaVoice(int chargeTicks) {
      return chargeTicks >= PLAYER_STELLA_LONG_VOICE_CUTOFF_TICKS;
   }

   private static double lerp(double minimum, double maximum, double progress) {
      return minimum + (maximum - minimum) * progress;
   }

   public static float stellaRemainingHealth(float initialHealth, int elapsedTicks) {
      int pulses = Math.min(STELLA_SACRIFICE_TICKS / STELLA_SACRIFICE_DAMAGE_INTERVAL,
         Math.max(0, elapsedTicks) / STELLA_SACRIFICE_DAMAGE_INTERVAL);
      return Math.max(0.0F, initialHealth * (1.0F - pulses * STELLA_SACRIFICE_DAMAGE_FRACTION));
   }

   public static float applyStellaSacrificePulse(float currentHealth, float maxHealth, boolean finalPulse) {
      float remaining = currentHealth - Math.max(0.0F, maxHealth) * STELLA_SACRIFICE_DAMAGE_FRACTION;
      return finalPulse ? Math.max(0.0F, remaining) : Math.max(1.0F, remaining);
   }
}

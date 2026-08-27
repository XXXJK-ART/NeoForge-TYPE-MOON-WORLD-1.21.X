package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class ParacelsusBalanceRules {
   public static final float DAMAGE_MULTIPLIER = 2.0F / 3.0F;
   public static final int MAX_FIRE_TICKS = 3 * 20;
   private static final String BURN_UNTIL_TAG = "ParacelsusBurnUntil";
   private static final String BURN_TOKEN_TAG = "ParacelsusBurnToken";

   private ParacelsusBalanceRules() {
   }

   public static float reduceDamage(float currentDamage) {
      return Math.max(0.0F, currentDamage) * DAMAGE_MULTIPLIER;
   }

   public static double reduceDamage(double currentDamage) {
      return Math.max(0.0, currentDamage) * DAMAGE_MULTIPLIER;
   }

   public static int capFireTicks(int requestedTicks) {
      return Math.max(0, Math.min(MAX_FIRE_TICKS, requestedTicks));
   }

   public static void applyFire(LivingEntity target, int requestedTicks) {
      if (target == null || requestedTicks <= 0) return;
      target.setRemainingFireTicks(Math.min(MAX_FIRE_TICKS,
         Math.max(target.getRemainingFireTicks(), capFireTicks(requestedTicks))));
   }

   /** Allows only one scheduled continuous-burn sequence per target at a time. */
   public static long startBurnSequence(LivingEntity target, ServerLevel level) {
      if (target == null || level == null) return -1L;
      long now = level.getGameTime();
      if (target.getPersistentData().getLong(BURN_UNTIL_TAG) > now) return -1L;
      long token = target.getPersistentData().getLong(BURN_TOKEN_TAG) + 1L;
      target.getPersistentData().putLong(BURN_TOKEN_TAG, token);
      target.getPersistentData().putLong(BURN_UNTIL_TAG, now + MAX_FIRE_TICKS);
      return token;
   }

   public static boolean isBurnSequenceActive(LivingEntity target, ServerLevel level, long token) {
      return target != null && level != null && target.level() == level && target.isAlive()
         && target.getPersistentData().getLong(BURN_TOKEN_TAG) == token
         && target.getPersistentData().getLong(BURN_UNTIL_TAG) >= level.getGameTime();
   }
}

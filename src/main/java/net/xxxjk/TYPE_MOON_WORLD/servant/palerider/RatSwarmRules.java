package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

public final class RatSwarmRules {
   public static final int MAX_RATS = 20;
   public static final float HEALTH_PER_RAT = 5.0F;
   public static final float MAX_HEALTH = MAX_RATS * HEALTH_PER_RAT;

   private RatSwarmRules() {
   }

   public static int ratsForHealth(float health) {
      if (health <= 0.0F) {
         return 0;
      }
      return Math.min(MAX_RATS, Math.max(1, (int)Math.ceil(health / HEALTH_PER_RAT)));
   }

   public static float maximumForHealth(float health) {
      return ratsForHealth(health) * HEALTH_PER_RAT;
   }

   public static String animationForHealth(float health) {
      return Integer.toString(Math.max(1, ratsForHealth(health)));
   }

   public static MergeResult merge(float firstHealth, float secondHealth) {
      float total = Math.max(0.0F, firstHealth) + Math.max(0.0F, secondHealth);
      float primary = Math.min(MAX_HEALTH, total);
      float remainder = Math.max(0.0F, total - primary);
      return new MergeResult(primary, remainder);
   }

   public record MergeResult(float primaryHealth, float remainderHealth) {
   }
}

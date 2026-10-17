package net.xxxjk.TYPE_MOON_WORLD.client;

/** Client-side snapshot of the server-authoritative servant defense state. */
public final class ServantCardDefenseClientState {
   private static int autoDodges, autoDodgeMax;
   private static long autoRestore;
   private static boolean guardBroken;
   private static double stamina;
   private static double staminaMax;
   private static double poise;
   private static double poiseMax;
   private static boolean guarding;
   private static long dodgeCooldownEnd;
   private static long guardBrokenEnd;
   private static double shieldHp;
   private static double shieldMax;
   private static boolean shieldActive;
   private static String result = "";
   private static long resultUntil;

   private ServantCardDefenseClientState() { }

   public static void apply(double staminaValue, double staminaLimit, double poiseValue, double poiseLimit,
                            boolean activeGuard, long dodgeEnd, long brokenEnd, String lastResult, long resultExpiry,
                            double shieldValue, double shieldLimit, boolean activeShield, int charges, int chargeMax, long restore, boolean broken) {
      stamina = Math.max(0.0, staminaValue);
      staminaMax = Math.max(0.0, staminaLimit);
      poise = Math.max(0.0, poiseValue);
      poiseMax = Math.max(0.0, poiseLimit);
      guarding = activeGuard;
      dodgeCooldownEnd = Math.max(0L, dodgeEnd);
      guardBrokenEnd = Math.max(0L, brokenEnd);
      result = lastResult == null ? "" : lastResult;
      resultUntil = Math.max(0L, resultExpiry);
      shieldHp = Math.max(0.0, shieldValue);
      shieldMax = Math.max(0.0, shieldLimit);
      shieldActive = activeShield;
      autoDodges = charges; autoDodgeMax = chargeMax; autoRestore = restore; guardBroken = broken;
   }

   public static int autoDodges() { return autoDodges; }
   public static int autoDodgeMax() { return autoDodgeMax; }
   public static long autoRestore() { return autoRestore; }
   public static boolean guardBroken() { return guardBroken; }
   public static double stamina() { return stamina; }
   public static double staminaMax() { return staminaMax; }
   public static double poise() { return poise; }
   public static double poiseMax() { return poiseMax; }
   public static boolean guarding() { return guarding; }
   public static long dodgeCooldownEnd() { return dodgeCooldownEnd; }
   public static long guardBrokenEnd() { return guardBrokenEnd; }
   public static double shieldHp() { return shieldHp; }
   public static double shieldMax() { return shieldMax; }
   public static boolean shieldActive() { return shieldActive; }
   public static String result() { return result; }
   public static long resultUntil() { return resultUntil; }

   public static void clear() {
      apply(0.0, 0.0, 0.0, 0.0, false, 0L, 0L, "", 0L, 0.0, 0.0, false, 0, 0, 0L, false);
   }
}

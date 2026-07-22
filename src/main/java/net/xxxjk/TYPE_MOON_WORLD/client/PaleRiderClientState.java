package net.xxxjk.TYPE_MOON_WORLD.client;

public final class PaleRiderClientState {
   public static int controlledCount;
   public static boolean possessing;
   public static boolean underworld;
   public static boolean calamity;
   public static boolean perfectStealth;

   private PaleRiderClientState() {}

   public static void update(int count, boolean possession, boolean underworldActive, boolean calamityActive, boolean stealth) {
      controlledCount = count;
      possessing = possession;
      underworld = underworldActive;
      calamity = calamityActive;
      perfectStealth = stealth;
   }
}

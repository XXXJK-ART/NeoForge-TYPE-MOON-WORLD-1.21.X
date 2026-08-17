package net.xxxjk.TYPE_MOON_WORLD.client;

public final class HundredFacesClientState {
   public static int personaCount;
   public static boolean attackEnabled = true;

   private HundredFacesClientState() {
   }

   public static void update(int count, boolean attack) {
      personaCount = Math.max(0, count);
      attackEnabled = attack;
   }
}

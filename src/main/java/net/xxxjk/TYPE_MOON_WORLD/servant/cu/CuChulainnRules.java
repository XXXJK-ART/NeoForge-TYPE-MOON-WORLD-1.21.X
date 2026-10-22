package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

/** Shared card/NPC values. Rune meanings and combat effects are local adaptations. */
public final class CuChulainnRules {
   public static final int RECOVERY_TICKS = 1200;
   public static final int LOCK_TICKS = 600;
   public static final double LOCK_RANGE = 4, THORN_RANGE = 5, THROW_RANGE = 48;
   public static final float BARRIER_SIZE = 2.2F, BARRIER_HALF_WIDTH = BARRIER_SIZE/2, BARRIER_HEIGHT = BARRIER_SIZE;
   public record Skill(int slot, String id, double mana, int cooldown, int duration, boolean spear) { }
   public enum Input { LOCK, THORN, THROW, FLIGHT }
   private CuChulainnRules() { }
   public static Skill skill(int slot) {
      return switch (slot) {
         case -1 -> new Skill(slot,"cu_crouch_thrust",0,18,12,true);
         case 0 -> new Skill(slot,"cu_ansuz",22,120,22,false);
         case 1 -> new Skill(slot,"cu_laguz",20,160,12,false);
         case 2 -> new Skill(slot,"cu_tiwaz",24,260,12,false);
         case 3 -> new Skill(slot,"cu_algiz",30,300,12,false);
         case 4 -> new Skill(slot,"cu_berkana",34,400,12,false);
         case 5 -> new Skill(slot,"cu_disengage",18,1000,12,false);
         case 6 -> new Skill(slot,"cu_rune_barrier",80,600,12,false);
         case 7 -> new Skill(slot,"cu_recall",8,60,12,false);
         case 8 -> new Skill(slot,"cu_sweep",16,120,20,true);
         case 9 -> new Skill(slot,"cu_flurry",18,160,20,true);
         default -> null;
      };
   }
   public static Input input(boolean crouchingAtStart, int ticks) {
      return crouchingAtStart ? ticks < 40 ? Input.THROW : Input.FLIGHT : ticks < 20 ? Input.LOCK : Input.THORN;
   }
   public static double fourBranches(double health, double maximum) {
      if (!Double.isFinite(health) || !Double.isFinite(maximum) || maximum <= 0) return 1;
      return 1 + .30 * (1 - Math.clamp(health / maximum, 0, 1));
   }
   public static double flightCost(int ticks) { return Math.clamp(300.0 + Math.max(0L,(long)ticks - 40) * 10,300,600); }
   public static double flightPayment(int ticks, double available) { return available < 300 ? 0 : Math.min(flightCost(ticks),available); }
   public static float flightDamage(double payment) { return (float)(500 * Math.clamp(payment,0,600) / 300); }
   public static boolean lockDistance(double d) { return Double.isFinite(d) && d >= 0 && d <= LOCK_RANGE; }
   public static boolean thornDistance(double d) { return Double.isFinite(d) && d >= 0 && d <= THORN_RANGE; }
}

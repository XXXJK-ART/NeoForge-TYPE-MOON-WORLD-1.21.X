package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.Collection;
import java.util.Locale;

/** Letter ranks are skill ranks, never parameter plus/minus multipliers. */
public final class CommonSkillRules {
   private CommonSkillRules() {}
   public record Bonus(double health, double attack, double speed) {}
   public record AvengerBonus(double health, double attack, double speed) {}
   public record Continuation(double healthRatio, int protectionTicks, float heal, int cooldownTicks) {}
   public static int rank(String value) {
      if (value == null) return 0;
      String s = value.toUpperCase(Locale.ROOT);
      if (s.startsWith("EX")) return 0;
      return s.isEmpty() ? 0 : switch (s.charAt(0)) { case 'E' -> 1; case 'D' -> 2; case 'C' -> 3; case 'B' -> 4; case 'A' -> 5; default -> 0; };
   }
   public static int highest(Collection<String> skills, String prefix) {
      int best = 0;
      for (String id : skills) {
         String key = id.substring(id.indexOf(':') + 1);
         if (key.startsWith(prefix)) best = Math.max(best, rank(key.substring(prefix.length())));
      }
      return best;
   }
   public static Bonus bonus(int madness, int divinity) {
      madness = Math.clamp(madness, 0, 5); divinity = Math.clamp(divinity, 0, 5);
      return new Bonus(20.0 * (madness + divinity), 3.0 * madness + 2.0 * divinity, .02 * madness + .01 * divinity);
   }
   public static AvengerBonus avenger(int rank) {
      int r = Math.clamp(rank, 0, 5);
      return new AvengerBonus(10.0 * r, 4.0 * r, .01 * r);
   }
   public static Continuation continuation(int rank) {
      int r = Math.clamp(rank, 1, 5);
      return new Continuation(new double[]{.01,.03,.05,.07,.10}[r-1], r * 20, r * 2, (20 - r * 2) * 1200);
   }
}

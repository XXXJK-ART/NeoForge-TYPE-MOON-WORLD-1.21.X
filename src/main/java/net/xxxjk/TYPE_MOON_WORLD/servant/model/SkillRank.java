package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.Locale;
import java.util.regex.Pattern;

/** Skill ranks are independent of parameter ranks, particularly EX. */
public final class SkillRank {
   private static final Pattern VALID = Pattern.compile("(?:EX|[EDCBA][+\\-]{0,3})");
   private SkillRank() { }
   public static int step(String rank) {
      String value = rank == null ? "" : rank.trim().toUpperCase(Locale.ROOT);
      if (!VALID.matcher(value).matches()) return 0;
      if (value.equals("EX")) return 5;
      int base = "EDCBA".indexOf(value.charAt(0));
      // Plus/minus retain the underlying rank; A+ and higher share the cap.
      return base == 4 && value.contains("+") ? 5 : base;
   }
   public static double modifier(String rank) { return step(rank) * 0.05; }
   public static int autoDodges(String rank) { return Math.max(0, step(rank) - 1); }
}

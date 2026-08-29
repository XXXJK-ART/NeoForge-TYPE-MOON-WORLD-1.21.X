package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

/** Complete user-facing and numeric specification for one rune position. */
public record RuneEffectSpec(String name, String description, String visual, double primaryValue,
   double secondaryValue, double radius, int durationTicks, String unit) {
   public RuneEffectSpec {
      name = name == null ? "" : name;
      description = description == null ? "" : description;
      visual = visual == null ? "" : visual;
      unit = unit == null ? "" : unit;
      primaryValue = Double.isFinite(primaryValue) ? primaryValue : 0.0D;
      secondaryValue = Double.isFinite(secondaryValue) ? secondaryValue : 0.0D;
      radius = Math.max(0.0D, radius);
      durationTicks = Math.max(0, durationTicks);
   }

   public static RuneEffectSpec empty() { return new RuneEffectSpec("", "", "", 0.0D, 0.0D, 0.0D, 0, ""); }
   public boolean isEmpty() { return name.isBlank() && description.isBlank(); }
   public double value() { return primaryValue; }
   public double secondary() { return secondaryValue; }
   public int duration() { return durationTicks; }
}

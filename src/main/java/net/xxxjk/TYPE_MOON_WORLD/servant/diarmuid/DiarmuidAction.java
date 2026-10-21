package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import java.util.List;

/** Authoritative timing, resources and spear selection for both cards and NPCs. */
public enum DiarmuidAction {
   CROSS_THRUST(-1, "cross_thrust", 6, 20, 14, 90, 1, hit(4, true, 10, 5), hit(8, false, 10, 3)),
   ADVANCING_THRUST(0, "advancing_thrust", 12, 80, 14, 90, 1, hit(6, true, 24, 5)),
   YELLOW_DOUBLE(1, "yellow_double", 10, 70, 13, 90, 1, hit(3, false, 10, 3), hit(7, false, 10, 3)),
   RED_SWEEP(2, "red_sweep", 14, 100, 17, 180, 3, hit(7, true, 22, 5)),
   RISING_THRUST(3, "rising_thrust", 14, 100, 16, 90, 1, hit(6, true, 20, 4)),
   LOW_SPIN(4, "low_spin", 16, 120, 17, 240, 3, hit(5, true, 12, 4), hit(9, false, 10, 3)),
   RETREATING_THRUST(5, "retreating_thrust", 12, 100, 17, 90, 1, hit(9, true, 24, 5)),
   MANA_LEAP(6, "mana_leap", 18, 260, 28, 0, 0),
   CROSS_GUARD(7, "cross_guard", 16, 180, 20, 90, 1),
   FIGURE_EIGHT(8, "figure_eight", 24, 180, 26, 360, 3,
      hit(6, true, 10, 4), hit(10, false, 10, 3), hit(14, true, 10, 4), hit(18, false, 10, 3)),
   ROSE_DANCE(9, "rose_dance", 36, 400, 40, 90, 1,
      hit(8, true, 14, 5), hit(12, false, 12, 3), hit(16, true, 14, 5),
      hit(20, false, 12, 3), hit(26, true, 22, 5), hit(30, false, 18, 3)),
   RED_BASIC(10, "red_basic", 0, 8, 4, 90, 1, hit(2, true, 18, 5)),
   YELLOW_BASIC(11, "yellow_basic", 0, 8, 4, 90, 1, hit(2, false, 17, 3)),
   GUARD_COUNTER(12, "guard_counter", 0, 0, 11, 90, 1, hit(3, true, 26, 5));

   public static final double RED_RANGE = 5.0;
   public static final double YELLOW_RANGE = 3.0;
   public static final int HAND_COOLDOWN = 8;
   public static final double YELLOW_MODEL_LENGTH = 0.8;
   public record Hit(int tick, boolean red, float damage, double range) { }

   private final int slot;
   private final String path;
   private final double mana;
   private final int cooldown;
   private final int duration;
   private final int angle;
   private final int targets;
   private final List<Hit> hits;

   DiarmuidAction(int slot, String path, double mana, int cooldown, int duration, int angle, int targets, Hit... hits) {
      this.slot = slot;
      this.path = path;
      this.mana = mana;
      this.cooldown = cooldown;
      this.duration = duration;
      this.angle = angle;
      this.targets = targets;
      this.hits = List.of(hits);
   }

   private static Hit hit(int tick, boolean red, float damage, double range) { return new Hit(tick, red, damage, range); }
   public int slot() { return slot; }
   public String path() { return path; }
   public String skillId() { return "diarmuid_" + path; }
   public String actionId() { return "typemoonworld:diarmuid/" + path; }
   public String animationPath() { return "diarmuid_" + path; }
   public int networkId() { return ordinal(); }
   public double mana() { return mana; }
   public int cooldown() { return cooldown; }
   public int duration() { return duration; }
   public int angle() { return angle; }
   public int targets() { return targets; }
   public List<Hit> hits() { return hits; }
   public boolean basic() { return slot >= 10; }
   public boolean requiresRed() { return this == CROSS_GUARD || this == MANA_LEAP || hits.stream().anyMatch(Hit::red); }
   public boolean requiresYellow() { return this == CROSS_GUARD || this == MANA_LEAP || hits.stream().anyMatch(h -> !h.red()); }
   public double range() {
      return this == MANA_LEAP ? 6 : this == CROSS_GUARD ? RED_RANGE : hits.stream().mapToDouble(Hit::range).max().orElse(0);
   }
   public static DiarmuidAction bySlot(int slot) {
      for (var action : values()) if (action.slot == slot) return action;
      return null;
   }
   public static DiarmuidAction bySkillId(String id) {
      for (var action : values()) if (action.skillId().equals(id)) return action;
      return null;
   }
   public static DiarmuidAction byNetworkId(int id) {
      return id >= 0 && id < values().length ? values()[id] : null;
   }
}

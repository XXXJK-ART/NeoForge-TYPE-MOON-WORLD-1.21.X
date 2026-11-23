package net.xxxjk.TYPE_MOON_WORLD.servant.heracles;

import java.util.HashSet;
import java.util.Set;

/** One manifestation, eleven revivals; poison never creates a pending adaptation. */
public record GodHandState(int lives, Set<String> adapted, String pending, int remainingTicks, Set<String> specialImmunities) {
   public GodHandState {
      lives = Math.clamp(lives, 0, 11); adapted = Set.copyOf(adapted); specialImmunities = Set.copyOf(specialImmunities);
   }
   public static GodHandState fresh() { return new GodHandState(11, Set.of(), "", 0, Set.of()); }
   public GodHandState revive(String origin, boolean poison) {
      if (lives == 0) return this;
      return new GodHandState(lives - 1, adapted, poison || adapted.contains(origin) ? "" : origin,
         poison || adapted.contains(origin) ? 0 : 100, specialImmunities);
   }
   public GodHandState tick() {
      if (remainingTicks <= 0) return this;
      if (remainingTicks > 1) return new GodHandState(lives, adapted, pending, remainingTicks - 1, specialImmunities);
      var next = new HashSet<>(adapted); next.add(pending);
      return new GodHandState(lives, next, "", 0, specialImmunities);
   }
   public GodHandState special(String skill) {
      var next = new HashSet<>(specialImmunities); next.add(skill);
      return new GodHandState(lives - 1, adapted, "", 0, next);
   }
}

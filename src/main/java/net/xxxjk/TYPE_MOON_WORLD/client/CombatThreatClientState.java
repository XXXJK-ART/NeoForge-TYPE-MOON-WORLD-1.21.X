package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.List;

public final class CombatThreatClientState {
   public record Entry(String actionId, float dx, float dz, int danger, int ticksToImpact, boolean blockable, boolean dodgeable) { }
   private static List<Entry> threats = List.of();
   private CombatThreatClientState() { }
   public static void apply(List<Entry> value) { threats = value == null ? List.of() : List.copyOf(value); }
   public static List<Entry> threats() { return threats; }
   public static void clear() { threats = List.of(); }
}

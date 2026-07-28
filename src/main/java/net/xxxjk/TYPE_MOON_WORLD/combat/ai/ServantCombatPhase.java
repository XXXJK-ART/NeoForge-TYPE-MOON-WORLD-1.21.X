package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

public enum ServantCombatPhase {
   PROBING,
   NORMAL,
   DECISIVE,
   LAST_STAND;

   public static ServantCombatPhase byName(String name) {
      if (name == null) return PROBING;
      try { return valueOf(name.toUpperCase()); } catch (IllegalArgumentException ignored) { return PROBING; }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServantActionRegistry {
   private static volatile Map<String, ServantActionProfile> PROFILES = Map.of();
   private ServantActionRegistry() { }

   public static void reload(Map<String, ServantActionProfile> profiles) {
      PROFILES = Collections.unmodifiableMap(new LinkedHashMap<>(profiles));
   }

   public static ServantActionProfile get(String servantId) {
      return PROFILES.get(servantId);
   }

   public static Map<String, ServantActionProfile> all() { return PROFILES; }
}

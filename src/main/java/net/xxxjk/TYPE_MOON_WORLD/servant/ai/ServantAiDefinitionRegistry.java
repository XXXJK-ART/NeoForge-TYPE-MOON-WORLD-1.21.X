package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServantAiDefinitionRegistry {
   private static volatile Map<String, ServantAiDefinition> DEFINITIONS = Map.of();
   private ServantAiDefinitionRegistry() { }
   public static void reload(Map<String, ServantAiDefinition> values) {
      DEFINITIONS = Collections.unmodifiableMap(new LinkedHashMap<>(values));
   }
   public static ServantAiDefinition get(String id) {
      ServantAiDefinition value = DEFINITIONS.get(id);
      return value != null ? value : DEFINITIONS.get("typemoonworld:" + id);
   }
   public static Map<String, ServantAiDefinition> all() { return DEFINITIONS; }
}

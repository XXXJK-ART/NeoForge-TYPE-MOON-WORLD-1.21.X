package net.xxxjk.TYPE_MOON_WORLD.servant.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition;

public final class ServantSkillDataRegistry {
   private static volatile Map<String, ServantSkillDefinition> DEFINITIONS = Map.of();
   private ServantSkillDataRegistry() { }
   public static void reload(Map<String, ServantSkillDefinition> values) { DEFINITIONS = Collections.unmodifiableMap(new LinkedHashMap<>(values)); }
   public static ServantSkillDefinition get(String id) {
      ServantSkillDefinition value = DEFINITIONS.get(id);
      return value != null ? value : DEFINITIONS.get("typemoonworld:" + id);
   }
   public static Map<String, ServantSkillDefinition> all() { return DEFINITIONS; }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;

public final class ServantNoblePhantasmDataRegistry {
   private static volatile Map<String, ServantNoblePhantasmDefinition> DEFINITIONS = Map.of();
   private ServantNoblePhantasmDataRegistry() { }
   public static void reload(Map<String, ServantNoblePhantasmDefinition> values) { DEFINITIONS = Collections.unmodifiableMap(new LinkedHashMap<>(values)); }
   public static ServantNoblePhantasmDefinition get(String id) {
      ServantNoblePhantasmDefinition value = DEFINITIONS.get(id);
      return value != null ? value : DEFINITIONS.get("typemoonworld:" + id);
   }
   public static Map<String, ServantNoblePhantasmDefinition> all() { return DEFINITIONS; }
}

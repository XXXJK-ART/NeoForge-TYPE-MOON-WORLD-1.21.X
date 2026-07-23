package net.xxxjk.TYPE_MOON_WORLD.servant.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public final class ServantDataRegistry {
   private static volatile Map<String, ServantDefinition> DEFINITIONS = Map.of();

   private ServantDataRegistry() {
   }

   public static void reload(Map<String, ServantDefinition> newDefinitions) {
      DEFINITIONS = Collections.unmodifiableMap(new LinkedHashMap<>(newDefinitions == null ? Map.of() : newDefinitions));
   }

   @Nullable
   public static ServantDefinition get(String id) {
      if (id == null) return null;
      ServantDefinition value = DEFINITIONS.get(id);
      if (value != null) return value;
      String prefix = net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID + ":";
      return id.startsWith(prefix) ? DEFINITIONS.get(id.substring(prefix.length())) : DEFINITIONS.get(prefix + id);
   }

   public static Map<String, ServantDefinition> getAll() {
      return Collections.unmodifiableMap(DEFINITIONS);
   }

   public static boolean contains(String id) {
      return get(id) != null;
   }

   public static int size() {
      return DEFINITIONS.size();
   }
}

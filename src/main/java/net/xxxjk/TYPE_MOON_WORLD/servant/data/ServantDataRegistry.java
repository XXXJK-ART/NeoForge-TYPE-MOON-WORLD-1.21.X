package net.xxxjk.TYPE_MOON_WORLD.servant.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public final class ServantDataRegistry {
   private static final Map<String, ServantDefinition> DEFINITIONS = new HashMap<>();

   private ServantDataRegistry() {
   }

   public static void reload(Map<String, ServantDefinition> newDefinitions) {
      DEFINITIONS.clear();
      DEFINITIONS.putAll(newDefinitions);
   }

   @Nullable
   public static ServantDefinition get(String id) {
      return DEFINITIONS.get(id);
   }

   public static Map<String, ServantDefinition> getAll() {
      return Collections.unmodifiableMap(DEFINITIONS);
   }

   public static boolean contains(String id) {
      return DEFINITIONS.containsKey(id);
   }

   public static int size() {
      return DEFINITIONS.size();
   }
}

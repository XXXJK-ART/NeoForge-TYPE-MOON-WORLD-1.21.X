package net.xxxjk.TYPE_MOON_WORLD.magic.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;

public final class MagicDefinitionLoader extends SimpleJsonResourceReloadListener {
   public static final String DIRECTORY = "magic/definitions";
   private static final Gson GSON = new GsonBuilder().create();

   public MagicDefinitionLoader() {
      super(GSON, DIRECTORY);
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, MagicDefinitionData> loaded = new LinkedHashMap<>();
      for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
         try {
            MagicDefinitionData definition = MagicDefinitionData.fromJson(entry.getValue().getAsJsonObject()).withId(entry.getKey());
            loaded.put(definition.id().toString(), definition);
         } catch (Exception exception) {
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error(
               "Invalid magic definition {}: {}", entry.getKey(), exception.getMessage()
            );
         }
      }
      MagicDefinitionRegistry.reload(loaded);
   }
}

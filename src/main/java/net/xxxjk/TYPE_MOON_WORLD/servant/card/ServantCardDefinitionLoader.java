package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry;

/** Loads data-driven card slot bindings. Executors are registered by Java through the public API. */
public final class ServantCardDefinitionLoader extends SimpleJsonResourceReloadListener {
   public static final String DIRECTORY = "servant/cards";
   private static final Gson GSON = new GsonBuilder().create();

   public ServantCardDefinitionLoader() {
      super(GSON, DIRECTORY);
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
      CardActionRegistry.clearDataBindings();
      for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
         try {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject json = entry.getValue().getAsJsonObject();
            String servantId = normalizeId(entry.getKey(), getString(json, "servant_id", entry.getKey().getPath()));
            if (!json.has("actions") || !json.get("actions").isJsonArray()) continue;
            JsonArray actions = json.getAsJsonArray("actions");
            for (JsonElement element : actions) {
               if (!element.isJsonObject()) continue;
               JsonObject action = element.getAsJsonObject();
               int slot = action.has("slot") ? action.get("slot").getAsInt() : -2;
               String rawActionId = action.has("action") ? action.get("action").getAsString() : "";
               String actionId = rawActionId.isBlank() ? "" : normalizeId(entry.getKey(), rawActionId);
               String translationKey = getString(action, "translation_key", "");
               ResourceLocation servantKey = ResourceLocation.tryParse(servantId);
               ResourceLocation actionKey = ResourceLocation.tryParse(actionId);
               if (servantKey != null && actionKey != null && slot >= -1 && slot <= 9) {
                  CardActionRegistry.bindDataSlot(servantKey, slot, actionKey.toString(), translationKey);
               }
            }
         } catch (Exception exception) {
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Failed to load servant card definition {}", entry.getKey(), exception);
         }
      }
   }

   private static String normalizeId(ResourceLocation resource, String value) {
      if (value == null || value.isBlank()) return resource.toString();
      return value.contains(":") ? value : resource.getNamespace() + ":" + value;
   }

   private static String getString(JsonObject json, String key, String fallback) {
      return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : fallback;
   }
}

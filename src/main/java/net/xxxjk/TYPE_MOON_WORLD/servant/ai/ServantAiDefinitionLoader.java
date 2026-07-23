package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ServantAiDefinitionLoader extends SimpleJsonResourceReloadListener {
   public ServantAiDefinitionLoader() { super(new GsonBuilder().create(), "servant/ai"); }
   @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, ServantAiDefinition> loaded = new LinkedHashMap<>();
      for (var entry : resources.entrySet()) {
         ServantAiDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
            .resultOrPartial(error -> net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Invalid servant AI {}: {}", entry.getKey(), error))
            .ifPresent(value -> loaded.put(entry.getKey().toString(), new ServantAiDefinition(entry.getKey().toString(), value.movement(), value.combat(), value.social(), value.command(), value.environment())));
      }
      ServantAiDefinitionRegistry.reload(loaded);
   }
}

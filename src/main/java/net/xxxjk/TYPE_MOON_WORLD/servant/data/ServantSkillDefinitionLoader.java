package net.xxxjk.TYPE_MOON_WORLD.servant.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition;

public final class ServantSkillDefinitionLoader extends SimpleJsonResourceReloadListener {
   public ServantSkillDefinitionLoader() { super(new GsonBuilder().create(), "servant/skills"); }
   @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, ServantSkillDefinition> loaded = new LinkedHashMap<>();
      for (var entry : resources.entrySet()) ServantSkillDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
         .resultOrPartial(error -> net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Invalid servant skill {}: {}", entry.getKey(), error))
         .ifPresent(value -> loaded.put(entry.getKey().toString(), new ServantSkillDefinition(entry.getKey().toString(), value.displayName(), value.displayNameZh(), value.type(), value.mpCost(), value.cooldownTicks(), value.durationTicks(), value.effects(), value.ai())));
      ServantSkillDataRegistry.reload(loaded);
   }
}

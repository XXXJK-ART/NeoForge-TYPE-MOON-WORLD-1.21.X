package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class ServantActionLoader extends SimpleJsonResourceReloadListener {
   public ServantActionLoader() { super(new GsonBuilder().create(), "servant/actions"); }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, ServantActionProfile> loaded = new LinkedHashMap<>();
      for (var entry : resources.entrySet()) {
         ServantActionProfile.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
            .resultOrPartial(error -> TYPE_MOON_WORLD.LOGGER.error("Invalid servant action profile {}: {}", entry.getKey(), error))
            .ifPresent(profile -> loaded.put(profile.servant(), profile));
      }
      ServantActionRegistry.reload(loaded);
   }
}

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
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;

public final class ServantNoblePhantasmDefinitionLoader extends SimpleJsonResourceReloadListener {
   public ServantNoblePhantasmDefinitionLoader() { super(new GsonBuilder().create(), "servant/noble_phantasms"); }
   @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, ServantNoblePhantasmDefinition> loaded = new LinkedHashMap<>();
      for (var entry : resources.entrySet()) ServantNoblePhantasmDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
         .resultOrPartial(error -> net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Invalid noble phantasm {}: {}", entry.getKey(), error))
         .ifPresent(value -> loaded.put(entry.getKey().toString(), new ServantNoblePhantasmDefinition(entry.getKey().toString(), value.displayName(), value.displayNameZh(), value.type(), value.rank(), value.mpCost(), value.overChargeSupported(), value.overChargeLevels(), value.baseDamageMultiplier(), value.range(), value.effects(), value.specialAttackConditions(), value.classRestrictions())));
      ServantNoblePhantasmDataRegistry.reload(loaded);
   }
}

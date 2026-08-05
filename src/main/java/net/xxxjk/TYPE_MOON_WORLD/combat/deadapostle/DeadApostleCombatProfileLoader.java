package net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class DeadApostleCombatProfileLoader extends SimpleJsonResourceReloadListener {
   public static final String DIRECTORY = "dead_apostle/definitions";
   private static final Gson GSON = new GsonBuilder().create();
   private static volatile Map<String, DeadApostleCombatProfile> profiles = Map.of();

   public DeadApostleCombatProfileLoader() {
      super(GSON, DIRECTORY);
   }

   public static DeadApostleCombatProfile get(String id) {
      DeadApostleCombatProfile profile = profiles.get(id);
      return profile != null ? profile : DeadApostleCombatProfile.neroChaos();
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager,
                        ProfilerFiller profiler) {
      Map<String, DeadApostleCombatProfile> loaded = new HashMap<>();
      for (Map.Entry<ResourceLocation, JsonElement> entry : elements.entrySet()) {
         try {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject json = entry.getValue().getAsJsonObject();
            String id = string(json, "id", entry.getKey().getPath());
            loaded.put(id, parse(json));
         } catch (Exception error) {
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error(
               "Failed to load dead apostle combat profile {}", entry.getKey(), error);
         }
      }
      profiles = Map.copyOf(loaded);
   }

   private static DeadApostleCombatProfile parse(JsonObject json) {
      return new DeadApostleCombatProfile(
         number(json, "max_health", 400.0),
         number(json, "attack_damage", 20.0),
         number(json, "movement_speed", 0.32),
         number(json, "armor", 12.0),
         number(json, "armor_toughness", 4.0),
         number(json, "knockback_resistance", 0.30),
         number(json, "follow_range", 48.0),
         number(json, "stamina_max", 140.0),
         number(json, "stamina_regen_per_second", 14.0),
         number(json, "block_reduction", 0.35),
         number(json, "block_stamina_cost", 9.0),
         number(json, "dodge_chance", 0.72),
         number(json, "urgent_dodge_chance", 0.90),
         integer(json, "dodge_cooldown_ticks", 13),
         integer(json, "dodge_invulnerability_ticks", 9),
         number(json, "poise_max", 170.0),
         number(json, "poise_regen_per_second", 11.0),
         number(json, "poise_damage_per_hit", 10.0),
         integer(json, "guard_break_ticks", 18)
      );
   }

   private static String string(JsonObject json, String key, String fallback) {
      return json.has(key) ? json.get(key).getAsString() : fallback;
   }

   private static double number(JsonObject json, String key, double fallback) {
      return json.has(key) ? json.get(key).getAsDouble() : fallback;
   }

   private static int integer(JsonObject json, String key, int fallback) {
      return json.has(key) ? json.get(key).getAsInt() : fallback;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.vfx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary;
import org.junit.jupiter.api.Test;

class UshiwakamaruVfxResourcesTest {
   private static final Set<String> SUPPORTED_VANILLA_PARTICLES = Set.of(
      "cloud", "smoke", "large_smoke", "campfire_cosy_smoke", "ash", "crit", "enchanted_hit",
      "sweep_attack", "gust", "end_rod", "poof", "flame", "lava", "snowflake", "soul", "sculk_soul",
      "soul_fire_flame", "wax_on", "wax_off", "flash", "explosion", "explosion_emitter", "heart",
      "happy_villager", "witch", "reverse_portal", "totem_of_undying", "enchanted_hit_small"
   );
   private static final List<String> EFFECT_IDS = List.of(
      "servant_ushiwakamaru_benkei_shield",
      "servant_ushiwakamaru_charisma",
      "servant_ushiwakamaru_clone_manifest",
      "servant_ushiwakamaru_eight_boat",
      "servant_ushiwakamaru_phase",
      "servant_ushiwakamaru_six_secret",
      "servant_ushiwakamaru_slash",
      "servant_ushiwakamaru_spider_slayer",
      "servant_ushiwakamaru_spider_slayer_impact",
      "servant_ushiwakamaru_swallow_dodge",
      "servant_ushiwakamaru_tengu_strategy",
      "servant_ushiwakamaru_usumidori",
      "servant_ushiwakamaru_usumidori_impact"
   );

   @Test
   void allUshiwakamaruEffectsPassTheRuntimeParser() throws Exception {
      Method parseEffect = EffectLibrary.class.getDeclaredMethod("parseEffect", ResourceLocation.class, JsonObject.class);
      parseEffect.setAccessible(true);
      Gson gson = new Gson();

      for (String effectId : EFFECT_IDS) {
         String path = "/assets/typemoonworld/effects/" + effectId + ".json";
         var stream = UshiwakamaruVfxResourcesTest.class.getResourceAsStream(path);
         assertNotNull(stream, () -> "Missing VFX resource " + path);
         try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            for (var emitterElement : json.getAsJsonArray("emitters")) {
               JsonObject emitter = emitterElement.getAsJsonObject();
               if (!emitter.has("vanilla_particles")) continue;
               for (var particleElement : emitter.getAsJsonArray("vanilla_particles")) {
                  String particle = particleElement.getAsJsonObject().get("type").getAsString();
                  assertTrue(SUPPORTED_VANILLA_PARTICLES.contains(particle),
                     () -> "Unsupported vanilla particle " + particle + " in " + effectId);
               }
               emitter.remove("vanilla_particles");
            }
            assertDoesNotThrow(
               () -> parseEffect.invoke(null, ResourceLocation.fromNamespaceAndPath("typemoonworld", effectId), json),
               () -> "Invalid VFX runtime definition " + effectId
            );
         }
      }
   }
}

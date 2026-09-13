package net.xxxjk.TYPE_MOON_WORLD.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary;
import org.junit.jupiter.api.Test;

class VFXMathAndSchemaTest {
   @Test
   void migratedRepresentativeEffectsExposeScreenAndMaterialMetadata() throws Exception {
      Method parseEffect = EffectLibrary.class.getDeclaredMethod("parseEffect", ResourceLocation.class, JsonObject.class);
      parseEffect.setAccessible(true);
      Gson gson = new Gson();
      for (String id : new String[]{"ea_impact", "artoria_excalibur_beam"}) {
         String path = "/assets/typemoonworld/effects/" + id + ".json";
         var stream = getClass().getResourceAsStream(path);
         assertNotNull(stream, path);
         try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            for (var emitterElement : json.getAsJsonArray("emitters")) {
               emitterElement.getAsJsonObject().remove("vanilla_particles");
            }
            var definition = (net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEffectDefinition)parseEffect.invoke(null, ResourceLocation.fromNamespaceAndPath("typemoonworld", id), json);
            assertEquals(3, definition.screenEffects().size());
            assertNotNull(definition.emitters());
         }
      }
   }

   @Test
   void legacyEffectsReceiveProgramMaterialDefaults() throws Exception {
      Method parseEffect = EffectLibrary.class.getDeclaredMethod("parseEffect", ResourceLocation.class, JsonObject.class);
      parseEffect.setAccessible(true);
      Gson gson = new Gson();
      for (String id : new String[]{"beam", "lightning", "servant_sasaki_tsubame", "orias_ground_impact", "jeanne_alter_np_fire_ring"}) {
         String path = "/assets/typemoonworld/effects/" + id + ".json";
         try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            JsonObject json = gson.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            for (var emitterElement : json.getAsJsonArray("emitters")) {
               emitterElement.getAsJsonObject().remove("vanilla_particles");
            }
            var definition = (net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEffectDefinition)parseEffect.invoke(
               null, ResourceLocation.fromNamespaceAndPath("typemoonworld", id), json);
            assertTrue(definition.emitters().stream().allMatch(emitter -> !"vanilla".equals(emitter.material().shader())), id);
            assertTrue(definition.emitters().stream().anyMatch(emitter -> emitter.renderer() != net.xxxjk.TYPE_MOON_WORLD.vfx.VFXRendererType.BILLBOARD), id);
         }
      }
   }

   @Test
   void legacyImpactPresetIsAvailableThroughRuntimeDefaults() throws Exception {
      Method defaults = net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXClientRuntime.class.getDeclaredMethod(
         "screenEffectsFor", String.class, net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEffectDefinition.class);
      defaults.setAccessible(true);
      Method parseEffect = EffectLibrary.class.getDeclaredMethod("parseEffect", ResourceLocation.class, JsonObject.class);
      parseEffect.setAccessible(true);
      try (var stream = getClass().getResourceAsStream("/assets/typemoonworld/effects/beam_clash_impact.json")) {
         assertNotNull(stream);
         JsonObject json = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
         for (var emitterElement : json.getAsJsonArray("emitters")) {
            emitterElement.getAsJsonObject().remove("vanilla_particles");
         }
         var definition = (net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEffectDefinition)parseEffect.invoke(
            null, ResourceLocation.fromNamespaceAndPath("typemoonworld", "beam_clash_impact"), json);
         @SuppressWarnings("unchecked")
         var effects = (java.util.List<net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXScreenEffectDefinition>)defaults.invoke(null, "beam_clash_impact", definition);
         assertNotEquals(0, effects.size());
         assertEquals("white_flash", effects.get(0).type());
      }
   }

   @Test
   void everyEffectParsesIntoTheProgramMaterialPath() throws Exception {
      Method parseEffect = EffectLibrary.class.getDeclaredMethod("parseEffect", ResourceLocation.class, JsonObject.class);
      parseEffect.setAccessible(true);
      Gson gson = new Gson();
      Path root = Path.of("src/main/resources/assets/typemoonworld/effects");
      try (var paths = Files.list(root)) {
         long parsed = 0L;
         for (Path file : paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
            String id = file.getFileName().toString().replaceFirst("\\.json$", "");
            JsonObject json = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            for (var emitterElement : json.getAsJsonArray("emitters")) {
               emitterElement.getAsJsonObject().remove("vanilla_particles");
            }
            var definition = (net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEffectDefinition)parseEffect.invoke(
               null, ResourceLocation.fromNamespaceAndPath("typemoonworld", id), json);
            assertTrue(definition.emitters().stream().allMatch(emitter -> !"vanilla".equals(emitter.material().shader())), id);
            parsed++;
         }
         assertEquals(181L, parsed);
      }
   }
}

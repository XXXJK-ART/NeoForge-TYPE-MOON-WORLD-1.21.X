package net.xxxjk.TYPE_MOON_WORLD.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
}

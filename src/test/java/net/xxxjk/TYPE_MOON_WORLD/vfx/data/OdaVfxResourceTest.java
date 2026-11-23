package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;

class OdaVfxResourceTest {
   @Test
   void odaSkillAndNoblePhantasmEffectsUseSupportedVfxSchema() throws Exception {
      Path effects = Path.of("src/main/resources/assets/typemoonworld/effects");
      for (String id : List.of(
         "servant_oda_sniper_charge",
         "servant_oda_sniper_release",
         "servant_oda_maou_skill",
         "servant_oda_three_thousand_charge",
         "servant_oda_three_thousand",
         "servant_oda_hajun_charge",
         "servant_oda_hajun")) {
         Path file = effects.resolve(id + ".json");
         var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
         for (var emitter : json.getAsJsonArray("emitters")) {
            emitter.getAsJsonObject().remove("vanilla_particles");
         }
         var parsed = EffectLibrary.parseEffect(ResourceLocation.fromNamespaceAndPath("typemoonworld", id), json);
         assertTrue(parsed.duration() > 0.0F, id + " must have a positive duration");
         assertTrue(!parsed.emitters().isEmpty(), id + " must define emitters");
      }
   }
}

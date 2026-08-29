package net.xxxjk.TYPE_MOON_WORLD.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class RuneParticleResourcesTest {
   private static final Map<String, String> PARTICLES = new LinkedHashMap<>();
   static {
      for (String id : new String[] {"fehu", "uruz", "thurisaz", "ansuz", "raidho", "kenaz", "gebo", "wunjo", "hagalaz", "nauthiz", "isa", "jera", "eihwaz", "perthro", "algiz", "sowilo", "tiwaz", "berkano", "ehwaz", "mannaz", "laguz", "ingwaz", "dagaz", "othala"}) {
         PARTICLES.put(id + "_rune", id + "_rune");
      }
   }

   @Test
   void runeParticleSpritesResolveToBundledTextures() throws Exception {
      Gson gson = new Gson();
      for (Map.Entry<String, String> entry : PARTICLES.entrySet()) {
         String jsonPath = "/assets/typemoonworld/particles/" + entry.getKey() + ".json";
         try (InputStream stream = RuneParticleResourcesTest.class.getResourceAsStream(jsonPath)) {
            assertNotNull(stream, () -> "Missing particle definition " + jsonPath);
            JsonObject json = gson.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            String sprite = json.getAsJsonArray("textures").get(0).getAsString();
            assertEquals("typemoonworld:" + entry.getValue(), sprite,
               () -> "Unexpected sprite id in " + jsonPath);

            String texturePath = "/assets/typemoonworld/textures/particle/" + entry.getValue() + ".png";
            try (InputStream texture = RuneParticleResourcesTest.class.getResourceAsStream(texturePath)) {
               assertTrue(texture != null, () -> "Missing particle texture " + texturePath);
            }
         }
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class OdaVoiceResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void addedVoicesAreRegisteredAndKeepTheNoblePhantasmsSeparate() throws Exception {
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      assertEquals(List.of(
         "typemoonworld:voice/oda_nobunaga/attack1",
         "typemoonworld:voice/oda_nobunaga/attack2",
         "typemoonworld:voice/oda_nobunaga/attack3"
      ), soundNames(sounds, "oda_nobunaga_voice_attack"));
      assertEquals(List.of(
         "typemoonworld:voice/oda_nobunaga/skill1",
         "typemoonworld:voice/oda_nobunaga/skill2",
         "typemoonworld:voice/oda_nobunaga/skill3",
         "typemoonworld:voice/oda_nobunaga/skill4"
      ), soundNames(sounds, "oda_nobunaga_voice_skill"));
      assertEquals("typemoonworld:voice/oda_nobunaga/np", soundNames(sounds, "np_chant_oda_three_thousand").getFirst());
      assertEquals("typemoonworld:voice/oda_nobunaga/hajun_release", soundNames(sounds, "np_chant_oda_hajun").getFirst());

      JsonObject hajun = json("assets/typemoonworld/np_casts.json").getAsJsonObject("oda_hajun");
      assertEquals(95, hajun.get("voice_ticks").getAsInt());
      assertEquals("typemoonworld:voice/oda_nobunaga/hajun_release", hajun.get("sound").getAsString());

      for (String file : List.of("attack3.ogg", "skill1.ogg", "skill2.ogg", "skill3.ogg", "skill4.ogg", "hajun_release.ogg")) {
         byte[] bytes = Files.readAllBytes(RESOURCES.resolve("assets/typemoonworld/sounds/voice/oda_nobunaga/" + file));
         String contents = new String(bytes, StandardCharsets.ISO_8859_1);
         assertTrue(contents.startsWith("OggS"), file + " is missing the Ogg container");
         assertTrue(contents.contains("vorbis"), file + " is missing Vorbis audio");
         assertTrue(!contents.contains("theora"), file + " must not contain a video stream");
      }
   }

   private static List<String> soundNames(JsonObject sounds, String event) {
      JsonArray entries = sounds.getAsJsonObject(event).getAsJsonArray("sounds");
      return java.util.stream.StreamSupport.stream(entries.spliterator(), false)
         .map(entry -> entry.getAsJsonObject().get("name").getAsString())
         .toList();
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
   }
}

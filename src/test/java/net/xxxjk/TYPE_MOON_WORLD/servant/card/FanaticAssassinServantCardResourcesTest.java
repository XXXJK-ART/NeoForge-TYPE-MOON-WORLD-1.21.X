package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class FanaticAssassinServantCardResourcesTest {
   @Test
   void registryAndCardModelUseRealArmorAndAssassinBack() throws Exception {
      ServantCardRegistry.Entry entry = ServantCardRegistry.byId("fanatic_assassin");
      assertNotNull(entry);
      assertTrue(entry.hasRealArmor());
      try (var stream = resource("/assets/typemoonworld/models/item/servant_card_fanatic_assassin.json")) {
         var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         assertEquals("typemoonworld:item/servant_card_backs/assassin",
            json.getAsJsonObject("textures").get("back").getAsString());
      }
   }

   @Test
   void compressedCardFacesHaveExpectedDimensionsAndSize() throws Exception {
      assertImage("/assets/typemoonworld/textures/item/card_faces_3d/servant/fanatic_assassin_card.png",
         292, 500, 400_000);
      assertImage("/assets/typemoonworld/textures/item/servant_cards/fanatic_assassin_card.png",
         181, 256, 150_000);
   }

   @Test
   void suppliedBodyTuningAnimationAndTwoArmorPiecesArePresent() throws Exception {
      try (var stream = resource("/assets/typemoonworld/geo/servant_card_fanatic_assassin.geo.json")) {
         var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         var description = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
            .getAsJsonObject("description");
         assertEquals("geometry.servant_card_fanatic_assassin", description.get("identifier").getAsString());
         assertEquals(128, description.get("texture_width").getAsInt());
         assertEquals(128, description.get("texture_height").getAsInt());
      }
      try (var stream = resource("/assets/typemoonworld/animations/servant_card_fanatic_assassin.animation.json")) {
         var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         var animation = root.getAsJsonObject("animations").getAsJsonObject("1");
         assertNotNull(animation);
         assertTrue(animation.getAsJsonObject("bones").has("袍子"));
         assertTrue(animation.getAsJsonObject("bones").has("bone3"));
         assertTrue(animation.getAsJsonObject("bones").has("bone4"));
      }
      assertPresent("/assets/typemoonworld/models/item/servant_card_fanatic_assassin_head.json");
      assertPresent("/assets/typemoonworld/models/item/servant_card_fanatic_assassin_chest.json");
      assertImage("/assets/typemoonworld/textures/models/armor/servant_card_fanatic_assassin.png",
         128, 128, 100_000);
      assertFalse(resourceExists("/assets/typemoonworld/models/item/servant_card_fanatic_assassin_legs.json"));
   }

   private static void assertImage(String path, int width, int height, long maxBytes) throws Exception {
      try (var stream = resource(path)) {
         byte[] bytes = stream.readAllBytes();
         assertTrue(bytes.length <= maxBytes, path + " size");
         BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
         assertNotNull(image, path);
         assertEquals(width, image.getWidth(), path);
         assertEquals(height, image.getHeight(), path);
      }
   }

   private static void assertPresent(String path) throws Exception {
      try (var stream = resource(path)) {
         assertTrue(stream.read() >= 0, path);
      }
   }

   private static boolean resourceExists(String path) throws Exception {
      try (var stream = FanaticAssassinServantCardResourcesTest.class.getResourceAsStream(path)) {
         return stream != null;
      }
   }

   private static java.io.InputStream resource(String path) {
      var stream = FanaticAssassinServantCardResourcesTest.class.getResourceAsStream(path);
      assertNotNull(stream, path);
      return stream;
   }
}

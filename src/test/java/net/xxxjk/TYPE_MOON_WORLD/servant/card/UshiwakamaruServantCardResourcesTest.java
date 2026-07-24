package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class UshiwakamaruServantCardResourcesTest {
   @Test
   void cardModelUsesTheRiderBack() throws Exception {
      try (var stream = resource("/assets/typemoonworld/models/item/servant_card_ushiwakamaru_rider.json")) {
         var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         assertEquals("typemoonworld:item/servant_card_backs/rider",
            json.getAsJsonObject("textures").get("back").getAsString());
      }
   }

   @Test
   void cardFacesHaveTheExpectedCompressedDimensions() throws Exception {
      assertDimensions("/assets/typemoonworld/textures/item/servant_cards/ushiwakamaru_rider_card.png", 181, 256);
      assertDimensions("/assets/typemoonworld/textures/item/card_faces_3d/servant/ushiwakamaru_rider_card.png", 292, 500);
   }

   @Test
   void armorResourcesAndAnimationArePresent() throws Exception {
      String[] paths = {
         "/assets/typemoonworld/geo/servant_card_ushiwakamaru_rider.geo.json",
         "/assets/typemoonworld/animations/servant_card_ushiwakamaru_rider.animation.json",
         "/assets/typemoonworld/textures/models/armor/servant_card_ushiwakamaru_rider.png",
         "/assets/typemoonworld/models/item/servant_card_ushiwakamaru_rider_head.json",
         "/assets/typemoonworld/models/item/servant_card_ushiwakamaru_rider_chest.json",
         "/assets/typemoonworld/models/item/servant_card_ushiwakamaru_rider_legs.json"
      };
      for (String path : paths) {
         try (var stream = resource(path)) {
            assertTrue(stream.read() >= 0, path);
         }
      }
      try (var stream = resource("/assets/typemoonworld/animations/servant_card_ushiwakamaru_rider.animation.json")) {
         var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         assertTrue(json.getAsJsonObject("animations").has("1"));
      }
   }

   private static void assertDimensions(String path, int width, int height) throws Exception {
      try (var stream = resource(path)) {
         BufferedImage image = ImageIO.read(stream);
         assertNotNull(image, path);
         assertEquals(width, image.getWidth(), path);
         assertEquals(height, image.getHeight(), path);
      }
   }

   private static java.io.InputStream resource(String path) {
      var stream = UshiwakamaruServantCardResourcesTest.class.getResourceAsStream(path);
      assertNotNull(stream, path);
      return stream;
   }
}

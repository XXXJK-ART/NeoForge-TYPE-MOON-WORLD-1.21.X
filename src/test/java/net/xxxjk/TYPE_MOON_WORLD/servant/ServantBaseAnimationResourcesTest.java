package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServantBaseAnimationResourcesTest {
   private static final Path ASSETS = Path.of("src/main/resources/assets/typemoonworld");

   @Test
   void allBasicPlayerAndNpcMotionsAreRegisteredAsPlayableEmotes() throws Exception {
      for (String id : new String[]{
         "servant_base_sword_huijian", "servant_base_sword_huijian1",
         "servant_base_fist_qingji1", "servant_base_fist_qingji2", "servant_base_fist_lianji",
         "servant_base_dodge_small_shanbi1", "servant_base_dodge_small_shanbi2",
         "servant_base_dodge_large_forward", "servant_base_dodge_large_back",
         "servant_base_dodge_large_left", "servant_base_dodge_large_right"}) {
         JsonObject animation = json("player_animations/" + id + ".json");
         assertEquals(id, animation.get("name").getAsString(), id);
         JsonObject emote = animation.getAsJsonObject("emote");
         assertTrue(emote.get("endTick").getAsInt() > 0, id);
         JsonArray moves = emote.getAsJsonArray("moves");
         assertFalse(moves.isEmpty(), id);
      }
   }

   @Test
   void largeRightDodgeIsTheMirroredLeftDodge() throws Exception {
      JsonArray leftMoves = json("player_animations/servant_base_dodge_large_left.json")
         .getAsJsonObject("emote").getAsJsonArray("moves");
      JsonArray rightMoves = json("player_animations/servant_base_dodge_large_right.json")
         .getAsJsonObject("emote").getAsJsonArray("moves");
      JsonObject left = moveAt(leftMoves, 8);
      JsonObject right = moveAt(rightMoves, 8);
      assertRotationMirror(left.getAsJsonObject("torso"), right.getAsJsonObject("torso"));
      assertRotationMirror(left.getAsJsonObject("leftArm"), right.getAsJsonObject("rightArm"));
   }

   private static JsonObject moveAt(JsonArray moves, int tick) {
      for (var move : moves) if (move.getAsJsonObject().get("tick").getAsInt() == tick) return move.getAsJsonObject();
      throw new AssertionError("Missing mirrored dodge keyframe at tick " + tick);
   }

   private static void assertRotationMirror(JsonObject left, JsonObject right) {
      assertEquals(left.get("pitch").getAsDouble(), right.get("pitch").getAsDouble(), 1.0E-4);
      assertEquals(left.get("yaw").getAsDouble(), -right.get("yaw").getAsDouble(), 1.0E-4);
      assertEquals(left.get("roll").getAsDouble(), -right.get("roll").getAsDouble(), 1.0E-4);
   }

   private static JsonObject json(String path) throws Exception {
      try (var reader = Files.newBufferedReader(ASSETS.resolve(path))) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServantBaseAnimationResourcesTest {
   private static final Path ASSETS = Path.of("src/main/resources/assets/typemoonworld");

   @Test
   void tapsAreShortAndOnlyContinuousAttacksLoop() throws Exception {
      for (String name : new String[]{"servant_base_sword_huijian1", "servant_base_fist_qingji1", "servant_base_fist_qingji2"}) {
         JsonObject emote = json("player_animations/" + name + ".json").getAsJsonObject("emote");
         assertTrue(emote.get("endTick").getAsInt() <= 12, name + " should be a brief single strike");
         assertFalse(emote.has("isLoop") && emote.get("isLoop").getAsBoolean(), name);
      }
      for (String name : new String[]{"servant_base_sword_huijian", "servant_base_fist_lianji"}) {
         try (var stream = Files.newInputStream(ASSETS.resolve("player_animations/" + name + ".json"))) {
            KeyframeAnimation clip = (KeyframeAnimation) AnimationCodecs.deserialize("json", stream).iterator().next();
            assertTrue(clip.isInfinite, name + " must continue only until explicitly stopped");
            KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(clip);
            for (int tick = 0; tick < clip.endTick * 3; tick++) player.tick();
            assertTrue(player.isActive(), name + " stopped before the input was released");
         }
      }
   }

   @Test
   void heraclesIdleAndPursuitHoldTheWeaponHandAtRest() throws Exception {
      JsonObject animations = json("animations/heracles.animation.json").getAsJsonObject("animations");
      JsonObject idleArm = animations.getAsJsonObject("animation.heracles.idle")
         .getAsJsonObject("bones").getAsJsonObject("right arm").getAsJsonObject("rotation");
      JsonObject walkArm = animations.getAsJsonObject("animation.heracles.walk")
         .getAsJsonObject("bones").getAsJsonObject("right arm").getAsJsonObject("rotation");
      for (String tick : new String[]{"0", "3", "6"}) {
         JsonArray rotation = idleArm.getAsJsonArray(tick);
         assertEquals(-14, rotation.get(0).getAsDouble(), "idle tick " + tick);
         assertEquals(-8, rotation.get(1).getAsDouble(), "idle tick " + tick);
         assertEquals(-8, rotation.get(2).getAsDouble(), "idle tick " + tick);
      }
      JsonArray walkRotation = walkArm.getAsJsonObject("0.6").getAsJsonArray("vector");
      assertEquals(0, walkRotation.get(0).getAsDouble());
      assertEquals(0, walkRotation.get(1).getAsDouble());
      assertEquals(0, walkRotation.get(2).getAsDouble());
   }

   @Test
   void basicActionsPreserveVanillaHeadLookAndLegMovementThroughoutPlayback() throws Exception {
      try (var resources = Files.list(ASSETS.resolve("player_animations"))) {
         for (Path path : resources.filter(p -> p.getFileName().toString().startsWith("servant_base_")).toList()) {
            try (var stream = Files.newInputStream(path)) {
               KeyframeAnimation clip = (KeyframeAnimation) AnimationCodecs.deserialize("json", stream).iterator().next();
               assertTrue(clip.getPart("torso").isEnabled(), path.toString());
               assertTrue(clip.getPart("rightArm").isEnabled(), path.toString());
               for (int tick = 0; tick < clip.endTick; tick++) {
                  KeyframeAnimationPlayer sample = new KeyframeAnimationPlayer(clip, tick);
                  sample.setupAnim(0.5F);
                  // Real model pivots and nonzero vanilla look/walk angles must pass through unchanged.
                  assertUnchanged(sample, "head", TransformType.POSITION, new Vec3f(0, 0, 0), path, tick);
                  assertUnchanged(sample, "leftLeg", TransformType.POSITION, new Vec3f(1.9F, 12, 0.1F), path, tick);
                  assertUnchanged(sample, "rightLeg", TransformType.POSITION, new Vec3f(-1.9F, 12, 0.1F), path, tick);
                  for (String part : new String[]{"head", "leftLeg", "rightLeg"}) {
                     assertUnchanged(sample, part, TransformType.ROTATION, new Vec3f(0.3F, -0.2F, 0.1F), path, tick);
                  }
               }
            }
         }
      }
   }

   private static void assertUnchanged(KeyframeAnimationPlayer sample, String part, TransformType type,
                                       Vec3f vanilla, Path path, int tick) {
      Vec3f actual = sample.get3DTransform(part, type, 0.5F, vanilla);
      String message = path.getFileName() + " " + part + " " + type + " tick=" + tick;
      assertEquals(vanilla.getX(), actual.getX(), 1.0E-5F, message + " x");
      assertEquals(vanilla.getY(), actual.getY(), 1.0E-5F, message + " y");
      assertEquals(vanilla.getZ(), actual.getZ(), 1.0E-5F, message + " z");
   }

   @Test
   void allBasicPlayerAndNpcMotionsAreRegisteredAsPlayableEmotes() throws Exception {
      for (String id : new String[]{
         "servant_base_sword_huijian", "servant_base_sword_huijian1",
         "servant_base_fist_qingji1", "servant_base_fist_qingji2", "servant_base_fist_lianji",
         "servant_base_dodge_small_shanbi1", "servant_base_dodge_small_shanbi2",
         "servant_base_dodge_large_forward", "servant_base_dodge_large_back",
         "servant_base_dodge_large_left", "servant_base_dodge_large_right", "servant_base_dodge_large_forward_right"}) {
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

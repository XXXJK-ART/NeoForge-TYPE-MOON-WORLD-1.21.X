package net.xxxjk.TYPE_MOON_WORLD.servant.cursedarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.loading.math.MathParser;

class CursedArmHassanWalkAnimationTest {
   private static JsonObject walkBones() throws Exception {
      return JsonParser.parseString(Files.readString(Path.of(
         "src/main/resources/assets/typemoonworld/animations/cursed_arm_hassan.animation.json")))
         .getAsJsonObject().getAsJsonObject("animations")
         .getAsJsonObject("animation.cursed_arm_hassan.walk").getAsJsonObject("bones");
   }

   @Test
   void walkUsesMovementPhaseForEverySwayAndLeavesHeadToLookControl() throws Exception {
      var bones = walkBones();
      assertFalse(bones.has("bone"), "Walking must not add a timed whole-body bounce");
      assertFalse(bones.has("head"));
      for (var bone : bones.entrySet()) {
         var rotation = bone.getValue().getAsJsonObject().get("rotation");
         assertTrue(rotation.isJsonArray(), bone.getKey() + " still uses timed keyframes");
         boolean changesWithPhase = false;
         for (var axis : rotation.getAsJsonArray()) {
            var expression = MathParser.parseJson(axis);
            MathParser.getVariableFor("query.cursed_arm_walk_amount").set(1);
            MathParser.getVariableFor("query.cursed_arm_walk_phase").set(0);
            double start = expression.get();
            MathParser.getVariableFor("query.cursed_arm_walk_phase").set(180);
            changesWithPhase |= Math.abs(start - expression.get()) > 0.00001;
            MathParser.getVariableFor("query.cursed_arm_walk_amount").set(0);
            assertEquals(0, expression.get(), 0.00001, bone.getKey() + " must settle at rest");
         }
         assertTrue(changesWithPhase, bone.getKey() + " must follow movement phase");
      }
   }

   @Test
   void legPhaseFollowsDistanceAndStrideFadesOutAtRest() throws Exception {
      var bones = walkBones();
      var left = MathParser.parseJson(bones.getAsJsonObject("left_leg").getAsJsonArray("rotation").get(0));
      var right = MathParser.parseJson(bones.getAsJsonObject("right_leg").getAsJsonArray("rotation").get(0));
      MathParser.getVariableFor("query.cursed_arm_walk_amount").set(1);
      for (double phase : new double[] {0, 90, 180, 270, 360}) {
         MathParser.getVariableFor("query.cursed_arm_walk_phase").set(phase);
         assertEquals(-left.get(), right.get(), 0.00001);
         assertEquals(24 * Math.cos(Math.toRadians(phase)), right.get(), 0.00001);
      }
      MathParser.getVariableFor("query.cursed_arm_walk_amount").set(0);
      assertEquals(0, left.get(), 0.00001);
      assertEquals(0, right.get(), 0.00001);
   }
}

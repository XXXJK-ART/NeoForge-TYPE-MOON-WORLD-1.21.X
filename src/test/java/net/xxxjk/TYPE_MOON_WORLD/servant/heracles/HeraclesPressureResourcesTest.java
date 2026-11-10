package net.xxxjk.TYPE_MOON_WORLD.servant.heracles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HeraclesPressureResourcesTest {
   @Test
   void loaderKeepsEveryHeraclesActionUsedByTheExecutor() throws Exception {
      JsonObject model = readJson("/data/typemoonworld/servant/definitions/heracles.json").getAsJsonObject("model");
      var method = ServantDefinitionLoader.class.getDeclaredMethod("parseAnimations", JsonObject.class, String.class);
      method.setAccessible(true);
      var loaded = (ServantAnimations)method.invoke(new ServantDefinitionLoader(), model, model.get("animation").getAsString());
      for (var entry : model.getAsJsonObject("animations").entrySet()) {
         if (entry.getKey().equals("idle") || entry.getKey().equals("walk")) continue;
         assertEquals(entry.getValue().getAsString(), loaded.actionAnimation(entry.getKey()).orElse(null), entry.getKey());
      }
   }

   @Test
   void playerAnimatorLoadsRecoveryAndAdaptationOnThePlayerSkeleton() throws IOException {
      for (String name : new String[]{"heracles_revive", "heracles_adapt", "heracles_impact"}) {
         String path = "/assets/typemoonworld/player_animations/" + name + ".json";
         try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            var animations = AnimationCodecs.deserialize("json", stream);
            assertEquals(1, animations.size(), path);
            assertEquals(name, animations.iterator().next().getName().replace("\"", ""), path);
         }
      }
   }
   @Test
   void pressureActionsHaveDistinctReadableMotion() throws IOException {
      JsonObject definition = readJson("/data/typemoonworld/servant/definitions/heracles.json");
      JsonObject mappedActions = definition.getAsJsonObject("model").getAsJsonObject("animations");
      JsonObject animationFile = readJson("/assets/typemoonworld/animations/heracles.animation.json")
         .getAsJsonObject("animations");

      for (String action : new String[]{"revive", "slam_windup"}) {
         assertTrue(mappedActions.has(action), action);
         String animationName = mappedActions.get(action).getAsString();
         JsonObject animation = animationFile.getAsJsonObject(animationName);
         assertNotNull(animation, animationName);
         assertEquals(false, animation.get("loop").getAsBoolean(), animationName);
         assertTrue(animation.getAsJsonObject("bones").has("upper_body"), animationName);
      }

      JsonObject approach = animationFile.getAsJsonObject("animation.heracles.frenzy_walk");
      assertEquals(1.6, approach.get("animation_length").getAsDouble(), 0.001);
      assertTrue(approach.getAsJsonObject("bones").has("right leg"));
      for (String arm : new String[]{"right arm", "left arm"}) {
         var rotations = approach.getAsJsonObject("bones").getAsJsonObject(arm).getAsJsonObject("rotation");
         double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
         for (var frame : rotations.entrySet()) {
            double pitch = frame.getValue().getAsJsonArray().get(0).getAsDouble();
            min = Math.min(min, pitch); max = Math.max(max, pitch);
         }
         assertTrue(max - min <= 8, "pursuit must keep the weapon arm steady: " + arm);
      }

      JsonObject idle = animationFile.getAsJsonObject("animation.heracles.idle");
      assertEquals(6.0, idle.get("animation_length").getAsDouble(), 0.001);
      var idleBody = idle.getAsJsonObject("bones").getAsJsonObject("upper_body").getAsJsonObject("rotation");
      double maxIdleLean = 0;
      boolean hasBriefDip = false;
      for (var frame : idleBody.entrySet()) maxIdleLean = Math.max(maxIdleLean,
         Math.abs(frame.getValue().getAsJsonArray().get(0).getAsDouble()));
      for (var frame : idleBody.entrySet()) hasBriefDip |= frame.getValue().getAsJsonArray().get(0).getAsDouble() > 10;
      assertTrue(maxIdleLean <= 20 && hasBriefDip, "idle should stay upright with only a brief forward dip");
   }

   private static JsonObject readJson(String path) throws IOException {
      try (var stream = HeraclesPressureResourcesTest.class.getResourceAsStream(path)) {
         assertNotNull(stream, path);
         return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
      }
   }
}

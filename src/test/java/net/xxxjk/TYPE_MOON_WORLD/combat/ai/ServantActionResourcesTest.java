package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ServantActionResourcesTest {
   private static final Path ACTIONS = Path.of("src/main/resources/data/typemoonworld/servant/actions");

   @Test
   void allServantProfilesDecodeAndExerciseRuntimeFields() throws Exception {
      Set<String> servants = new HashSet<>();
      try (var paths = Files.list(ACTIONS)) {
         long explicitSharedActions = 0;
         for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
            var json = JsonParser.parseString(Files.readString(path));
            ServantActionProfile profile = ServantActionProfile.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
            servants.add(profile.servant());
            assertTrue(profile.actions().size() >= 2, path.toString());
            for (AiActionDescriptor action : profile.actions()) {
               assertTrue(action.maximumRange() >= action.minimumRange());
               assertTrue(action.timing().activeTicks() >= 1);
               assertTrue(action.threat().danger() > 0);
               assertFalse(action.tags().isEmpty());
               if (!action.maneuver().equals(AiActionDescriptor.ManeuverSpec.NONE)) {
                  assertTrue(action.maneuver().effectiveApproachRange(action.maximumRange()) <= 48.0);
                  assertTrue(action.maneuver().effectiveDamageScale(
                     action.tags().contains(AiActionDescriptor.Tag.FINISHER)) > 0.0);
                  assertTrue(action.maneuver().interruptResistance() >= 0);
                  explicitSharedActions++;
               }
            }
         }
         assertTrue(explicitSharedActions >= 8, "too few explicitly shared actions");
      }
      assertEquals(24, servants.size());
      assertTrue(servants.contains("arash"));
      assertTrue(servants.contains("artoria_pendragon"));
      assertTrue(servants.contains("gilgamesh"));
      assertTrue(servants.contains("nightingale"));
      assertTrue(servants.contains("hundred_faces_hassan"));
      assertTrue(servants.contains("diarmuid_ua_duibhne"));
      assertTrue(servants.contains("lancelot_berserker"));
      assertTrue(servants.contains("zhao_yun_rider"));
      try (var paths = Files.list(ACTIONS)) {
         long maneuverProfiles = paths.filter(file -> file.toString().endsWith(".json")).filter(file -> {
            try {
               ServantActionProfile profile = ServantActionProfile.CODEC.parse(JsonOps.INSTANCE,
                  JsonParser.parseString(Files.readString(file))).result().orElseThrow();
               return profile.actions().stream().anyMatch(action -> !action.maneuver().equals(AiActionDescriptor.ManeuverSpec.NONE));
            } catch (Exception exception) {
               throw new RuntimeException(exception);
            }
         }).count();
         assertTrue(maneuverProfiles >= 8, "too few servants participate in the shared maneuver system");
      }
   }

   @Test
   void terrainTierDefaultsMatchAcceptanceValues() {
      assertEquals(1.5, net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.CHIP).radius());
      assertEquals(2.5, net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL).radius());
      assertEquals(4.0, net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.MEDIUM).radius());
      assertEquals(6.0, net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.HEAVY).radius());
      assertTrue(net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.MEDIUM).limitsSelfFootDepth());
      assertEquals(0, net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL).physicalDebrisCount());
      assertEquals(4, net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.MEDIUM).physicalDebrisCount());
      assertEquals(8, net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.HEAVY).physicalDebrisCount());
   }

   @Test
   void sharedActionCooldownIncludesEveryCommittedPhase() {
      String json = "{\"id\":\"typemoonworld:test/shared\",\"tags\":[\"melee\"],"
         + "\"timing\":{\"windup\":5,\"active\":4,\"recovery\":7},"
         + "\"maneuver\":{\"combo_cost\":2}}";
      AiActionDescriptor action = AiActionDescriptor.CODEC.parse(JsonOps.INSTANCE,
         JsonParser.parseString(json)).result().orElseThrow();
      assertEquals(20, ServantPlannedActionExecutor.cooldownTicks(action));
   }
}

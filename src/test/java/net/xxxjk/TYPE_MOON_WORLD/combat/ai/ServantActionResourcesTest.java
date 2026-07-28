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
   void allNineteenProfilesDecodeAndExerciseRuntimeFields() throws Exception {
      Set<String> servants = new HashSet<>();
      try (var paths = Files.list(ACTIONS)) {
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
            }
         }
      }
      assertEquals(19, servants.size());
      assertTrue(servants.contains("arash"));
      assertTrue(servants.contains("artoria_pendragon"));
      assertTrue(servants.contains("gilgamesh"));
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
}

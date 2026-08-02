package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import net.xxxjk.typemoonworld.api.AdvancedAiTacticProfile;
import net.xxxjk.typemoonworld.api.AiCombatStyle;
import net.xxxjk.typemoonworld.api.AiTacticProfile;
import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;

class ServantTacticalResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources/data/typemoonworld/servant");

   @Test
   void allServantsUseDedicatedBoundedTacticalProfiles() throws Exception {
      Set<String> profileIds = new HashSet<>();
      try (var definitions = Files.list(RESOURCES.resolve("definitions"))) {
         for (Path path : definitions.filter(file -> file.toString().endsWith(".json")).toList()) {
            var definition = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            String profileId = definition.get("ai_config").getAsString();
            assertFalse(profileId.startsWith("default_"), path.toString());
            assertTrue(profileIds.add(profileId), "shared profile: " + profileId);
            Path profilePath = RESOURCES.resolve("ai").resolve(profileId + ".json");
            assertTrue(Files.isRegularFile(profilePath), profilePath.toString());
            ServantAiDefinition profile = ServantAiDefinition.CODEC.parse(JsonOps.INSTANCE,
               JsonParser.parseString(Files.readString(profilePath))).result().orElseThrow();
            assertTrue(profile.tactical().preferredRange() >= profile.tactical().minimumRange());
            assertTrue(profile.tactical().maximumRange() >= profile.tactical().preferredRange());
            assertTrue(profile.tactical().maximumRange() <= 48.0);
         }
      }
      assertEquals(21, profileIds.size());
   }

   @Test
   void addonAdvancedProfileMapsIntoUnifiedTacticalFields() {
      AiTacticProfile base = new AiTacticProfile(30, 14, 0.25, 9, List.of());
      AdvancedAiTacticProfile advanced = new AdvancedAiTacticProfile(base, AiCombatStyle.AMBUSHER,
         3.0, 12.0, 30.0, 11.0, 0.8, 5.0, 0.7, 0.6, 0.9, "medium");
      ServantAiDefinition.Tactical tactical = ServantTacticalProfileResolver.fromAdvanced(advanced);
      assertEquals("ambusher", tactical.style());
      assertEquals(11.0, tactical.repositionDistance());
      assertEquals(0.7, tactical.verticalMobility());
      assertEquals(0.9, tactical.collateralCaution());
      assertEquals("medium", tactical.maximumTerrainImpact());
   }

   @Test
   void sideForwardReengagementAlwaysAdvancesAndStrafes() {
      Vec3 toward = new Vec3(1.0, 0.0, 0.0);
      for (double angle : new double[]{45.0, -45.0, 70.0, -70.0}) {
         Vec3 direction = ServantManeuverService.sideForwardDirection(toward, angle);
         assertTrue(direction.dot(toward) > 0.0, "must retain a forward component");
         assertTrue(Math.abs(direction.z) > 0.5, "must retain a lateral component");
      }
   }
}

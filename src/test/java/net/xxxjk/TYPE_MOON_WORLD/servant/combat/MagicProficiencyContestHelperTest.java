package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MagicProficiencyContestHelperTest {
   @Test
   void strictGapThresholdsUseAbsoluteProficiencyPoints() {
      assertEquals(
         MagicProficiencyContestHelper.Result.NORMAL,
         MagicProficiencyContestHelper.contestByValues(70.0, 80.0)
      );
      assertEquals(
         MagicProficiencyContestHelper.Result.RESISTED,
         MagicProficiencyContestHelper.contestByValues(69.9, 80.0)
      );
      assertEquals(
         MagicProficiencyContestHelper.Result.RESISTED,
         MagicProficiencyContestHelper.contestByValues(50.0, 80.0)
      );
      assertEquals(
         MagicProficiencyContestHelper.Result.COUNTERED,
         MagicProficiencyContestHelper.contestByValues(49.9, 80.0)
      );
   }

   @Test
   void lowerOrEqualTargetProficiencyDoesNotBlockMagic() {
      assertEquals(
         MagicProficiencyContestHelper.Result.NORMAL,
         MagicProficiencyContestHelper.contestByValues(80.0, 69.0)
      );
      assertEquals(
         MagicProficiencyContestHelper.Result.NORMAL,
         MagicProficiencyContestHelper.contestByValues(80.0, 80.0)
      );
   }

   @Test
   void fixedProficiencyMappingUsesExistingFields() throws Exception {
      String helper = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/combat/MagicProficiencyContestHelper.java"));

      assertTrue(helper.contains("case \"suggestion_magic\" -> vars.proficiency_suggestion_magic"));
      assertTrue(helper.contains("case \"binding_magic\" -> vars.proficiency_binding_magic"));
      assertTrue(helper.contains("case \"gravity_magic\" -> vars.proficiency_gravity_magic"));
      assertTrue(helper.contains("case \"gander\", \"gandr_machine_gun\" -> vars.proficiency_gander"));
      assertTrue(helper.contains("case \"fire_magic\", \"ruby_flame_sword\" -> vars.proficiency_fire_magic"));
      assertTrue(helper.contains("\"jewel_machine_gun\""));
      assertTrue(helper.contains("vars.proficiency_jewel_magic_shoot"));
      assertTrue(helper.contains("case \"jewel_magic_release\", \"emerald_use\" -> vars.proficiency_jewel_magic_release"));
   }

   @Test
   void magicIdsNormalizeNamespaceAndMartialIdsAreRecognizedByCaller() {
      assertEquals("gander", MagicProficiencyContestHelper.normalizeMagicId("typemoonworld:gander"));
      assertEquals("bajiquan", MagicProficiencyContestHelper.normalizeMagicId("bajiquan"));
   }
}

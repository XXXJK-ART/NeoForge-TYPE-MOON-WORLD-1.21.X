package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import org.junit.jupiter.api.Test;

class GemCompatibilityServiceTest {
   private static final List<String> AREA_MAGICS = List.of(
      "healing_magic", "suggestion_magic", "binding_magic",
      "fire_magic", "water_magic", "wind_magic", "earth_magic"
   );

   @Test
   void areaMagicsAreAvailableForEngraving() {
      for (String magicId : AREA_MAGICS) {
         assertTrue(GemCompatibilityService.isWhitelistedMagic(magicId), magicId);
      }
   }

   @Test
   void matchingGemsImproveElementalEngravingChance() {
      assertHigherAffinity(GemType.RUBY, GemType.SAPPHIRE, "fire_magic");
      assertHigherAffinity(GemType.SAPPHIRE, GemType.RUBY, "water_magic");
      assertHigherAffinity(GemType.CYAN, GemType.TOPAZ, "wind_magic");
      assertHigherAffinity(GemType.TOPAZ, GemType.CYAN, "earth_magic");
      assertHigherAffinity(GemType.EMERALD, GemType.BLACK_SHARD, "healing_magic");
   }

   @Test
   void betterQualityImprovesEngravingChance() {
      int poor = GemCompatibilityService.calculateEngraveSuccessChance(GemQuality.POOR, GemType.RUBY, "fire_magic", 50.0);
      int normal = GemCompatibilityService.calculateEngraveSuccessChance(GemQuality.NORMAL, GemType.RUBY, "fire_magic", 50.0);
      int high = GemCompatibilityService.calculateEngraveSuccessChance(GemQuality.HIGH, GemType.RUBY, "fire_magic", 50.0);
      assertTrue(poor < normal);
      assertTrue(normal < high);
   }

   private static void assertHigherAffinity(GemType preferred, GemType opposed, String magicId) {
      int preferredChance = GemCompatibilityService.calculateEngraveSuccessChance(GemQuality.NORMAL, preferred, magicId, 50.0);
      int opposedChance = GemCompatibilityService.calculateEngraveSuccessChance(GemQuality.NORMAL, opposed, magicId, 50.0);
      assertTrue(preferredChance > opposedChance, magicId);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GawainSunlightRulesTest {
   @Test
   void clearDirectDaylightActivatesNumeralOfTheSaint() {
      assertTrue(GawainSunlightRules.isActive(true, 0L, false, false, true));
      assertTrue(GawainSunlightRules.isActive(true, 11999L, false, false, true));
      assertTrue(GawainSunlightRules.isActive(true, 24000L, false, false, true));
   }

   @Test
   void nightWeatherCoverAndSkylessDimensionsDisableIt() {
      assertFalse(GawainSunlightRules.isActive(true, 12000L, false, false, true));
      assertFalse(GawainSunlightRules.isActive(true, 6000L, true, false, true));
      assertFalse(GawainSunlightRules.isActive(true, 6000L, false, true, true));
      assertFalse(GawainSunlightRules.isActive(true, 6000L, false, false, false));
      assertFalse(GawainSunlightRules.isActive(false, 6000L, false, false, true));
   }

   @Test
   void negativeWorldTimesUseAStableDayCycle() {
      assertTrue(GawainSunlightRules.isActive(true, -24000L, false, false, true));
      assertFalse(GawainSunlightRules.isActive(true, -1L, false, false, true));
   }
}

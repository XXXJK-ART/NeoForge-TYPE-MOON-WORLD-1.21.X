package net.xxxjk.TYPE_MOON_WORLD.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ChurchDeadApostleRulesTest {
   @Test void blackKeyDamageUsesExpansionAndCount() {
      assertEquals(3.0F, ChurchDeadApostleRules.blackKeyMeleeDamage(false, 3));
      assertEquals(9.0F, ChurchDeadApostleRules.blackKeyMeleeDamage(true, 1));
      assertEquals(12.0F, ChurchDeadApostleRules.blackKeyMeleeDamage(true, 2));
      assertEquals(15.0F, ChurchDeadApostleRules.blackKeyMeleeDamage(true, 3));
   }

   @Test void stigmaAndCassockBoundariesAreExact() {
      assertTrue(ChurchDeadApostleRules.shouldStigmaDelay(20.0F, false));
      assertFalse(ChurchDeadApostleRules.shouldStigmaDelay(21.0F, false));
      assertFalse(ChurchDeadApostleRules.shouldStigmaDelay(9.0F, true));
      assertTrue(ChurchDeadApostleRules.cassockBlocks(9.999F));
      assertFalse(ChurchDeadApostleRules.cassockBlocks(10.0F));
   }

   @Test void blackKeyThrowsUseSymmetricYawSpread() {
      assertEquals(0.0F, ChurchDeadApostleRules.blackKeyThrowYawOffset(1, 0));
      assertEquals(-3.5F, ChurchDeadApostleRules.blackKeyThrowYawOffset(2, 0));
      assertEquals(3.5F, ChurchDeadApostleRules.blackKeyThrowYawOffset(2, 1));
      assertEquals(-7.0F, ChurchDeadApostleRules.blackKeyThrowYawOffset(3, 0));
      assertEquals(0.0F, ChurchDeadApostleRules.blackKeyThrowYawOffset(3, 1));
      assertEquals(7.0F, ChurchDeadApostleRules.blackKeyThrowYawOffset(3, 2));
   }

   @Test void blackKeyOnlyBreaksLowHardnessOrdinaryBlocks() {
      assertEquals(2, ChurchDeadApostleRules.BLACK_KEY_MAX_BROKEN_BLOCKS);
      assertTrue(ChurchDeadApostleRules.blackKeyCanBreakBlock(0.0F, false, false));
      assertTrue(ChurchDeadApostleRules.blackKeyCanBreakBlock(1.5F, false, false));
      assertFalse(ChurchDeadApostleRules.blackKeyCanBreakBlock(1.5001F, false, false));
      assertFalse(ChurchDeadApostleRules.blackKeyCanBreakBlock(-1.0F, false, false));
      assertFalse(ChurchDeadApostleRules.blackKeyCanBreakBlock(1.0F, true, false));
      assertFalse(ChurchDeadApostleRules.blackKeyCanBreakBlock(1.0F, false, true));
   }

   @Test void conversionDistributionBoundariesMatchSpecification() {
      assertEquals(ChurchDeadApostleRules.ConversionStage.THE_DEAD, ChurchDeadApostleRules.conversionStage(0.699999));
      assertEquals(ChurchDeadApostleRules.ConversionStage.GHOUL, ChurchDeadApostleRules.conversionStage(0.70));
      assertEquals(ChurchDeadApostleRules.ConversionStage.LIVING_DEAD, ChurchDeadApostleRules.conversionStage(0.99));
      assertEquals(ChurchDeadApostleRules.ConversionStage.NIGHT_KIN, ChurchDeadApostleRules.conversionStage(0.999));
   }
}

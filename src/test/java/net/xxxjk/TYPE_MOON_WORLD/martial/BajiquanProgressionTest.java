package net.xxxjk.TYPE_MOON_WORLD.martial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BajiquanProgressionTest {
   @Test void moveThresholdsMatchDesign() {
      assertEquals(0.0, BajiquanMove.PUNCH.requiredProficiency());
      assertEquals(0.5, BajiquanMove.RIGHT_KICK.requiredProficiency());
      assertEquals(1.0, BajiquanMove.ELBOW.requiredProficiency());
      assertEquals(5.0, BajiquanMove.CHOP.requiredProficiency());
      assertEquals(10.0, BajiquanMove.CLAMP.requiredProficiency());
      assertEquals(25.0, BajiquanMove.HIGH_JUMP.requiredProficiency());
      assertEquals(30.0, BajiquanMove.PARRY.requiredProficiency());
      assertEquals(40.0, BajiquanMove.PUSH.requiredProficiency());
      assertEquals(50.0, BajiquanMove.FA_JIN.requiredProficiency());
      assertEquals(80.0, BajiquanMove.FIERCE_TIGER.requiredProficiency());
      assertEquals(100.0, BajiquanMove.CIRCLE_REALM.requiredProficiency());
   }

   @Test void bodyPointCurveTotalsEightThousandTwoHundred() {
      int total = 0;
      for (int earned = 0; earned < 40; earned++) total += BodyTrainingService.pointCostForEarned(earned);
      assertEquals(8200, total);
      assertEquals(10, BodyTrainingService.pointCostForEarned(0));
      assertEquals(400, BodyTrainingService.pointCostForEarned(39));
      assertEquals(0, BodyTrainingService.pointCostForEarned(40));
   }

   @Test void stagedAttributesMatchTenLevelCurve() {
      assertEquals(0.05, BodyTrainingService.stagedPercent(5), 1.0E-9);
      assertEquals(0.13, BodyTrainingService.stagedPercent(9), 1.0E-9);
      assertEquals(0.18, BodyTrainingService.stagedPercent(10), 1.0E-9);
      assertEquals(1.0, BodyTrainingService.strengthBonus(5), 1.0E-9);
      assertEquals(2.6, BodyTrainingService.strengthBonus(9), 1.0E-9);
      assertEquals(3.6, BodyTrainingService.strengthBonus(10), 1.0E-9);
   }

   @Test void legalComboCancelsAreExplicit() {
      assertTrue(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.PUNCH, BajiquanMove.SHOULDER, false));
      assertTrue(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.FLURRY, BajiquanMove.PALM, false));
      assertTrue(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.PALM, BajiquanMove.CHARGED_TREMOR, false));
      assertTrue(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.CHARGED_TREMOR, BajiquanMove.DOUBLE_PALM, false));
      assertTrue(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.KNEE, BajiquanMove.DOWN_KICK, false));
      assertTrue(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.PARRY, BajiquanMove.FA_JIN, false));
      assertTrue(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.PUNCH, BajiquanMove.FIERCE_TIGER, false));
      assertFalse(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.PALM, BajiquanMove.PUNCH, false));
      assertFalse(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.DOUBLE_PALM, BajiquanMove.PUNCH, false));
      assertFalse(BajiquanComboRules.isLegalRecoveryCancel(BajiquanMove.STOMP, BajiquanMove.PUSH, false));
   }
}

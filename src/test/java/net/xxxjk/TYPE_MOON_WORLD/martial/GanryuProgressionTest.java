package net.xxxjk.TYPE_MOON_WORLD.martial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GanryuProgressionTest {
   @Test void moveThresholdsMatchDesign() {
      assertEquals(0.5, GanryuMove.HIGH_JUMP.requiredProficiency());
      assertEquals(0.5, GanryuMove.STONE_FLOWER.requiredProficiency());
      assertEquals(0.5, GanryuMove.STONE_FLOWER_SECOND.requiredProficiency());
      assertEquals(5.0, GanryuMove.SPARROW_THRUST.requiredProficiency());
      assertEquals(5.0, GanryuMove.SPARROW_THRUST_SECOND.requiredProficiency());
      assertEquals(10.0, GanryuMove.STANCE.requiredProficiency());
      assertEquals(25.0, GanryuMove.SPRING_BUD.requiredProficiency());
      assertEquals(25.0, GanryuMove.SPRING_BUD_SECOND.requiredProficiency());
      assertEquals(50.0, GanryuMove.UKEMI.requiredProficiency());
      assertEquals(55.0, GanryuMove.SPARROW_SLASH.requiredProficiency());
      assertEquals(60.0, GanryuMove.FLOWER_BUD.requiredProficiency());
      assertEquals(80.0, GanryuMove.TSUBAME_GAESHI.requiredProficiency());
   }

   @Test void tsubameAlsoRequiresDefeatingTheSwordsman() {
      assertFalse(GanryuCombatService.isUnlocked(true, 80.0, false, GanryuMove.TSUBAME_GAESHI));
      assertTrue(GanryuCombatService.isUnlocked(true, 80.0, true, GanryuMove.TSUBAME_GAESHI));
   }

   @Test void sequenceStagesWrapAndExpire() {
      assertEquals(1, GanryuCombatService.nextSequenceStage(0, 3, false));
      assertEquals(2, GanryuCombatService.nextSequenceStage(1, 3, false));
      assertEquals(3, GanryuCombatService.nextSequenceStage(2, 3, false));
      assertEquals(1, GanryuCombatService.nextSequenceStage(3, 3, false));
      assertEquals(1, GanryuCombatService.nextSequenceStage(2, 3, true));
      assertEquals(1, GanryuCombatService.nextSequenceStage(1, 2, true));
   }

   @Test void basicAttacksBridgeTheInitialProficiencyGap() {
      assertTrue(GanryuCombatService.usesBasicAttack(0.0));
      assertTrue(GanryuCombatService.usesBasicAttack(0.49));
      assertFalse(GanryuCombatService.usesBasicAttack(0.5));
   }

   @Test void techniqueReducesRecoveryByAtMostThirtyEightPercent() {
      assertEquals(10, GanryuCombatService.recoveryTicks(10, 0));
      assertEquals(7, GanryuCombatService.recoveryTicks(10, 20));
      assertEquals(25, GanryuCombatService.recoveryTicks(40, 20));
      assertEquals(25, GanryuCombatService.recoveryTicks(40, Integer.MAX_VALUE));
   }

   @Test void stanceChargeAndDamageCapsMatchDesign() {
      assertEquals(0.0F, GanryuCombatService.stanceCharge(-10), 1.0E-6F);
      assertEquals(0.5F, GanryuCombatService.stanceCharge(30), 1.0E-6F);
      assertEquals(1.0F, GanryuCombatService.stanceCharge(60), 1.0E-6F);
      assertEquals(6.0F, GanryuCombatService.stanceDamage(4.0F, 8.0F, 0.5F), 1.0E-6F);
      assertEquals(15.0F, GanryuCombatService.stanceDamage(10.0F, 20.0F, 0.5F), 1.0E-6F);
      assertEquals(8.0F, GanryuCombatService.stanceDamage(4.0F, 8.0F, 2.0F), 1.0E-6F);
      assertEquals(20.0F, GanryuCombatService.stanceDamage(10.0F, 20.0F, 2.0F), 1.0E-6F);
      assertEquals(120.0F, GanryuCombatService.stanceDamage(60.0F, 120.0F, 1.0F), 1.0E-6F);
   }

   @Test void tsubameUsesAnEightyPercentRoll() {
      assertTrue(GanryuCombatService.tsubameLands(0.0F));
      assertTrue(GanryuCombatService.tsubameLands(0.799999F));
      assertFalse(GanryuCombatService.tsubameLands(0.8F));
      assertFalse(GanryuCombatService.tsubameLands(1.0F));
   }

   @Test void normalMovesUseGanryuProficiencyAndTwelvePointMaximumBonus() {
      assertEquals(17.0F, GanryuCombatService.scaledDamage(5.0F, 20, 100.0), 1.0E-6F);
   }

   @Test void souwaAndSharedUkemiMigrateAtTheirThresholds() {
      assertEquals(1, GanryuCombatService.souwaBonus(true, 25.0));
      assertEquals(3, GanryuCombatService.souwaBonus(true, 50.0));
      assertEquals(6, GanryuCombatService.souwaBonus(true, 100.0));
      assertTrue(MartialUkemiService.shouldBeLearned(false, 0.0, 50.0));
      assertTrue(MartialUkemiService.shouldBeLearned(false, 30.0, 0.0));
      assertFalse(MartialUkemiService.shouldBeLearned(false, 29.99, 49.99));
   }

   @Test void handQualificationRequiresOnlyEmptyHandsOrAllowedBlades() {
      assertTrue(GanryuCombatService.isValidHandCombination(false, true, true, false));
      assertTrue(GanryuCombatService.isValidHandCombination(true, false, false, true));
      assertTrue(GanryuCombatService.isValidHandCombination(false, true, false, true));
      assertFalse(GanryuCombatService.isValidHandCombination(true, false, true, false));
      assertFalse(GanryuCombatService.isValidHandCombination(false, true, false, false));
      assertFalse(GanryuCombatService.isValidHandCombination(false, false, false, true));
   }
}

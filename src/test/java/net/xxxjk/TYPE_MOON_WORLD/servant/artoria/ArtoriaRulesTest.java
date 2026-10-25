package net.xxxjk.TYPE_MOON_WORLD.servant.artoria;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArtoriaRulesTest {
   @Test void npcKeepsWindBarrierUntilDecisivePhaseAndStillPrefersWindHammer() {
      var probing = net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase.PROBING;
      var normal = net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase.NORMAL;
      var decisive = net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase.DECISIVE;
      assertFalse(ArtoriaRules.npcMayUseExcalibur(probing, 0.0F));
      assertFalse(ArtoriaRules.npcMayUseExcalibur(normal, 0.0F));
      assertTrue(ArtoriaRules.npcMayUseExcalibur(decisive, 0.0F));
      assertFalse(ArtoriaRules.npcMayUseExcalibur(decisive, ArtoriaRules.NPC_EXCALIBUR_SELECTION_CHANCE));
      assertTrue(ArtoriaRules.npcPrioritizesWindHammer(probing, 0.99F));
      assertTrue(ArtoriaRules.npcPrioritizesWindHammer(normal, 0.99F));
      assertTrue(ArtoriaRules.npcPrioritizesWindHammer(decisive, 0.0F));
      assertFalse(ArtoriaRules.npcPrioritizesWindHammer(decisive, ArtoriaRules.NPC_DECISIVE_WIND_HAMMER_CHANCE));
   }

   @Test void swordExamplesKeepTheRevealedComponentOutsideSpecialAttack() {
      assertEquals(33, ArtoriaRules.basicDamage(33, 1, 13, 1, false, false));
      assertEquals(33, ArtoriaRules.basicDamage(33, 1, 13, 1, true, false));
      assertEquals(330, ArtoriaRules.basicDamage(33, 1, 13, 1, true, true));
      assertEquals(660, ArtoriaRules.basicDamage(33, 2, 13, 1, true, true));
   }
   @Test void chargeScalesOnlyTheIsolatedBonusAndCannotExceedFullCharge() {
      assertEquals(0, ArtoriaRules.revealedBonus(13, .5, false));
      assertEquals(0, ArtoriaRules.revealedBonus(13, 2, true));
      assertEquals(0, ArtoriaRules.revealedBonus(13, -1, false));
   }
   @Test void sustainedBeamHasFifteenFixedPulses() {
      assertEquals(15, ArtoriaRules.EXCALIBUR_TICKS / ArtoriaRules.PULSE_INTERVAL);
      assertEquals(7500, ArtoriaRules.PULSES * ArtoriaRules.PULSE_DAMAGE);
      assertEquals(150, ArtoriaRules.EXCALIBUR_MP);
      assertEquals(180 * 20, ArtoriaRules.EXCALIBUR_COOLDOWN);
   }
   @Test void burstAndRecoveryMatchConfirmedTiming() {
      assertEquals(30, 100 * ArtoriaRules.BURST_DEFENSE_MULTIPLIER);
      assertEquals(60, ArtoriaRules.BURST_START_MP);
      assertEquals(30, ArtoriaRules.BURST_DRAIN_MP);
      assertEquals(40, ArtoriaRules.JET_TICKS);
      assertEquals(300, ArtoriaRules.INSTINCT_RECOVERY_TICKS);
      assertEquals(.15, ArtoriaRules.BURST_SPEED_BONUS);
   }
   @Test void shortExcaliburUsesTheFinalQuarterPulseProfile() {
      assertEquals(38, ArtoriaRules.EXCALIBUR_SHORT_TICKS);
      assertEquals(4, ArtoriaRules.EXCALIBUR_SHORT_PULSES);
      assertEquals(120, ArtoriaRules.EXCALIBUR_SHORT_PULSE_DAMAGE);
      assertEquals(480, ArtoriaRules.EXCALIBUR_SHORT_PULSES * ArtoriaRules.EXCALIBUR_SHORT_PULSE_DAMAGE);
      assertEquals(37.5, ArtoriaRules.EXCALIBUR_SHORT_RANGE);
      assertEquals(60, ArtoriaRules.EXCALIBUR_SHORT_CHARGE_TICKS);
   }
   @Test void windHammerHasAVisibleHalfSecondWindup() {
      assertEquals(10, ArtoriaRules.WIND_HAMMER_WINDUP_TICKS);
   }
   @Test void windDefenseIsBoundedToFiveSeconds() {
      assertEquals(100, ArtoriaRules.WIND_DEFENSE_MAX_TICKS);
      assertEquals(6.0, ArtoriaRules.WIND_DEFENSE_RADIUS);
      assertEquals(35.0, ArtoriaRules.WIND_DEFENSE_MP);
      assertEquals(40, ArtoriaRules.WIND_DEFENSE_COOLDOWN);
   }
   @Test void windHammerRestoresTheFormerHitProfilesAndConeDimensions() {
      assertEquals(90.0F, ArtoriaRules.WIND_HAMMER_PLAYER_DAMAGE);
      assertEquals(125.0F, ArtoriaRules.WIND_HAMMER_PLAYER_BURST_DAMAGE);
      assertEquals(10.5, ArtoriaRules.WIND_HAMMER_PLAYER_RANGE);
      assertEquals(12.5, ArtoriaRules.WIND_HAMMER_PLAYER_BURST_RANGE);
      assertEquals(.48, ArtoriaRules.WIND_HAMMER_PLAYER_WIDTH_SCALE);
      assertEquals(.58, ArtoriaRules.WIND_HAMMER_PLAYER_BURST_WIDTH_SCALE);
      assertEquals(300.0F, ArtoriaRules.WIND_HAMMER_NPC_DAMAGE);
      assertEquals(400.0F, ArtoriaRules.WIND_HAMMER_NPC_BURST_DAMAGE);
      assertEquals(10.0, ArtoriaRules.WIND_HAMMER_NPC_RANGE);
      assertEquals(.45, ArtoriaRules.WIND_HAMMER_NPC_WIDTH_SCALE);
      assertEquals(40, ArtoriaRules.WIND_REGATHER_TICKS);
   }
   @Test void manaBurstOwnsTheTenfoldExposedSwordMultiplierAndHitCost() {
      assertEquals(10.0, ArtoriaRules.BURST_SWORD_MULTIPLIER);
      assertEquals(20.0, ArtoriaRules.BURST_SWORD_HIT_COST);
   }
}

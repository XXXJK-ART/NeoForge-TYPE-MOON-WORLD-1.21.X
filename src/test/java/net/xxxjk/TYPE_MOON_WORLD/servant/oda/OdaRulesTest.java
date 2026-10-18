package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.NpType;

class OdaRulesTest {
   @Test void minusRankParsesCalculatesAndDisplays() {
      assertEquals(StatRank.A_MINUS, StatRank.fromKey("A-"));
      assertEquals(25, StatRank.fromKey("A-").coefficient());
      assertEquals("A-", StatRank.fromKey("A-").displayKey(false));
      assertEquals(5, ServantParams.of("B", false, "B", false, "B", false, "A", false, "A-", false).critRatePercent());
   }
   @Test void additiveSpecialZoneMatchesConfirmedExamples() {
      assertEquals(5, OdaRules.special(1, ServantEra.MYTHOLOGY, false));
      assertEquals(6, OdaRules.special(1, ServantEra.MYTHOLOGY, true));
      assertEquals(69.6, 10 * OdaRules.special(1, ServantEra.MYTHOLOGY, true) * (1 + OdaRules.strategy("B")), 1e-8);
      assertEquals(1, OdaRules.special(0, null, false));
      assertEquals(.5, OdaRules.eraFinal(ServantEra.MODERN));
      assertEquals(.25, OdaRules.eraFinal(ServantEra.FUTURE));
      assertEquals(1, OdaRules.eraFinal(null));
   }
   @Test void divinityIgnoresModifiersAndCapsBeyondA() {
      assertEquals(1, OdaRules.divinity("E-", true));
      assertEquals(4, OdaRules.divinity("divinity_b_plus", true));
      assertEquals(5, OdaRules.divinity("EX", true));
      assertEquals(5, OdaRules.divinity("A++", true));
      assertEquals(1, OdaRules.divinity("", true));
      assertEquals(0, OdaRules.divinity("", false));
   }
   @Test void strategyUsesTypesAndAcceptsLegacyAliases() {
      assertEquals(NpType.ARMY, NpType.fromKey("anti_army"));
      assertEquals(NpType.CASTLE, NpType.fromKey("anti_fortress"));
      assertEquals(NpType.CASTLE, NpType.fromKey("anti_castle"));
      assertEquals(NpType.ANTI_DIVINE, NpType.fromKey("anti_god"));
      assertTrue(OdaRules.armyOrCastle(NpType.ARMY));
      assertTrue(OdaRules.armyOrCastle(NpType.CASTLE));
      assertFalse(OdaRules.armyOrCastle(NpType.ANTI_DIVINE));
      assertFalse(OdaRules.armyOrCastle(null));
      for (int i = 0; i < 5; i++) assertEquals((i + 1) * .04, OdaRules.strategy(new String[]{"E","D","C","B","A"}[i]), 1e-8);
   }
   @Test void chargeAndInputBoundaryAreExact() {
      assertEquals(10, OdaRules.chargeDamage(0));
      assertEquals(30, OdaRules.chargeDamage(20));
      assertEquals(50, OdaRules.chargeDamage(40));
      assertEquals(50, OdaRules.chargeDamage(100));
      assertEquals(3, OdaRules.chargeSpeed(0));
      assertEquals(4, OdaRules.chargeSpeed(40));
      assertTrue(OdaRules.followMode(11));
      assertFalse(OdaRules.followMode(12));
   }
   @Test void threeThousandUsesThreeNinetyNineGunWavesAndACompactSpiral() {
      assertEquals(99, OdaRules.THREE_THOUSAND_GUNS_PER_WAVE);
      assertEquals(297, OdaRules.THREE_THOUSAND_TOTAL_GUNS);
      assertEquals(3, OdaRules.THREE_THOUSAND_SHOTS_PER_GUN);
      assertEquals(30, OdaRules.THREE_THOUSAND_SHOT_INTERVAL);
      assertEquals(600, OdaRules.THREE_THOUSAND_MAX_LIFE);
      assertEquals(891, OdaRules.THREE_THOUSAND_TOTAL_SHOTS);
      assertEquals(0, OdaRules.threeThousandWave(98));
      assertEquals(1, OdaRules.threeThousandWave(99));
      assertEquals(2, OdaRules.threeThousandWave(296));
      assertEquals(98, OdaRules.threeThousandGunIndexInWave(296));
      for (int wave = 0; wave < OdaRules.THREE_THOUSAND_WAVE_COUNT; wave++) {
         for (int tier = 0; tier < OdaRules.THREE_THOUSAND_TIER_COUNT; tier++) {
            int firstGunIndex = wave * OdaRules.THREE_THOUSAND_GUNS_PER_WAVE
               + tier * OdaRules.THREE_THOUSAND_GUNS_PER_TIER;
            assertEquals(wave, OdaRules.threeThousandWave(firstGunIndex));
            assertEquals(tier, OdaRules.threeThousandTier(OdaRules.threeThousandGunIndexInWave(firstGunIndex)));
            assertEquals(tier * 3, OdaRules.threeThousandTierDelay(tier));
            assertTrue(OdaRules.threeThousandHeight(tier, wave) > (tier == 0 ? Double.NEGATIVE_INFINITY : OdaRules.threeThousandHeight(tier - 1, wave)));
            assertTrue(OdaRules.threeThousandRadius(tier, wave) > (tier == 0 ? 0 : OdaRules.threeThousandRadius(tier - 1, wave)));
            for (int slot = 0; slot < OdaRules.THREE_THOUSAND_GUNS_PER_TIER; slot++) {
               int shotDelay = OdaRules.threeThousandShotDelay(slot);
               assertTrue(shotDelay >= 2 && shotDelay <= 4, "Each gun should fire shortly after appearing");
               if (slot > 0) assertTrue(OdaRules.threeThousandHeight(tier, slot, wave)
                  > OdaRules.threeThousandHeight(tier, slot - 1, wave), "Each turn must climb within its tier");
               if (tier > 0 && slot == 0) assertTrue(OdaRules.threeThousandHeight(tier, slot, wave)
                  > OdaRules.threeThousandHeight(tier - 1, 10, wave), "The next turn must continue upward");
            }
         }
      }
      assertEquals(1.5, OdaRules.threeThousandRadius(0, 0), 1e-8);
      assertEquals(4.5, OdaRules.threeThousandRadius(8, 10, 2), 1e-8);
      assertEquals(-3.675, OdaRules.threeThousandHeight(0, 0, 0), 1e-8);
      assertEquals(3.675, OdaRules.threeThousandHeight(8, 10, 2), 1e-8);
      assertEquals(OdaRules.threeThousandAngle(1, 0, 0), OdaRules.threeThousandAngle(1, 0, 1));
   }
   @Test void everyGunHasANeighborBetweenPointEightAndOneBlockAway() {
      for (int wave = 0; wave < 3; wave++) {
         double[][] positions = new double[99][3];
         for (int index = 0; index < 99; index++) {
            int tier = index / 11, slot = index % 11;
            double angle = OdaRules.threeThousandAngle(tier, slot, wave);
            double radius = OdaRules.threeThousandRadius(tier, slot, wave);
            double x = Math.cos(angle) * radius, z = Math.sin(angle) * radius;
            double y = OdaRules.threeThousandHeight(tier, slot, wave);
            assertTrue(Math.abs(y) < 5, "The cone must surround its center within ten blocks of height");
            if (index > 0) assertTrue(radius > OdaRules.threeThousandRadius((index - 1) / 11, (index - 1) % 11, wave),
               "The tornado must widen continuously from bottom to top");
            assertEquals(radius, OdaRules.threeThousandRadius(tier, slot, 0), 1e-8, "Replacement waves must reuse the same positions");
            positions[index] = new double[]{x, y, z};
         }
         for (int i = 0; i < positions.length; i++) {
            double nearest = Double.POSITIVE_INFINITY;
            for (int j = 0; j < positions.length; j++) {
               if (i == j) continue;
               double distance = Math.sqrt(Math.pow(positions[i][0] - positions[j][0], 2)
                  + Math.pow(positions[i][1] - positions[j][1], 2) + Math.pow(positions[i][2] - positions[j][2], 2));
               nearest = Math.min(nearest, distance);
            }
            assertTrue(nearest >= .8 && nearest <= 1.0, "Gun spacing outside 0.8-1 block: " + nearest);
         }
      }
   }
   @Test void chantLiftEasesIntoAThreeBlockHover() {
      assertEquals(0, OdaRules.threeThousandLiftProgress(0));
      assertEquals(.5, OdaRules.threeThousandLiftProgress(20));
      assertEquals(1, OdaRules.threeThousandLiftProgress(40));
      assertEquals(1, OdaRules.threeThousandLiftProgress(200));
      assertEquals(3, OdaRules.THREE_THOUSAND_LIFT_HEIGHT);
   }
}

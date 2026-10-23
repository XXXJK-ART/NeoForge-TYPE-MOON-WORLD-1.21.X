package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaHajunDuelRules.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.*;

class OdaHajunDuelRulesTest {
   @Test void equalPanelsLeaveAllFourOutcomesReachable() {
      var weights = weights(40, 40);
      assertEquals(.4, weights.odaWins(), 1e-12);
      assertEquals(.4, weights.opponentsWin(), 1e-12);
      assertEquals(.15, weights.neitherWins(), 1e-12);
      assertEquals(.05, weights.bothDie(), 1e-12);
      assertEquals(Outcome.ODA_WINS, weights.roll(0));
      assertEquals(Outcome.OPPONENTS_WIN, weights.roll(.5));
      assertEquals(Outcome.NEITHER_WINS, weights.roll(.9));
      assertEquals(Outcome.BOTH_DIE, weights.roll(.99));
      assertEquals(Outcome.BOTH_DIE, weights.roll(1));
   }
   @Test void strongerSideWinsMoreOftenButEveryOutcomeKeepsAWeight() {
      double last = 0;
      for (double power : new double[]{1, 10, 40, 100, 1000}) {
         var weights = weights(power, 40);
         assertTrue(weights.odaWins() > last);
         last = weights.odaWins();
         assertEquals(1, weights.odaWins() + weights.opponentsWin() + weights.neitherWins() + weights.bothDie(), 1e-12);
         assertTrue(weights.opponentsWin() > 0 && weights.neitherWins() > 0 && weights.bothDie() > 0);
         assertEquals(weights.odaWins(), weights(40, power).opponentsWin(), 1e-12);
      }
   }
   @Test void divineAndEraCounterUsesTheExistingAdditiveZoneExactlyOnce() {
      assertEquals(1, matchupMultiplier(0, null));
      assertEquals(5, matchupMultiplier(1, ServantEra.MYTHOLOGY));
      assertEquals(9, matchupMultiplier(5, ServantEra.MYTHOLOGY));
      assertEquals(1, matchupMultiplier(0, ServantEra.EARLY_MODERN));
      assertEquals(.5, matchupMultiplier(0, ServantEra.MODERN));
      assertEquals(.25, matchupMultiplier(0, ServantEra.FUTURE));
      assertTrue(weights(40, 40 / matchupMultiplier(1, ServantEra.MYTHOLOGY)).odaWins() > weights(40, 40).odaWins());
      assertTrue(weights(40, 40 / matchupMultiplier(0, ServantEra.FUTURE)).odaWins() < weights(40, 40).odaWins());
   }
   @Test void panelPreservesPlusMinusAndDoublePlusFromCommonConversions() {
      var oda = ServantParams.of("B",false,"B",false,"B",false,"A",false,"A-",false);
      assertEquals(40.5, panelScore(oda), 1e-12);
      var strong = ServantParams.of("B",false,"A++",false,"B",false,"A",false,"A-",false);
      assertEquals(68, panelScore(strong), 1e-12);
      assertEquals(.25, healthFactor(0));
      assertEquals(1, healthFactor(1));
      assertEquals(.625, healthFactor(.5));
   }
}

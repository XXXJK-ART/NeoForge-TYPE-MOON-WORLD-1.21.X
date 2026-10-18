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
}

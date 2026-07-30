package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterServantLinkRulesTest {
   @Test
   void independentActionRanksUseConfiguredOnlineDurations() {
      assertEquals(7 * 24000,
         MasterServantLinkService.independentDurationTicksForSkills(List.of("independent_action_a")));
      assertEquals(2 * 24000,
         MasterServantLinkService.independentDurationTicksForSkills(List.of("independent_action_b")));
      assertEquals(24000,
         MasterServantLinkService.independentDurationTicksForSkills(List.of("independent_action_c")));
      assertEquals(0, MasterServantLinkService.independentDurationTicksForSkills(List.of("battle_continuation_a")));
   }

   @Test
   void manaSupplyFallsLinearlyAndStopsAtRangeOrDimensionBoundary() {
      assertEquals(1.0, MasterServantLinkService.manaSupplyFraction(60.0, true), 1.0E-9);
      assertEquals(0.5, MasterServantLinkService.manaSupplyFraction(105.0, true), 1.0E-9);
      assertEquals(0.0, MasterServantLinkService.manaSupplyFraction(150.0, true), 1.0E-9);
      assertEquals(0.0, MasterServantLinkService.manaSupplyFraction(20.0, false), 1.0E-9);
   }

   @Test
   void passiveManaRegenDistinguishesNativeContractedAndMasterlessServants() {
      assertEquals(4.0, ServantCardManaService.passiveRegenForContractState(
         MasterServantLinkService.SERVANT_CONTRACT_NATIVE, 4.0, 1.5), 1.0E-9);
      assertEquals(1.5, ServantCardManaService.passiveRegenForContractState(
         MasterServantLinkService.SERVANT_CONTRACT_CONTRACTED, 4.0, 1.5), 1.0E-9);
      assertEquals(0.0, ServantCardManaService.passiveRegenForContractState(
         MasterServantLinkService.SERVANT_CONTRACT_MASTERLESS, 4.0, 1.5), 1.0E-9);
   }

   @Test
   void playerContractsRequireExactlyOneMasterCardAndOneServantCard() {
      assertTrue(MasterStateManager.isPlayerCardContractPair(true, false, false, true));
      assertFalse(MasterStateManager.isPlayerCardContractPair(false, false, false, true));
      assertFalse(MasterStateManager.isPlayerCardContractPair(true, false, true, false));
      assertFalse(MasterStateManager.isPlayerCardContractPair(false, true, false, true));
      assertFalse(MasterStateManager.isPlayerCardContractPair(true, false, true, true));
   }
}

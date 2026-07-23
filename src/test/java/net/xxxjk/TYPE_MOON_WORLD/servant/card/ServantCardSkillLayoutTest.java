package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ServantCardSkillLayoutTest {
   @Test
   void artoriaManaBurstBeamUsesRequestedCost() {
      ServantCardSkillAction action = ServantCardSkillLayout.actionFor("artoria_pendragon", 6, false);
      assertNotNull(action);
      assertEquals("mana_burst_beam", action.effectId());
      assertEquals(100.0, action.mpCost());
      assertEquals(300, action.cooldownTicks());
   }

   @Test
   void workshopReturnsAreExpensiveAndHaveLongerCooldowns() {
      ServantCardSkillAction medea = ServantCardSkillLayout.actionFor("medea", 7, false);
      ServantCardSkillAction paracelsus = ServantCardSkillLayout.actionFor("paracelsus", 3, false);
      assertNotNull(medea);
      assertNotNull(paracelsus);
      assertEquals(120.0, medea.mpCost());
      assertEquals(400, medea.cooldownTicks());
      assertEquals(120.0, paracelsus.mpCost());
      assertEquals(400, paracelsus.cooldownTicks());
   }
}

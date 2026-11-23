package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaRules;

class ArtoriaSkillLayoutTest {
   @Test
   void activeSwordSlotsUseTheSharedRuleValuesAndLeaveOtherInputsAlone() {
      Map<Integer, String> swordSlots = Map.of(
         2, ArtoriaRules.KNIGHT_TRIPLE_SLASH,
         4, ArtoriaRules.INTERCEPT_THRUST,
         5, ArtoriaRules.SPINNING_SLASH,
         7, ArtoriaRules.UPWARD_SLASH,
         8, ArtoriaRules.GUARD_COUNTER);

      swordSlots.forEach((slot, effect) -> {
         ServantCardSkillAction action = ServantCardSkillLayout.actionFor(ArtoriaRules.ID, slot, false);
         assertNotNull(action, "Missing Artoria slot " + slot);
         assertEquals(effect, action.effectId());
         assertEquals(ArtoriaRules.skillMpCost(effect), action.mpCost());
         assertEquals(ArtoriaRules.skillCooldown(effect), action.cooldownTicks());
      });

      assertEquals("artoria_small_combo", ServantCardSkillLayout.actionFor(ArtoriaRules.ID, -1, false).effectId());
      ServantCardSkillAction manaBurst = ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 0, false);
      assertEquals("mana_burst", manaBurst.effectId());
      assertEquals(ArtoriaRules.BURST_START_MP, manaBurst.mpCost());
      assertEquals("invisible_air_hammer", ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 1, false).effectId());
      assertEquals("invisible_air_release", ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 3, false).effectId());
      ServantCardSkillAction windDefense = ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 6, false);
      assertEquals(ArtoriaRules.WIND_DEFENSE, windDefense.effectId());
      assertEquals(ArtoriaRules.WIND_DEFENSE_MP, windDefense.mpCost());
      assertEquals(ArtoriaRules.WIND_DEFENSE_COOLDOWN, windDefense.cooldownTicks());
      assertEquals(ArtoriaRules.WIND_BARRIER_TOGGLE, ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 9, false).effectId());
      assertNull(ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 10, false));
      assertNull(ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 10, false));
      assertFalse(ServantCardTransformManager.isNoblePhantasmAction(ArtoriaRules.ID, 9));
      assertFalse(ServantCardTransformManager.usesSharedNoblePhantasmCooldown(ArtoriaRules.ID, 9));
   }
}

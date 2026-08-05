package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

   @Test
   void gilgameshDivineShieldIsAResourceToggle() {
      ServantCardSkillAction action = ServantCardSkillLayout.actionFor("gilgamesh", 5, false);
      assertNotNull(action);
      assertEquals("gilgamesh_divine_shield", action.effectId());
      assertEquals(30.0, action.mpCost());
      assertEquals(0, action.cooldownTicks());
   }

   @Test
   void casterGilgameshUsesIndependentSupportAndCannonLayout() {
      String[] effects = {
         "gilgamesh_key",
         "caster_gilgamesh_slate_volley",
         "caster_gilgamesh_leader",
         "caster_gilgamesh_return",
         "caster_gilgamesh_item_creation",
         "caster_gilgamesh_workshop",
         "caster_gilgamesh_cannon_calibration",
         "gilgamesh_clairvoyance",
         "gilgamesh_divine_shield"
      };
      double[] costs = {0.0, 0.0, 20.0, 15.0, 0.0, 0.0, 0.0, 18.0, 30.0};
      int[] cooldowns = {0, 24, 35 * 20, 30 * 20, 0, 80, 20, 160, 0};
      for (int slot = 0; slot <= 8; slot++) {
         ServantCardSkillAction action = ServantCardSkillLayout.actionFor("gilgamesh_caster", slot, false);
         assertNotNull(action, "slot " + slot);
         assertEquals(effects[slot], action.effectId(), "slot " + slot + " effect");
         assertEquals(costs[slot], action.mpCost(), "slot " + slot + " MP");
         assertEquals(cooldowns[slot], action.cooldownTicks(), "slot " + slot + " cooldown");
      }
      ServantCardSkillAction noble = ServantCardSkillLayout.actionFor("gilgamesh_caster", 9, false);
      assertNotNull(noble);
      assertEquals("caster_gilgamesh_royal_cannon", noble.effectId());
      assertEquals(0.0, noble.mpCost());
      assertEquals(0, noble.cooldownTicks());
      assertEquals(false, ServantCardTransformManager.isNoblePhantasmAction("gilgamesh_caster", 9));
      assertEquals(false, ServantCardTransformManager.usesSharedNoblePhantasmCooldown("gilgamesh_caster", 9));
   }

   @Test
   void ushiwakamaruSkillsMatchTheRequestedLayout() {
      double[] costs = {25.0, 15.0, 6.0, 8.0, 15.0, 50.0, 50.0, 100.0, 50.0, 150.0};
      int[] cooldowns = {400, 600, 140, 120, 200, 600, 600, 600, 600, 600};
      for (int slot = 0; slot < 10; slot++) {
         ServantCardSkillAction action = ServantCardSkillLayout.actionFor("ushiwakamaru_rider", slot, false);
         assertNotNull(action);
         assertEquals(costs[slot], action.mpCost(), "slot " + slot + " MP");
         assertEquals(cooldowns[slot], action.cooldownTicks(), "slot " + slot + " cooldown");
      }
   }

   @Test
   void ushiwakamaruHasFiveIndependentNoblePhantasmSlots() {
      for (int slot = 5; slot <= 9; slot++) {
         assertEquals(true, ServantCardTransformManager.isNoblePhantasmAction("ushiwakamaru_rider", slot));
         assertEquals(false, ServantCardTransformManager.usesSharedNoblePhantasmCooldown("ushiwakamaru_rider", slot));
      }
      assertEquals(false, ServantCardTransformManager.isNoblePhantasmAction("ushiwakamaru_rider", 4));
   }

   @Test
   void onlyEightBoatUsesUshiwakamaruNoblePhantasmVoice() {
      assertEquals(false, ServantCardVoiceHelper.usesUshiwakamaruNoblePhantasmVoice("ushiwakamaru_six_secret"));
      assertEquals(false, ServantCardVoiceHelper.usesUshiwakamaruNoblePhantasmVoice("ushiwakamaru_usumidori"));
      assertEquals(false, ServantCardVoiceHelper.usesUshiwakamaruNoblePhantasmVoice("ushiwakamaru_benkei"));
      assertEquals(false, ServantCardVoiceHelper.usesUshiwakamaruNoblePhantasmVoice("ushiwakamaru_spider_slayer"));
      assertEquals(true, ServantCardVoiceHelper.usesUshiwakamaruNoblePhantasmVoice("ushiwakamaru_eight_boat"));
   }

   @Test
   void fanaticUsesNineIndependentTechniqueSlotsAndLeavesSlotNineEmpty() {
      double[] costs = {15.0, 50.0, 40.0, 30.0, 30.0, 20.0, 40.0, 30.0, 60.0};
      int[] cooldowns = {300, 600, 600, 600, 600, 600, 600, 600, 600};
      for (int slot = 0; slot < 9; slot++) {
         ServantCardSkillAction action = ServantCardSkillLayout.actionFor("fanatic_assassin", slot, false);
         assertNotNull(action, "slot " + slot);
         assertEquals(costs[slot], action.mpCost(), "slot " + slot + " MP");
         assertEquals(cooldowns[slot], action.cooldownTicks(), "slot " + slot + " cooldown");
      }
      assertNull(ServantCardSkillLayout.actionFor("fanatic_assassin", 9, false));
      assertEquals(false, ServantCardTransformManager.isNoblePhantasmAction("fanatic_assassin", 8));
   }
}

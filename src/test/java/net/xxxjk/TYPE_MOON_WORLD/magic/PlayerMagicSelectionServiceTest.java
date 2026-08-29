package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

class PlayerMagicSelectionServiceTest {
   @Test
   void legacyReinforcementIdsUseTheUnifiedRuntimeId() {
      assertEquals("reinforcement", PlayerMagicSelectionService.canonicalRuntimeMagicId("reinforcement_self"));
      assertEquals("reinforcement", PlayerMagicSelectionService.canonicalRuntimeMagicId("reinforcement_other"));
      assertEquals("reinforcement", PlayerMagicSelectionService.canonicalRuntimeMagicId("reinforcement_item"));
      assertEquals("projection", PlayerMagicSelectionService.canonicalRuntimeMagicId("projection"));
   }

   @Test
   void legacyReinforcementEntryPreservesItsTargetInThePreset() {
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry =
         new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(0, 0);
      entry.magicId = "reinforcement_item";

      PlayerMagicSelectionService.normalizeRuntimeWheelEntry(null, entry);

      assertEquals("reinforcement", entry.magicId);
      assertEquals(2, entry.presetPayload.getInt("reinforcement_target"));
   }

   @Test
   void unifiedReinforcementStillRequiresItsPreset() {
      assertTrue(PlayerMagicSelectionService.requiresPresetConfiguration("reinforcement"));
      assertTrue(PlayerMagicSelectionService.requiresPresetConfiguration("reinforcement_self"));
   }
}

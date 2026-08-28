package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class RuneProgramRulesTest {
   @Test
   void registryContainsTwentyFourCompleteDefinitions() {
      assertEquals(24, RuneRegistry.all().size());
      for (RuneDefinition definition : RuneRegistry.all()) {
         for (RunePosition position : RunePosition.values()) assertFalse(definition.semantic(position).isBlank());
      }
   }

   @Test
   void bodyAllowsNoTriggerButRejectsTrigger() {
      RuneProgram body = program(List.of(), List.of("wunjo"), List.of(), List.of(), RuneReleaseMode.BODY);
      assertTrue(body.validate().valid());
      body.setSlot(RunePosition.TRIGGER, 0, "fehu");
      assertFalse(body.validate().valid());
      assertTrue(body.validate().errors().contains("release_mode_conflict"));
   }

   @Test
   void weaponRejectsProjectileStyleTrigger() {
      RuneProgram weapon = program(List.of("hagalaz"), List.of("kenaz"), List.of(), List.of(), RuneReleaseMode.WEAPON);
      assertFalse(weapon.validate().valid());
      assertTrue(weapon.validate().errors().contains("projectile_trigger_conflict"));
   }

   @Test
   void costUsesTriggerAndModifierMultipliers() {
      RuneProgram program = program(List.of("fehu", "uruz"), List.of("kenaz"), List.of("fehu", "uruz"), List.of(), RuneReleaseMode.DIRECT_AIR);
      assertEquals(500.0, RuneProgramCostService.calculate(program), 0.001);
   }

   @Test
   void costAddsComplexityMultiplierAfterFiveRunes() {
      RuneProgram program = program(List.of("fehu"), List.of("kenaz", "wunjo", "laguz", "berkano"), List.of("uruz"), List.of(), RuneReleaseMode.DIRECT_AIR);
      // Six runes: base 300, trigger/modifier multiplier 1.5, complexity 1.08.
      assertEquals(486.0, RuneProgramCostService.calculate(program), 0.001);
   }

   @Test
   void weaponRejectsAllProjectileTriggerFamilies() {
      for (String trigger : List.of("fehu", "ansuz", "kenaz", "hagalaz", "sowilo")) {
         RuneProgram weapon = program(List.of(trigger), List.of(), List.of(), List.of(), RuneReleaseMode.WEAPON);
         assertFalse(weapon.validate().valid(), trigger);
         assertTrue(weapon.validate().errors().contains("projectile_trigger_conflict"), trigger);
      }
   }

   @Test
   void nbtRoundTripKeepsUuidAndTwentySlots() {
      RuneProgram original = program(List.of("fehu"), List.of("kenaz"), List.of("gebo"), List.of("othala"), RuneReleaseMode.BLOCK_TRAP);
      RuneProgram restored = RuneProgram.fromNBT(original.serializeNBT());
      assertEquals(original.uuid(), restored.uuid());
      assertEquals(20, restored.slots().size());
      assertEquals(original.releaseMode(), restored.releaseMode());
   }

   private static RuneProgram program(List<String> triggers, List<String> effects, List<String> modifiers, List<String> terminals, RuneReleaseMode mode) {
      return new RuneProgram(UUID.randomUUID(), "test", triggers, effects, modifiers, terminals, mode, new CompoundTag(), 1L, 1L);
   }
}

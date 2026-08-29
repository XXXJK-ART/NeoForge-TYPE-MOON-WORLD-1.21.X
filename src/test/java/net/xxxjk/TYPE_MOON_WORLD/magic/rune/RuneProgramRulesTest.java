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
   void weaponAllowsProjectileStyleTriggerWithoutLegacyConflict() {
      RuneProgram weapon = program(List.of("hagalaz"), List.of("kenaz"), List.of(), List.of(), RuneReleaseMode.WEAPON);
      assertTrue(weapon.validate().valid());
      assertFalse(weapon.validate().errors().contains("projectile_trigger_conflict"));
   }

   @Test
   void costUsesTriggerAndModifierMultipliers() {
      RuneProgram program = program(List.of("fehu", "uruz"), List.of("kenaz"), List.of("fehu", "uruz"), List.of(), RuneReleaseMode.DIRECT_AIR);
      assertEquals(500.0 / 3.0, RuneProgramCostService.calculate(program), 0.001);
   }

   @Test
   void costAddsComplexityMultiplierAfterFiveRunes() {
      RuneProgram program = program(List.of("fehu"), List.of("kenaz", "wunjo", "laguz", "berkano"), List.of("uruz"), List.of(), RuneReleaseMode.DIRECT_AIR);
      // Six runes: base 300, trigger/modifier multiplier 1.5, complexity 1.08, then /3.
      assertEquals(486.0 / 3.0, RuneProgramCostService.calculate(program), 0.001);
   }

   @Test
   void weaponAllowsAllProjectileTriggerFamilies() {
      for (String trigger : List.of("fehu", "ansuz", "kenaz", "hagalaz", "sowilo")) {
         RuneProgram weapon = program(List.of(trigger), List.of(), List.of(), List.of(), RuneReleaseMode.WEAPON);
         assertTrue(weapon.validate().valid(), trigger);
         assertFalse(weapon.validate().errors().contains("projectile_trigger_conflict"), trigger);
      }
   }

   @Test
   void directProgramMayBeSavedWithoutTrigger() {
      RuneProgram program = program(List.of(), List.of("kenaz"), List.of("uruz"), List.of(), RuneReleaseMode.DIRECT_AIR);
      assertTrue(program.validate().valid());
      assertFalse(program.validate().errors().contains("missing_trigger"));
   }

   @Test
   void releaseModesFollowProgramStructure() {
      RuneProgram full = program(List.of("fehu"), List.of("kenaz"), List.of(), List.of("othala"), RuneReleaseMode.DIRECT_AIR);
      RuneProgram enchantment = program(List.of(), List.of("kenaz"), List.of("uruz"), List.of(), RuneReleaseMode.WEAPON);
      RuneProgram reinforcement = program(List.of(), List.of(), List.of("uruz"), List.of("othala"), RuneReleaseMode.ARMOR);
      RuneProgram inscription = program(List.of(), List.of(), List.of(), List.of(), RuneReleaseMode.RUNE_STONE);
      RuneProgram invalid = program(List.of(), List.of("kenaz"), List.of(), List.of("othala"), RuneReleaseMode.WEAPON);

      assertEquals(RuneProgramKind.FULL_RELEASE, full.kind());
      assertEquals(RuneProgramKind.ENCHANTMENT, enchantment.kind());
      assertEquals(RuneProgramKind.REINFORCEMENT, reinforcement.kind());
      assertEquals(RuneProgramKind.INSCRIPTION, inscription.kind());
      assertEquals(RuneProgramKind.INVALID, invalid.kind());
      assertTrue(RuneProgramService.matchesReleaseMode(full, RuneReleaseMode.DIRECT_AIR));
      assertTrue(RuneProgramService.matchesReleaseMode(enchantment, RuneReleaseMode.WEAPON));
      assertTrue(RuneProgramService.matchesReleaseMode(reinforcement, RuneReleaseMode.ARMOR));
      assertTrue(RuneProgramService.matchesReleaseMode(inscription, RuneReleaseMode.RUNE_STONE));
      assertFalse(RuneProgramService.matchesReleaseMode(invalid, RuneReleaseMode.WEAPON));
   }

   @Test
   void everyRuneCanBeUsedAsStandaloneDirectTrigger() {
      for (String id : RuneRegistry.ids()) {
         RuneProgram single = program(List.of(id), List.of(), List.of(), List.of(), RuneReleaseMode.DIRECT_AIR);
         assertTrue(single.validate().valid(), id);
         assertTrue(RuneProgramService.matchesReleaseMode(single, RuneReleaseMode.DIRECT_AIR), id);
      }
   }

   @Test
   void multiRuneCombinationsRemainCastable() {
      List<List<String>> combinations = List.of(
         List.of("kenaz", "uruz", "fehu", "othala"),
         List.of("hagalaz", "isa", "laguz", "jera"),
         List.of("thurisaz", "sowilo", "tiwaz", "ansuz"),
         List.of("algiz", "berkano", "ehwaz", "mannaz"),
         List.of("nauthiz", "perthro", "eihwaz", "dagaz"),
         List.of("fehu", "uruz", "thurisaz", "ansuz", "raidho", "kenaz", "gebo", "wunjo"),
         List.of("hagalaz", "nauthiz", "isa", "jera", "eihwaz", "perthro", "algiz", "sowilo"),
         List.of("tiwaz", "berkano", "ehwaz", "mannaz", "laguz", "ingwaz", "dagaz", "othala"),
         List.of("kenaz", "hagalaz", "thurisaz", "algiz", "berkano", "raidho"),
         List.of("uruz", "sowilo", "nauthiz", "isa", "laguz", "othala")
      );
      for (List<String> ids : combinations) {
         RuneProgram direct = program(ids, List.of(), List.of(), List.of(), RuneReleaseMode.DIRECT_AIR);
         assertTrue(direct.validate().valid(), ids.toString());
         assertTrue(RuneProgramService.matchesReleaseMode(direct, RuneReleaseMode.DIRECT_AIR), ids.toString());
      }
   }

   @Test
   void newModeNamesMigrateToLegacyMediaIds() {
      assertEquals(RuneReleaseMode.DIRECT_AIR, RuneReleaseMode.byName("full_release"));
      assertEquals(RuneReleaseMode.WEAPON, RuneReleaseMode.byName("enchantment"));
      assertEquals(RuneReleaseMode.ARMOR, RuneReleaseMode.byName("reinforcement"));
      assertEquals(RuneReleaseMode.RUNE_STONE, RuneReleaseMode.byName("inscription"));
   }

   @Test
   void nbtRoundTripKeepsUuidAndTwentySlots() {
      RuneProgram original = program(List.of("fehu"), List.of("kenaz"), List.of("gebo"), List.of("othala"), RuneReleaseMode.BLOCK_TRAP);
      RuneProgram restored = RuneProgram.fromNBT(original.serializeNBT());
      assertEquals(original.uuid(), restored.uuid());
      assertEquals(20, restored.slots().size());
      assertEquals(original.releaseMode(), restored.releaseMode());
   }

   @Test
   void releaseConfigDoesNotPersistTriggerCountPreset() {
      CompoundTag input = new CompoundTag();
      input.putInt("triggers", 5);
      input.putInt("delay", 20);
      CompoundTag normalized = RuneProgramService.normalizeReleaseConfig(input);
      assertFalse(normalized.contains("triggers"));
      assertEquals(20, normalized.getInt("delay"));
   }

   private static RuneProgram program(List<String> triggers, List<String> effects, List<String> modifiers, List<String> terminals, RuneReleaseMode mode) {
      return new RuneProgram(UUID.randomUUID(), "test", triggers, effects, modifiers, terminals, mode, new CompoundTag(), 1L, 1L);
   }
}

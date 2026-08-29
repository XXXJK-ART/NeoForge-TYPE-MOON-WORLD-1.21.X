package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Structural and bytecode smoke matrix for materially different rune programs. */
class RuneCombinationMatrixTest {
   private static final List<String> IDS = List.copyOf(RuneRegistry.ids());

   @Test
   void sixtyDifferentCombinationsRemainValidAndCompilable() {
      assertEquals(24, IDS.size());
      boolean[] seen = new boolean[IDS.size()];
      int tested = 0;
      for (int i = 0; i < 20; i++) {
         tested += assertProgram(fullRelease(i), RuneProgramKind.FULL_RELEASE, seen);
         tested += assertProgram(enchantment(i), RuneProgramKind.ENCHANTMENT, seen);
         tested += assertProgram(reinforcement(i), RuneProgramKind.REINFORCEMENT, seen);
      }
      assertEquals(60, tested);
      for (int i = 0; i < seen.length; i++) assertTrue(seen[i], "rune was never exercised: " + IDS.get(i));
   }

   @Test
   void orderedModifierProgramsKeepModifierSlotsInCompiledForm() {
      RuneProgram program = RuneProgram.ordered(UUID.randomUUID(), "ordered-modifier",
         List.of("uruz", "berkano"),
         List.of(RunePosition.MODIFIER, RunePosition.EFFECT),
         RuneReleaseMode.DIRECT_AIR, new net.minecraft.nbt.CompoundTag(), 1L, 1L);
      assertEquals(List.of(RunePosition.MODIFIER, RunePosition.EFFECT), program.sequencePositions());
      assertEquals(2, program.bytecode().size());
      assertTrue(program.validate().valid());
   }

   private static int assertProgram(RuneProgram program, RuneProgramKind expected, boolean[] seen) {
      assertEquals(expected, program.kind(), program.sequence().toString());
      assertTrue(program.validate().valid(), program.sequence().toString());
      assertEquals(program.sequence().size(), program.bytecode().size(), program.sequence().toString());
      assertTrue(Double.isFinite(RuneProgramCostService.calculate(program)));
      assertTrue(RuneProgramCostService.calculate(program) > 0.0D);
      for (String id : program.sequence()) seen[IDS.indexOf(id)] = true;
      return 1;
   }

   private static RuneProgram fullRelease(int index) {
      return program(index, RuneReleaseMode.DIRECT_AIR,
         pick(index, 2, 7), pick(index + 3, 2, 5), pick(index + 9, 1, 11), pick(index + 13, 1, 13));
   }

   private static RuneProgram enchantment(int index) {
      return program(index + 5, RuneReleaseMode.WEAPON,
         List.of(), pick(index + 2, 2, 5), pick(index + 11, 2, 7), List.of());
   }

   private static RuneProgram reinforcement(int index) {
      return program(index + 10, RuneReleaseMode.ARMOR,
         List.of(), List.of(), pick(index + 1, 2, 5), pick(index + 15, 1, 7));
   }

   private static RuneProgram program(int index, RuneReleaseMode mode, List<String> triggers,
      List<String> effects, List<String> modifiers, List<String> terminals) {
      return new RuneProgram(UUID.nameUUIDFromBytes(("rune-matrix-" + mode + "-" + index)
         .getBytes(StandardCharsets.UTF_8)), "matrix-" + mode + "-" + index,
         triggers, effects, modifiers, terminals, mode, new net.minecraft.nbt.CompoundTag(), 1L, 1L);
   }

   private static List<String> pick(int start, int count, int step) {
      ArrayList<String> result = new ArrayList<>();
      for (int i = 0; i < count; i++) result.add(IDS.get(Math.floorMod(start + i * step, IDS.size())));
      return List.copyOf(result);
   }
}

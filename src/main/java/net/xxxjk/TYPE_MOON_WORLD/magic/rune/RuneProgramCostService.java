package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

public final class RuneProgramCostService {
   private RuneProgramCostService() { }
   public static double baseCost(RuneProgram program) {
      if (program == null) return 0.0D;
      double cost = 0.0D; for (String id : program.slots()) { RuneDefinition def = RuneRegistry.get(id); if (def != null) cost += def.baseCost(); }
      return cost;
   }
   public static double calculate(RuneProgram program) {
      if (program == null) return 0.0D;
      int triggers = 0, modifiers = 0, total = 0;
      for (String id : program.slots()) if (id != null && !id.isEmpty()) total++;
      for (String id : program.slots(RunePosition.TRIGGER)) if (!id.isEmpty()) triggers++;
      for (String id : program.slots(RunePosition.MODIFIER)) if (!id.isEmpty()) modifiers++;
      double complexity = 1.0D + Math.max(0, total - 5) * 0.08D;
      return baseCost(program) * (1.0D + triggers * 0.3D + modifiers * 0.2D) * complexity / 3.0D;
   }
}

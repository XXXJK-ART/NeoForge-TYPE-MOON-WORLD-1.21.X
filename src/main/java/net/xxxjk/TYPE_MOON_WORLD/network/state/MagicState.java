package net.xxxjk.TYPE_MOON_WORLD.network.state;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Domain view for magic/circuit state stored in the player attachment. */
public final class MagicState {
   private final TypeMoonWorldModVariables.PlayerVariables vars;

   public MagicState(TypeMoonWorldModVariables.PlayerVariables vars) {
      this.vars = vars;
   }

   public double mana() { return vars.player_mana; }
   public double maxMana() { return vars.player_max_mana; }
   public boolean circuitOpen() { return vars.is_magic_circuit_open; }
   public boolean analysisActive() { return vars.magic_analysis_active; }
   public List<String> learnedMagics() { return vars.learned_magics; }
   public void setMana(double value) { vars.player_mana = Math.max(0.0D, value); vars.markDirty(TypeMoonWorldModVariables.StateDomain.MAGIC); }
   public void setCircuitOpen(boolean value) { vars.is_magic_circuit_open = value; vars.markDirty(TypeMoonWorldModVariables.StateDomain.MAGIC); }
   public void setAnalysisActive(boolean value) { vars.magic_analysis_active = value; vars.markDirty(TypeMoonWorldModVariables.StateDomain.MAGIC); }
}

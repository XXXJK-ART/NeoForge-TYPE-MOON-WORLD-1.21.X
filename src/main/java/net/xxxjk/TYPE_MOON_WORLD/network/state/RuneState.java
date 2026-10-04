package net.xxxjk.TYPE_MOON_WORLD.network.state;

import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgram;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Domain view for rune unlocks and programs. */
public final class RuneState {
   private final TypeMoonWorldModVariables.PlayerVariables vars;
   public RuneState(TypeMoonWorldModVariables.PlayerVariables vars) { this.vars = vars; }
   public boolean originUnlocked() { return vars.rune_origin_unlocked; }
   public List<String> learnedRunes() { return vars.learned_runes; }
   public List<RuneProgram> programs() { return vars.rune_programs; }
   public void setOriginUnlocked(boolean value) { vars.rune_origin_unlocked = value; vars.markDirty(TypeMoonWorldModVariables.StateDomain.RUNE); }
}

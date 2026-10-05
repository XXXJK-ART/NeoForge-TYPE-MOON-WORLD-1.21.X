package net.xxxjk.TYPE_MOON_WORLD.network.state;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Domain view for master/servant contract state. */
public final class MasterState {
   private final TypeMoonWorldModVariables.PlayerVariables vars;
   public MasterState(TypeMoonWorldModVariables.PlayerVariables vars) { this.vars = vars; }
   public boolean active() { return vars.master_active; }
   public String servantUuid() { return vars.master_servant_uuid; }
   public int commandSpells() { return vars.master_command_spells; }
   public void setActive(boolean value) { vars.master_active = value; vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MASTER); }
   public void setServantUuid(String value) { vars.master_servant_uuid = value == null ? "" : value; vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MASTER); }
   public void setCommandSpells(int value) { vars.master_command_spells = Math.max(0, value); vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MASTER); }
}

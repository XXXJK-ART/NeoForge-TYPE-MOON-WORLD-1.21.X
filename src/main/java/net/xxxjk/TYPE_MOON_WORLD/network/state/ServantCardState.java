package net.xxxjk.TYPE_MOON_WORLD.network.state;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Domain view for the active servant card runtime. */
public final class ServantCardState {
   private final TypeMoonWorldModVariables.PlayerVariables vars;
   public ServantCardState(TypeMoonWorldModVariables.PlayerVariables vars) { this.vars = vars; }
   public boolean transformed() { return vars.servant_card_transformed; }
   public String id() { return vars.servant_card_id; }
   public double mana() { return vars.servant_card_mana; }
   public void setTransformed(boolean value) { vars.servant_card_transformed = value; vars.markDirty(TypeMoonWorldModVariables.StateDomain.SERVANT_CARD); }
   public void setId(String value) { vars.servant_card_id = value == null ? "" : value; vars.markDirty(TypeMoonWorldModVariables.StateDomain.SERVANT_CARD); }
   public void setMana(double value) { vars.servant_card_mana = Math.max(0.0D, value); vars.markDirty(TypeMoonWorldModVariables.StateDomain.SERVANT_CARD); }
}

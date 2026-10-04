package net.xxxjk.TYPE_MOON_WORLD.network.state;

import java.util.Map;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank;

/** Domain view for passive/talent progression. */
public final class PassiveState {
   private final TypeMoonWorldModVariables.PlayerVariables vars;
   public PassiveState(TypeMoonWorldModVariables.PlayerVariables vars) { this.vars = vars; }
   public Map<String, Double> proficiencies() { return vars.talent_proficiencies; }
   public Map<String, PassiveRank> ranks() { return vars.passive_ranks; }
   public int bodyStrength() { return vars.body_strength; }
   public int bodySpeed() { return vars.body_speed; }
   public void setBodyStrength(int value) { vars.body_strength = Math.max(0, value); vars.markDirty(TypeMoonWorldModVariables.StateDomain.PASSIVE); }
   public void setBodySpeed(int value) { vars.body_speed = Math.max(0, value); vars.markDirty(TypeMoonWorldModVariables.StateDomain.PASSIVE); }
}

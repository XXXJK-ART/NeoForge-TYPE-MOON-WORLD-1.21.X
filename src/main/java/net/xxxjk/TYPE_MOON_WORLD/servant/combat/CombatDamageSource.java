package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.world.damagesource.DamageSource;

/** Immutable provenance carried by this specific hit, including nested synchronous damage. */
public final class CombatDamageSource extends DamageSource {
   private final AttackOrigin origin;
   public CombatDamageSource(DamageSource base, AttackOrigin origin) {
      super(base.typeHolder(), base.getDirectEntity(), base.getEntity(), base.getSourcePosition());
      this.origin = origin;
   }
   public AttackOrigin origin() { return origin; }
}

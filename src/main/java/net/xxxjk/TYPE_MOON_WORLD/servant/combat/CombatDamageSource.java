package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.world.damagesource.DamageSource;

/** Immutable provenance carried by this specific hit, including nested synchronous damage. */
public final class CombatDamageSource extends DamageSource implements net.xxxjk.typemoonworld.api.WrappedDamageSource {
   private final DamageSource base;
   private final AttackOrigin origin;
   private final float basicCharge;
   private final boolean bypassesMagicDefense;
   public CombatDamageSource(DamageSource base, AttackOrigin origin) {
      this(base, origin, 1.0F);
   }
   public CombatDamageSource(DamageSource base, AttackOrigin origin, float basicCharge) {
      this(base, origin, basicCharge, false);
   }
   public CombatDamageSource(DamageSource base, AttackOrigin origin, float basicCharge, boolean bypassesMagicDefense) {
      super(base.typeHolder(), base.getDirectEntity(), base.getEntity(), base.getSourcePosition());
      this.base = base;
      this.origin = origin;
      this.basicCharge = Math.clamp(basicCharge, 0, 1);
      this.bypassesMagicDefense = bypassesMagicDefense;
   }
   public boolean bypassesMagicDefense() { return bypassesMagicDefense; }
   public DamageSource baseSource() { return base; }
   public AttackOrigin origin() { return origin; }
   public float basicCharge() { return basicCharge; }
}

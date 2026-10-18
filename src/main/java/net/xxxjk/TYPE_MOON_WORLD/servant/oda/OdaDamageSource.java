package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.NpType;

/** Immutable provenance; never infer an NP from a gun's appearance or an AoE radius. */
public final class OdaDamageSource extends DamageSource {
   public enum Ability { WEAPON, THRUST, ORBIT, ROTATION, PURSUIT, PURSUIT_EXPLOSION, SNIPER, CROSSFIRE, SCORCHED, DASH, THREE_THOUSAND, HAJUN }
   private final Ability ability;
   public OdaDamageSource(DamageSource base, Entity direct, LivingEntity caster, Ability ability) {
      super(base.typeHolder(), direct, caster);
      this.ability = ability;
   }
   public Ability ability() { return ability; }
   public NpType npType() { return ability == Ability.THREE_THOUSAND ? NpType.ARMY : ability == Ability.HAJUN ? NpType.ANTI_DIVINE : null; }
}

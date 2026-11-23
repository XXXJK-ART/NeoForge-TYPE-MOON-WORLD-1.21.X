package net.xxxjk.typemoonworld.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Damage provenance for a hit explicitly produced by magecraft. */
public final class MagecraftDamageSource extends DamageSource implements WrappedDamageSource {
   private final DamageSource base;
   private final String magicId;
   private final MagicComplexity complexity;
   private final String noblePhantasmId;
   private final double casterProficiency;

   private MagecraftDamageSource(
      DamageSource base, LivingEntity caster, String magicId,
      MagicComplexity complexity, String noblePhantasmId, double casterProficiency
   ) {
      super(base.typeHolder(), base.getDirectEntity(), caster != null ? caster : base.getEntity(), base.getSourcePosition());
      this.base = base;
      this.magicId = magicId == null ? "" : magicId;
      this.complexity = complexity == null ? resolveComplexity(magicId) : complexity;
      this.noblePhantasmId = noblePhantasmId == null ? "" : noblePhantasmId;
      this.casterProficiency = casterProficiency;
   }

   public static MagecraftDamageSource of(DamageSource base, String magicId, LivingEntity caster) {
      return of(base, magicId, null, caster);
   }

   public static MagecraftDamageSource of(
      DamageSource base, String magicId, MagicComplexity complexity, LivingEntity caster
   ) {
      return new MagecraftDamageSource(base, caster, magicId, complexity, "", Double.NaN);
   }

   public static MagecraftDamageSource of(DamageSource base, String magicId, LivingEntity caster, double proficiency) {
      return new MagecraftDamageSource(base, caster, magicId, null, "", proficiency);
   }

   public double casterProficiency() { return casterProficiency; }

   public static MagecraftDamageSource noblePhantasm(
      DamageSource base, String noblePhantasmId, MagicComplexity complexity, LivingEntity caster
   ) {
      return new MagecraftDamageSource(base, caster, noblePhantasmId, complexity, noblePhantasmId, Double.NaN);
   }

   @Override
   public DamageSource baseSource() { return this.base; }

   public static MagecraftDamageSource find(DamageSource source) {
      while (source != null) {
         if (source instanceof MagecraftDamageSource magecraft) return magecraft;
         if (!(source instanceof WrappedDamageSource wrapped)) return null;
         source = wrapped.baseSource();
      }
      return null;
   }

   public String magicId() {
      return this.magicId;
   }

   public MagicComplexity complexity() {
      return this.complexity;
   }

   public boolean isNoblePhantasmMagic() {
      return !this.noblePhantasmId.isBlank();
   }

   public String noblePhantasmId() {
      return this.noblePhantasmId;
   }

   public static MagicComplexity resolveComplexity(String magicId) {
      if (magicId == null || magicId.isBlank()) return MagicComplexity.ONE_VERSE;
      var id = net.minecraft.resources.ResourceLocation.tryParse(magicId.indexOf(':') < 0 ? "typemoonworld:" + magicId : magicId);
      if (id == null) return MagicComplexity.ONE_VERSE;
      return TypeMoonWorldApi.addon("typemoonworld").magics().definition(id)
         .map(MagicDefinitionData::resolvedResistanceComplexity)
         .orElseGet(() -> MagicComplexity.infer(id, 0.0D, null));
   }
}

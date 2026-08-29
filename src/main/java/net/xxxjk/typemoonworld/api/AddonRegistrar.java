package net.xxxjk.typemoonworld.api;

/** Registration surface exposed to one addon namespace. */
public interface AddonRegistrar {
   String modId();
   ServantRegistry servants();
   ServantCardRegistry cards();
   MagicRegistry magics();
   SkillRegistry skills();
   NoblePhantasmRegistry noblePhantasms();
   LifecycleHookRegistry lifecycle();
   ClientExtensionRegistry client();
   GemRegistry gems();
   MasterProfileRegistry masters();
   ProjectionRegistry projections();
   CommandSpellRegistry commandSpells();
   ProjectileRegistry projectiles();
   EffectsRegistry effects();
   DamageTypeRegistry damageTypes();
   AiTacticsRegistry ai();
   boolean registerAttributeProvider(MagicAttributeProvider provider);
   boolean registerMagicAvailability(MagicAvailabilityProvider provider);
}

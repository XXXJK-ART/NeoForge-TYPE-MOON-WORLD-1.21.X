package net.xxxjk.typemoonworld.api;

import net.minecraft.world.damagesource.DamageSource;

/** Preserves per-hit provenance when damage source decorators are combined. */
public interface WrappedDamageSource {
   DamageSource baseSource();
}

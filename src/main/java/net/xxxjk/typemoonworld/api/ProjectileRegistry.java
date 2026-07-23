package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

/** NP projectile routing; addon-owned entity types can be supplied by the executor. */
public interface ProjectileRegistry {
   boolean register(ResourceLocation id, NoblePhantasmProjectileExecutor executor);
   boolean fire(ResourceLocation id, NoblePhantasmProjectileContext context);
}

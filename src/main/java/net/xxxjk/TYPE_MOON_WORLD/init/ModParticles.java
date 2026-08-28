package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
   public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES =
      DeferredRegister.create(Registries.PARTICLE_TYPE, "typemoonworld");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> ANSUZ_RUNE =
      PARTICLE_TYPES.register("ansuz_rune", () -> new SimpleParticleType(false));
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> LAGUZ_RUNE =
      PARTICLE_TYPES.register("laguz_rune", () -> new SimpleParticleType(false));
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> TIWAZ_RUNE =
      PARTICLE_TYPES.register("tiwaz_rune", () -> new SimpleParticleType(false));
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> ALGIZ_RUNE =
      PARTICLE_TYPES.register("algiz_rune", () -> new SimpleParticleType(false));
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> BERKANA_RUNE =
      PARTICLE_TYPES.register("berkana_rune", () -> new SimpleParticleType(false));
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> RUNE_BARRIER =
      PARTICLE_TYPES.register("rune_barrier", () -> new SimpleParticleType(false));

   private ModParticles() {
   }

   public static void register(IEventBus eventBus) {
      PARTICLE_TYPES.register(eventBus);
   }
}

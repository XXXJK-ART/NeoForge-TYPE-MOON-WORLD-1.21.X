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
   /** Elemental particles backed by the custom art in textures/particle. */
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> ELEMENTAL_FLAME =
      PARTICLE_TYPES.register("elemental_flame", () -> new SimpleParticleType(false));
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> ELEMENTAL_FOAM =
      PARTICLE_TYPES.register("elemental_foam", () -> new SimpleParticleType(false));
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> ELEMENTAL_LIGHTNING =
      PARTICLE_TYPES.register("elemental_lightning", () -> new SimpleParticleType(false));
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> FEHU_RUNE = rune("fehu");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> URUZ_RUNE = rune("uruz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> THURISAZ_RUNE = rune("thurisaz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> RAIDHO_RUNE = rune("raidho");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> KENAZ_RUNE = rune("kenaz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> GEBO_RUNE = rune("gebo");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> WUNJO_RUNE = rune("wunjo");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> HAGALAZ_RUNE = rune("hagalaz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> NAUTHIZ_RUNE = rune("nauthiz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> ISA_RUNE = rune("isa");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> JERA_RUNE = rune("jera");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> EIHWAZ_RUNE = rune("eihwaz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> PERTHRO_RUNE = rune("perthro");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> SOWILO_RUNE = rune("sowilo");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> BERKANO_RUNE = rune("berkano");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> EHWAZ_RUNE = rune("ehwaz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> MANNAZ_RUNE = rune("mannaz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> INGWAZ_RUNE = rune("ingwaz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> DAGAZ_RUNE = rune("dagaz");
   public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> OTHALA_RUNE = rune("othala");

   private static DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> rune(String id) {
      return PARTICLE_TYPES.register(id + "_rune", () -> new SimpleParticleType(false));
   }

   private ModParticles() {
   }

   public static void register(IEventBus eventBus) {
      PARTICLE_TYPES.register(eventBus);
   }
}

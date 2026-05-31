package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, "typemoonworld");
   public static final DeferredHolder<SoundEvent, SoundEvent> CYM_GEM_BIUBIUBIU = register("cym_gem_biubiubiu");
   public static final DeferredHolder<SoundEvent, SoundEvent> CYQ_GEM_SHOOT_STAR = register("cyq_gem_shoot_star");

   private ModSounds() {
   }

   private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("typemoonworld", name);
      return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
   }

   public static void register(IEventBus eventBus) {
      SOUND_EVENTS.register(eventBus);
   }
}

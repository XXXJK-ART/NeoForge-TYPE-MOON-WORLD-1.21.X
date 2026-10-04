package net.xxxjk.typemoonworld.api;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Stable lifecycle boundary used by optional addon implementations. */
public interface AddonEntrypoint {
   void initialize(IEventBus modEventBus, ModContainer modContainer);

   default void registerPayloads(PayloadRegistrar registrar) {
   }
}

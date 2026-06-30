package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary;

@EventBusSubscriber(
   modid = TYPE_MOON_WORLD.MOD_ID,
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class VFXClientEvents {
   private VFXClientEvents() {
   }

   @SubscribeEvent
   public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(EffectLibrary.INSTANCE);
   }

   @EventBusSubscriber(
      modid = TYPE_MOON_WORLD.MOD_ID,
      value = {Dist.CLIENT}
   )
   public static final class ForgeBus {
      private ForgeBus() {
      }

      @SubscribeEvent
      public static void registerClientCommands(RegisterClientCommandsEvent event) {
         VFXClientCommands.register(event.getDispatcher());
      }
   }
}

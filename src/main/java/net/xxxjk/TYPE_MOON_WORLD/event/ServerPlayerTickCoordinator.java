package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Single server-side entry point for player tick work.
 *
 * Event handlers that only advance a service belong in this coordinator.  A
 * service still owns its state and may cheaply return when it is inactive.
 */
@EventBusSubscriber(modid = "typemoonworld")
public final class ServerPlayerTickCoordinator {
   private ServerPlayerTickCoordinator() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player)
         || player.level().isClientSide()
         || !player.isAlive()
         || player.isRemoved()) {
         return;
      }

      CommonEvents.tickGoldenFleece(player);
      CommonEvents.tickPlayer(player);
      ServerPlayerTickHooks.run(player);
   }
}

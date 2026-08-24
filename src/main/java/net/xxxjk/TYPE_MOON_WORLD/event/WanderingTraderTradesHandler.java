package net.xxxjk.TYPE_MOON_WORLD.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class WanderingTraderTradesHandler {
   @SubscribeEvent
   public static void onWandererTrades(WandererTradesEvent event) {
      // Reserved for a later dedicated wandering-trader magic trade design.
   }
}

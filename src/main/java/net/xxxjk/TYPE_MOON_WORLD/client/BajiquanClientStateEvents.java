package net.xxxjk.TYPE_MOON_WORLD.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT)
public final class BajiquanClientStateEvents {
   private BajiquanClientStateEvents() {}

   @SubscribeEvent
   public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
      BajiquanPoseClient.clear();
      GanryuPoseClient.clear();
      CircleRealmClient.clear();
   }
}

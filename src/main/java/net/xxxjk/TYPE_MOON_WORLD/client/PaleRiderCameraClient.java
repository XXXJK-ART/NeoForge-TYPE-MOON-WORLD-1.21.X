package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public final class PaleRiderCameraClient {
   private static int pendingEntityId = -1;

   private PaleRiderCameraClient() {}

   public static void follow(int entityId) {
      pendingEntityId = entityId;
      applyPending();
   }

   public static void reset() {
      pendingEntityId = -1;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) mc.setCameraEntity(mc.player);
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      applyPending();
   }

   private static void applyPending() {
      Minecraft mc = Minecraft.getInstance();
      if (pendingEntityId >= 0 && mc.level != null && mc.level.getEntity(pendingEntityId) != null) {
         mc.setCameraEntity(mc.level.getEntity(pendingEntityId));
      }
   }
}

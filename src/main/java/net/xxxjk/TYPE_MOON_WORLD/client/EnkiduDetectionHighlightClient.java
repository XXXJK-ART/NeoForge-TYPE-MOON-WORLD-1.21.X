package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public final class EnkiduDetectionHighlightClient {
   private static final Map<Integer, Integer> HIGHLIGHT_UNTIL = new HashMap<>();
   private static int clientTick;

   private EnkiduDetectionHighlightClient() {
   }

   public static void apply(List<Integer> entityIds, int ticks) {
      int until = clientTick + Math.max(1, ticks);
      for (Integer id : entityIds) {
         if (id != null) {
            HIGHLIGHT_UNTIL.put(id, until);
            Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(id);
            if (entity != null) {
               entity.setGlowingTag(true);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      clientTick++;
      if (HIGHLIGHT_UNTIL.isEmpty() || Minecraft.getInstance().level == null) {
         return;
      }
      HIGHLIGHT_UNTIL.entrySet().removeIf(entry -> {
         if (entry.getValue() > clientTick) {
            return false;
         }
         Entity entity = Minecraft.getInstance().level.getEntity(entry.getKey());
         if (entity != null) {
            entity.setGlowingTag(false);
         }
         return true;
      });
   }
}

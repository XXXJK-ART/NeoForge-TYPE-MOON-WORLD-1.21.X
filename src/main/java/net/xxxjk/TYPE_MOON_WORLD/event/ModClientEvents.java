package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementLayer;

@EventBusSubscriber(
   modid = "typemoonworld",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public class ModClientEvents {
   @SubscribeEvent
   public static void registerLayerDefinitions(AddLayers event) {
      for (Model skinType : event.getSkins()) {
         if (event.getSkin(skinType) instanceof PlayerRenderer playerRenderer) {
            playerRenderer.addLayer(new ReinforcementLayer(playerRenderer));
         }
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RenderArmEvent;

@EventBusSubscriber(
   modid = "typemoonworld",
   value = {Dist.CLIENT},
   bus = Bus.GAME
)
public class ModClientArmEvents {
   private static final int EMISSIVE_LIGHT = 15728880;
   private static final int SEMI_TRANSPARENT_WHITE = -855638017;

   @SubscribeEvent
   public static void onRenderArm(RenderArmEvent event) {
   }
}

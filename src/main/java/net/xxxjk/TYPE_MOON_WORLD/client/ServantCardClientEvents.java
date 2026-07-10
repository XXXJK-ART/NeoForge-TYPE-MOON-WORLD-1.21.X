package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

@EventBusSubscriber(value = {Dist.CLIENT})
public final class ServantCardClientEvents {
   private ServantCardClientEvents() {
   }

   @SubscribeEvent
   public static void onComputeFov(ComputeFovModifierEvent event) {
      if (event.getPlayer() != Minecraft.getInstance().player) {
         return;
      }
      ItemStack using = event.getPlayer().getUseItem();
      if (!using.is(ModItems.NAMELESS_BOW.get())) {
         return;
      }
      int useTicks = event.getPlayer().getTicksUsingItem();
      if (useTicks < 20) {
         return;
      }
      int zoomSteps = Math.min(3, (useTicks - 20) / 10);
      float magnification = 2.0F + zoomSteps;
      float zoom = 1.0F / magnification;
      event.setNewFovModifier(event.getNewFovModifier() * zoom);
   }

   @SubscribeEvent
   public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
      if (ServantCardConcealmentClient.isPerfectlyConcealed(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onRenderNameTag(RenderNameTagEvent event) {
      if (event.getEntity() instanceof Player player && ServantCardConcealmentClient.isPerfectlyConcealed(player)) {
         event.setCanRender(TriState.FALSE);
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;

@EventBusSubscriber(value = Dist.CLIENT, modid = "typemoonworld")
public final class ClairvoyanceClientState {
   private static boolean active;
   private static int zoom = 1;
   private static int maxZoom = 2;

   private ClairvoyanceClientState() {
   }

   public static void apply(boolean toggle, int maximum) {
      maxZoom = Math.max(2, Math.min(12, maximum));
      if (!toggle) {
         reset();
      } else {
         active = true;
         zoom = Math.max(1, Math.min(active ? zoom : 2, maxZoom));
      }
   }

   public static boolean isActive() {
      return active;
   }

   public static int zoom() {
      return zoom;
   }

   public static void reset() {
      active = false;
      zoom = 1;
      maxZoom = 2;
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onMouseScroll(MouseScrollingEvent event) {
      if (!active || Minecraft.getInstance().screen != null || event.getScrollDeltaY() == 0.0) return;
      zoom = Math.max(1, Math.min(maxZoom, zoom + (event.getScrollDeltaY() > 0.0 ? 1 : -1)));
      event.setCanceled(true);
   }

   @SubscribeEvent
   public static void onComputeFov(ComputeFovModifierEvent event) {
      if (active && event.getPlayer() == Minecraft.getInstance().player) {
         event.setNewFovModifier(event.getNewFovModifier() / Math.max(1.0F, zoom));
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      if (!active) return;
      var player = Minecraft.getInstance().player;
      if (player == null || !player.isAlive()) {
         reset();
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (PassiveService.effectsSuppressed(vars)
         || !TalentService.CLAIRVOYANCE.equals(PlayerMagicSelectionService.getCurrentMagicId(vars))
         || !TalentService.owns(vars, TalentService.CLAIRVOYANCE)) {
         reset();
      }
   }

   @SubscribeEvent
   public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
      reset();
   }
}

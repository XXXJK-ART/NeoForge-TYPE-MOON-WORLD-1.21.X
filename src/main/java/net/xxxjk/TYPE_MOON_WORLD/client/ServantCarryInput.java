package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterCarryDismountMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT)
public final class ServantCarryInput {
   private static boolean consumeTab, consumeUse, shiftWasDown, wasCarried, wasCarrying;
   private ServantCarryInput() {}
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void key(InputEvent.Key event) {
      var mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null || event.getKey() != GLFW.GLFW_KEY_TAB) return;
      if (event.getAction() == GLFW.GLFW_PRESS && (ServantMasterCarryService.isCarryingMaster(mc.player)
         || ServantMasterCarryService.isCarriedMaster(mc.player))) {
         consumeTab = true;
         PacketDistributor.sendToServer(new MasterCarryDismountMessage());
      }
      if (consumeTab) suppressTab(mc);
   }
   public static boolean consumesTab() { return consumeTab; }
   public static boolean consumesUse() { return consumeUse; }
   public static void consumeUseUntilRelease() { consumeUse = true; }
   private static void suppressTab(Minecraft mc) {
      mc.options.keyPlayerList.setDown(false);
      TypeMoonWorldModKeyMappings.OPEN_PROJECTION_PRESET.setDown(false);
      while (mc.options.keyPlayerList.consumeClick()) {}
      while (TypeMoonWorldModKeyMappings.OPEN_PROJECTION_PRESET.consumeClick()) {}
   }
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void tick(ClientTickEvent.Pre event) {
      var mc = Minecraft.getInstance();
      if (mc.player == null) { consumeTab = false; wasCarried = false; return; }
      if (consumeTab) {
         suppressTab(mc);
         if (!InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_TAB)) consumeTab = false;
      }
      boolean shift = mc.options.keyShift.isDown();
      boolean carried = ServantMasterCarryService.isCarriedMaster(mc.player);
      boolean carrying = ServantMasterCarryService.isCarryingMaster(mc.player);
      if (!mc.options.keyUse.isDown()) consumeUse = false;
      else if (carrying && !wasCarrying) consumeUse = true;
      if (carried && wasCarried && shift && !shiftWasDown && mc.screen == null) PacketDistributor.sendToServer(new MasterCarryDismountMessage());
      shiftWasDown = shift;
      wasCarried = carried;
      wasCarrying = carrying;
   }
   @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
      ServantMasterCarryService.clearClientPairs();
      consumeTab = false; consumeUse = false; shiftWasDown = false; wasCarried = false; wasCarrying = false;
   }
}

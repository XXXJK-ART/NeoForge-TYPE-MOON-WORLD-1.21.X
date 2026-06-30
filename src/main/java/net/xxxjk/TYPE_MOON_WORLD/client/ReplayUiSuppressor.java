package net.xxxjk.TYPE_MOON_WORLD.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(
   modid = "typemoonworld",
   value = {Dist.CLIENT}
)
public final class ReplayUiSuppressor {
   private static boolean replayReflectionInitialized = false;
   private static Field replayModuleInstanceField = null;
   private static Method getReplayHandlerMethod = null;

   private ReplayUiSuppressor() {
   }

   public static boolean shouldHideTypeMoonHud() {
      return isVanillaGuiHidden() || isReForgedReplayActive();
   }

   public static boolean shouldSuppressTypeMoonScreens() {
      return isReForgedReplayActive();
   }

   public static boolean isVanillaGuiHidden() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft != null && minecraft.options != null && minecraft.options.hideGui;
   }

   public static boolean isReForgedReplayActive() {
      if (!isReForgedPlayLoaded()) {
         return false;
      }

      initReplayReflection();
      if (replayModuleInstanceField == null || getReplayHandlerMethod == null) {
         return false;
      }

      try {
         Object replayModule = replayModuleInstanceField.get(null);
         return replayModule != null && getReplayHandlerMethod.invoke(replayModule) != null;
      } catch (ReflectiveOperationException | RuntimeException ignored) {
         return false;
      }
   }

   private static boolean isReForgedPlayLoaded() {
      ModList modList = ModList.get();
      return modList != null && (modList.isLoaded("reforgedplaymod") || modList.isLoaded("replaymod"));
   }

   private static void initReplayReflection() {
      if (replayReflectionInitialized) {
         return;
      }

      replayReflectionInitialized = true;
      try {
         Class<?> replayClass = Class.forName("com.replaymod.replay.ReplayModReplay");
         replayModuleInstanceField = replayClass.getField("instance");
         getReplayHandlerMethod = replayClass.getMethod("getReplayHandler");
      } catch (ReflectiveOperationException | LinkageError ignored) {
         replayModuleInstanceField = null;
         getReplayHandlerMethod = null;
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onScreenOpening(ScreenEvent.Opening event) {
      if (shouldSuppressTypeMoonScreens() && isTypeMoonScreen(event.getNewScreen())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
      if (shouldSuppressTypeMoonScreens() && isTypeMoonScreen(event.getScreen())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null && shouldSuppressTypeMoonScreens() && isTypeMoonScreen(minecraft.screen)) {
         minecraft.setScreen(null);
      }
   }

   private static boolean isTypeMoonScreen(Screen screen) {
      if (screen == null) {
         return false;
      }

      Package screenPackage = screen.getClass().getPackage();
      if (screenPackage == null) {
         return false;
      }

      String packageName = screenPackage.getName();
      return packageName.startsWith("net.xxxjk.TYPE_MOON_WORLD.client.gui")
         || packageName.startsWith("net.xxxjk.TYPE_MOON_WORLD.client.screens");
   }
}

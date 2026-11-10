package net.xxxjk.TYPE_MOON_WORLD.client;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class PaleRiderGrayscaleClient {
   private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "shaders/post/pale_rider_grayscale.json");
   private static PostChain chain;
   private static boolean active;
   private static int width;
   private static int height;

   private PaleRiderGrayscaleClient() {}

   public static void setActive(boolean enabled) {
      active = enabled;
      if (!enabled) closeChain();
   }

   @SubscribeEvent
   public static void render(RenderGuiEvent.Post event) {
      if (!active) return;
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.getMainRenderTarget() == null) return;
      try {
         if (chain == null) {
            chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), EFFECT);
            resize(minecraft);
         }
         if (width != minecraft.getWindow().getWidth() || height != minecraft.getWindow().getHeight()) resize(minecraft);
         chain.process(event.getPartialTick().getGameTimeDeltaPartialTick(false));
      } catch (IOException | RuntimeException exception) {
         TYPE_MOON_WORLD.LOGGER.error("Failed to apply Pale Rider grayscale screen effect", exception);
         closeChain();
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      if (active && Minecraft.getInstance().level == null) setActive(false);
   }

   private static void resize(Minecraft minecraft) {
      width = minecraft.getWindow().getWidth();
      height = minecraft.getWindow().getHeight();
      chain.resize(width, height);
   }

   private static void closeChain() {
      if (chain != null) {
         chain.close();
         chain = null;
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class PaleRiderDesaturationRenderer {
   private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "shaders/post/pale_rider_desaturate.json");
   private static PostChain chain;
   private static int width;
   private static int height;

   private PaleRiderDesaturationRenderer() {
   }

   @SubscribeEvent
   public static void render(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
      Minecraft minecraft = Minecraft.getInstance();
      float strength = VFXEnvironmentManager.desaturationStrength();
      if (minecraft.level == null || strength <= 0.001F) {
         close();
         return;
      }
      try {
         ensureChain(minecraft);
         if (chain != null) {
            RenderSystem.assertOnRenderThread();
            chain.process(event.getPartialTick().getGameTimeDeltaTicks());
            minecraft.getMainRenderTarget().bindWrite(false);
         }
      } catch (Exception exception) {
         TYPE_MOON_WORLD.LOGGER.warn("Failed to render Pale Rider desaturation", exception);
         close();
      }
   }

   private static void ensureChain(Minecraft minecraft) throws IOException {
      if (chain == null) {
         chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), EFFECT);
      }
      int newWidth = minecraft.getWindow().getWidth();
      int newHeight = minecraft.getWindow().getHeight();
      if (newWidth != width || newHeight != height) {
         width = newWidth;
         height = newHeight;
         chain.resize(width, height);
      }
   }

   private static void close() {
      if (chain != null) {
         chain.close();
         chain = null;
      }
   }
}

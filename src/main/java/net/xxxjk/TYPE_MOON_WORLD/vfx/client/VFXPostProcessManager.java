package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXScreenEffectDefinition;

/** Shared, single-pass screen compositor for short-lived VFX transitions. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class VFXPostProcessManager {
   private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "shaders/post/vfx_screen_effects.json");
   private static final ResourceLocation BLOOM_EFFECT = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "shaders/post/vfx_bloom.json");
   private static final List<ActiveScreenEffect> ACTIVE = new ArrayList<>();
   private static PostChain chain;
   private static PostChain bloomChain;
   private static int width;
   private static int height;
   private static float time;
   private static volatile boolean resetPending;

   private VFXPostProcessManager() { }

   public static void add(List<VFXScreenEffectDefinition> effects) {
      if (effects == null || effects.isEmpty() || !Config.vfxScreenEffects) return;
      for (VFXScreenEffectDefinition effect : effects) {
         if (effect.type().isBlank() || effect.duration() <= 0.0F) continue;
         ACTIVE.add(new ActiveScreenEffect(effect.type(), effect.intensity(), effect.duration(), effect.radius()));
      }
   }

   public static void clear() {
      ACTIVE.clear();
      time = 0.0F;
      close();
      closeBloom();
   }

   /** Schedules GL resource disposal on the client tick/render thread. */
   public static void requestReset() { resetPending = true; }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      if (resetPending) {
         resetPending = false;
         clear();
      }
      if (ACTIVE.isEmpty()) return;
      time += 1.0F / 20.0F;
      Iterator<ActiveScreenEffect> iterator = ACTIVE.iterator();
      while (iterator.hasNext()) {
         ActiveScreenEffect effect = iterator.next();
         effect.age += 1.0F / 20.0F;
         if (effect.age >= effect.duration) iterator.remove();
      }
      if (ACTIVE.isEmpty()) close();
   }

   @SubscribeEvent
   public static void render(RenderGuiEvent.Pre event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) return;
      try {
         RenderSystem.assertOnRenderThread();
         if (Config.vfxBloom && VFXRenderManager.hasBloomEffects()) {
            ensureBloomChain(minecraft);
            if (bloomChain != null) {
               bloomChain.setUniform("Threshold", 0.72F);
               bloomChain.setUniform("Intensity", VFXRenderManager.pressure() == net.xxxjk.TYPE_MOON_WORLD.vfx.VFXPerformanceBudget.Pressure.CRITICAL ? 0.45F : 0.85F);
               bloomChain.process(event.getPartialTick().getGameTimeDeltaTicks());
               minecraft.getMainRenderTarget().bindWrite(true);
            }
         } else {
            closeBloom();
         }
         if (ACTIVE.isEmpty() || !Config.vfxScreenEffects) {
            close();
            return;
         }
         ensureChain(minecraft);
         if (chain == null) return;
         float white = Math.max(strength("white_flash"), Math.max(strength("whiteout"), strength("whiteout_wave")));
         float glass = Math.max(strength("glass_break"), Math.max(strength("glass_shatter"), strength("portal_rift")));
         float timestop = Math.max(strength("timestop_border"), Math.max(strength("time_stop"), strength("timestop")));
         float chromatic = Math.max(strength("chromatic_aberration"), strength("color_split"));
         float radial = Math.max(strength("radial_blur"), strength("impact_blur"));
         float vignette = Math.max(strength("vignette"), Math.max(timestop * 0.7F, radial * 0.25F));
         chain.setUniform("WhiteFlash", white);
         chain.setUniform("GlassBreak", glass);
         chain.setUniform("TimeStop", timestop);
         chain.setUniform("Chromatic", Config.vfxDistortion ? chromatic : 0.0F);
         chain.setUniform("RadialBlur", Config.vfxDistortion ? radial : 0.0F);
         chain.setUniform("Vignette", vignette);
         chain.setUniform("Time", time);
         chain.process(event.getPartialTick().getGameTimeDeltaTicks());
         minecraft.getMainRenderTarget().bindWrite(true);
      } catch (Exception exception) {
         TYPE_MOON_WORLD.LOGGER.warn("Failed to render VFX screen effects", exception);
         close();
      } finally {
         // A failed/short-circuited pass may leave a temporary target bound.
         // Always hand the frame back to vanilla's main target before HUD
         // layers continue rendering.
         minecraft.getMainRenderTarget().bindWrite(true);
         // PostPass leaves the depth function/blend state configured for its
         // fullscreen quad. Restore vanilla state for the first-person hand
         // pass (and for the HUD if this hook runs from RenderGuiEvent.Pre).
         restoreRenderState();
      }
   }

   private static void restoreRenderState() {
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515); // GL_LEQUAL
      RenderSystem.depthMask(true);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.resetTextureMatrix();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static float strength(String type) {
      float result = 0.0F;
      for (ActiveScreenEffect effect : ACTIVE) {
         if (!effect.type.equals(type)) continue;
         float progress = effect.age / Math.max(0.001F, effect.duration);
         float fade = progress < 0.18F ? progress / 0.18F : (progress > 0.72F ? (1.0F - progress) / 0.28F : 1.0F);
         result = Math.max(result, effect.intensity * Math.max(0.0F, Math.min(1.0F, fade)));
      }
      return result;
   }

   private static void ensureChain(Minecraft minecraft) throws IOException {
      if (chain == null) {
         chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), EFFECT);
         width = 0;
         height = 0;
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

   private static void ensureBloomChain(Minecraft minecraft) throws IOException {
      if (bloomChain == null) {
         bloomChain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), BLOOM_EFFECT);
      }
      float scale = bloomScale();
      int targetWidth = Math.max(1, Math.round(minecraft.getWindow().getWidth() * scale));
      int targetHeight = Math.max(1, Math.round(minecraft.getWindow().getHeight() * scale));
      var bright = bloomChain.getTempTarget("bright");
      var blur = bloomChain.getTempTarget("blur");
      var swap = bloomChain.getTempTarget("swap");
      if (bright != null && (bright.width != targetWidth || bright.height != targetHeight)) bright.resize(targetWidth, targetHeight, Minecraft.ON_OSX);
      if (blur != null && (blur.width != targetWidth || blur.height != targetHeight)) blur.resize(targetWidth, targetHeight, Minecraft.ON_OSX);
      int screenWidth = minecraft.getWindow().getWidth();
      int screenHeight = minecraft.getWindow().getHeight();
      if (swap != null && (swap.width != screenWidth || swap.height != screenHeight)) swap.resize(screenWidth, screenHeight, Minecraft.ON_OSX);
   }

   private static float bloomScale() {
      if ("HIGH".equalsIgnoreCase(Config.vfxQuality)) return 1.0F;
      if ("MEDIUM".equalsIgnoreCase(Config.vfxQuality)) return 0.75F;
      if ("LOW".equalsIgnoreCase(Config.vfxQuality)) return 0.5F;
      return switch (VFXRenderManager.pressure()) {
         case NORMAL -> 0.75F;
         case PRESSURED, CRITICAL -> 0.5F;
      };
   }

   private static void closeBloom() {
      if (bloomChain != null) {
         bloomChain.close();
         bloomChain = null;
      }
   }

   private static final class ActiveScreenEffect {
      private final String type;
      private final float intensity;
      private final float duration;
      @SuppressWarnings("unused")
      private final float radius;
      private float age;

      private ActiveScreenEffect(String type, float intensity, float duration, float radius) {
         this.type = type;
         this.intensity = intensity;
         this.duration = duration;
         this.radius = radius;
      }
   }
}

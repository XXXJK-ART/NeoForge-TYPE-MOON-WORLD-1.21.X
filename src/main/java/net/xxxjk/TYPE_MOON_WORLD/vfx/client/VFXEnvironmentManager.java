package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEnvironmentDefinition;

@EventBusSubscriber(
   modid = TYPE_MOON_WORLD.MOD_ID,
   value = {Dist.CLIENT}
)
public final class VFXEnvironmentManager {
   private static final List<ActiveEnvironment> ACTIVE = new ArrayList<>();
   private static boolean forcedRainApplied;

   private VFXEnvironmentManager() {
   }

   public static void add(VFXEnvironmentDefinition definition, double x, double y, double z) {
      ACTIVE.add(new ActiveEnvironment(definition, x, y, z));
   }

   public static void clear() {
      ACTIVE.clear();
      clearForcedRain();
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      Iterator<ActiveEnvironment> iterator = ACTIVE.iterator();
      while (iterator.hasNext()) {
         if (!iterator.next().tick()) {
            iterator.remove();
         }
      }
      applyRainWeather();
   }

   private static void applyRainWeather() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         forcedRainApplied = false;
         return;
      }
      float rain = 0.0F;
      for (ActiveEnvironment active : ACTIVE) {
         if ("rain_weather".equals(active.definition.type())) {
            rain = Math.max(rain, active.alpha());
         }
      }
      if (rain > 0.0F) {
         minecraft.level.setRainLevel(Math.min(1.0F, rain));
         minecraft.level.setThunderLevel(0.0F);
         forcedRainApplied = true;
      } else if (forcedRainApplied) {
         clearForcedRain();
      }
   }

   private static void clearForcedRain() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null) {
         minecraft.level.setRainLevel(0.0F);
         minecraft.level.setThunderLevel(0.0F);
      }
      forcedRainApplied = false;
   }

   @SubscribeEvent
   public static void onRenderGui(RenderGuiEvent.Pre event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player == null || ACTIVE.isEmpty()) {
         return;
      }
      GuiGraphics graphics = event.getGuiGraphics();
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      int overlay = 0;
      for (ActiveEnvironment active : ACTIVE) {
         if (!"screen_tint".equals(active.definition.type())) {
            continue;
         }
         float alpha = active.alpha();
         int color = active.definition.color();
         int argb = ((Math.min(255, Math.round(alpha * 255.0F)) & 255) << 24) | (color & 0x00FFFFFF);
         overlay = blendOver(overlay, argb);
      }
      if ((overlay >>> 24) > 0) {
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         graphics.fill(0, 0, width, height, overlay);
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
      }
   }

   @SubscribeEvent
   public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
      FogMix fog = fogMix();
      if (fog.alpha <= 0.0F) {
         return;
      }
      event.setRed(lerp(event.getRed(), fog.red, fog.alpha));
      event.setGreen(lerp(event.getGreen(), fog.green, fog.alpha));
      event.setBlue(lerp(event.getBlue(), fog.blue, fog.alpha));
   }

   @SubscribeEvent
   public static void onRenderFog(ViewportEvent.RenderFog event) {
      FogMix fog = fogMix();
      if (fog.alpha <= 0.0F) {
         return;
      }
      float factor = 1.0F - fog.alpha * 0.72F;
      event.scaleNearPlaneDistance(Math.max(0.08F, factor * 0.55F));
      event.scaleFarPlaneDistance(Math.max(0.12F, factor));
      event.setCanceled(true);
   }

   private static FogMix fogMix() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player == null || ACTIVE.isEmpty()) {
         return FogMix.NONE;
      }
      float alpha = 0.0F;
      float red = 0.0F;
      float green = 0.0F;
      float blue = 0.0F;
      for (ActiveEnvironment active : ACTIVE) {
         if (!active.isWorldFog()) {
            continue;
         }
         float currentAlpha = active.alpha();
         if (currentAlpha <= 0.0F) {
            continue;
         }
         int color = active.definition.color();
         float cr = ((color >>> 16) & 255) / 255.0F;
         float cg = ((color >>> 8) & 255) / 255.0F;
         float cb = (color & 255) / 255.0F;
         float nextAlpha = currentAlpha + alpha * (1.0F - currentAlpha);
         if (nextAlpha > 0.0F) {
            red = (cr * currentAlpha + red * alpha * (1.0F - currentAlpha)) / nextAlpha;
            green = (cg * currentAlpha + green * alpha * (1.0F - currentAlpha)) / nextAlpha;
            blue = (cb * currentAlpha + blue * alpha * (1.0F - currentAlpha)) / nextAlpha;
         }
         alpha = nextAlpha;
      }
      return new FogMix(red, green, blue, Math.min(0.92F, alpha));
   }

   private static float lerp(float from, float to, float alpha) {
      return from + (to - from) * alpha;
   }

   private static int blendOver(int base, int over) {
      float ba = ((base >>> 24) & 255) / 255.0F;
      float br = ((base >>> 16) & 255) / 255.0F;
      float bg = ((base >>> 8) & 255) / 255.0F;
      float bb = (base & 255) / 255.0F;
      float oa = ((over >>> 24) & 255) / 255.0F;
      float or = ((over >>> 16) & 255) / 255.0F;
      float og = ((over >>> 8) & 255) / 255.0F;
      float ob = (over & 255) / 255.0F;
      float outA = oa + ba * (1.0F - oa);
      if (outA <= 0.0F) {
         return 0;
      }
      float outR = (or * oa + br * ba * (1.0F - oa)) / outA;
      float outG = (og * oa + bg * ba * (1.0F - oa)) / outA;
      float outB = (ob * oa + bb * ba * (1.0F - oa)) / outA;
      return (Math.min(255, Math.round(outA * 255.0F)) & 255) << 24
         | (Math.min(255, Math.round(outR * 255.0F)) & 255) << 16
         | (Math.min(255, Math.round(outG * 255.0F)) & 255) << 8
         | Math.min(255, Math.round(outB * 255.0F)) & 255;
   }

   private static final class ActiveEnvironment {
      private final VFXEnvironmentDefinition definition;
      private final double x;
      private final double y;
      private final double z;
      private int ageTicks;

      private ActiveEnvironment(VFXEnvironmentDefinition definition, double x, double y, double z) {
         this.definition = definition;
         this.x = x;
         this.y = y;
         this.z = z;
      }

      private boolean tick() {
         this.ageTicks++;
         return this.ageTicks <= Math.max(1, Math.round(this.definition.endTime() * 20.0F));
      }

      private float alpha() {
         float time = this.ageTicks / 20.0F;
         float fadeIn = this.definition.fadeIn();
         float fadeOut = this.definition.fadeOut();
         float start = this.definition.startTime();
         float end = this.definition.endTime();
         if (time < start || time > end) {
            return 0.0F;
         }
         float local = (time - start) / Math.max(0.001F, end - start);
         float fadeInAlpha = fadeIn <= 0.0F ? 1.0F : Math.min(1.0F, (time - start) / fadeIn);
         float fadeOutAlpha = fadeOut <= 0.0F ? 1.0F : Math.min(1.0F, (end - time) / fadeOut);
         return Math.max(0.0F, Math.min(1.0F, Math.min(fadeInAlpha, fadeOutAlpha) * this.definition.intensity() * (0.5F + 0.5F * local)));
      }

      private boolean isWorldFog() {
         String type = this.definition.type();
         return "world_darkness".equals(type) || "red_fog".equals(type) || "thunderstorm_hint".equals(type);
      }
   }

   private record FogMix(float red, float green, float blue, float alpha) {
      private static final FogMix NONE = new FogMix(0.0F, 0.0F, 0.0F, 0.0F);
   }
}

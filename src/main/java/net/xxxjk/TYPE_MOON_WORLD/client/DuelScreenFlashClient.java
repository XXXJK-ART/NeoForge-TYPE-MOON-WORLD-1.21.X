package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Renders the brief white impact flash over the HUD. */
@EventBusSubscriber(value = Dist.CLIENT)
public final class DuelScreenFlashClient {
   private static long expiresAtNanos;
   private static long durationNanos;
   private static float strength;

   private DuelScreenFlashClient() { }

   public static void apply(int ticks, float alpha) {
      durationNanos = Math.max(1L, ticks) * 50_000_000L;
      expiresAtNanos = System.nanoTime() + durationNanos;
      strength = Math.max(0.0F, Math.min(1.0F, alpha));
   }

   @SubscribeEvent
   public static void render(RenderGuiEvent.Post event) {
      long remaining = expiresAtNanos - System.nanoTime();
      if (remaining <= 0L || durationNanos <= 0L) return;
      float fade = Math.max(0.0F, Math.min(1.0F, remaining / (float)durationNanos));
      int alpha = Math.max(1, Math.min(255, Math.round(strength * fade * 255.0F)));
      GuiGraphics gui = event.getGuiGraphics();
      gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), (alpha << 24) | 0x00FFFFFF);
   }
}

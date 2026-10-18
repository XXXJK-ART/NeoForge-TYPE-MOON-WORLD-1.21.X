package net.xxxjk.TYPE_MOON_WORLD.client;

import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Legacy packet compatibility only. Duel impacts never draw a fullscreen flash,
 * including requests received from an older server or an addon.
 */
public final class DuelScreenFlashClient {
   private DuelScreenFlashClient() { }

   public static void apply(int ticks, float alpha) { }
   public static void render(RenderGuiEvent.Post event) { }
}
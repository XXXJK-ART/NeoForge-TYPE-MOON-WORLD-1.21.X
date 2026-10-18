package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import java.util.List;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXScreenEffectDefinition;

/**
 * Compatibility entry points for removed fullscreen VFX.
 *
 * Screen effects and scene-wide bloom must never allocate a PostChain or touch
 * the main framebuffer, even when an old config or addon requests them.
 * World-space emitters and their emissive materials render independently.
 */
public final class VFXPostProcessManager {
   private VFXPostProcessManager() { }

   public static void add(List<VFXScreenEffectDefinition> effects) { }
   public static void clear() { }
   public static void requestReset() { }
   public static void tick(ClientTickEvent.Post event) { }
   public static void render(RenderGuiEvent.Pre event) { }
}
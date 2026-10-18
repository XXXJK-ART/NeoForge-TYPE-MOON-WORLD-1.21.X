package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import java.util.UUID;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.VFXEnvironmentDefinition;

/**
 * Compatibility entry points for removed VFX environment treatments.
 * Old effect JSON may still describe tint, fog, desaturation or weather, but
 * combat visuals must not change the world's colors, fog distances or weather.
 * There are deliberately no event subscriptions or retained active effects.
 */
public final class VFXEnvironmentManager {
   private VFXEnvironmentManager() { }

   public static void add(VFXEnvironmentDefinition definition, double x, double y, double z) { }
   public static void add(VFXEnvironmentDefinition definition, double x, double y, double z, UUID targetUuid) { }
   public static void clear() { }
   public static void onClientTick(ClientTickEvent.Post event) { }
   public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) { }
   public static void onRenderFog(ViewportEvent.RenderFog event) { }
   public static float desaturationStrength() { return 0.0F; }
}
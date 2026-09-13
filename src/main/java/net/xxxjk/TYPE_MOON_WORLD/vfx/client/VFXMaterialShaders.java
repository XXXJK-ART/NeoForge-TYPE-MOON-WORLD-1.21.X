package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/** Whitelist of clean-room GLSL 150 VFX materials. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VFXMaterialShaders {
   private static final String[] IDS = {"vfx_energy", "vfx_polar", "vfx_sdf", "vfx_smoke"};
   private static final Map<String, ShaderInstance> SHADERS = new HashMap<>();

   private VFXMaterialShaders() { }

   @SubscribeEvent
   public static void register(RegisterShadersEvent event) throws IOException {
      for (String id : IDS) {
         ShaderInstance shader = new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, id), DefaultVertexFormat.POSITION_TEX_COLOR);
         event.registerShader(shader, loaded -> SHADERS.put(id, loaded));
      }
   }

   public static ShaderInstance get(String id) { return SHADERS.get(id); }

   public static boolean isRegistered(String id) {
      if (id == null || id.equals("vanilla")) return false;
      for (String allowed : IDS) if (allowed.equals(id)) return true;
      return false;
   }

   public static void updateGlobals(float time, float progress, float bloom) {
      for (ShaderInstance shader : SHADERS.values()) {
         set(shader, "Time", time);
         set(shader, "EffectProgress", progress);
         set(shader, "BloomStrength", bloom);
      }
   }

   private static void set(ShaderInstance shader, String name, float value) {
      if (shader != null && shader.getUniform(name) != null) shader.getUniform(name).set(value);
   }
}

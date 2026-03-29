package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.ShaderInstance;

public final class GanderOrbShaderRegistry {
   private static ShaderInstance shader;

   private GanderOrbShaderRegistry() {
   }

   public static void setShader(ShaderInstance loadedShader) {
      shader = loadedShader;
   }

   public static ShaderInstance getShader() {
      return shader;
   }

   public static boolean isReady() {
      return shader != null;
   }
}

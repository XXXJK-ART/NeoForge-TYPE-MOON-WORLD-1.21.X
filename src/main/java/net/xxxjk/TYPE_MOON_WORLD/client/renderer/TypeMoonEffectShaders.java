package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.ShaderInstance;

public final class TypeMoonEffectShaders {
   private static ShaderInstance gravityShell;
   private static ShaderInstance expandingRing;
   private static ShaderInstance ubwAnalysisRipple;

   private TypeMoonEffectShaders() {
   }

   public static void setGravityShell(ShaderInstance shader) {
      gravityShell = shader;
   }

   public static void setExpandingRing(ShaderInstance shader) {
      expandingRing = shader;
   }

   public static void setUbwAnalysisRipple(ShaderInstance shader) {
      ubwAnalysisRipple = shader;
   }

   public static ShaderInstance getGravityShell() {
      return gravityShell;
   }

   public static ShaderInstance getExpandingRing() {
      return expandingRing;
   }

   public static ShaderInstance getUbwAnalysisRipple() {
      return ubwAnalysisRipple;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.TexturingStateShard;
import net.minecraft.resources.ResourceLocation;

public class ClippedEntityRenderType extends RenderType {
   public enum ClipMode {
      MANIFEST,
      DISSOLVE
   }

   private ClippedEntityRenderType(
      String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   public static RenderType servant(ResourceLocation texture, float threshold, ClipMode mode, float minY, float maxY) {
      float clippedThreshold = Math.max(0.0F, Math.min(1.0F, threshold));
      TexturingStateShard clippingState = new TexturingStateShard(
         "servant_clipped_uniforms",
         () -> setupUniforms(clippedThreshold, mode, minY, maxY),
         () -> {
         }
      );
      return create(
         "typemoonworld_servant_clipped",
         DefaultVertexFormat.NEW_ENTITY,
         VertexFormat.Mode.QUADS,
         256,
         true,
         false,
         CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> TypeMoonEffectShaders.getClippedEntity() != null
               ? TypeMoonEffectShaders.getClippedEntity()
               : GameRenderer.getRendertypeEntityTranslucentShader()))
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderStateShard.NO_CULL)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setOverlayState(RenderStateShard.OVERLAY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setTexturingState(clippingState)
            .createCompositeState(false)
      );
   }

   private static void setupUniforms(float threshold, ClipMode mode, float minY, float maxY) {
      ShaderInstance shader = TypeMoonEffectShaders.getClippedEntity();
      if (shader == null) {
         return;
      }
      set(shader, "ClipThreshold", threshold);
      set(shader, "ClipMode", mode == ClipMode.DISSOLVE ? 1.0F : 0.0F);
      set(shader, "ClipMinY", minY);
      set(shader, "ClipMaxY", Math.max(minY + 0.01F, maxY));
      set(shader, "ClipSoftness", 0.035F);
      set(shader, "EdgeWidth", 0.055F);
      set(shader, "EdgeColor", 1.0F, 0.88F, 0.45F, mode == ClipMode.DISSOLVE ? 0.95F : 0.82F);
   }

   private static void set(ShaderInstance shader, String name, float value) {
      if (shader.getUniform(name) != null) {
         shader.getUniform(name).set(value);
      }
   }

   private static void set(ShaderInstance shader, String name, float x, float y, float z, float w) {
      if (shader.getUniform(name) != null) {
         shader.getUniform(name).set(x, y, z, w);
      }
   }
}

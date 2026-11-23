package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;

/** Untextured, emissive vertex gradients dedicated to Diarmuid's spear ribbons. */
public final class DiarmuidTrailRenderType extends RenderType {
   private static final RenderType TRAIL = create(
      "typemoonworld_diarmuid_gradient_trail", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
      8192, false, true, CompositeState.builder()
         .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));

   private DiarmuidTrailRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                                  boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
      super(name, format, mode, size, crumbling, sort, setup, clear);
   }

   public static RenderType trail() { return TRAIL; }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class GanderOrbRenderType extends RenderType {
   private static final RenderType GANDER_ORB = create(
      "typemoonworld_gander_orb",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      VertexFormat.Mode.QUADS,
      256,
      true,
      true,
      RenderType.CompositeState.builder()
         .setShaderState(new RenderStateShard.ShaderStateShard(GanderOrbShaderRegistry::getShader))
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
         .setCullState(RenderStateShard.NO_CULL)
         .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
         .createCompositeState(true)
   );

   private GanderOrbRenderType(
      String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   public static RenderType orb() {
      return GANDER_ORB;
   }
}

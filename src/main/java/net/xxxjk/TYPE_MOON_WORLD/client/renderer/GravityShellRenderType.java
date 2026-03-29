package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class GravityShellRenderType extends RenderType {
   private static final RenderType SHELL = create(
      "typemoonworld_gravity_shell",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      VertexFormat.Mode.QUADS,
      256,
      true,
      true,
      CompositeState.builder()
         .setShaderState(new RenderStateShard.ShaderStateShard(TypeMoonEffectShaders::getGravityShell))
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .setCullState(RenderStateShard.NO_CULL)
         .createCompositeState(true)
   );

   private GravityShellRenderType(
      String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   public static RenderType shell() {
      return SHELL;
   }
}

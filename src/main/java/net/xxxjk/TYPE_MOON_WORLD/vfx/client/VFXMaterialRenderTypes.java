package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Cached RenderTypes for whitelisted VFX material/texture/blend tuples. */
public final class VFXMaterialRenderTypes extends RenderType {
   private static final Map<Key, RenderType> CACHE = new HashMap<>();

   private VFXMaterialRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
      super(name, format, mode, size, crumbling, sort, setup, clear);
   }

   public static RenderType get(String shaderId, ResourceLocation texture, boolean additive) {
      return CACHE.computeIfAbsent(new Key(shaderId, texture, additive), VFXMaterialRenderTypes::createType);
   }

   private static RenderType createType(Key key) {
      return create(
         "typemoonworld_" + key.shaderId + "_" + (key.additive ? "add" : "alpha"),
         DefaultVertexFormat.POSITION_TEX_COLOR,
         VertexFormat.Mode.QUADS,
         1536,
         false,
         true,
         CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> {
               var shader = VFXMaterialShaders.get(key.shaderId);
               return shader != null ? shader : GameRenderer.getPositionTexColorShader();
            }))
            .setTextureState(new RenderStateShard.TextureStateShard(key.texture, false, false))
            .setTransparencyState(key.additive ? RenderStateShard.ADDITIVE_TRANSPARENCY : RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setCullState(RenderStateShard.NO_CULL)
            .createCompositeState(true)
      );
   }

   private record Key(String shaderId, ResourceLocation texture, boolean additive) { }
}

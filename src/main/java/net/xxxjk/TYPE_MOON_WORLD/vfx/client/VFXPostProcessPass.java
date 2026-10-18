package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/** Runs scene replacement passes without changing the world's depth buffer or later draws. */
final class VFXPostProcessPass {
   private VFXPostProcessPass() { }

   static void process(PostChain chain, float partialTick) {
      boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
      boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
      int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
      int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
      int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
      int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
      int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
      int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
      try {
         // EffectInstance's blend cache is shared across chains and does not
         // track RenderType changes between frames. Seed replacement blending.
         RenderSystem.disableBlend();
         RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ZERO);
         RenderSystem.disableDepthTest();
         // PostPass clears its output, including minecraft:main. Preserve depth
         // for subsequent screen effects that sample it and for later overlays.
         RenderSystem.depthMask(false);
         chain.process(partialTick);
      } finally {
         Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
         RenderSystem.activeTexture(activeTexture);
         RenderSystem.depthMask(depthMask);
         RenderSystem.depthFunc(depthFunc);
         if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
         RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
         if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
      }
   }
}

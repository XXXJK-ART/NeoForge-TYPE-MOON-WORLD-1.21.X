package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicProjectileEntity;

public class ElementalMagicProjectileRenderer extends EntityRenderer<ElementalMagicProjectileEntity> {
   public ElementalMagicProjectileRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(ElementalMagicProjectileEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float[] color = colorForElement(entity.getElement());
      MagicOrbProjectileRenderHelper.renderOrb(this, entity, partialTicks, poseStack, buffer, this.entityRenderDispatcher.cameraOrientation(), entity.getVisualScale(), color[0], color[1], color[2]);
      MagicOrbProjectileRenderHelper.renderTrail(this, entity, partialTicks, poseStack, buffer, this.entityRenderDispatcher.camera.getPosition(), entity.tracePos, color[0], color[1], color[2]);
      super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(ElementalMagicProjectileEntity entity) {
      return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
   }

   private static float[] colorForElement(int element) {
      return switch (element) {
         case ElementalMagicProjectileEntity.ELEMENT_WATER -> new float[]{0.16F, 0.66F, 1.0F};
         case ElementalMagicProjectileEntity.ELEMENT_WIND -> new float[]{0.58F, 1.0F, 0.78F};
         case ElementalMagicProjectileEntity.ELEMENT_EARTH -> new float[]{0.76F, 0.52F, 0.22F};
         default -> new float[]{1.0F, 0.28F, 0.08F};
      };
   }
}

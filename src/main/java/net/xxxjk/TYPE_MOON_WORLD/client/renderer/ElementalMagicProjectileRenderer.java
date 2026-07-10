package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicProjectileEntity;

public class ElementalMagicProjectileRenderer extends EntityRenderer<ElementalMagicProjectileEntity> {
   private final ItemRenderer itemRenderer;

   public ElementalMagicProjectileRenderer(Context context) {
      super(context);
      this.itemRenderer = context.getItemRenderer();
   }

   @Override
   public void render(ElementalMagicProjectileEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float scale = entity.getVisualScale();
      poseStack.scale(scale, scale, scale);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
      poseStack.popPose();
      super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(ElementalMagicProjectileEntity entity) {
      return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
   }
}

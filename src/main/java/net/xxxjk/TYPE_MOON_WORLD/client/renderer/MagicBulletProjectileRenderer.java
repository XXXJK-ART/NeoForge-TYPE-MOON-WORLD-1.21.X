package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity;

public class MagicBulletProjectileRenderer extends EntityRenderer<MagicBulletProjectileEntity> {
   public MagicBulletProjectileRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(MagicBulletProjectileEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float[] color = colorForElement(entity.getElement());
      MagicOrbProjectileRenderHelper.renderOrb(this, entity, partialTicks, poseStack, buffer, this.entityRenderDispatcher.cameraOrientation(), entity.getVisualScale(), color[0], color[1], color[2]);
      MagicOrbProjectileRenderHelper.renderTrail(this, entity, partialTicks, poseStack, buffer, this.entityRenderDispatcher.camera.getPosition(), entity.tracePos, color[0], color[1], color[2]);
      super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(MagicBulletProjectileEntity entity) {
      return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
   }

   private static float[] colorForElement(int element) {
      return switch (element) {
         case MagicBulletProjectileEntity.ELEMENT_FIRE -> new float[]{1.0F, 0.24F, 0.08F};
         case MagicBulletProjectileEntity.ELEMENT_WATER -> new float[]{0.18F, 0.62F, 1.0F};
         case MagicBulletProjectileEntity.ELEMENT_EARTH -> new float[]{0.72F, 0.48F, 0.18F};
         case MagicBulletProjectileEntity.ELEMENT_WIND -> new float[]{0.55F, 1.0F, 0.74F};
         default -> new float[]{0.18F, 0.46F, 1.0F};
      };
   }
}

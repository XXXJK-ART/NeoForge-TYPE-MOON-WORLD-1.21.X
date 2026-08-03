package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoyalCannonProjectileEntity;

public final class RoyalCannonProjectileRenderer extends EntityRenderer<RoyalCannonProjectileEntity> {
   private static final float GOLD_R = 1.0F;
   private static final float GOLD_G = 0.72F;
   private static final float GOLD_B = 0.12F;

   public RoyalCannonProjectileRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(RoyalCannonProjectileEntity entity, float entityYaw, float partialTick,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      MagicOrbProjectileRenderHelper.renderOrb(this, entity, partialTick, poseStack, buffer,
         this.entityRenderDispatcher.cameraOrientation(), 0.95F, GOLD_R, GOLD_G, GOLD_B);
      MagicOrbProjectileRenderHelper.renderTrail(this, entity, partialTick, poseStack, buffer,
         this.entityRenderDispatcher.camera.getPosition(), entity.tracePos, GOLD_R, GOLD_G, GOLD_B);
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(RoyalCannonProjectileEntity entity) {
      return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
   }
}

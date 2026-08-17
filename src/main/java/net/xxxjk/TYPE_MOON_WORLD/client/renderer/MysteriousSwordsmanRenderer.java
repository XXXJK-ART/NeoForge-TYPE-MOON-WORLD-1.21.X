package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;

public class MysteriousSwordsmanRenderer extends HumanoidMobRenderer<MysteriousSwordsmanEntity, PlayerModel<MysteriousSwordsmanEntity>> {
   private static final ResourceLocation SASAKI_KOJIRO_TEXTURE = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "textures/entity/sasaki_kojiro.png");
   private static final float SASAKI_KOJIRO_SCALE = 0.926F;

   public MysteriousSwordsmanRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.addLayer(new FaceShadowLayer(this));
   }

   @Override
   public void render(MysteriousSwordsmanEntity entity, float yaw, float partialTick, PoseStack poseStack,
                      MultiBufferSource buffers, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(SASAKI_KOJIRO_SCALE, SASAKI_KOJIRO_SCALE, SASAKI_KOJIRO_SCALE);
      super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
      poseStack.popPose();
   }

   @Override
   public ResourceLocation getTextureLocation(MysteriousSwordsmanEntity entity) {
      return SASAKI_KOJIRO_TEXTURE;
   }

   private static final class FaceShadowLayer extends RenderLayer<MysteriousSwordsmanEntity, PlayerModel<MysteriousSwordsmanEntity>> {
      private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
         TYPE_MOON_WORLD.MOD_ID, "textures/particle/particle_white.png");
      private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(WHITE_TEXTURE);

      private FaceShadowLayer(RenderLayerParent<MysteriousSwordsmanEntity, PlayerModel<MysteriousSwordsmanEntity>> renderer) {
         super(renderer);
      }

      @Override
      public void render(
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight,
         MysteriousSwordsmanEntity entity,
         float limbSwing,
         float limbSwingAmount,
         float partialTick,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         if (entity.isInvisible()) {
            return;
         }

         poseStack.pushPose();
         this.getParentModel().head.translateAndRotate(poseStack);
         VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
         float halfWidth = 0.255F;
         float top = -0.185F;
         float bottom = 0.155F;
         float z = -0.252F;
         int color = 0x8A000000;
         vertex(consumer, poseStack, -halfWidth, top, z, color, 0.0F, 0.0F);
         vertex(consumer, poseStack, -halfWidth, bottom, z, color, 0.0F, 1.0F);
         vertex(consumer, poseStack, halfWidth, bottom, z, color, 1.0F, 1.0F);
         vertex(consumer, poseStack, halfWidth, top, z, color, 1.0F, 0.0F);
         vertex(consumer, poseStack, halfWidth, top, z, color, 1.0F, 0.0F);
         vertex(consumer, poseStack, halfWidth, bottom, z, color, 1.0F, 1.0F);
         vertex(consumer, poseStack, -halfWidth, bottom, z, color, 0.0F, 1.0F);
         vertex(consumer, poseStack, -halfWidth, top, z, color, 0.0F, 0.0F);
         poseStack.popPose();
      }

      private static void vertex(VertexConsumer consumer, PoseStack poseStack, float x, float y, float z, int color, float u, float v) {
         consumer.addVertex(poseStack.last(), x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(poseStack.last(), 0.0F, 0.0F, -1.0F);
      }
   }
}

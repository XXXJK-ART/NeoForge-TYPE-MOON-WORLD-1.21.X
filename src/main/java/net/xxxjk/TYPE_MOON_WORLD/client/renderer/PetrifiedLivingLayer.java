package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;

public class PetrifiedLivingLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   private static final ResourceLocation STONE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/stone.png");

   public PetrifiedLivingLayer(RenderLayerParent<T, M> renderer) {
      super(renderer);
   }

   @Override
   public void render(
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      T livingEntity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (livingEntity.isInvisible() || !livingEntity.hasEffect(ModMobEffects.PETRIFIED)) {
         return;
      }

      poseStack.pushPose();
      poseStack.scale(1.01F, 1.01F, 1.01F);
      VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(STONE_TEXTURE));
      this.getParentModel().renderToBuffer(
         poseStack,
         buffer,
         packedLight,
         LivingEntityRenderer.getOverlayCoords(livingEntity, 0.0F),
         -1
      );
      poseStack.popPose();
   }
}

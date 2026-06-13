package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.util.Color;

public class BaseServantRenderer<T extends ServantEntity> extends GeoEntityRenderer<T> {
   public BaseServantRenderer(Context renderManager, GeoModel<T> model, float scale) {
      super(renderManager, model);
      this.withScale(scale);
      this.addRenderLayer(new PetrifiedGeoLayer<>(this));
      this.addRenderLayer(
         new BlockAndItemGeoLayer<T>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, T animatable) {
               return BaseServantRenderer.this.getStackForBone(bone, animatable);
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
               return BaseServantRenderer.this.getTransformTypeForStack(bone, stack, animatable);
            }

            @Override
            protected void renderStackForBone(
               PoseStack poseStack,
               GeoBone bone,
               ItemStack stack,
               T animatable,
               MultiBufferSource bufferSource,
               float partialTick,
               int packedLight,
               int packedOverlay
            ) {
               BaseServantRenderer.this.preRenderStackForBone(poseStack, bone, stack, animatable);
               super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            }
         }
      );
   }

   protected ItemStack getStackForBone(GeoBone bone, T animatable) {
      return switch (bone.getName()) {
         case "right arm" -> animatable.getMainHandItem();
         case "left arm" -> animatable.getOffhandItem();
         default -> ItemStack.EMPTY;
      };
   }

   protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
      return "left arm".equals(bone.getName()) ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
   }

   protected void preRenderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, T animatable) {
      var offset = animatable.getHandItemOffset();
      poseStack.translate(offset.x, offset.y, offset.z);
      poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(-13.0F));
   }

   @Override
   protected float getDeathMaxRotation(T animatable) {
      return animatable.isSpiritualDissolving() ? 0.0F : super.getDeathMaxRotation(animatable);
   }

   @Override
   public int getPackedOverlay(T animatable, float u, float partialTick) {
      return animatable.isSpiritualDissolving() ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(animatable, u, partialTick);
   }

   @Override
   public Color getRenderColor(T animatable, float partialTick, int packedLight) {
      if (!animatable.isSpiritualDissolving()) {
         return super.getRenderColor(animatable, partialTick, packedLight);
      }

      float progress = animatable.getSpiritualDissolveProgress(partialTick);
      float fade = progress < 0.55F ? 1.0F : Math.max(0.0F, 1.0F - (progress - 0.55F) / 0.45F);
      int alpha = Math.max(0, Math.min(255, Math.round(fade * 255.0F)));
      return Color.ofARGB(alpha, 255, 255, 255);
   }
}

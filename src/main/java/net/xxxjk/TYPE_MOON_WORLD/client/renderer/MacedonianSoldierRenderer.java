package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.model.MacedonianSoldierModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public final class MacedonianSoldierRenderer extends GeoEntityRenderer<MacedonianSoldierEntity> {
   public MacedonianSoldierRenderer(Context context) {
      super(context, new MacedonianSoldierModel());
      this.addRenderLayer(new PetrifiedGeoLayer<>(this));
      this.addRenderLayer(new BlockAndItemGeoLayer<MacedonianSoldierEntity>(this) {
         @Override
         protected ItemStack getStackForBone(GeoBone bone, MacedonianSoldierEntity animatable) {
            return switch (bone.getName()) {
               case "RightArm" -> animatable.getMainHandItem();
               case "LeftArm" -> animatable.getOffhandDisplayItem();
               default -> ItemStack.EMPTY;
            };
         }

         @Override
         protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, MacedonianSoldierEntity animatable) {
            return "LeftArm".equals(bone.getName()) ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
         }

         @Override
         protected void renderStackForBone(
            PoseStack poseStack,
            GeoBone bone,
            ItemStack stack,
            MacedonianSoldierEntity animatable,
            MultiBufferSource bufferSource,
            float partialTick,
            int packedLight,
            int packedOverlay
         ) {
            if ("LeftArm".equals(bone.getName())) {
               poseStack.translate(0.0, -0.42, 0.10);
               poseStack.mulPose(Axis.XP.rotationDegrees(-84.0F));
               poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
            } else {
               poseStack.translate(0.0, -0.78, 0.03);
               poseStack.mulPose(Axis.XP.rotationDegrees(-96.0F));
               poseStack.mulPose(Axis.ZP.rotationDegrees(-12.0F));
            }
            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
         }
      });
   }
}

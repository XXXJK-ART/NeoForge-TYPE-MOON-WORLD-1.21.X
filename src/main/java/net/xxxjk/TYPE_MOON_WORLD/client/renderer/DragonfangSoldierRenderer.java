package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.model.DragonfangSoldierModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class DragonfangSoldierRenderer extends GeoEntityRenderer<DragonfangSoldierEntity> {
   public DragonfangSoldierRenderer(Context context) {
      super(context, new DragonfangSoldierModel());
      this.addRenderLayer(new PetrifiedGeoLayer<>(this));
      this.addRenderLayer(new BlockAndItemGeoLayer<DragonfangSoldierEntity>(this) {
         @Override
         protected ItemStack getStackForBone(GeoBone bone, DragonfangSoldierEntity animatable) {
            return switch (bone.getName()) {
               case "RightArm" -> animatable.getMainHandItem();
               case "LeftArm" -> animatable.getOffhandDisplayItem();
               default -> ItemStack.EMPTY;
            };
         }

         @Override
         protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, DragonfangSoldierEntity animatable) {
            return "LeftArm".equals(bone.getName()) ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
         }

         @Override
         protected void renderStackForBone(
            PoseStack poseStack,
            GeoBone bone,
            ItemStack stack,
            DragonfangSoldierEntity animatable,
            MultiBufferSource bufferSource,
            float partialTick,
            int packedLight,
            int packedOverlay
         ) {
            poseStack.translate(0.0, -0.6, 0.02);
            poseStack.mulPose(Axis.XP.rotationDegrees(-95.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees("LeftArm".equals(bone.getName()) ? 18.0F : -18.0F));
            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
         }
      });
   }
}

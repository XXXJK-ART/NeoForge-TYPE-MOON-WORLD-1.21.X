package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class BaseServantRenderer<T extends ServantEntity> extends GeoEntityRenderer<T> {
   public BaseServantRenderer(Context renderManager, GeoModel<T> model, float scale) {
      super(renderManager, model);
      this.withScale(scale);
      this.addRenderLayer(
         new BlockAndItemGeoLayer<T>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, T animatable) {
               return "right arm".equals(bone.getName()) ? animatable.getMainHandItem() : ItemStack.EMPTY;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
               return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
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
               var offset = animatable.getHandItemOffset();
               poseStack.translate(offset.x, offset.y, offset.z);
               poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
               poseStack.mulPose(Axis.ZP.rotationDegrees(-13.0F));
               super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            }
         }
      );
   }
}

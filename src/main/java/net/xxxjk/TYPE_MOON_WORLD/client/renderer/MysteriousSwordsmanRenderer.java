package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.model.MysteriousSwordsmanModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class MysteriousSwordsmanRenderer extends GeoEntityRenderer<MysteriousSwordsmanEntity> {
   private static final float MODEL_SCALE = 0.95F * 0.7F;

   public MysteriousSwordsmanRenderer(EntityRendererProvider.Context context) {
      super(context, new MysteriousSwordsmanModel());
      this.withScale(MODEL_SCALE);
      this.addRenderLayer(new BlockAndItemGeoLayer<MysteriousSwordsmanEntity>(this) {
         @Override protected ItemStack getStackForBone(GeoBone bone, MysteriousSwordsmanEntity animatable) {
            return "right arm".equals(bone.getName()) ? animatable.getMainHandItem() : ItemStack.EMPTY;
         }

         @Override protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, MysteriousSwordsmanEntity animatable) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
         }

         @Override protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
            MysteriousSwordsmanEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
            poseStack.translate(0.1, -0.8, -0.2);
            poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-13.0F));
            super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
         }
      });
   }
}

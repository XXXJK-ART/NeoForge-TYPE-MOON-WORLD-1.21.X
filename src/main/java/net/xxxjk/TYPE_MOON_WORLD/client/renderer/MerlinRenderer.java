package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.model.MerlinModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class MerlinRenderer extends GeoEntityRenderer<MerlinEntity> {
    public MerlinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MerlinModel());

        this.addRenderLayer(new BlockAndItemGeoLayer<MerlinEntity>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, MerlinEntity animatable) {
                if ("right_arm".equals(bone.getName())) {
                    return animatable.getMainHandItem();
                }
                return ItemStack.EMPTY;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, MerlinEntity animatable) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, MerlinEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                poseStack.translate(0.15F, -0.8F, 0.15F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-13.0F));
                poseStack.scale(1.2F, 1.2F, 1.2F);
                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }
}

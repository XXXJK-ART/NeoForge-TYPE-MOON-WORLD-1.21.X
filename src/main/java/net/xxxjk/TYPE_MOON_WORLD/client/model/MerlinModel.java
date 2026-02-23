package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class MerlinModel extends GeoModel<MerlinEntity> {
    @Override
    public ResourceLocation getModelResource(MerlinEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/merlin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MerlinEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/merlin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MerlinEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/merlin.animation.json");
    }

    @Override
    public void setCustomAnimations(MerlinEntity animatable, long instanceId, AnimationState<MerlinEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");
        GeoBone hair = getAnimationProcessor().getBone("bone13");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            float yawDeg = entityData.netHeadYaw();
            float clampedYawDeg = Mth.clamp(yawDeg, -40.0F, 40.0F);
            head.setRotY(clampedYawDeg * Mth.DEG_TO_RAD);
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            if (hair != null) {
                float pitchRad = entityData.headPitch() * Mth.DEG_TO_RAD;
                hair.setRotX(-pitchRad * 0.9F);
            }
        }

        float limbSwing = animationState.getLimbSwing();
        float limbSwingAmount = animationState.getLimbSwingAmount();

        GeoBone rightLeg = getAnimationProcessor().getBone("right_leg");
        GeoBone leftLeg = getAnimationProcessor().getBone("left_leg");
        if (rightLeg != null && leftLeg != null) {
            rightLeg.setRotX(Mth.cos(limbSwing * 0.6662F) * 0.7F * limbSwingAmount);
            leftLeg.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.7F * limbSwingAmount);
        }

        GeoBone rightArm = getAnimationProcessor().getBone("right_arm");
        GeoBone leftArm = getAnimationProcessor().getBone("left_arm");
        if (rightArm != null && leftArm != null) {
            rightArm.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.5F * limbSwingAmount);
            leftArm.setRotX(Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount);

            if (animatable.getHealth() <= 900.0F) {
                rightArm.setRotX(0.3F);
                rightArm.setRotY(0.2F);
            }
        }
    }
}

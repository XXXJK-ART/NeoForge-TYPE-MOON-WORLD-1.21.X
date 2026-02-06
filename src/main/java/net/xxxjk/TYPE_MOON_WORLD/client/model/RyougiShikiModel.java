package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class RyougiShikiModel extends GeoModel<RyougiShikiEntity> {
    @Override
    public ResourceLocation getModelResource(RyougiShikiEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/ryougi_shiki.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RyougiShikiEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/ryougi_shiki.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RyougiShikiEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/ryougi_shiki.animation.json");
    }

    @Override
    public void setCustomAnimations(RyougiShikiEntity animatable, long instanceId, AnimationState<RyougiShikiEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
        }
        
        // Handle Leg Animations manually (Walking)
        GeoBone rightLeg = getAnimationProcessor().getBone("right leg");
        GeoBone leftLeg = getAnimationProcessor().getBone("left leg");
        if (rightLeg != null && leftLeg != null) {
             float limbSwing = animationState.getLimbSwing();
             float limbSwingAmount = animationState.getLimbSwingAmount();
             rightLeg.setRotX(Mth.cos(limbSwing * 0.6662F) * 0.7F * limbSwingAmount);
             leftLeg.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.7F * limbSwingAmount);
        }

        GeoBone rightArm = getAnimationProcessor().getBone("right arm");
        GeoBone leftArm = getAnimationProcessor().getBone("left arm");
        
        if (rightArm != null && leftArm != null) {
             float limbSwing = animationState.getLimbSwing();
             float limbSwingAmount = animationState.getLimbSwingAmount();
             
             rightArm.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F);
             
             if (animatable.hasLeftArm()) {
                 leftArm.setRotX(Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F);
             } else {
                 leftArm.setRotX(0.0F);
             }
             
             if (animatable.isDefending()) {
                rightArm.setRotX(0.9F);
                rightArm.setRotY(0.4F);
             }
        }
        
        // Play State Animation (1, 2, 3, 4)
        // This sets the base pose/state for the model
        String animationName = "1";
        if (animatable.hasLeftArm() && animatable.hasSword()) {
            animationName = "1";
        } else if (animatable.hasLeftArm() && !animatable.hasSword()) {
            animationName = "2";
        } else if (!animatable.hasLeftArm() && animatable.hasSword()) {
            animationName = "3";
        } else if (!animatable.hasLeftArm() && !animatable.hasSword()) {
            animationName = "4";
        }
        
        // We use setAnimation only if it's not already playing to avoid resetting it every tick
        // However, since these are "states" (loop: hold_on_last_frame), we might want to just ensure the correct one is active.
        // GeckoLib 4.x usually handles this via controllers in the Entity class, but here we are in setCustomAnimations which is for manual bone manipulation.
        // To use the "animations" defined in the json file, we should register a controller in the Entity class.
        
        // BUT, since the user said "1 2 3 4 are animation names", we should probably use them in the Entity's registerControllers.
        // Let's modify the Entity class to use these animations.
    }
}

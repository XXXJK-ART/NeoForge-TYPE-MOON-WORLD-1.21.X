package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class StoneManModel extends GeoModel<StoneManEntity> {
    private static final float BASE_HEAD_PITCH_DEG = -2.0F;

    @Override
    public ResourceLocation getModelResource(StoneManEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/stone_man.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StoneManEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/stone_man.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StoneManEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/stone_man.animation.json");
    }

    @Override
    public void setCustomAnimations(StoneManEntity animatable, long instanceId, AnimationState<StoneManEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");
        if (head == null || animatable.getAttackAnimTicks() > 0) {
            return;
        }

        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (entityData == null) {
            return;
        }

        float yawDeg = Mth.clamp(entityData.netHeadYaw(), -45.0F, 45.0F);
        float pitchDeg = Mth.clamp(entityData.headPitch(), -25.0F, 25.0F);
        head.setRotY(yawDeg * Mth.DEG_TO_RAD);
        head.setRotX((BASE_HEAD_PITCH_DEG + pitchDeg) * Mth.DEG_TO_RAD);
    }
}

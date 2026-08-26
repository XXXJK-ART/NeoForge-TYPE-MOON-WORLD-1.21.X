package io.github.typemoonaddon.client.model;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.entity.ShadowPiercingRhoAiasEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public final class ShadowPiercingRhoAiasModel extends GeoModel<ShadowPiercingRhoAiasEntity> {
    private static final ResourceLocation MODEL = TypeMoonAddon.id("geo/shadow_piercing_rho_aias.geo.json");
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/entity/shadow_piercing_rho_aias.png");
    private static final ResourceLocation ANIMATION = TypeMoonAddon.id(
        "animations/shadow_piercing_rho_aias.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(ShadowPiercingRhoAiasEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ShadowPiercingRhoAiasEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ShadowPiercingRhoAiasEntity entity) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(
        ShadowPiercingRhoAiasEntity entity,
        long instanceId,
        AnimationState<ShadowPiercingRhoAiasEntity> animationState
    ) {
        super.setCustomAnimations(entity, instanceId, animationState);
        float facingRotation = -entity.getFacingYaw() * Mth.DEG_TO_RAD;
        rotateRoot("Rhoaias", facingRotation);
        rotateRoot("Rhoaias2", facingRotation);
    }

    private void rotateRoot(String boneName, float facingRotation) {
        GeoBone root = getAnimationProcessor().getBone(boneName);
        if (root != null) {
            root.setRotY(facingRotation);
        }
    }
}

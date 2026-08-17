package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.SakuraBlackMudService;
import com.example.typemoonaddon.registry.AddonMobEffects;
import com.example.typemoonaddon.magic.SakuraPollutionService;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class BlackMudCorruptionLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/misc/black_mud_corruption.png");

    public BlackMudCorruptionLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        T entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        MobEffectInstance effect = entity.getEffect(AddonMobEffects.BLACK_MUD_CORRUPTION);
        float pollutionProgress = SakuraPollutionService.progress(entity);
        if ((effect == null || SakuraBlackMudService.isImmune(entity)) && pollutionProgress <= 0.0F) {
            return;
        }
        float mudProgress = effect == null || SakuraBlackMudService.isImmune(entity)
            ? 0.0F
            : (effect.getAmplifier() + 1.0F) / SakuraBlackMudService.CORRUPTION_STAGES;
        float progress = Mth.clamp(Math.max(mudProgress, pollutionProgress), 0.0F, 1.0F);
        int alpha = Mth.clamp((int)(progress * 255.0F), 1, 255);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.getParentModel().renderToBuffer(
            poseStack,
            consumer,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            alpha << 24 | 0x00FFFFFF
        );
    }
}

package io.github.typemoonaddon.client.renderer;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.magic.BlackMudService;
import io.github.typemoonaddon.registry.ModEffects;
import io.github.typemoonaddon.magic.PollutionService;
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
        MobEffectInstance effect = entity.getEffect(ModEffects.BLACK_MUD_CORRUPTION);
        float pollutionProgress = PollutionService.progress(entity);
        if ((effect == null || BlackMudService.isImmune(entity)) && pollutionProgress <= 0.0F) {
            return;
        }
        float mudProgress = effect == null || BlackMudService.isImmune(entity)
            ? 0.0F
            : (effect.getAmplifier() + 1.0F) / BlackMudService.CORRUPTION_STAGES;
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

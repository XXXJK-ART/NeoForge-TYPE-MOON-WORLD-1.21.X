package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.TypeMoonAddon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.example.typemoonaddon.entity.BoundaryMarkEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ProjectileVisualEffectHelper;

/** Small emissive rune marker; the actual boundary is server-side and remains unframed. */
public final class BoundaryMarkRenderer extends EntityRenderer<BoundaryMarkEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public BoundaryMarkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BoundaryMarkEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float[] color = color(entity.getBoundaryType());
        float ratio = (float) Math.max(0.25D, entity.getRemainingRatio());
        float viewerAlpha = viewerAlpha(entity);
        if (viewerAlpha <= 0.02F) {
            return;
        }
        if (entity.isBound()) {
            color = new float[]{color[0] * 0.78F, color[1] * 0.78F, color[2] * 0.78F};
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        poseStack.pushPose();
        float size = (0.22F + entity.getPower() * 0.05F) * ratio;
        ProjectileVisualEffectHelper.drawCenteredQuad(poseStack.last(), consumer, size, size,
                color[0], color[1], color[2], (entity.isActive() ? 0.82F : 0.2F) * ratio * viewerAlpha);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static float viewerAlpha(BoundaryMarkEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0.0F;
        }
        if (minecraft.player.getUUID().equals(entity.getAuthorId())) {
            return 1.0F;
        }
        if (!entity.isBound()) {
            return 1.0F;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double boundaryProficiency = vars.magic_proficiencies.getOrDefault(TypeMoonAddon.id(entity.getBoundaryType()).toString(), 0.0D);
        double analysisProficiency = vars.proficiency_magic_analysis;
        double score = boundaryProficiency * 0.75D + analysisProficiency * 0.25D;
        double distance = minecraft.player.distanceTo(entity);
        double limit = Math.max(8.0D, 18.0D + score * 0.45D - entity.getComplexity() * 0.02D);
        if (distance > limit) {
            return 0.0F;
        }
        return Mth.clamp((float) (0.15D + score / 125.0D), 0.12F, 1.0F);
    }

    private static float[] color(String type) {
        return switch (type) {
            case "warning_boundary" -> new float[]{1.0F, 0.55F, 0.1F};
            case "defense_boundary" -> new float[]{0.25F, 0.7F, 1.0F};
            case "suggestion_boundary" -> new float[]{0.75F, 0.35F, 1.0F};
            case "anti_magic_boundary" -> new float[]{0.1F, 0.2F, 0.9F};
            case "guard_boundary" -> new float[]{1.0F, 0.2F, 0.2F};
            case "interference_boundary" -> new float[]{0.25F, 1.0F, 0.35F};
            default -> new float[]{0.9F, 0.9F, 1.0F};
        };
    }

    @Override
    public ResourceLocation getTextureLocation(BoundaryMarkEntity entity) {
        return TEXTURE;
    }
}

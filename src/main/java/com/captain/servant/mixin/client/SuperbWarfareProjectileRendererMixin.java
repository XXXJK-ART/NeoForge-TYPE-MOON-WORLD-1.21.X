package com.captain.servant.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer", remap = false)
public abstract class SuperbWarfareProjectileRendererMixin {
    private static final String MARKER = "FateSupportKnightProjectile";
    private static final ResourceLocation KNIGHT_GLINT = ResourceLocation.fromNamespaceAndPath(
            "fatesupport",
            "textures/misc/knight_of_owner_glint.png"
    );
    private static final ThreadLocal<VertexConsumer> FATESUPPORT_GLINT = new ThreadLocal<>();

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            require = 0
    )
    private void fatesupport$beginOwnerizedRender(
            Entity entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        FATESUPPORT_GLINT.remove();
        if (entity.getCustomName() == null || !MARKER.equals(entity.getCustomName().getString())) {
            return;
        }
        float time = (System.currentTimeMillis() % 30_000L) / 30_000.0F;
        FATESUPPORT_GLINT.set(buffers.getBuffer(RenderType.energySwirl(KNIGHT_GLINT, time, time * 0.6F)));
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "STORE"),
            ordinal = 0,
            require = 0
    )
    private VertexConsumer fatesupport$addOwnerizedGlint(VertexConsumer original) {
        VertexConsumer glint = FATESUPPORT_GLINT.get();
        return glint == null ? original : VertexMultiConsumer.create(glint, original);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"),
            require = 0
    )
    private void fatesupport$endOwnerizedRender(
            Entity entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        FATESUPPORT_GLINT.remove();
    }
}

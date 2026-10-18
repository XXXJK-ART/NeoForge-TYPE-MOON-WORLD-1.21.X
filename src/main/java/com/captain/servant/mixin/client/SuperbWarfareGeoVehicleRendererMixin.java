package com.captain.servant.mixin.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer", remap = false)
public abstract class SuperbWarfareGeoVehicleRendererMixin {
    private static final String VISUAL_MARKER = "FateSupportKnightVisual";
    private static final String OWNER_MARKER = "FateSupportKnightOfOwner";
    private static final ResourceLocation KNIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "fatesupport",
            "textures/misc/knight_of_owner_glint.png"
    );
    private static final ThreadLocal<Boolean> FATESUPPORT_OWNERIZED_RENDER =
            ThreadLocal.withInitial(() -> false);

    private static final String RENDER_METHOD =
            "render(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;FF"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER_METHOD, at = @At("HEAD"), require = 0)
    private void fatesupport$beginOwnerizedVehicleRender(
            @Coerce Object vehicle,
            float yaw,
            float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        FATESUPPORT_OWNERIZED_RENDER.set(fatesupport$isOwnerized(vehicle));
    }

    @ModifyArg(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucent("
                            + "Lnet/minecraft/resources/ResourceLocation;)"
                            + "Lnet/minecraft/client/renderer/RenderType;",
                    ordinal = 0
            ),
            index = 0,
            require = 0
    )
    private ResourceLocation fatesupport$replaceOwnerizedCubeTexture(ResourceLocation original) {
        return FATESUPPORT_OWNERIZED_RENDER.get() ? KNIGHT_TEXTURE : original;
    }

    @ModifyArg(
            method = RENDER_METHOD,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/mcmodderanchor/simplebedrockmodel/v1/client/renderer/"
                            + "BedrockModelRenderTypes;polyMeshCutout("
                            + "Lnet/minecraft/resources/ResourceLocation;)"
                            + "Lnet/minecraft/client/renderer/RenderType;",
                    ordinal = 0
            ),
            index = 0,
            require = 0
    )
    private ResourceLocation fatesupport$replaceOwnerizedPolyMeshTexture(ResourceLocation original) {
        return FATESUPPORT_OWNERIZED_RENDER.get() ? KNIGHT_TEXTURE : original;
    }

    @Inject(
            method = "renderEmissive(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;"
                    + "Lcom/atsuishio/superbwarfare/client/model/entity/VehicleModelInstance;FF"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void fatesupport$disableOriginalEmissiveTexture(
            @Coerce Object vehicle,
            @Coerce Object model,
            float yaw,
            float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        if (fatesupport$isOwnerized(vehicle)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = RENDER_METHOD, at = @At("RETURN"), require = 0)
    private void fatesupport$endOwnerizedVehicleRender(
            @Coerce Object vehicle,
            float yaw,
            float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        FATESUPPORT_OWNERIZED_RENDER.remove();
    }

    private static boolean fatesupport$isOwnerized(Object vehicle) {
        if (!(vehicle instanceof Entity entity)) {
            return false;
        }

        try {
            Method getOverride = vehicle.getClass().getMethod("getOverride");
            Object value = getOverride.invoke(vehicle);
            if (value instanceof String override
                    && (override.contains(VISUAL_MARKER) || override.contains(OWNER_MARKER))) {
                return true;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // Older vehicle implementations fall back to persistent data.
        }

        return entity.getPersistentData().getBoolean("FateSupportKnightVehicle");
    }
}

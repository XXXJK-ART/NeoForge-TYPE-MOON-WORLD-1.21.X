package com.captain.servant.mixin.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.client.renderer.entity.VehicleRenderer", remap = false)
public abstract class SuperbWarfareVehicleRendererMixin {
    private static final String MARKER = "\"FateSupportKnightVisual\":true";
    private static final ResourceLocation KNIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "fatesupport",
            "textures/misc/knight_of_owner_glint.png"
    );

    private static final String VEHICLE_RENDER_RECURSIVELY =
            "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;"
                    + "Lsoftware/bernie/geckolib/cache/object/GeoBone;"
                    + "Lnet/minecraft/client/renderer/RenderType;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V";

    private static final String GECKOLIB_RENDER_RECURSIVELY =
            "Lsoftware/bernie/geckolib/renderer/GeoEntityRenderer;"
                    + "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/world/entity/Entity;"
                    + "Lsoftware/bernie/geckolib/cache/object/GeoBone;"
                    + "Lnet/minecraft/client/renderer/RenderType;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V";

    @Inject(
            method = "getRenderType(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/MultiBufferSource;F)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void fatesupport$replaceOwnerizedRenderType(
            @Coerce Object vehicle,
            ResourceLocation texture,
            MultiBufferSource buffers,
            float partialTick,
            CallbackInfoReturnable<RenderType> callbackInfo
    ) {
        if (fatesupport$isOwnerized(vehicle)) {
            callbackInfo.setReturnValue(RenderType.entityTranslucent(KNIGHT_TEXTURE));
        }
    }

    @Inject(
            method = "getTextureLocation(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void fatesupport$replaceOwnerizedTexture(
            @Coerce Object vehicle,
            CallbackInfoReturnable<ResourceLocation> callbackInfo
    ) {
        if (fatesupport$isOwnerized(vehicle)) {
            callbackInfo.setReturnValue(KNIGHT_TEXTURE);
        }
    }

    @Inject(
            method = "getTextureLocation(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void fatesupport$replaceOwnerizedBridgeTexture(
            @Coerce Object vehicle,
            CallbackInfoReturnable<ResourceLocation> callbackInfo
    ) {
        if (fatesupport$isOwnerized(vehicle)) {
            callbackInfo.setReturnValue(KNIGHT_TEXTURE);
        }
    }

    /**
     * The current SuperbWarfare renderer calls GeckoLib directly from its own
     * renderRecursively override. Modifying that call guarantees that the
     * ownerization texture reaches the final model draw even when the vehicle
     * renderer replaces the texture returned by getTextureLocation.
     */
    @ModifyArgs(
            method = VEHICLE_RENDER_RECURSIVELY,
            at = @At(
                    value = "INVOKE",
                    target = GECKOLIB_RENDER_RECURSIVELY
            ),
            require = 0
    )
    private void fatesupport$overlayOwnerizedTexture(Args args) {
        Object animatable = args.get(1);
        if (!(animatable instanceof Entity entity) || !fatesupport$isOwnerized(entity)) {
            return;
        }

        MultiBufferSource buffers = args.get(4);
        VertexConsumer original = args.get(5);
        if (buffers == null || original == null) {
            return;
        }

        VertexConsumer ownerized = buffers.getBuffer(RenderType.entityTranslucent(KNIGHT_TEXTURE));
        args.set(5, VertexMultiConsumer.create(ownerized, original));
    }

    private static boolean fatesupport$isOwnerized(Object vehicle) {
        try {
            Method getOverride = vehicle.getClass().getMethod("getOverride");
            Object value = getOverride.invoke(vehicle);
            if (value instanceof String override
                    && (override.contains(MARKER)
                    || override.contains("FateSupportKnightVisual")
                    || override.contains("FateSupportKnightOfOwner"))) {
                return true;
            }

            Method getSkinId = vehicle.getClass().getMethod("getSkinId");
            Object skinId = getSkinId.invoke(vehicle);
            if (skinId instanceof String skin
                    && (skin.contains("FateSupportKnightVisual")
                    || skin.contains("FateSupportKnightOfOwner"))) {
                return true;
            }

            return vehicle instanceof net.minecraft.world.entity.Entity entity
                    && entity.getPersistentData().getBoolean("FateSupportKnightVehicle");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return vehicle instanceof net.minecraft.world.entity.Entity entity
                    && entity.getPersistentData().getBoolean("FateSupportKnightVehicle");
        }
    }
}

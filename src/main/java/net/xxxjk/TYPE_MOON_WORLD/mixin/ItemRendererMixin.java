package net.xxxjk.TYPE_MOON_WORLD.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemRenderer.class, priority = 1001)
public abstract class ItemRendererMixin {
    private static final ThreadLocal<ItemStack> TARGET_STACK = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> TARGET_IS_GUI_3D = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "render", at = @At("HEAD"))
    private void captureTargetStack(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, int packedOverlay, BakedModel model, CallbackInfo ci) {
        TARGET_STACK.set(stack);
        TARGET_IS_GUI_3D.set(model.isGui3d());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void clearTargetStack(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
                                  MultiBufferSource buffer, int packedLight, int packedOverlay, BakedModel model, CallbackInfo ci) {
        TARGET_STACK.remove();
        TARGET_IS_GUI_3D.remove();
    }

    @ModifyExpressionValue(method = "getFoilBuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;glintTranslucent()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType replaceGlintTranslucent(RenderType prev) {
        if (!shouldUseMagicGlint()) {
            return prev;
        }
        if (isBlockItemTarget()) {
            return ReinforcementRenderType.glintTranslucentBlock();
        }
        return isTarget3D() ? ReinforcementRenderType.glintTranslucent3d() : ReinforcementRenderType.glintTranslucent();
    }

    @ModifyExpressionValue(method = "getFoilBuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;glint()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType replaceGlint(RenderType prev) {
        if (!shouldUseMagicGlint()) {
            return prev;
        }
        if (isBlockItemTarget()) {
            return ReinforcementRenderType.glintBlock();
        }
        return isTarget3D() ? ReinforcementRenderType.glint3d() : ReinforcementRenderType.glint();
    }

    @ModifyExpressionValue(method = "getFoilBuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entityGlint()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType replaceEntityGlint(RenderType prev) {
        if (!shouldUseMagicGlint()) {
            return prev;
        }
        if (isBlockItemTarget()) {
            return ReinforcementRenderType.entityGlintBlock();
        }
        return isTarget3D() ? ReinforcementRenderType.entityGlint3d() : ReinforcementRenderType.entityGlint();
    }

    @ModifyExpressionValue(method = "getFoilBufferDirect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;glint()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType replaceGlintDirect(RenderType prev) {
        if (!shouldUseMagicGlint()) {
            return prev;
        }
        if (isBlockItemTarget()) {
            return ReinforcementRenderType.glintDirectBlock();
        }
        return isTarget3D() ? ReinforcementRenderType.glintDirect3d() : ReinforcementRenderType.glintDirect();
    }

    @ModifyExpressionValue(method = "getFoilBufferDirect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entityGlintDirect()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType replaceEntityGlintDirect(RenderType prev) {
        if (!shouldUseMagicGlint()) {
            return prev;
        }
        if (isBlockItemTarget()) {
            return ReinforcementRenderType.entityGlintDirectBlock();
        }
        return isTarget3D() ? ReinforcementRenderType.entityGlintDirect3d() : ReinforcementRenderType.entityGlintDirect();
    }

    private static boolean shouldUseMagicGlint() {
        return hasMagicTextureTag(TARGET_STACK.get());
    }

    private static boolean isTarget3D() {
        return Boolean.TRUE.equals(TARGET_IS_GUI_3D.get());
    }

    private static boolean isBlockItemTarget() {
        ItemStack stack = TARGET_STACK.get();
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }

    private static boolean hasMagicTextureTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        var tag = customData.copyTag();
        if (tag.contains("Reinforced") && tag.getBoolean("Reinforced")) {
            return true;
        }
        if (tag.contains("ReinforcedLevel")
                || tag.contains("ReinforcedEnchantment")
                || tag.contains("ReinforcementTemporary")) {
            return true;
        }
        return tag.contains("is_projected") || tag.contains("is_infinite_projection");
    }
}

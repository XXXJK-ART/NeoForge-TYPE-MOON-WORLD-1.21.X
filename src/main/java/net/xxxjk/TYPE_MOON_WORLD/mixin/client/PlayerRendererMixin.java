package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType.ReinforcementPart;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    private static final int EMISSIVE_LIGHT = 15728880;
    private static final int HAND_GLOW_COLOR = 0xEEFFFFFF;

    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void renderRightHandOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (!shouldRenderArm(player, HumanoidArm.RIGHT)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        VertexConsumer vc = buffer.getBuffer(ReinforcementRenderType.getSkinRenderType(ReinforcementPart.ARM, player));
        model.rightArm.render(poseStack, vc, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, HAND_GLOW_COLOR);
        model.rightSleeve.render(poseStack, vc, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, HAND_GLOW_COLOR);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void renderLeftHandOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (!shouldRenderArm(player, HumanoidArm.LEFT)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        VertexConsumer vc = buffer.getBuffer(ReinforcementRenderType.getSkinRenderType(ReinforcementPart.ARM, player));
        model.leftArm.render(poseStack, vc, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, HAND_GLOW_COLOR);
        model.leftSleeve.render(poseStack, vc, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, HAND_GLOW_COLOR);
    }

    private static boolean shouldRenderArm(AbstractClientPlayer player, HumanoidArm armSide) {
        boolean hasStrength = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH)
                || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
        return hasStrength || isArmHoldingMagicTextureItem(player, armSide);
    }

    private static boolean isArmHoldingMagicTextureItem(AbstractClientPlayer player, HumanoidArm armSide) {
        ItemStack stack = getArmStack(player, armSide);
        if (stack.isEmpty()) {
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

    private static ItemStack getArmStack(AbstractClientPlayer player, HumanoidArm armSide) {
        HumanoidArm mainArm = player.getMainArm();
        if (armSide == mainArm) {
            return player.getItemInHand(InteractionHand.MAIN_HAND);
        }
        return player.getItemInHand(InteractionHand.OFF_HAND);
    }
}

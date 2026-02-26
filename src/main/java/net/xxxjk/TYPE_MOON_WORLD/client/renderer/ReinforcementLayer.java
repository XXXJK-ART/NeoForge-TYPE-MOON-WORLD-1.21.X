package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType.ReinforcementPart;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;

public class ReinforcementLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final int EMISSIVE_LIGHT = 15728880;
    private static final int SEMI_TRANSPARENT_WHITE = 0xEEFFFFFF;

    public ReinforcementLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }

        boolean hasStrength = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
        boolean hasDefense = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
        boolean hasAgility = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
        boolean hasSight = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
        boolean rightArmByItem = isArmHoldingMagicTextureItem(player, HumanoidArm.RIGHT);
        boolean leftArmByItem = isArmHoldingMagicTextureItem(player, HumanoidArm.LEFT);

        if (!hasStrength && !hasDefense && !hasAgility && !hasSight && !rightArmByItem && !leftArmByItem) {
            return;
        }

        poseStack.pushPose();
        // 轻微外扩，避免与原模型表面重叠导致覆盖/闪烁
        poseStack.scale(1.013f, 1.013f, 1.013f);

        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        ModelVisibilitySnapshot snapshot = ModelVisibilitySnapshot.capture(model);

        if (hasSight) {
            renderHeadPart(model, poseStack, buffer, player);
        }
        if (hasDefense) {
            renderBodyPart(model, poseStack, buffer, player);
        }
        if (hasStrength) {
            renderArmPart(model, poseStack, buffer, player);
        } else {
            if (rightArmByItem) {
                renderRightArmPart(model, poseStack, buffer, player);
            }
            if (leftArmByItem) {
                renderLeftArmPart(model, poseStack, buffer, player);
            }
        }
        if (hasAgility) {
            renderLegPart(model, poseStack, buffer, player);
        }

        snapshot.restore(model);
        poseStack.popPose();
    }

    private static void renderHeadPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
        setAllHidden(model);
        model.head.visible = true;
        model.hat.visible = true;
        renderCurrentVisible(model, poseStack, buffer, ReinforcementPart.HEAD, player);
    }

    private static void renderBodyPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
        setAllHidden(model);
        model.body.visible = true;
        model.jacket.visible = true;
        renderCurrentVisible(model, poseStack, buffer, ReinforcementPart.BODY, player);
    }

    private static void renderArmPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
        setAllHidden(model);
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightSleeve.visible = true;
        model.leftSleeve.visible = true;
        renderCurrentVisible(model, poseStack, buffer, ReinforcementPart.ARM, player);
    }

    private static void renderRightArmPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
        setAllHidden(model);
        model.rightArm.visible = true;
        model.rightSleeve.visible = true;
        renderCurrentVisible(model, poseStack, buffer, ReinforcementPart.ARM, player);
    }

    private static void renderLeftArmPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
        setAllHidden(model);
        model.leftArm.visible = true;
        model.leftSleeve.visible = true;
        renderCurrentVisible(model, poseStack, buffer, ReinforcementPart.ARM, player);
    }

    private static void renderLegPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
        setAllHidden(model);
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        model.rightPants.visible = true;
        model.leftPants.visible = true;
        renderCurrentVisible(model, poseStack, buffer, ReinforcementPart.LEG, player);
    }

    private static void renderCurrentVisible(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, ReinforcementPart part, AbstractClientPlayer player) {
        VertexConsumer vertexConsumer = buffer.getBuffer(ReinforcementRenderType.getSkinRenderType(part, player));
        model.renderToBuffer(poseStack, vertexConsumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, SEMI_TRANSPARENT_WHITE);
    }

    private static void setAllHidden(PlayerModel<AbstractClientPlayer> model) {
        model.head.visible = false;
        model.hat.visible = false;
        model.body.visible = false;
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightLeg.visible = false;
        model.leftLeg.visible = false;
        model.jacket.visible = false;
        model.rightSleeve.visible = false;
        model.leftSleeve.visible = false;
        model.rightPants.visible = false;
        model.leftPants.visible = false;
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

    private record ModelVisibilitySnapshot(
            boolean head,
            boolean hat,
            boolean body,
            boolean rightArm,
            boolean leftArm,
            boolean rightLeg,
            boolean leftLeg,
            boolean jacket,
            boolean rightSleeve,
            boolean leftSleeve,
            boolean rightPants,
            boolean leftPants
    ) {
        private static ModelVisibilitySnapshot capture(PlayerModel<AbstractClientPlayer> model) {
            return new ModelVisibilitySnapshot(
                    model.head.visible,
                    model.hat.visible,
                    model.body.visible,
                    model.rightArm.visible,
                    model.leftArm.visible,
                    model.rightLeg.visible,
                    model.leftLeg.visible,
                    model.jacket.visible,
                    model.rightSleeve.visible,
                    model.leftSleeve.visible,
                    model.rightPants.visible,
                    model.leftPants.visible
            );
        }

        private void restore(PlayerModel<AbstractClientPlayer> model) {
            model.head.visible = head;
            model.hat.visible = hat;
            model.body.visible = body;
            model.rightArm.visible = rightArm;
            model.leftArm.visible = leftArm;
            model.rightLeg.visible = rightLeg;
            model.leftLeg.visible = leftLeg;
            model.jacket.visible = jacket;
            model.rightSleeve.visible = rightSleeve;
            model.leftSleeve.visible = leftSleeve;
            model.rightPants.visible = rightPants;
            model.leftPants.visible = leftPants;
        }
    }
}

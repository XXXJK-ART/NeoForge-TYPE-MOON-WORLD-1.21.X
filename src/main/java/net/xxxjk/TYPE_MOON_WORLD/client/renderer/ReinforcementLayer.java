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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;

public class ReinforcementLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final int EMISSIVE_LIGHT = 15728880;
   private static final int SEMI_TRANSPARENT_WHITE = -285212673;

   public ReinforcementLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
      super(renderer);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (!player.isInvisible()) {
         boolean hasStrength = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
         boolean hasDefense = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
         boolean hasAgility = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
         boolean hasSight = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
         boolean rightArmByItem = isArmHoldingMagicTextureItem(player, HumanoidArm.RIGHT);
         boolean leftArmByItem = isArmHoldingMagicTextureItem(player, HumanoidArm.LEFT);
         boolean rightArmByCrest = MagicCrestVisualHelper.shouldRenderCrestArm(player, HumanoidArm.RIGHT);
         boolean leftArmByCrest = MagicCrestVisualHelper.shouldRenderCrestArm(player, HumanoidArm.LEFT);
         if (hasStrength || hasDefense || hasAgility || hasSight || rightArmByItem || leftArmByItem || rightArmByCrest || leftArmByCrest) {
            poseStack.pushPose();
            poseStack.scale(1.013F, 1.013F, 1.013F);
            PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)this.getParentModel();
            ReinforcementLayer.ModelVisibilitySnapshot snapshot = ReinforcementLayer.ModelVisibilitySnapshot.capture(model);
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

            if (rightArmByCrest) {
               renderRightCrestArm(model, poseStack, buffer);
            }

            if (leftArmByCrest) {
               renderLeftCrestArm(model, poseStack, buffer);
            }

            if (hasAgility) {
               renderLegPart(model, poseStack, buffer, player);
            }

            snapshot.restore(model);
            poseStack.popPose();
         }
      }
   }

   private static void renderHeadPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
      setAllHidden(model);
      model.head.visible = true;
      model.hat.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.HEAD, player);
   }

   private static void renderBodyPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
      setAllHidden(model);
      model.body.visible = true;
      model.jacket.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.BODY, player);
   }

   private static void renderArmPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
      setAllHidden(model);
      model.rightArm.visible = true;
      model.leftArm.visible = true;
      model.rightSleeve.visible = true;
      model.leftSleeve.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.ARM, player);
   }

   private static void renderRightArmPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
      setAllHidden(model);
      model.rightArm.visible = true;
      model.rightSleeve.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.ARM, player);
   }

   private static void renderLeftArmPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
      setAllHidden(model);
      model.leftArm.visible = true;
      model.leftSleeve.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.ARM, player);
   }

   private static void renderRightCrestArm(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer) {
      setAllHidden(model);
      model.rightArm.visible = true;
      model.rightSleeve.visible = true;
      MagicCrestVisualHelper.renderArm(model, buffer, HumanoidArm.RIGHT, poseStack);
   }

   private static void renderLeftCrestArm(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer) {
      setAllHidden(model);
      model.leftArm.visible = true;
      model.leftSleeve.visible = true;
      MagicCrestVisualHelper.renderArm(model, buffer, HumanoidArm.LEFT, poseStack);
   }

   private static void renderLegPart(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, AbstractClientPlayer player) {
      setAllHidden(model);
      model.rightLeg.visible = true;
      model.leftLeg.visible = true;
      model.rightPants.visible = true;
      model.leftPants.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.LEG, player);
   }

   private static void renderCurrentVisible(
      PlayerModel<AbstractClientPlayer> model,
      PoseStack poseStack,
      MultiBufferSource buffer,
      ReinforcementRenderType.ReinforcementPart part,
      AbstractClientPlayer player
   ) {
      VertexConsumer vertexConsumer = buffer.getBuffer(ReinforcementRenderType.getSkinRenderType(part, player));
      model.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, -285212673);
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
      } else {
         CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (customData == null) {
            return false;
         } else {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Reinforced") && tag.getBoolean("Reinforced")) {
               return true;
            } else {
               return !tag.contains("ReinforcedLevel") && !tag.contains("ReinforcedEnchantment") && !tag.contains("ReinforcementTemporary")
                  ? tag.contains("is_projected") || tag.contains("is_infinite_projection")
                  : true;
            }
         }
      }
   }

   private static ItemStack getArmStack(AbstractClientPlayer player, HumanoidArm armSide) {
      HumanoidArm mainArm = player.getMainArm();
      return armSide == mainArm ? player.getItemInHand(InteractionHand.MAIN_HAND) : player.getItemInHand(InteractionHand.OFF_HAND);
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
      private static ReinforcementLayer.ModelVisibilitySnapshot capture(PlayerModel<AbstractClientPlayer> model) {
         return new ReinforcementLayer.ModelVisibilitySnapshot(
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
         model.head.visible = this.head;
         model.hat.visible = this.hat;
         model.body.visible = this.body;
         model.rightArm.visible = this.rightArm;
         model.leftArm.visible = this.leftArm;
         model.rightLeg.visible = this.rightLeg;
         model.leftLeg.visible = this.leftLeg;
         model.jacket.visible = this.jacket;
         model.rightSleeve.visible = this.rightSleeve;
         model.leftSleeve.visible = this.leftSleeve;
         model.rightPants.visible = this.rightPants;
         model.leftPants.visible = this.leftPants;
      }
   }
}

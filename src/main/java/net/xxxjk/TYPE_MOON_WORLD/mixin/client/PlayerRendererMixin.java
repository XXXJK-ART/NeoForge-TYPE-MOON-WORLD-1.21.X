package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MagicCrestVisualHelper;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public abstract class PlayerRendererMixin {
   private static final int EMISSIVE_LIGHT = 15728880;
   private static final int HAND_GLOW_COLOR = -285212673;

   @Inject(
      method = {"renderRightHand"},
      at = {@At("HEAD")}
   )
   private void applyRightHandCastingPose(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
      this.applyFirstPersonCastingPose(player, HumanoidArm.RIGHT);
   }

   @Inject(
      method = {"renderLeftHand"},
      at = {@At("HEAD")}
   )
   private void applyLeftHandCastingPose(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
      this.applyFirstPersonCastingPose(player, HumanoidArm.LEFT);
   }

   @Inject(
      method = {"renderRightHand"},
      at = {@At("TAIL")}
   )
   private void renderRightHandOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
      if (shouldRenderArm(player, HumanoidArm.RIGHT)) {
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)((PlayerRenderer)(Object)this).getModel();
         VertexConsumer vc = buffer.getBuffer(ReinforcementRenderType.getSkinRenderType(ReinforcementRenderType.ReinforcementPart.ARM, player));
         model.rightArm.render(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY, -285212673);
         model.rightSleeve.render(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY, -285212673);
      }

      if (MagicCrestVisualHelper.shouldRenderCrestArm(player, HumanoidArm.RIGHT)) {
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)((PlayerRenderer)(Object)this).getModel();
         MagicCrestVisualHelper.renderArm(model, buffer, HumanoidArm.RIGHT, poseStack);
      }
   }

   @Inject(
      method = {"renderLeftHand"},
      at = {@At("TAIL")}
   )
   private void renderLeftHandOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
      if (shouldRenderArm(player, HumanoidArm.LEFT)) {
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)((PlayerRenderer)(Object)this).getModel();
         VertexConsumer vc = buffer.getBuffer(ReinforcementRenderType.getSkinRenderType(ReinforcementRenderType.ReinforcementPart.ARM, player));
         model.leftArm.render(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY, -285212673);
         model.leftSleeve.render(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY, -285212673);
      }

      if (MagicCrestVisualHelper.shouldRenderCrestArm(player, HumanoidArm.LEFT)) {
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)((PlayerRenderer)(Object)this).getModel();
         MagicCrestVisualHelper.renderArm(model, buffer, HumanoidArm.LEFT, poseStack);
      }
   }

   private static boolean shouldRenderArm(AbstractClientPlayer player, HumanoidArm armSide) {
      boolean hasStrength = player.hasEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH) || player.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
      return hasStrength || isArmHoldingMagicTextureItem(player, armSide);
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

   private void applyFirstPersonCastingPose(AbstractClientPlayer player, HumanoidArm armToRender) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.player.getId() == player.getId()) {
         boolean ganderCharging = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGanderCharging();
         boolean gandrMachineGunCasting = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGandrMachineGunCasting();
         boolean tapCastPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalTapCastPoseActive();
         boolean machineGunFiringPose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalMachineGunFiringPoseActive();
         if (ganderCharging || gandrMachineGunCasting || tapCastPose || machineGunFiringPose) {
            HumanoidArm castingArm = TypeMoonWorldModKeyMappings.KeyEventListener.getLocalCastingArm();
            if (castingArm == armToRender) {
               PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)((PlayerRenderer)(Object)this).getModel();
               float pitchRad = Mth.clamp(player.getXRot(), -80.0F, 80.0F) * (float) (Math.PI / 180.0);
               float raiseRot = -1.35F + pitchRad * 0.85F;
               if (castingArm == HumanoidArm.LEFT) {
                  model.leftArm.xRot = raiseRot;
                  model.leftArm.yRot = 0.08F;
                  model.leftArm.zRot = -0.02F;
                  model.leftSleeve.copyFrom(model.leftArm);
               } else {
                  model.rightArm.xRot = raiseRot;
                  model.rightArm.yRot = -0.08F;
                  model.rightArm.zRot = 0.02F;
                  model.rightSleeve.copyFrom(model.rightArm);
               }
            }
         }
      }
   }
}

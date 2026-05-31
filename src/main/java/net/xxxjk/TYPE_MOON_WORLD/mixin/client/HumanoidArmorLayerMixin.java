package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HumanoidArmorLayer.class})
public abstract class HumanoidArmorLayerMixin {
   private static final ThreadLocal<ItemStack> CURRENT_ARMOR_STACK = new ThreadLocal<>();
   private static final int ARMOR_PATTERN_COLOR = -285212673;
   private static final int FULL_BRIGHT = 15728880;

   @Inject(
      method = {"renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V"},
      at = {@At("HEAD")},
      require = 0
   )
   private void captureArmorStack(
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      LivingEntity livingEntity,
      EquipmentSlot slot,
      int packedLight,
      HumanoidModel<?> model,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      CallbackInfo ci
   ) {
      CURRENT_ARMOR_STACK.set(livingEntity.getItemBySlot(slot));
   }

   @Inject(
      method = {"renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V"},
      at = {@At("RETURN")},
      require = 0
   )
   private void clearArmorStack(
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      LivingEntity livingEntity,
      EquipmentSlot slot,
      int packedLight,
      HumanoidModel<?> model,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      CallbackInfo ci
   ) {
      ItemStack stack = CURRENT_ARMOR_STACK.get();
      if (hasProjectionTag(stack)) {
         poseStack.pushPose();
         poseStack.scale(1.01F, 1.01F, 1.01F);
         VertexConsumer consumer = bufferSource.getBuffer(ReinforcementRenderType.getSkinRenderType(resolvePart(slot), livingEntity));
         model.renderToBuffer(poseStack, consumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ARMOR_PATTERN_COLOR);
         poseStack.popPose();
      }

      CURRENT_ARMOR_STACK.remove();
   }

   @ModifyExpressionValue(
      method = {"renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/RenderType;armorEntityGlint()Lnet/minecraft/client/renderer/RenderType;"
      )},
      require = 0
   )
   private RenderType replaceArmorEntityGlint(RenderType original) {
      return !hasMagicTextureTag(CURRENT_ARMOR_STACK.get()) ? original : ReinforcementRenderType.entityGlint();
   }

   private static boolean hasMagicTextureTag(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
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
      } else {
         return false;
      }
   }

   private static boolean hasProjectionTag(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else {
         CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (customData == null) {
            return false;
         } else {
            CompoundTag tag = customData.copyTag();
            return tag.contains("is_projected") && tag.getBoolean("is_projected")
               || tag.contains("is_infinite_projection") && tag.getBoolean("is_infinite_projection");
         }
      }
   }

   private static ReinforcementRenderType.ReinforcementPart resolvePart(EquipmentSlot slot) {
      return switch (slot) {
         case HEAD -> ReinforcementRenderType.ReinforcementPart.HEAD;
         case CHEST -> ReinforcementRenderType.ReinforcementPart.BODY;
         case LEGS, FEET -> ReinforcementRenderType.ReinforcementPart.LEG;
         default -> ReinforcementRenderType.ReinforcementPart.BODY;
      };
   }
}

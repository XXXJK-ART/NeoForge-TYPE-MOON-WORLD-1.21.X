package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;

public class MysticMagicianMagicOverlayLayer extends RenderLayer<MysticMagicianEntity, PlayerModel<MysticMagicianEntity>> {
   private static final int EMISSIVE_LIGHT = 15728880;
   private static final int SEMI_TRANSPARENT_WHITE = -285212673;

   public MysticMagicianMagicOverlayLayer(RenderLayerParent<MysticMagicianEntity, PlayerModel<MysticMagicianEntity>> renderer) {
      super(renderer);
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      MysticMagicianEntity entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (!entity.isInvisible()) {
         boolean hasStrength = entity.hasEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH) || entity.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
         boolean hasDefense = entity.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE) || entity.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
         boolean hasAgility = entity.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY) || entity.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
         boolean hasSight = entity.hasEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT) || entity.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
         MysticMagicianMagicOverlayLayer.ArmItemFlags rightArmFlags = inspectArmItem(entity, HumanoidArm.RIGHT);
         MysticMagicianMagicOverlayLayer.ArmItemFlags leftArmFlags = inspectArmItem(entity, HumanoidArm.LEFT);
         boolean hasProjectionItem = rightArmFlags.projected || leftArmFlags.projected;
         if (hasStrength || hasDefense || hasAgility || hasSight || rightArmFlags.magicTexture || leftArmFlags.magicTexture) {
            poseStack.pushPose();
            poseStack.scale(1.013F, 1.013F, 1.013F);
            PlayerModel<MysticMagicianEntity> model = (PlayerModel<MysticMagicianEntity>)this.getParentModel();
            MysticMagicianMagicOverlayLayer.ModelVisibilitySnapshot snapshot = MysticMagicianMagicOverlayLayer.ModelVisibilitySnapshot.capture(model);
            if (hasSight) {
               renderHeadPart(model, poseStack, buffer, entity);
            }

            if (hasDefense || hasProjectionItem) {
               renderBodyPart(model, poseStack, buffer, entity);
            }

            if (hasStrength) {
               renderArmPart(model, poseStack, buffer, entity);
            } else {
               if (rightArmFlags.magicTexture) {
                  renderRightArmPart(model, poseStack, buffer, entity);
               }

               if (leftArmFlags.magicTexture) {
                  renderLeftArmPart(model, poseStack, buffer, entity);
               }
            }

            if (hasAgility) {
               renderLegPart(model, poseStack, buffer, entity);
            }

            snapshot.restore(model);
            poseStack.popPose();
         }
      }
   }

   private static void renderHeadPart(PlayerModel<MysticMagicianEntity> model, PoseStack poseStack, MultiBufferSource buffer, MysticMagicianEntity entity) {
      setAllHidden(model);
      model.head.visible = true;
      model.hat.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.HEAD, entity);
   }

   private static void renderBodyPart(PlayerModel<MysticMagicianEntity> model, PoseStack poseStack, MultiBufferSource buffer, MysticMagicianEntity entity) {
      setAllHidden(model);
      model.body.visible = true;
      model.jacket.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.BODY, entity);
   }

   private static void renderArmPart(PlayerModel<MysticMagicianEntity> model, PoseStack poseStack, MultiBufferSource buffer, MysticMagicianEntity entity) {
      setAllHidden(model);
      model.rightArm.visible = true;
      model.leftArm.visible = true;
      model.rightSleeve.visible = true;
      model.leftSleeve.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.ARM, entity);
   }

   private static void renderRightArmPart(PlayerModel<MysticMagicianEntity> model, PoseStack poseStack, MultiBufferSource buffer, MysticMagicianEntity entity) {
      setAllHidden(model);
      model.rightArm.visible = true;
      model.rightSleeve.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.ARM, entity);
   }

   private static void renderLeftArmPart(PlayerModel<MysticMagicianEntity> model, PoseStack poseStack, MultiBufferSource buffer, MysticMagicianEntity entity) {
      setAllHidden(model);
      model.leftArm.visible = true;
      model.leftSleeve.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.ARM, entity);
   }

   private static void renderLegPart(PlayerModel<MysticMagicianEntity> model, PoseStack poseStack, MultiBufferSource buffer, MysticMagicianEntity entity) {
      setAllHidden(model);
      model.rightLeg.visible = true;
      model.leftLeg.visible = true;
      model.rightPants.visible = true;
      model.leftPants.visible = true;
      renderCurrentVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.LEG, entity);
   }

   private static void renderCurrentVisible(
      PlayerModel<MysticMagicianEntity> model,
      PoseStack poseStack,
      MultiBufferSource buffer,
      ReinforcementRenderType.ReinforcementPart part,
      MysticMagicianEntity entity
   ) {
      VertexConsumer vertexConsumer = buffer.getBuffer(ReinforcementRenderType.getSkinRenderType(part, entity));
      model.renderToBuffer(poseStack, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY, -285212673);
   }

   private static void setAllHidden(PlayerModel<MysticMagicianEntity> model) {
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

   private static MysticMagicianMagicOverlayLayer.ArmItemFlags inspectArmItem(MysticMagicianEntity entity, HumanoidArm armSide) {
      ItemStack stack = getArmStack(entity, armSide);
      if (stack.isEmpty()) {
         return MysticMagicianMagicOverlayLayer.ArmItemFlags.EMPTY;
      } else {
         CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (customData == null) {
            return MysticMagicianMagicOverlayLayer.ArmItemFlags.EMPTY;
         } else {
            CompoundTag tag = customData.copyTag();
            boolean projected = tag.contains("is_projected") && tag.getBoolean("is_projected")
               || tag.contains("is_infinite_projection") && tag.getBoolean("is_infinite_projection");
            boolean reinforced = tag.contains("Reinforced") && tag.getBoolean("Reinforced")
               || tag.contains("ReinforcedLevel")
               || tag.contains("ReinforcedEnchantment")
               || tag.contains("ReinforcementTemporary");
            boolean magicTexture = projected || reinforced;
            return new MysticMagicianMagicOverlayLayer.ArmItemFlags(magicTexture, projected);
         }
      }
   }

   private static ItemStack getArmStack(MysticMagicianEntity entity, HumanoidArm armSide) {
      HumanoidArm mainArm = entity.getMainArm();
      return armSide == mainArm ? entity.getMainHandItem() : entity.getOffhandItem();
   }

   private record ArmItemFlags(boolean magicTexture, boolean projected) {
      private static final MysticMagicianMagicOverlayLayer.ArmItemFlags EMPTY = new MysticMagicianMagicOverlayLayer.ArmItemFlags(false, false);
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
      private static MysticMagicianMagicOverlayLayer.ModelVisibilitySnapshot capture(PlayerModel<MysticMagicianEntity> model) {
         return new MysticMagicianMagicOverlayLayer.ModelVisibilitySnapshot(
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

      private void restore(PlayerModel<MysticMagicianEntity> model) {
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

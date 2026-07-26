package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.ChurchExecutorEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;

/** Renders the same magic-circuit skin used by mystic magicians over church executors. */
public final class ChurchExecutorReinforcementLayer
   extends RenderLayer<ChurchExecutorEntity, PlayerModel<ChurchExecutorEntity>> {
   private static final int EMISSIVE_LIGHT = 15728880;
   private static final int SEMI_TRANSPARENT_WHITE = -285212673;
   private final PlayerModel<ChurchExecutorEntity> steveOverlayModel;
   private final PlayerModel<ChurchExecutorEntity> alexOverlayModel;

   public ChurchExecutorReinforcementLayer(
      RenderLayerParent<ChurchExecutorEntity, PlayerModel<ChurchExecutorEntity>> renderer, Context context) {
      super(renderer);
      steveOverlayModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
      alexOverlayModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
   }

   @Override
   public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ChurchExecutorEntity entity,
                      float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                      float netHeadYaw, float headPitch) {
      if (entity.isInvisible()) return;
      boolean strength = entity.hasEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH)
         || entity.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
      boolean defense = entity.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE)
         || entity.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
      boolean agility = entity.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY)
         || entity.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
      boolean sight = entity.hasEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT)
         || entity.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
      if (!strength && !defense && !agility && !sight) return;

      PlayerModel<ChurchExecutorEntity> overlay = entity.isFemale() ? alexOverlayModel : steveOverlayModel;
      getParentModel().copyPropertiesTo(overlay);
      ModelVisibilitySnapshot snapshot = ModelVisibilitySnapshot.capture(overlay);
      poseStack.pushPose();
      poseStack.scale(1.013F, 1.013F, 1.013F);
      if (sight) renderHead(overlay, poseStack, buffer, entity);
      if (defense) renderBody(overlay, poseStack, buffer, entity);
      if (strength) renderArms(overlay, poseStack, buffer, entity);
      if (agility) renderLegs(overlay, poseStack, buffer, entity);
      snapshot.restore(overlay);
      poseStack.popPose();
   }

   private static void renderHead(PlayerModel<ChurchExecutorEntity> model, PoseStack poseStack,
                                  MultiBufferSource buffer, ChurchExecutorEntity entity) {
      setAllHidden(model);
      model.head.visible = true;
      model.hat.visible = true;
      renderVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.HEAD, entity);
   }

   private static void renderBody(PlayerModel<ChurchExecutorEntity> model, PoseStack poseStack,
                                  MultiBufferSource buffer, ChurchExecutorEntity entity) {
      setAllHidden(model);
      model.body.visible = true;
      model.jacket.visible = true;
      renderVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.BODY, entity);
   }

   private static void renderArms(PlayerModel<ChurchExecutorEntity> model, PoseStack poseStack,
                                  MultiBufferSource buffer, ChurchExecutorEntity entity) {
      setAllHidden(model);
      model.rightArm.visible = true;
      model.leftArm.visible = true;
      model.rightSleeve.visible = true;
      model.leftSleeve.visible = true;
      renderVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.ARM, entity);
   }

   private static void renderLegs(PlayerModel<ChurchExecutorEntity> model, PoseStack poseStack,
                                  MultiBufferSource buffer, ChurchExecutorEntity entity) {
      setAllHidden(model);
      model.rightLeg.visible = true;
      model.leftLeg.visible = true;
      model.rightPants.visible = true;
      model.leftPants.visible = true;
      renderVisible(model, poseStack, buffer, ReinforcementRenderType.ReinforcementPart.LEG, entity);
   }

   private static void renderVisible(PlayerModel<ChurchExecutorEntity> model, PoseStack poseStack,
                                     MultiBufferSource buffer, ReinforcementRenderType.ReinforcementPart part,
                                     ChurchExecutorEntity entity) {
      VertexConsumer consumer = buffer.getBuffer(ReinforcementRenderType.getSkinRenderType(part, entity));
      model.renderToBuffer(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, SEMI_TRANSPARENT_WHITE);
   }

   private static void setAllHidden(PlayerModel<ChurchExecutorEntity> model) {
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

   private record ModelVisibilitySnapshot(boolean head, boolean hat, boolean body, boolean rightArm, boolean leftArm,
                                          boolean rightLeg, boolean leftLeg, boolean jacket, boolean rightSleeve,
                                          boolean leftSleeve, boolean rightPants, boolean leftPants) {
      private static ModelVisibilitySnapshot capture(PlayerModel<ChurchExecutorEntity> model) {
         return new ModelVisibilitySnapshot(model.head.visible, model.hat.visible, model.body.visible,
            model.rightArm.visible, model.leftArm.visible, model.rightLeg.visible, model.leftLeg.visible,
            model.jacket.visible, model.rightSleeve.visible, model.leftSleeve.visible,
            model.rightPants.visible, model.leftPants.visible);
      }

      private void restore(PlayerModel<ChurchExecutorEntity> model) {
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

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;

public final class MacedonianSoldierRenderer extends HumanoidMobRenderer<MacedonianSoldierEntity, PlayerModel<MacedonianSoldierEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "textures/entity/macedonian_soldier.png");

   public MacedonianSoldierRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
      this.addLayer(new PetrifiedLivingLayer<>(this));
   }

   @Override
   public void render(MacedonianSoldierEntity entity, float yaw, float partialTick, PoseStack poseStack,
                      MultiBufferSource buffer, int packedLight) {
      float scale = entity.getVisualScale();
      this.model.rightArmPose = HumanoidModel.ArmPose.ITEM;
      this.model.leftArmPose = HumanoidModel.ArmPose.ITEM;
      poseStack.pushPose();
      poseStack.scale(scale, scale, scale);
      super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
      poseStack.popPose();
      this.model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
      this.model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
   }

   @Override
   public ResourceLocation getTextureLocation(MacedonianSoldierEntity entity) {
      return TEXTURE;
   }
}

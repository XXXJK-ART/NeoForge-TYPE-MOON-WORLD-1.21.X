package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.ChurchExecutorEntity;

public class ChurchExecutorRenderer extends HumanoidMobRenderer<ChurchExecutorEntity, PlayerModel<ChurchExecutorEntity>> {
   private static final ResourceLocation[] TEXTURES = {
      texture("church_executor_male_1"), texture("church_executor_male_2"), texture("church_executor_male_3"),
      texture("church_executor_female")
   };
   private final PlayerModel<ChurchExecutorEntity> male;
   private final PlayerModel<ChurchExecutorEntity> female;

   public ChurchExecutorRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      male = getModel(); female = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.addLayer(new ChurchExecutorReinforcementLayer(this, context));
   }

   @Override public void render(ChurchExecutorEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
      model = entity.isFemale() ? female : male;
      model.rightArmPose = entity.isCrossbowAiming() ? HumanoidModel.ArmPose.CROSSBOW_HOLD : HumanoidModel.ArmPose.EMPTY;
      model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
      super.render(entity, yaw, partialTick, pose, buffer, light);
      model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
   }

   @Override public ResourceLocation getTextureLocation(ChurchExecutorEntity entity) {
      return TEXTURES[Mth.clamp(entity.getSkinVariant(), 0, TEXTURES.length - 1)];
   }

   private static ResourceLocation texture(String name) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/" + name + ".png"); }
}

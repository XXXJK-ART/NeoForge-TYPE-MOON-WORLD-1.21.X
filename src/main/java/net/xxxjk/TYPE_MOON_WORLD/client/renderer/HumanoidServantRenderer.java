package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardConcealmentClient;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class HumanoidServantRenderer<T extends ServantEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {
   private final ResourceLocation texture;

   public HumanoidServantRenderer(EntityRendererProvider.Context context, String textureName) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.texture = ResourceLocation.fromNamespaceAndPath(
         TYPE_MOON_WORLD.MOD_ID, "textures/entity/" + textureName + ".png");
      this.addLayer(new HumanoidArmorLayer<>(this,
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
   }

   @Override
   public void render(T entity, float yaw, float partialTick, PoseStack poseStack,
                      MultiBufferSource buffers, int packedLight) {
      if (ServantCardConcealmentClient.isPerfectlyConcealed(entity)) return;
      float scale = visualScale(entity.getServantId());
      if (scale == 1.0F) {
         super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
         return;
      }
      poseStack.pushPose();
      poseStack.scale(scale, scale, scale);
      super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
      poseStack.popPose();
   }

   @Override
   public ResourceLocation getTextureLocation(T entity) {
      return this.texture;
   }

   private static float visualScale(String servantId) {
      return switch (servantId == null ? "" : servantId) {
         case "oda_nobunaga" -> 0.800F;
         case "artoria_pendragon" -> 0.811F;
         case "fanatic_assassin", "medea" -> 0.858F;
         case "nightingale" -> 0.868F;
         case "li_shuwen" -> 0.874F;
         case "senko_muramasa" -> 0.879F;
         case "ushiwakamaru_rider" -> 0.884F;
         case "medusa" -> 0.905F;
         case "emiya_archer" -> 0.921F;
         case "sasaki_kojiro" -> 0.926F;
         case "gawain" -> 0.947F;
         case "enkidu", "gilgamesh", "gilgamesh_caster" -> 0.958F;
         case "paracelsus" -> 0.963F;
         case "zhao_yun_rider" -> 0.968F;
         case "arash", "cu_chulainn" -> 0.974F;
         default -> 1.0F;
      };
   }
}

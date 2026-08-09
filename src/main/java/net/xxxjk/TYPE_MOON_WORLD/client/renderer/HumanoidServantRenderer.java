package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardConcealmentClient;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

public class HumanoidServantRenderer<T extends ServantEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {
   private final Function<T, ResourceLocation> textureResolver;
   private float renderPartialTick;

   public HumanoidServantRenderer(EntityRendererProvider.Context context, String textureName) {
      this(context, entity -> ResourceLocation.fromNamespaceAndPath(
         TYPE_MOON_WORLD.MOD_ID, "textures/entity/" + textureName + ".png"));
   }

   protected HumanoidServantRenderer(EntityRendererProvider.Context context,
                                     Function<T, ResourceLocation> textureResolver) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.textureResolver = textureResolver;
      this.addLayer(new ClippedAwareArmorLayer<>(this,
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
   }

   @Override
   public void render(T entity, float yaw, float partialTick, PoseStack poseStack,
                      MultiBufferSource buffers, int packedLight) {
      if (ServantCardConcealmentClient.isPerfectlyConcealed(entity)) return;
      this.renderPartialTick = partialTick;
      float scale = visualScale(entity.getServantId());
      boolean dissolving = entity.isSpiritualDissolving();
      float dissolveProgress = dissolving ? entity.getSpiritualDissolveProgress(partialTick) : 0.0F;
      float manifestProgress = entity.getSpiritualManifestProgress(partialTick);
      if (scale == 1.0F) {
         if (dissolving || manifestProgress < 1.0F) {
            poseStack.pushPose();
            applySpiritualPose(poseStack, manifestProgress, dissolveProgress, dissolving);
            super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
            poseStack.popPose();
            return;
         }
         super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
         return;
      }
      poseStack.pushPose();
      if (dissolving || manifestProgress < 1.0F) {
         applySpiritualPose(poseStack, manifestProgress, dissolveProgress, dissolving);
      }
      poseStack.scale(scale, scale, scale);
      super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
      poseStack.popPose();
   }

   @Override
   public ResourceLocation getTextureLocation(T entity) {
      return this.textureResolver.apply(entity);
   }

   @Override
   @Nullable
   protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
      if (bodyVisible && ServantClipRenderHelper.shouldClip(entity, this.renderPartialTick)) {
         return ServantClipRenderHelper.renderType(entity, this.getTextureLocation(entity), this.renderPartialTick);
      }
      return super.getRenderType(entity, bodyVisible, translucent, glowing);
   }

   private static void applySpiritualPose(PoseStack poseStack, float manifestProgress, float dissolveProgress, boolean dissolving) {
      float manifest = manifestProgress * manifestProgress * (3.0F - 2.0F * manifestProgress);
      float dissolve = dissolveProgress * dissolveProgress * (3.0F - 2.0F * dissolveProgress);
      poseStack.translate(0.0F, dissolving ? dissolve * 0.16F : (1.0F - manifest) * -0.1F, 0.0F);
      float manifestWidth = 0.88F + manifest * 0.12F;
      float manifestHeight = 0.92F + manifest * 0.08F;
      float horizontal = manifestWidth - dissolve * 0.035F;
      float vertical = manifestHeight + dissolve * 0.025F;
      poseStack.scale(horizontal, vertical, horizontal);
   }

   private static float visualScale(String servantId) {
      String normalizedId = servantId == null ? "" : servantId;
      int separator = normalizedId.indexOf(':');
      if (separator >= 0) {
         normalizedId = normalizedId.substring(separator + 1);
      }
      return switch (normalizedId) {
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

   private static class ClippedAwareArmorLayer<T extends ServantEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
      extends HumanoidArmorLayer<T, M, A> {
      ClippedAwareArmorLayer(net.minecraft.client.renderer.entity.RenderLayerParent<T, M> renderer, A innerModel, A outerModel,
                             net.minecraft.client.resources.model.ModelManager modelManager) {
         super(renderer, innerModel, outerModel, modelManager);
      }

      @Override
      public void render(
         PoseStack poseStack,
         MultiBufferSource buffer,
         int packedLight,
         T livingEntity,
         float limbSwing,
         float limbSwingAmount,
         float partialTicks,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         if (ServantClipRenderHelper.shouldClip(livingEntity, partialTicks)) {
            return;
         }
         super.render(poseStack, buffer, packedLight, livingEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
      }
   }
}

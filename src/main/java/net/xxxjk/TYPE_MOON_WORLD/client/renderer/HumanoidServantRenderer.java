package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardConcealmentClient;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanPersonaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;
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
      super(context, new ServantPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.textureResolver = textureResolver;
      this.addLayer(new HumanoidArmorLayer<>(this,
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
         new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
      this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
   }

   @Override
   public void render(T entity, float yaw, float partialTick, PoseStack poseStack,
                      MultiBufferSource buffers, int packedLight) {
      if (ServantCardConcealmentClient.isPerfectlyConcealed(entity)) return;
      this.renderPartialTick = partialTick;
      float scale = visualScale(entity);
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

   @Override
   protected float getFlipDegrees(T livingEntity) {
      return livingEntity.isSpiritualDissolving() ? 0.0F : super.getFlipDegrees(livingEntity);
   }

   private static float visualScale(ServantEntity entity) {
      if (entity instanceof HundredFacesHassanPersonaEntity persona) return persona.getVisualScale();
      if (entity instanceof HundredFacesHassanEntity hassan) return hassan.getVisualScale();
      return visualScale(entity.getServantId());
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
         case "jeanne_alter" -> 0.837F;
         case "okita_souji_saber" -> 0.811F;
         case "fanatic_assassin", "medea" -> 0.858F;
         case "nightingale" -> 0.868F;
         case "li_shuwen" -> 0.874F;
         case "senko_muramasa" -> 0.879F;
         case "ushiwakamaru_rider" -> 0.884F;
         case "baobhan_sith" -> 0.900F;
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

   private static final class ServantPlayerModel<T extends ServantEntity> extends PlayerModel<T> {
      private ServantPlayerModel(ModelPart root, boolean slim) {
         super(root, slim);
      }

      @Override
      public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
         boolean standOnGordiusWheel = entity instanceof IskandarEntity && entity.getVehicle() instanceof GordiusWheelEntity;
         if (standOnGordiusWheel) {
            this.riding = false;
         }
         super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
         applyCombatPose(entity);
         applyArtoriaCombatPose(entity, ageInTicks);
         if (isJeanneAlter(entity.getServantId())) {
            this.leftArm.xRot = -(float)Math.PI / 2.0F;
            this.leftArm.yRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.leftSleeve.copyFrom(this.leftArm);
         }
      }

      private void applyCombatPose(T entity) {
         float phase = Mth.clamp(entity.getCombatPoseTicks() / 6.0F, 0.0F, 1.0F);
         if (entity.getCombatPose() == 1) {
            this.body.yRot += 0.24F * phase;
            this.rightArm.xRot = -0.55F + 0.25F * phase;
            this.leftArm.xRot = -0.15F - 0.30F * phase;
            this.rightLeg.xRot = 0.22F * phase;
            this.leftLeg.xRot = -0.12F * phase;
         } else if (entity.getCombatPose() == 2) {
            this.rightArm.xRot = -1.05F * phase;
            this.leftArm.xRot = -1.05F * phase;
            this.rightArm.yRot = -0.55F * phase;
            this.leftArm.yRot = 0.55F * phase;
            this.rightArm.zRot = 0.18F * phase;
            this.leftArm.zRot = -0.18F * phase;
            this.body.xRot = 0.10F * phase;
         }
      }

      private void applyArtoriaCombatPose(T entity, float ageInTicks) {
         if (!(entity instanceof ArtoriaPendragonEntity artoria)
            || (!artoria.isPerformingAction() && !artoria.isAttackSwinging())) {
            return;
         }
         float phase = Mth.frac(ageInTicks * 0.11F);
         float strike = Mth.sin(phase * (float)Math.PI);
         float recoil = Mth.sin((phase + 0.5F) * (float)Math.PI);
         this.rightArm.xRot = -1.05F + strike * 1.55F;
         this.rightArm.yRot = -0.18F - strike * 0.32F;
         this.rightArm.zRot = -0.16F + recoil * 0.12F;
         this.leftArm.xRot = -0.18F - strike * 0.22F;
         this.body.yRot = recoil * 0.14F;
      }

      private static boolean isJeanneAlter(String servantId) {
         if (servantId == null) return false;
         int separator = servantId.indexOf(':');
         return "jeanne_alter".equals(separator >= 0 ? servantId.substring(separator + 1) : servantId);
      }
   }
}

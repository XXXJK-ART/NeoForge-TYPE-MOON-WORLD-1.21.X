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
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
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
         if (entity instanceof OdaNobunagaEntity oda) {
            applyOdaActionPose(oda, ageInTicks - entity.tickCount);
         }
         if (entity instanceof SasakiKojiroEntity sasaki) {
            applySasakiActionPose(sasaki, ageInTicks - sasaki.tickCount);
         }
         if (entity.getVehicle() instanceof net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity gun && gun.isChantLift()) {
            net.xxxjk.TYPE_MOON_WORLD.client.OdaChantPose.apply(this, gun.chantPoseProgress(ageInTicks - entity.tickCount));
         }
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

      private void applySasakiActionPose(SasakiKojiroEntity sasaki, float partialTick) {
         SasakiKojiroAction action = sasaki.getSasakiAction();
         if (action == null) return;
         float age = sasaki.getSasakiActionAge(partialTick);
         if (age < 0.0F || age >= action.durationTicks()) return;
         float progress = Mth.clamp(age / action.durationTicks(), 0.0F, 1.0F);
         float sweep = Mth.sin(progress * (float)Math.PI);
         float recovery = 1.0F - Mth.clamp((progress - 0.55F) / 0.45F, 0.0F, 1.0F);
         switch (action) {
            case CROUCH_CUTBACK -> {
               float draw = 1.0F - Mth.clamp(progress * 2.2F, 0.0F, 1.0F);
               this.body.xRot -= sweep * 0.18F;
               this.body.yRot -= sweep * 0.32F;
               this.rightArm.xRot = Mth.lerp(sweep, this.rightArm.xRot, -0.75F - draw * 0.85F);
               this.rightArm.zRot -= sweep * 0.55F;
               this.leftArm.xRot = Mth.lerp(sweep, this.leftArm.xRot, -0.42F);
               this.rightLeg.xRot += sweep * 0.6F;
               this.leftLeg.xRot -= sweep * 0.32F;
            }
            case VERTICAL_SLASH, DOWNWARD_SLASH -> {
               float windup = 1.0F - Mth.clamp(progress * 2.0F, 0.0F, 1.0F);
               this.body.yRot -= sweep * 0.16F;
               this.body.xRot += sweep * 0.12F;
               this.rightArm.xRot = Mth.lerp(sweep, this.rightArm.xRot, -0.22F - windup * 1.25F + sweep * 1.18F);
               this.rightArm.zRot = Mth.lerp(sweep, this.rightArm.zRot, -0.12F - sweep * 0.5F);
               this.leftArm.xRot = Mth.lerp(sweep, this.leftArm.xRot, -0.72F);
               this.leftLeg.xRot -= sweep * 0.16F;
            }
            case UPWARD_SLASH -> {
               this.body.xRot -= sweep * 0.12F;
               this.body.yRot += sweep * 0.18F;
               this.rightArm.xRot = Mth.lerp(sweep, this.rightArm.xRot, -2.1F + progress * 0.75F);
               this.rightArm.zRot = Mth.lerp(sweep, this.rightArm.zRot, 0.32F + sweep * 0.22F);
               this.leftArm.xRot = Mth.lerp(sweep, this.leftArm.xRot, -0.38F);
               this.rightLeg.xRot += sweep * 0.2F;
            }
            case HORIZONTAL_SLASH -> {
               float draw = 1.0F - Mth.clamp(progress * 2.0F, 0.0F, 1.0F);
               this.body.yRot -= sweep * 0.56F;
               this.rightArm.xRot = Mth.lerp(sweep, this.rightArm.xRot, -0.68F - draw * 0.64F);
               this.rightArm.zRot = Mth.lerp(sweep, this.rightArm.zRot, -0.25F - sweep * 0.95F);
               this.leftArm.xRot = Mth.lerp(sweep, this.leftArm.xRot, -0.55F);
               this.rightLeg.xRot -= sweep * 0.18F;
            }
            case TRANSPARENCY, PRESENCE_CONCEALMENT -> {
               this.body.xRot -= recovery * 0.1F;
               this.rightArm.xRot = Mth.lerp(recovery, this.rightArm.xRot, -0.82F);
               this.leftArm.xRot = Mth.lerp(recovery, this.leftArm.xRot, -0.5F);
               this.rightArm.zRot = Mth.lerp(recovery, this.rightArm.zRot, -0.18F);
            }
            case THRUST -> {
               this.body.xRot -= sweep * 0.32F;
               this.rightArm.xRot = Mth.lerp(sweep, this.rightArm.xRot, -1.42F + progress * 0.45F);
               this.rightArm.zRot = Mth.lerp(sweep, this.rightArm.zRot, -0.08F);
               this.leftArm.xRot = Mth.lerp(sweep, this.leftArm.xRot, -0.66F);
               this.rightLeg.xRot = Mth.lerp(sweep, this.rightLeg.xRot, -0.55F);
               this.leftLeg.xRot = Mth.lerp(sweep, this.leftLeg.xRot, 0.38F);
            }
            case SPINNING_SLASH -> {
               float turn = Mth.sin(progress * (float)Math.PI * 2.0F);
               this.body.yRot += turn * 0.58F;
               this.rightArm.xRot = Mth.lerp(sweep, this.rightArm.xRot, -0.9F - sweep * 0.5F);
               this.leftArm.xRot = Mth.lerp(sweep, this.leftArm.xRot, -0.7F);
               this.rightArm.zRot -= sweep * 0.62F;
               this.leftLeg.xRot -= sweep * 0.2F;
            }
            case COUNTER_STANCE -> {
               this.body.xRot += recovery * 0.08F;
               this.rightArm.xRot = Mth.lerp(recovery, this.rightArm.xRot, -1.12F);
               this.leftArm.xRot = Mth.lerp(recovery, this.leftArm.xRot, -0.92F);
               this.rightArm.yRot = Mth.lerp(recovery, this.rightArm.yRot, -0.32F);
               this.leftArm.yRot = Mth.lerp(recovery, this.leftArm.yRot, 0.38F);
               this.rightLeg.xRot += recovery * 0.12F;
            }
            case TSUBAME_GAESHI -> {
               net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.applyMainPose(
                  this, net.xxxjk.TYPE_MOON_WORLD.client.SasakiTsubameAfterimageClient.animationTime(age));
            }
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

      private void applyOdaActionPose(OdaNobunagaEntity oda, float partialTick) {
         String action = oda.getActionAnimation();
         float age = oda.getActionAnimationAge(partialTick);
         switch (action) {
            case "attack_shot" -> {
               float pose = actionEnvelope(age, 12.0F, 3.0F, 3.0F);
               float recoil = pulse(age, 4.0F, 1.4F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -1.25F + recoil * 0.38F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -1.08F + recoil * 0.18F);
               this.rightArm.yRot = Mth.lerp(pose, this.rightArm.yRot, -0.12F);
               this.leftArm.yRot = Mth.lerp(pose, this.leftArm.yRot, 0.12F);
               this.body.xRot -= recoil * 0.08F;
            }
            case "attack_slash" -> {
               float pose = actionEnvelope(age, 16.0F, 2.0F, 3.0F);
               float windup = 1.0F - Mth.clamp(age / 4.0F, 0.0F, 1.0F);
               float cut = pulse(age, 7.5F, 4.5F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -0.35F - windup * 0.65F - cut * 0.8F);
               this.rightArm.yRot = Mth.lerp(pose, this.rightArm.yRot, -0.18F - cut * 0.3F);
               this.rightArm.zRot = Mth.lerp(pose, this.rightArm.zRot, -0.2F - cut * 1.05F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -0.65F + cut * 0.3F);
               this.body.yRot -= cut * 0.42F;
               this.body.xRot += cut * 0.1F;
            }
            case "attack_volley" -> {
               float pose = actionEnvelope(age, 24.0F, 2.0F, 3.0F);
               float recoil = Math.max(Math.max(pulse(age, 4.0F, 1.2F), pulse(age, 10.0F, 1.2F)), pulse(age, 16.0F, 1.2F));
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -1.18F + recoil * 0.28F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -1.1F + recoil * 0.22F);
               this.rightArm.yRot = Mth.lerp(pose, this.rightArm.yRot, -0.22F);
               this.leftArm.yRot = Mth.lerp(pose, this.leftArm.yRot, 0.22F);
               this.body.xRot -= recoil * 0.06F;
            }
            case "charged_shot" -> {
               float pose = actionEnvelope(age, 42.0F, 5.0F, 5.0F);
               float aim = Mth.clamp(age / 26.0F, 0.0F, 1.0F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -0.55F - aim * 0.78F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -0.5F - aim * 0.62F);
               this.rightArm.zRot = Mth.lerp(pose, this.rightArm.zRot, -0.16F);
               this.leftArm.zRot = Mth.lerp(pose, this.leftArm.zRot, 0.16F);
               this.body.xRot += pose * 0.07F;
            }
            case "skill_command" -> {
               float pose = actionEnvelope(age, 24.0F, 4.0F, 4.0F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -1.58F);
               this.rightArm.zRot = Mth.lerp(pose, this.rightArm.zRot, -0.38F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -0.72F);
               this.leftArm.zRot = Mth.lerp(pose, this.leftArm.zRot, 0.4F);
               this.body.yRot -= pose * 0.14F;
            }
            case "skill_maou" -> {
               float pose = actionEnvelope(age, 34.0F, 6.0F, 6.0F);
               float spread = Mth.sin(Mth.clamp(age / 22.0F, 0.0F, 1.0F) * (float)Math.PI * 0.5F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -0.72F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -0.72F);
               this.rightArm.zRot = Mth.lerp(pose, this.rightArm.zRot, -0.34F - spread * 0.42F);
               this.leftArm.zRot = Mth.lerp(pose, this.leftArm.zRot, 0.34F + spread * 0.42F);
               this.body.xRot -= pose * 0.08F;
            }
            case "np_three_thousand_chant" -> {
               float pose = actionEnvelope(age, 56.0F, 8.0F, 6.0F);
               float rise = Mth.sin(Mth.clamp(age / 36.0F, 0.0F, 1.0F) * (float)Math.PI * 0.5F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -0.95F - rise * 0.45F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -0.95F - rise * 0.45F);
               this.rightArm.zRot = Mth.lerp(pose, this.rightArm.zRot, -0.3F - rise * 0.28F);
               this.leftArm.zRot = Mth.lerp(pose, this.leftArm.zRot, 0.3F + rise * 0.28F);
               this.body.xRot -= pose * 0.06F;
            }
            case "np_three_thousand_release" -> {
               float pose = actionEnvelope(age, 22.0F, 2.0F, 5.0F);
               float release = pulse(age, 7.0F, 6.0F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -1.35F + release * 0.3F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -1.35F + release * 0.3F);
               this.rightArm.yRot = Mth.lerp(pose, this.rightArm.yRot, -0.14F);
               this.leftArm.yRot = Mth.lerp(pose, this.leftArm.yRot, 0.14F);
               this.body.xRot -= release * 0.12F;
            }
            case "np_hajun_chant" -> {
               float pose = actionEnvelope(age, 112.0F, 8.0F, 8.0F);
               float spread = Mth.sin(Mth.clamp(age / 92.0F, 0.0F, 1.0F) * (float)Math.PI * 0.5F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -0.48F - spread * 0.52F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -0.48F - spread * 0.52F);
               this.rightArm.zRot = Mth.lerp(pose, this.rightArm.zRot, -0.28F - spread * 0.7F);
               this.leftArm.zRot = Mth.lerp(pose, this.leftArm.zRot, 0.28F + spread * 0.7F);
               this.body.xRot += pose * 0.09F;
               this.head.xRot -= pose * 0.1F;
            }
            case "np_hajun_release" -> {
               float pose = actionEnvelope(age, 32.0F, 3.0F, 6.0F);
               float release = pulse(age, 8.0F, 7.0F);
               this.rightArm.xRot = Mth.lerp(pose, this.rightArm.xRot, -0.3F + release * 0.38F);
               this.leftArm.xRot = Mth.lerp(pose, this.leftArm.xRot, -0.3F + release * 0.38F);
               this.rightArm.zRot = Mth.lerp(pose, this.rightArm.zRot, -0.8F);
               this.leftArm.zRot = Mth.lerp(pose, this.leftArm.zRot, 0.8F);
               this.body.xRot -= release * 0.18F;
            }
            default -> { }
         }
      }

      private static float actionEnvelope(float age, float duration, float fadeIn, float fadeOut) {
         return Math.min(Mth.clamp(age / fadeIn, 0.0F, 1.0F), Mth.clamp((duration - age) / fadeOut, 0.0F, 1.0F));
      }

      private static float pulse(float age, float center, float width) {
         return Mth.clamp(1.0F - Math.abs(age - center) / width, 0.0F, 1.0F);
      }

      private static boolean isJeanneAlter(String servantId) {
         if (servantId == null) return false;
         int separator = servantId.indexOf(':');
         return "jeanne_alter".equals(separator >= 0 ? servantId.substring(separator + 1) : servantId);
      }
   }
}

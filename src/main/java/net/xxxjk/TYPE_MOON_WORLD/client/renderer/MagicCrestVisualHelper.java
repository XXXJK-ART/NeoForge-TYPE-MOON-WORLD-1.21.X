package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class MagicCrestVisualHelper {
   public static final ResourceLocation CREST_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/magic_crest_64.png");
   private static final int EMISSIVE_LIGHT = 15728880;
   private static final int CREST_COLOR = -285212673;

   private MagicCrestVisualHelper() {
   }

   public static boolean shouldRenderAnyCrestArm(AbstractClientPlayer player) {
      return shouldRenderCrestArm(player, HumanoidArm.LEFT) || shouldRenderCrestArm(player, HumanoidArm.RIGHT);
   }

   public static boolean shouldRenderCrestArm(AbstractClientPlayer player, HumanoidArm armSide) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null || minecraft.player.getId() != player.getId()) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars == null || !vars.isCurrentSelectionFromCrest()) {
         return false;
      }

      boolean activePose = TypeMoonWorldModKeyMappings.KeyEventListener.isLocalTapCastPoseActive()
         || TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGanderCharging()
         || TypeMoonWorldModKeyMappings.KeyEventListener.isLocalGandrMachineGunCasting()
         || TypeMoonWorldModKeyMappings.KeyEventListener.isLocalMachineGunFiringPoseActive();
      return activePose && TypeMoonWorldModKeyMappings.KeyEventListener.getLocalCastingArm() == armSide;
   }

   public static boolean shouldRenderCrestArm(MysticMagicianEntity entity, HumanoidArm armSide) {
      if (entity == null || !entity.isCastingPoseActive()) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars != null && vars.isCurrentSelectionFromCrest() && entity.getMainArm() == armSide;
   }

   public static void renderArm(PlayerModel<AbstractClientPlayer> model, MultiBufferSource buffer, HumanoidArm armSide, com.mojang.blaze3d.vertex.PoseStack poseStack) {
      var consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(CREST_TEXTURE));
      if (armSide == HumanoidArm.RIGHT) {
         model.rightArm.render(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, CREST_COLOR);
         model.rightSleeve.render(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, CREST_COLOR);
      } else {
         model.leftArm.render(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, CREST_COLOR);
         model.leftSleeve.render(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, CREST_COLOR);
      }
   }

   public static void renderNpcArm(PlayerModel<MysticMagicianEntity> model, MultiBufferSource buffer, HumanoidArm armSide, com.mojang.blaze3d.vertex.PoseStack poseStack) {
      var consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(CREST_TEXTURE));
      if (armSide == HumanoidArm.RIGHT) {
         model.rightArm.render(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, CREST_COLOR);
         model.rightSleeve.render(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, CREST_COLOR);
      } else {
         model.leftArm.render(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, CREST_COLOR);
         model.leftSleeve.render(poseStack, consumer, EMISSIVE_LIGHT, OverlayTexture.NO_OVERLAY, CREST_COLOR);
      }
   }
}

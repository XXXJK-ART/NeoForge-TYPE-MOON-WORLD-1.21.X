package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.client.CommandSpellVisualClient;

public final class CommandSpellMarkRenderer {
   private static final int EMISSIVE_LIGHT = 15728880;
   private static final ResourceLocation COMMAND_SPELL_3 = texture("command_spell_3");
   private static final ResourceLocation COMMAND_SPELL_2 = texture("command_spell_2");
   private static final ResourceLocation COMMAND_SPELL_1 = texture("command_spell_1");
   private static final ResourceLocation COMMAND_SPELL_0 = texture("command_spell_0");

   private static final float HAND_BACK_X = -0.191F;
   private static final float HAND_BACK_Y_MIN = 0.31F;
   private static final float HAND_BACK_Y_MAX = 0.69F;
   private static final float HAND_BACK_Z_MIN = -0.19F;
   private static final float HAND_BACK_Z_MAX = 0.19F;

   private CommandSpellMarkRenderer() {
   }

   public static void renderRightHandMark(
      PlayerModel<AbstractClientPlayer> model,
      AbstractClientPlayer player,
      PoseStack poseStack,
      MultiBufferSource buffer
   ) {
      int count = CommandSpellVisualClient.getCommandSpellCount(player);
      if (count < 0 || player.isInvisible()) {
         return;
      }
      renderRightHandMark(model, poseStack, buffer, count);
   }

   public static void renderRightHandMark(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, int commandSpells) {
      ResourceLocation texture = textureFor(commandSpells);
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
      poseStack.pushPose();
      model.rightArm.translateAndRotate(poseStack);
      drawDoubleSidedQuad(poseStack, consumer);
      poseStack.popPose();
   }

   private static void drawDoubleSidedQuad(PoseStack poseStack, VertexConsumer consumer) {
      vertex(consumer, poseStack, HAND_BACK_X, HAND_BACK_Y_MIN, HAND_BACK_Z_MIN, 0.0F, 1.0F);
      vertex(consumer, poseStack, HAND_BACK_X, HAND_BACK_Y_MIN, HAND_BACK_Z_MAX, 1.0F, 1.0F);
      vertex(consumer, poseStack, HAND_BACK_X, HAND_BACK_Y_MAX, HAND_BACK_Z_MAX, 1.0F, 0.0F);
      vertex(consumer, poseStack, HAND_BACK_X, HAND_BACK_Y_MAX, HAND_BACK_Z_MIN, 0.0F, 0.0F);

      vertex(consumer, poseStack, HAND_BACK_X, HAND_BACK_Y_MAX, HAND_BACK_Z_MIN, 0.0F, 0.0F);
      vertex(consumer, poseStack, HAND_BACK_X, HAND_BACK_Y_MAX, HAND_BACK_Z_MAX, 1.0F, 0.0F);
      vertex(consumer, poseStack, HAND_BACK_X, HAND_BACK_Y_MIN, HAND_BACK_Z_MAX, 1.0F, 1.0F);
      vertex(consumer, poseStack, HAND_BACK_X, HAND_BACK_Y_MIN, HAND_BACK_Z_MIN, 0.0F, 1.0F);
   }

   private static void vertex(VertexConsumer consumer, PoseStack poseStack, float x, float y, float z, float u, float v) {
      consumer.addVertex(poseStack.last(), x, y, z)
         .setColor(1.0F, 1.0F, 1.0F, 0.92F)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(EMISSIVE_LIGHT)
         .setNormal(-1.0F, 0.0F, 0.0F);
   }

   private static ResourceLocation textureFor(int commandSpells) {
      if (commandSpells >= 3) {
         return COMMAND_SPELL_3;
      }
      if (commandSpells == 2) {
         return COMMAND_SPELL_2;
      }
      if (commandSpells == 1) {
         return COMMAND_SPELL_1;
      }
      return COMMAND_SPELL_0;
   }

   private static ResourceLocation texture(String name) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/command_spell/" + name + ".png");
   }
}

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
      renderRightHandMark(model, poseStack, buffer, count, CommandSpellVisualClient.getCommandSpellStyle(player));
   }

   public static void renderRightHandMark(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, int commandSpells) {
      renderRightHandMark(model, poseStack, buffer, commandSpells, "default");
   }

   public static void renderRightHandMark(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffer, int commandSpells, String style) {
      ResourceLocation texture = textureFor(commandSpells, style);
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
      poseStack.pushPose();
      if ("elsa_saijo".equals(style)) {
         model.body.translateAndRotate(poseStack);
         drawChestQuad(poseStack, consumer);
      } else {
         model.rightArm.translateAndRotate(poseStack);
         drawDoubleSidedQuad(poseStack, consumer, "supervisor".equals(style));
      }
      poseStack.popPose();
   }

   private static void drawDoubleSidedQuad(PoseStack poseStack, VertexConsumer consumer, boolean fullArm) {
      float minY = fullArm ? 0.02F : HAND_BACK_Y_MIN;
      float maxY = fullArm ? 0.98F : HAND_BACK_Y_MAX;
      vertex(consumer, poseStack, HAND_BACK_X, minY, HAND_BACK_Z_MIN, 0.0F, 1.0F);
      vertex(consumer, poseStack, HAND_BACK_X, minY, HAND_BACK_Z_MAX, 1.0F, 1.0F);
      vertex(consumer, poseStack, HAND_BACK_X, maxY, HAND_BACK_Z_MAX, 1.0F, 0.0F);
      vertex(consumer, poseStack, HAND_BACK_X, maxY, HAND_BACK_Z_MIN, 0.0F, 0.0F);

      vertex(consumer, poseStack, HAND_BACK_X, maxY, HAND_BACK_Z_MIN, 0.0F, 0.0F);
      vertex(consumer, poseStack, HAND_BACK_X, maxY, HAND_BACK_Z_MAX, 1.0F, 0.0F);
      vertex(consumer, poseStack, HAND_BACK_X, minY, HAND_BACK_Z_MAX, 1.0F, 1.0F);
      vertex(consumer, poseStack, HAND_BACK_X, minY, HAND_BACK_Z_MIN, 0.0F, 1.0F);
   }

   private static void drawChestQuad(PoseStack poseStack, VertexConsumer consumer) {
      float z = -0.255F;
      vertex(consumer, poseStack, -0.28F, 0.18F, z, 0.0F, 0.0F);
      vertex(consumer, poseStack, 0.28F, 0.18F, z, 1.0F, 0.0F);
      vertex(consumer, poseStack, 0.28F, 0.82F, z, 1.0F, 1.0F);
      vertex(consumer, poseStack, -0.28F, 0.82F, z, 0.0F, 1.0F);
   }

   private static void vertex(VertexConsumer consumer, PoseStack poseStack, float x, float y, float z, float u, float v) {
      consumer.addVertex(poseStack.last(), x, y, z)
         .setColor(1.0F, 1.0F, 1.0F, 0.92F)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(EMISSIVE_LIGHT)
         .setNormal(-1.0F, 0.0F, 0.0F);
   }

   private static ResourceLocation textureFor(int commandSpells, String style) {
      String safeStyle = sanitizeStyle(style);
      if ("supervisor".equals(safeStyle)) {
         int stage = 12 - Math.max(0, Math.min(11, commandSpells));
         return texture(safeStyle, Integer.toString(stage));
      }
      if ("elsa_saijo".equals(safeStyle)) {
         int stage = 4 - Math.max(0, Math.min(3, commandSpells));
         return texture(safeStyle, Integer.toString(stage));
      }
      String suffix;
      if (commandSpells >= 3) {
         suffix = "3";
      } else if (commandSpells == 2) {
         suffix = "2";
      } else if (commandSpells == 1) {
         suffix = "1";
      } else {
         suffix = "0";
      }
      return texture(safeStyle, suffix);
   }

   private static String sanitizeStyle(String style) {
      if (style == null || style.isBlank()) {
         return "default";
      }
      String value = style.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "");
      return value.isBlank() ? "default" : value;
   }

   private static ResourceLocation texture(String style, String count) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/command_spell/" + style + "/command_spell_" + count + ".png");
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ExcaliburModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ExcaliburRenderer extends GeoItemRenderer<ExcaliburItem> {
   public ExcaliburRenderer() {
      super(new ExcaliburModel());
   }

   @Override
   public RenderType getRenderType(ExcaliburItem animatable, net.minecraft.resources.ResourceLocation texture,
                                  @Nullable MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucentEmissive(texture);
   }

   @Override
   public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (shouldHidePlantedWindDefenseSword(stack, displayContext)) return;
      if (shouldHideWindVeiledExcalibur(stack, displayContext)) {
         renderVeiledBlade(poseStack, bufferSource);
         return;
      }
      super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
      if (isManaBurstBlade(stack)) {
         float time = Minecraft.getInstance().level == null ? 0.0F
            : (Minecraft.getInstance().level.getGameTime()
               + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)) * 0.18F;
         renderGoldenBlade(poseStack, bufferSource, time);
      }
   }

   /** A restrained item-space silhouette shared by first- and third-person views. */
   private static void renderVeiledBlade(PoseStack poses, MultiBufferSource buffers) {
      VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/particle/particle_white.png")));
      poses.pushPose();
      poses.translate(.5, .51, .5);
      int segments = 20;
      for (int i = 0; i < segments; i++) {
         float t0 = i / (float)segments, t1 = (i + 1) / (float)segments;
         float x0 = .10F + t0 * .90F, x1 = .10F + t1 * .90F;
         float w0 = .018F + (float)Math.sin(Math.PI * t0) * .06F;
         float w1 = .018F + (float)Math.sin(Math.PI * t1) * .06F;
         veiledFace(poses.last(), buffer, x0, x1, w0, w1, .052F, 0, 1, .16F);
         veiledFace(poses.last(), buffer, x0, x1, w0, w1, -.052F, 1, 0, .16F);
         veiledEdge(poses.last(), buffer, x0, -w0, x1, -w1, .052F);
         veiledEdge(poses.last(), buffer, x0, w0, x1, w1, .052F);
         veiledEdge(poses.last(), buffer, x0, -w0, x1, -w1, -.052F);
         veiledEdge(poses.last(), buffer, x0, w0, x1, w1, -.052F);
      }
      poses.popPose();
   }

   private static void renderGoldenBlade(PoseStack poses, MultiBufferSource buffers, float time) {
      VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/particle/beam_gradient.png")));
      poses.pushPose();
      poses.translate(.5, .51, .5);
      int segments = 24;
      for (int i = 0; i < segments; i++) {
         float t0 = i / (float)segments, t1 = (i + 1) / (float)segments;
         float x0 = .09F + t0 * .94F, x1 = .09F + t1 * .94F;
         float w0 = .028F + (float)Math.sin(Math.PI * t0) * .075F;
         float w1 = .028F + (float)Math.sin(Math.PI * t1) * .075F;
         goldenFace(poses.last(), buffer, x0, x1, w0, w1, .06F, 0, 1, .62F);
         goldenFace(poses.last(), buffer, x0, x1, w0, w1, -.06F, 1, 0, .62F);
         goldenEdge(poses.last(), buffer, x0, -w0, x1, -w1, .06F);
         goldenEdge(poses.last(), buffer, x0, w0, x1, w1, .06F);
      }
      for (int strand = 0; strand < 3; strand++) {
         for (int i = 0; i < 30; i++) {
            float t0 = i / 30.0F, t1 = (i + 1) / 30.0F;
            double p0 = time - t0 * 9.0 + strand * Math.PI * .67;
            double p1 = time - t1 * 9.0 + strand * Math.PI * .67;
            goldenRibbon(poses.last(), buffer, .08F + t0 * .98F, .08F + t1 * .98F,
               (float)Math.sin(p0) * .08F, (float)Math.sin(p1) * .08F,
               .072F + (float)Math.cos(p0) * .015F, .072F + (float)Math.cos(p1) * .015F);
         }
      }
      poses.popPose();
   }

   private static void veiledFace(PoseStack.Pose pose, VertexConsumer buffer, float x0, float x1,
                                  float w0, float w1, float z, float u0, float u1, float alpha) {
      veiledVertex(pose, buffer, x0, -w0, z, 0, u0, alpha);
      veiledVertex(pose, buffer, x0, w0, z, 0, u1, alpha);
      veiledVertex(pose, buffer, x1, w1, z, 1, u1, alpha);
      veiledVertex(pose, buffer, x1, -w1, z, 1, u0, alpha);
   }

   private static void veiledEdge(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0,
                                  float x1, float y1, float z) {
      veiledVertex(pose, buffer, x0, y0 - .005F, z, 0, 0, .54F);
      veiledVertex(pose, buffer, x0, y0 + .005F, z, 0, 1, .54F);
      veiledVertex(pose, buffer, x1, y1 + .005F, z, 1, 1, .54F);
      veiledVertex(pose, buffer, x1, y1 - .005F, z, 1, 0, .54F);
   }

   private static void goldenFace(PoseStack.Pose pose, VertexConsumer buffer, float x0, float x1,
                                  float w0, float w1, float z, float u0, float u1, float alpha) {
      goldenVertex(pose, buffer, x0, -w0, z, 0, u0, alpha);
      goldenVertex(pose, buffer, x0, w0, z, 0, u1, alpha);
      goldenVertex(pose, buffer, x1, w1, z, 1, u1, alpha);
      goldenVertex(pose, buffer, x1, -w1, z, 1, u0, alpha);
   }

   private static void goldenEdge(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0,
                                  float x1, float y1, float z) {
      goldenVertex(pose, buffer, x0, y0 - .007F, z, 0, 0, .86F);
      goldenVertex(pose, buffer, x0, y0 + .007F, z, 0, 1, .86F);
      goldenVertex(pose, buffer, x1, y1 + .007F, z, 1, 1, .86F);
      goldenVertex(pose, buffer, x1, y1 - .007F, z, 1, 0, .86F);
   }

   private static void goldenRibbon(PoseStack.Pose pose, VertexConsumer buffer, float x0, float x1,
                                    float y0, float y1, float z0, float z1) {
      goldenVertex(pose, buffer, x0, y0 - .008F, z0, 0, 0, .72F);
      goldenVertex(pose, buffer, x0, y0 + .008F, z0, 0, 1, .72F);
      goldenVertex(pose, buffer, x1, y1 + .008F, z1, 1, 1, .72F);
      goldenVertex(pose, buffer, x1, y1 - .008F, z1, 1, 0, .72F);
   }

   private static void veiledVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                                    float u, float v, float alpha) {
      buffer.addVertex(pose, x, y, z).setColor(.40F, .76F, 1.0F, alpha).setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0, 1, 0);
   }

   private static void goldenVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                                    float u, float v, float alpha) {
      buffer.addVertex(pose, x, y, z).setColor(1.0F, .65F, .08F, alpha).setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0, 1, 0);
   }

   private static boolean isManaBurstBlade(ItemStack stack) {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      return customData != null && customData.copyTag().getBoolean("ArtoriaManaBurstActive");
   }

   private static boolean shouldHidePlantedWindDefenseSword(ItemStack stack, ItemDisplayContext displayContext) {
      if (!isHandDisplay(displayContext)) return false;
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      return customData != null && customData.copyTag().getBoolean("ArtoriaWindDefenseActive");
   }

   private static void windVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                                  float u, float v, float alpha) {
      buffer.addVertex(pose, x, y, z).setColor(.63F, .88F, 1.0F, alpha).setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0, 1, 0);
   }

   private static boolean shouldHideWindVeiledExcalibur(ItemStack stack, ItemDisplayContext displayContext) {
      if (!isHandDisplay(displayContext)) {
         return false;
      }
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return false;
      }
      var tag = customData.copyTag();
      if (!tag.getBoolean("ServantCardArtoriaWindVeiled")) {
         return false;
      }
      if (Minecraft.getInstance().player == null) {
         return false;
      }
      long revealUntil = tag.getLong("ServantCardArtoriaWindRevealUntil");
      if (revealUntil > Minecraft.getInstance().player.level().getGameTime()) {
         return false;
      }
      return true;
   }

   private static boolean isHandDisplay(ItemDisplayContext displayContext) {
      return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
         || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
         || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
         || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
   }
}

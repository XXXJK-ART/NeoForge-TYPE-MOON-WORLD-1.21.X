package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import net.minecraft.world.phys.Vec3;

public final class ArashParticleArrowRenderer extends EntityRenderer<ArashParticleArrowEntity> {
   private static final ResourceLocation LIGHT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
   private static final ResourceLocation CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/particle/particle_white.png");
   public ArashParticleArrowRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(ArashParticleArrowEntity entity, float yaw, float partialTick,
         PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      float progress = Math.min(1.0F, entity.getFlightTicks() / (float)ArashCombatRules.ARROW_MAX_FLIGHT_TICKS);
      if (entity.isArashSpecial()) {
         renderEnergy(entity, progress, partialTick, poseStack, buffers);
         super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
         return;
      }
      ItemStack stack = entity.getItem();
      if (stack.isEmpty()) return;
      poseStack.pushPose();
      float interpolatedYaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTick;
      float interpolatedPitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTick;
      poseStack.mulPose(Axis.YP.rotationDegrees(interpolatedYaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-interpolatedPitch));
      poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
      float size = 1.35F;
      poseStack.scale(size, size, size);
      Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE,
         packedLight,
         OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
      poseStack.popPose();
      super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
   }

   private void renderEnergy(ArashParticleArrowEntity entity, float progress, float partialTick,
         PoseStack poses, MultiBufferSource buffers) {
      int color = ArashCombatRules.arrowColor(entity.getFlightTicks());
      float red = ((color >> 16) & 255) / 255.0F;
      float green = ((color >> 8) & 255) / 255.0F;
      float blue = (color & 255) / 255.0F;
      float variantScale = entity.getVariant() == ArashParticleArrowEntity.LARGE_ENERGY ? 1.6F
         : entity.getVariant() == ArashParticleArrowEntity.RAIN ? 0.65F : 1.0F;
      float size = (1.0F + progress * 1.8F) * variantScale;
      poses.pushPose();
      // Local +Z follows the velocity, including steeply falling rain. No camera-facing core.
      Vec3 motion = entity.getDeltaMovement();
      float flightYaw = motion.lengthSqr() > 1.0E-8
         ? (float)(Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG)
         : Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
      float flightPitch = motion.lengthSqr() > 1.0E-8
         ? (float)(Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG)
         : Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
      poses.mulPose(Axis.YP.rotationDegrees(flightYaw));
      poses.mulPose(Axis.XP.rotationDegrees(-flightPitch));
      poses.scale(size, size, size);
      var consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(CORE_TEXTURE));
      drawSpindle(poses.last(), consumer, -1.15F, -0.9F, 0.42F, 0.07F,
         red * 0.7F + 0.3F, green * 0.7F + 0.3F, blue * 0.7F + 0.3F, 1.0F);
      drawSpindle(poses.last(), consumer, 0.12F, 0.28F, 0.9F, 0.23F, red, green, blue, 1.0F);
      drawSpindle(poses.last(), consumer, -1.35F, 0.1F, 1.05F, 0.32F,
         red, green, blue, 0.18F + progress * 0.22F);
      poses.popPose();
      ProjectileVisualEffectHelper.renderRibbonTrail(entity.getFlightTrace(), entity.getPosition(partialTick),
         this.entityRenderDispatcher.camera.getPosition(), poses, buffers, LIGHT_TEXTURE,
         size * 0.24F, color, 0.55F + progress * 0.45F);
   }

   /** Closed eight-sided volume: a bright shaft, pointed head and translucent outer light. */
   private static void drawSpindle(PoseStack.Pose pose, VertexConsumer consumer,
         float back, float middle, float front, float radius, float r, float g, float b, float alpha) {
      for (int i = 0; i < 8; i++) {
         double angle = i * Math.PI / 4.0;
         double next = (i + 1) * Math.PI / 4.0;
         float x1 = (float)Math.cos(angle) * radius, y1 = (float)Math.sin(angle) * radius;
         float x2 = (float)Math.cos(next) * radius, y2 = (float)Math.sin(next) * radius;
         float shade = 0.78F + 0.22F * (float)Math.abs(Math.cos(angle));
         vertex(pose, consumer, 0, 0, back, r * shade, g * shade, b, alpha);
         vertex(pose, consumer, x2, y2, middle, r * shade, g * shade, b, alpha);
         vertex(pose, consumer, x1, y1, middle, r * shade, g * shade, b, alpha);
         vertex(pose, consumer, 0, 0, back, r * shade, g * shade, b, alpha);
         vertex(pose, consumer, x1, y1, middle, r * shade, g * shade, b, alpha);
         vertex(pose, consumer, x2, y2, middle, r * shade, g * shade, b, alpha);
         vertex(pose, consumer, 0, 0, front, r * shade, g * shade, b, alpha);
         vertex(pose, consumer, 0, 0, front, r * shade, g * shade, b, alpha);
      }
   }

   private static void vertex(PoseStack.Pose pose, VertexConsumer consumer,
         float x, float y, float z, float r, float g, float b, float alpha) {
      consumer.addVertex(pose, x, y, z).setColor(r, g, b, alpha).setUv(0.5F, 0.5F)
         .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(ArashParticleArrowEntity entity) {
      return entity.isArashSpecial() ? CORE_TEXTURE : InventoryMenu.BLOCK_ATLAS;
   }
}

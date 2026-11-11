package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;

public final class ArashParticleArrowRenderer extends EntityRenderer<ArashParticleArrowEntity> {
   private static final ResourceLocation LIGHT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
   public ArashParticleArrowRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public void render(ArashParticleArrowEntity entity, float yaw, float partialTick,
         PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      ItemStack stack = entity.getItem();
      if (stack.isEmpty()) return;
      poseStack.pushPose();
      float interpolatedYaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTick;
      float interpolatedPitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTick;
      poseStack.mulPose(Axis.YP.rotationDegrees(interpolatedYaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-interpolatedPitch));
      poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
      float progress = Math.min(1.0F, entity.getFlightTicks() / 240.0F);
      float size = entity.isArashSpecial() ? 1.35F + progress * 1.2F : 1.35F;
      poseStack.scale(size, size, size);
      Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE,
         entity.isArashSpecial() ? LightTexture.FULL_BRIGHT : packedLight,
         OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
      poseStack.popPose();
      if (entity.isArashSpecial()) renderEnergy(entity, progress, partialTick, poseStack, buffers);
      super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
   }

   private void renderEnergy(ArashParticleArrowEntity entity, float progress, float partialTick,
         PoseStack poses, MultiBufferSource buffers) {
      int color = progress < 0.34F ? 0xFFFFFF : progress < 0.67F ? 0x33FF6B : 0xFF1505;
      float red = ((color >> 16) & 255) / 255.0F;
      float green = ((color >> 8) & 255) / 255.0F;
      float blue = (color & 255) / 255.0F;
      float variantScale = entity.getVariant() == ArashParticleArrowEntity.LARGE_ENERGY ? 1.6F
         : entity.getVariant() == ArashParticleArrowEntity.RAIN ? 0.65F : 1.0F;
      float radius = (0.16F + progress * 0.64F) * variantScale;
      float alpha = 0.4F + progress * 0.5F;
      poses.pushPose();
      poses.mulPose(this.entityRenderDispatcher.cameraOrientation());
      var consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(LIGHT_TEXTURE));
      ProjectileVisualEffectHelper.drawCenteredQuad(poses.last(), consumer, radius, radius,
         red, green, blue, alpha);
      poses.translate(0.0, 0.0, 0.002);
      ProjectileVisualEffectHelper.drawCenteredQuad(poses.last(), consumer, radius * 0.32F, radius * 0.32F,
         1.0F, 1.0F, 1.0F, 0.9F);
      poses.popPose();
      ProjectileVisualEffectHelper.renderRibbonTrail(entity.getFlightTrace(), entity.getPosition(partialTick),
         this.entityRenderDispatcher.camera.getPosition(), poses, buffers, LIGHT_TEXTURE,
         radius * 1.5F, color, 0.55F + progress * 0.45F);
   }

   @Override
   public ResourceLocation getTextureLocation(ArashParticleArrowEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

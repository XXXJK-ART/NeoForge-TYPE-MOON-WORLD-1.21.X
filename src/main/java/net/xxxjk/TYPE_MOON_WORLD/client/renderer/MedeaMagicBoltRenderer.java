package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public class MedeaMagicBoltRenderer extends EntityRenderer<MedeaMagicBoltEntity> {
   private final ItemRenderer itemRenderer;

   public MedeaMagicBoltRenderer(Context context) {
      super(context);
      this.itemRenderer = context.getItemRenderer();
   }

   @Override
   public void render(MedeaMagicBoltEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      if (entity.getMode() == MedeaMagicBoltEntity.Mode.RULE_BREAKER) {
         poseStack.scale(0.7F, 0.7F, 0.7F);
         poseStack.mulPose(Axis.YP.rotationDegrees(entity.tickCount * 20.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
         this.itemRenderer.renderStatic(new ItemStack(ModItems.RULE_BREAKER.get()), ItemDisplayContext.GROUND, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
      } else {
         float scale = switch (entity.getMode()) {
            case SUPER_BOLT -> 1.45F;
            case FIRE_BOLT, FROST_BOLT -> 1.05F;
            default -> 0.9F;
         };
         float[] primary = getPrimaryColor(entity);
         float[] accent = getAccentColor(entity);
         poseStack.scale(scale, scale, scale);
         poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         com.mojang.blaze3d.vertex.VertexConsumer consumer = buffer.getBuffer(GanderOrbRenderType.orb());
         com.mojang.blaze3d.vertex.PoseStack.Pose pose = poseStack.last();
         drawOrbQuad(pose, consumer, entity.getMode() == MedeaMagicBoltEntity.Mode.SUPER_BOLT ? 0.18F : 0.12F, primary[0], primary[1], primary[2], 0.96F);
         drawOrbQuad(pose, consumer, entity.getMode() == MedeaMagicBoltEntity.Mode.SUPER_BOLT ? 0.3F : 0.22F, accent[0], accent[1], accent[2], 0.78F);
      }
      poseStack.popPose();

      if (entity.tracePos.size() >= 2) {
         Vec3 currentPos = entity.getPosition(partialTicks);
         Vec3 cameraPos = this.entityRenderDispatcher.camera.getPosition();
         poseStack.pushPose();
         ProjectileVisualEffectHelper.renderRibbonTrail(
            entity.tracePos,
            currentPos,
            cameraPos,
            poseStack,
            buffer,
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png"),
            getTrailWidth(entity),
            getTrailColor(entity),
            0.8F
         );
         poseStack.popPose();
      }
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private void drawOrbQuad(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, float halfSize, float r, float g, float b, float a) {
      consumer.addVertex(pose, -halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F);
      consumer.addVertex(pose, halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F);
      consumer.addVertex(pose, halfSize, halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F);
      consumer.addVertex(pose, -halfSize, halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(MedeaMagicBoltEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   private float getTrailWidth(MedeaMagicBoltEntity entity) {
      return switch (entity.getMode()) {
         case RULE_BREAKER -> 0.18F;
         case SUPER_BOLT -> 0.38F;
         case FIRE_BOLT, FROST_BOLT -> 0.3F;
         default -> 0.24F;
      };
   }

   private int getTrailColor(MedeaMagicBoltEntity entity) {
      return switch (entity.getMode()) {
         case RULE_BREAKER -> 0xC3A0FF;
         case SUPER_BOLT -> 0xC7EBFF;
         case FIRE_BOLT -> 0xFF7A33;
         case FROST_BOLT -> 0x9FEFFF;
         default -> 0x6A7DFF;
      };
   }

   private float[] getPrimaryColor(MedeaMagicBoltEntity entity) {
      return switch (entity.getMode()) {
         case SUPER_BOLT -> new float[]{0.72F, 0.9F, 1.0F};
         case FIRE_BOLT -> new float[]{1.0F, 0.48F, 0.18F};
         case FROST_BOLT -> new float[]{0.72F, 0.95F, 1.0F};
         default -> new float[]{0.12F, 0.18F, 0.72F};
      };
   }

   private float[] getAccentColor(MedeaMagicBoltEntity entity) {
      return switch (entity.getMode()) {
         case SUPER_BOLT -> new float[]{0.55F, 0.78F, 1.0F};
         case FIRE_BOLT -> new float[]{1.0F, 0.76F, 0.22F};
         case FROST_BOLT -> new float[]{0.46F, 0.76F, 1.0F};
         default -> new float[]{0.22F, 0.58F, 0.24F};
      };
   }
}

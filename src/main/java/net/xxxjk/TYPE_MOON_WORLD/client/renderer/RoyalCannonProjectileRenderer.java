package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoyalCannonProjectileEntity;

public final class RoyalCannonProjectileRenderer extends EntityRenderer<RoyalCannonProjectileEntity> {
   private static final float GOLD_R = 1.0F;
   private static final float GOLD_G = 0.74F;
   private static final float GOLD_B = 0.16F;
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public RoyalCannonProjectileRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(RoyalCannonProjectileEntity entity, float entityYaw, float partialTick,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float scale = entity.getVisualScale();
      float time = entity.level().getGameTime() + partialTick;
      poseStack.pushPose();
      poseStack.scale(scale, scale, scale);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(time * 5.5F));
      VertexConsumer consumer = buffer.getBuffer(GanderOrbRenderType.orb());
      PoseStack.Pose pose = poseStack.last();
      drawOrbQuad(pose, consumer, entity.isExplosiveVisual() ? 0.16F : 0.11F, 1.0F, 0.98F, 0.76F, 0.96F);
      drawOrbQuad(pose, consumer, entity.isExplosiveVisual() ? 0.29F : 0.21F, GOLD_R, GOLD_G, GOLD_B, 0.80F);
      drawOrbQuad(pose, consumer, entity.isExplosiveVisual() ? 0.40F : 0.30F, 1.0F, 0.86F, 0.25F, 0.36F);
      poseStack.popPose();

      if (entity.tracePos.size() >= 2) {
         Vec3 currentPos = entity.getPosition(partialTick);
         Vec3 cameraPos = this.entityRenderDispatcher.camera.getPosition();
         poseStack.pushPose();
         ProjectileVisualEffectHelper.renderRibbonTrail(
            entity.tracePos,
            currentPos,
            cameraPos,
            poseStack,
            buffer,
            TRAIL_TEXTURE,
            entity.isExplosiveVisual() ? 0.34F : 0.22F,
            entity.isExplosiveVisual() ? 0xFFE66A : 0xFFD85A,
            0.82F
         );
         poseStack.popPose();
      }
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   private void drawOrbQuad(PoseStack.Pose pose, VertexConsumer consumer, float halfSize, float r, float g, float b, float a) {
      consumer.addVertex(pose, -halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F);
      consumer.addVertex(pose, halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F);
      consumer.addVertex(pose, halfSize, halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F);
      consumer.addVertex(pose, -halfSize, halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(RoyalCannonProjectileEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}

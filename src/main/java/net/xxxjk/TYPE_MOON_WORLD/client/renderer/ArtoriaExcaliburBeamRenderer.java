package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;

public class ArtoriaExcaliburBeamRenderer extends EntityRenderer<ArtoriaExcaliburBeamEntity> {
   private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public ArtoriaExcaliburBeamRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(ArtoriaExcaliburBeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Vec3 start = entity.position();
      Vec3 end = entity.getEndPos();
      Vec3 currentPos = entity.getPosition(partialTicks);
      Vec3 cameraPos = this.entityRenderDispatcher.camera.getPosition();
      poseStack.pushPose();
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM_TEXTURE));
      Vec3 localStart = start.subtract(currentPos);
      Vec3 localEnd = end.subtract(currentPos);
      Vec3 view = cameraPos.subtract(currentPos);
      float pulse = 0.88F + 0.12F * (float)Math.sin((entity.tickCount + partialTicks) * 0.45F);
      drawLayer(poseStack.last(), consumer, localStart, localEnd, view, 10.0F * pulse, 1.0F, 0.62F, 0.05F, 0.28F);
      drawLayer(poseStack.last(), consumer, localStart, localEnd, view, 7.5F * pulse, 1.0F, 0.78F, 0.12F, 0.48F);
      drawLayer(poseStack.last(), consumer, localStart, localEnd, view, 5.0F, 1.0F, 0.92F, 0.35F, 0.76F);
      drawLayer(poseStack.last(), consumer, localStart, localEnd, view, 3.0F, 1.0F, 1.0F, 0.76F, 0.96F);
      drawLayer(poseStack.last(), consumer, localStart, localEnd, view, 1.5F, 1.0F, 1.0F, 1.0F, 1.0F);
      drawCrossLayer(poseStack.last(), consumer, localStart, localEnd, 10.0F * pulse, 1.0F, 0.82F, 0.2F, 0.34F);
      drawCrossLayer(poseStack.last(), consumer, localStart, localEnd, 5.0F, 1.0F, 1.0F, 0.65F, 0.58F);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private void drawLayer(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 viewOffset, float width, float r, float g, float b, float a) {
      Vec3 dir = end.subtract(start);
      if (dir.lengthSqr() < 1.0E-6) {
         return;
      }
      Vec3 viewDir = start.subtract(viewOffset);
      Vec3 cross = dir.cross(viewDir);
      if (cross.lengthSqr() < 1.0E-6) {
         cross = dir.cross(new Vec3(0.0, 1.0, 0.0));
      }
      Vec3 right = cross.normalize().scale(width * 0.5F);
      Vec3 v0 = start.subtract(right);
      Vec3 v1 = start.add(right);
      Vec3 v2 = end.subtract(right.scale(0.45));
      Vec3 v3 = end.add(right.scale(0.45));
      vertex(pose, consumer, v0, r, g, b, a, 0.0F, 0.0F);
      vertex(pose, consumer, v2, r, g, b, a, 1.0F, 0.0F);
      vertex(pose, consumer, v3, r, g, b, a, 1.0F, 1.0F);
      vertex(pose, consumer, v1, r, g, b, a, 0.0F, 1.0F);
   }

   private void drawCrossLayer(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, float width, float r, float g, float b, float a) {
      Vec3 dir = end.subtract(start);
      if (dir.lengthSqr() < 1.0E-6) {
         return;
      }
      Vec3 forward = dir.normalize();
      Vec3 worldUp = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(worldUp);
      if (right.lengthSqr() < 1.0E-6) {
         right = forward.cross(new Vec3(1.0, 0.0, 0.0));
      }
      right = right.normalize().scale(width * 0.5F);
      Vec3 up = right.cross(forward);
      if (up.lengthSqr() < 1.0E-6) {
         up = new Vec3(0.0, width * 0.28F, 0.0);
      } else {
         up = up.normalize().scale(width * 0.28F);
      }
      drawQuad(pose, consumer, start.subtract(right), end.subtract(right.scale(0.45)), end.add(right.scale(0.45)), start.add(right), r, g, b, a);
      drawQuad(pose, consumer, start.subtract(up), end.subtract(up.scale(0.45)), end.add(up.scale(0.45)), start.add(up), r, g, b, a * 0.86F);
   }

   private void drawQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3, float r, float g, float b, float a) {
      vertex(pose, consumer, v0, r, g, b, a, 0.0F, 0.0F);
      vertex(pose, consumer, v1, r, g, b, a, 1.0F, 0.0F);
      vertex(pose, consumer, v2, r, g, b, a, 1.0F, 1.0F);
      vertex(pose, consumer, v3, r, g, b, a, 0.0F, 1.0F);
   }

   private void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 pos, float r, float g, float b, float a, float u, float v) {
      consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(ArtoriaExcaliburBeamEntity entity) {
      return BEAM_TEXTURE;
   }
}

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
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;

public class MedeaBeamEffectRenderer extends EntityRenderer<MedeaBeamEffectEntity> {
   private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public MedeaBeamEffectRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(MedeaBeamEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Vec3 start = entity.position();
      Vec3 end = entity.getEndPos();
      Vec3 currentPos = entity.getPosition(partialTicks);
      Vec3 cameraPos = this.entityRenderDispatcher.camera.getPosition();
      poseStack.pushPose();
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM_TEXTURE));
      drawLayer(poseStack.last(), consumer, start.subtract(currentPos), end.subtract(currentPos), cameraPos.subtract(currentPos), 0.82F, 0.96F, 0.78F, 1.0F, 0.58F);
      drawLayer(poseStack.last(), consumer, start.subtract(currentPos), end.subtract(currentPos), cameraPos.subtract(currentPos), 0.54F, 0.9F, 0.66F, 1.0F, 0.8F);
      drawLayer(poseStack.last(), consumer, start.subtract(currentPos), end.subtract(currentPos), cameraPos.subtract(currentPos), 0.28F, 1.0F, 0.9F, 1.0F, 0.94F);
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
      Vec3 v2 = end.subtract(right);
      Vec3 v3 = end.add(right);
      vertex(pose, consumer, v0, r, g, b, a, 0.0F, 0.0F);
      vertex(pose, consumer, v2, r, g, b, a, 1.0F, 0.0F);
      vertex(pose, consumer, v3, r, g, b, a, 1.0F, 1.0F);
      vertex(pose, consumer, v1, r, g, b, a, 0.0F, 1.0F);
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
   public ResourceLocation getTextureLocation(MedeaBeamEffectEntity entity) {
      return BEAM_TEXTURE;
   }
}

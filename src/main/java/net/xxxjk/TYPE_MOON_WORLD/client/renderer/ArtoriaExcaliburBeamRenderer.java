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
   private static final ResourceLocation INVISIBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/empty.png");
   private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/particle/beam_gradient.png");

   public ArtoriaExcaliburBeamRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(ArtoriaExcaliburBeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      Vec3 end = entity.getEndPos().subtract(entity.position());
      if (end.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 forward = end.normalize();
      Vec3 worldUp = Math.abs(forward.y) > 0.94 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(worldUp).normalize();
      Vec3 up = right.cross(forward).normalize();
      float progress = Math.min(1.0F, entity.tickCount / (float)Math.max(1, entity.getDuration()));
      float fade = 1.0F - Math.max(0.0F, progress - 0.82F) / 0.18F;
      float scale = entity.isGoldenVariant() ? 0.72F : 1.0F;
      float width = (float)entity.beamHalfWidth() * scale;
      int color = entity.isGoldenVariant() ? 0xFFFFC44A : 0xFFDDFBFF;
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM_TEXTURE));
      drawQuad(poseStack, consumer, end, right, width * 0.78F, color, fade * 0.26F);
      drawQuad(poseStack, consumer, end, up, width * 0.52F, 0xFFFFFFFF, fade * 0.9F);
      drawQuad(poseStack, consumer, end, right, width * 0.22F, 0xFFFFFFFF, fade);
   }

   private static void drawQuad(PoseStack poseStack, VertexConsumer consumer, Vec3 end, Vec3 axis,
                                float halfWidth, int argb, float alpha) {
      float r = ((argb >>> 16) & 255) / 255.0F;
      float g = ((argb >>> 8) & 255) / 255.0F;
      float b = (argb & 255) / 255.0F;
      PoseStack.Pose pose = poseStack.last();
      vertex(pose, consumer, 0.0F, 0.0F, 0.0F, axis, -halfWidth, r, g, b, alpha, 0.0F, 0.5F);
      vertex(pose, consumer, 0.0F, 0.0F, 0.0F, axis, halfWidth, r, g, b, alpha, 0.0F, 1.0F);
      vertex(pose, consumer, (float)end.x, (float)end.y, (float)end.z, axis, halfWidth, r, g, b, alpha, 1.0F, 1.0F);
      vertex(pose, consumer, (float)end.x, (float)end.y, (float)end.z, axis, -halfWidth, r, g, b, alpha, 1.0F, 0.5F);
   }

   private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z,
                              Vec3 axis, float offset, float r, float g, float b, float a, float u, float v) {
      consumer.addVertex(pose, (float)(x + axis.x * offset), (float)(y + axis.y * offset), (float)(z + axis.z * offset))
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(ArtoriaExcaliburBeamEntity entity) {
      return INVISIBLE_TEXTURE;
   }
}

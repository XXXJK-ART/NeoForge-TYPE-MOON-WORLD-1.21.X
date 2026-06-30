package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;

public class GaeBulgProjectileRenderer<T extends GaeBulgProjectileEntity> extends EntityRenderer<T> {
   private final GaeBulgRenderer itemRenderer;

   public GaeBulgProjectileRenderer(Context context) {
      super(context);
      this.itemRenderer = new GaeBulgRenderer();
   }

   @Override
   public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float ryaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
      float rpitch = Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot());
      ItemStack itemStack = entity.getItem();

      // Gae Bulg's spearhead points toward local -Z in the geo model, so flip 180 degrees after facing motion.
      poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-rpitch));
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.scale(0.95F, 0.95F, 0.95F);
      if (!itemStack.isEmpty()) {
         this.itemRenderer.renderByItem(itemStack, ItemDisplayContext.NONE, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
      }
      poseStack.popPose();
      this.renderDeathThornTrail(entity, partialTicks, poseStack, buffer);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private void renderDeathThornTrail(T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
      if (entity.getMode() != GaeBulgProjectileEntity.Mode.SINGLE || entity.tracePos.size() < 2) {
         return;
      }

      List<Vec3> points = new ArrayList<>(entity.tracePos);
      Vec3 currentPos = entity.getPosition(partialTicks);
      points.add(currentPos);
      Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png")));
      poseStack.pushPose();
      Pose pose = poseStack.last();
      int total = points.size() - 1;
      for (int i = 0; i < total; i++) {
         Vec3 start = points.get(i).subtract(currentPos);
         Vec3 end = points.get(i + 1).subtract(currentPos);
         float p1 = i / (float)total;
         float p2 = (i + 1) / (float)total;
         drawSegment(pose, consumer, start, end, camPos.subtract(currentPos), 0.18F + 0.18F * p1, 0.18F + 0.18F * p2, 0.08F + 0.38F * p1, 0.08F + 0.38F * p2);
      }
      poseStack.popPose();
   }

   private static void drawSegment(Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 viewOffset, float width1, float width2, float alpha1, float alpha2) {
      Vec3 dir = end.subtract(start);
      if (dir.lengthSqr() < 1.0E-6) {
         return;
      }
      Vec3 right = dir.cross(start.subtract(viewOffset));
      if (right.lengthSqr() < 1.0E-6) {
         return;
      }
      right = right.normalize();
      Vec3 a = right.scale(width1 * 0.5);
      Vec3 b = right.scale(width2 * 0.5);
      vertex(pose, consumer, start.subtract(a), alpha1, 0.0F);
      vertex(pose, consumer, end.subtract(b), alpha2, 1.0F);
      vertex(pose, consumer, end.add(b), alpha2, 1.0F);
      vertex(pose, consumer, start.add(a), alpha1, 0.0F);
   }

   private static void vertex(Pose pose, VertexConsumer consumer, Vec3 pos, float alpha, float u) {
      consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z)
         .setColor(0.55F, 0.0F, 0.035F, alpha)
         .setUv(u, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public ResourceLocation getTextureLocation(T entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

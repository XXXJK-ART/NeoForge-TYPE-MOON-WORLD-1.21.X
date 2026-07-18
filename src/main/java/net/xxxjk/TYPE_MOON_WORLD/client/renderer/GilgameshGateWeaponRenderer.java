package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshGateWeaponModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GilgameshGateWeaponRenderer extends GeoEntityRenderer<GilgameshGateWeaponProjectileEntity> {
   private static final ResourceLocation GATE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public GilgameshGateWeaponRenderer(Context context) { super(context, new GilgameshGateWeaponModel()); }

   @Override public void render(GilgameshGateWeaponProjectileEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      Vec3 motion = e.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6) motion = new Vec3(0.0, 0.0, 1.0);
      motion = motion.normalize();
      float progress = smooth(e.getSummonProgress(partial));

      if (e.getLaunchDelay() > 0 && e.tickCount <= e.getLaunchDelay()) {
         renderGate(e, partial, motion, progress, pose, buffers);
      }

      pose.pushPose();
      pose.translate(motion.x * progress * 0.72, motion.y * progress * 0.72, motion.z * progress * 0.72);
      pose.mulPose(weaponRotation(e.getWeaponId(), motion));
      float scale = weaponScale(e.getWeaponId()) * (0.08F + progress * 0.92F);
      pose.scale(scale, scale, scale);
      super.render(e, yaw, partial, pose, buffers, light);
      pose.popPose();
   }

   private static void renderGate(GilgameshGateWeaponProjectileEntity entity, float partial, Vec3 direction, float progress,
                                  PoseStack pose, MultiBufferSource buffers) {
      pose.pushPose();
      pose.mulPose(new Quaternionf().rotationTo(
         new Vector3f(0.0F, 0.0F, 1.0F),
         new Vector3f((float)direction.x, (float)direction.y, (float)direction.z)
      ));
      float open = 0.18F + progress * 0.82F;
      pose.scale(open, open, open);
      VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(GATE_TEXTURE));
      float spin = (entity.tickCount + partial) * 0.055F;
      drawRing(pose, consumer, 1.32F, 0.13F, -0.28F, spin, 0.92F);
      drawRing(pose, consumer, 1.02F, 0.055F, -0.26F, -spin * 1.35F, 0.72F);
      drawRing(pose, consumer, 0.82F, 0.035F, -0.24F, spin * 1.8F, 0.48F);
      pose.popPose();
   }

   private static void drawRing(PoseStack pose, VertexConsumer consumer, float radius, float width, float z, float rotation, float alpha) {
      int segments = 40;
      for (int i = 0; i < segments; i++) {
         float a0 = rotation + (float)(Math.PI * 2.0 * i / segments);
         float a1 = rotation + (float)(Math.PI * 2.0 * (i + 1) / segments);
         float inner = radius - width;
         vertex(pose, consumer, (float)Math.cos(a0) * inner, (float)Math.sin(a0) * inner, z, 0.0F, 0.0F, alpha);
         vertex(pose, consumer, (float)Math.cos(a1) * inner, (float)Math.sin(a1) * inner, z, 1.0F, 0.0F, alpha);
         vertex(pose, consumer, (float)Math.cos(a1) * radius, (float)Math.sin(a1) * radius, z, 1.0F, 1.0F, alpha);
         vertex(pose, consumer, (float)Math.cos(a0) * radius, (float)Math.sin(a0) * radius, z, 0.0F, 1.0F, alpha);
      }
   }

   private static void vertex(PoseStack pose, VertexConsumer consumer, float x, float y, float z, float u, float v, float alpha) {
      consumer.addVertex(pose.last(), x, y, z)
         .setColor(1.0F, 0.72F, 0.08F, alpha)
         .setUv(u, v)
         .setOverlay(0)
         .setLight(15728880)
         .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
   }

   private static Quaternionf weaponRotation(String weaponId, Vec3 direction) {
      Vector3f sourceAxis = switch (weaponId) {
         case "durandal" -> new Vector3f(0.0F, -1.0F, 0.0F);
         case "fangtian_huaji" -> new Vector3f(0.0F, 0.0F, -1.0F);
         default -> new Vector3f(0.0F, 1.0F, 0.0F);
      };
      return new Quaternionf().rotationTo(sourceAxis, new Vector3f((float)direction.x, (float)direction.y, (float)direction.z));
   }

   private static float weaponScale(String weaponId) {
      return switch (weaponId) {
         case "durandal", "vajra" -> 0.5F;
         case "gram" -> 0.55F;
         case "harpe" -> 0.7F;
         case "fangtian_huaji" -> 1.0F;
         default -> 0.65F;
      };
   }

   private static float smooth(float value) {
      return value * value * (3.0F - 2.0F * value);
   }
}

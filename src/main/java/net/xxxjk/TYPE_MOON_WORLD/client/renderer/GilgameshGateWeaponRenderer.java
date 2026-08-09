package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshGateWeaponModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GilgameshGateWeaponRenderer extends GeoEntityRenderer<GilgameshGateWeaponProjectileEntity> {
   public GilgameshGateWeaponRenderer(Context context) { super(context, new GilgameshGateWeaponModel()); }

   @Override public void render(GilgameshGateWeaponProjectileEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      // Inventory and other screens still render the world behind the GUI. Do
      // not spend a GeckoLib model pass on every gate while the screen is open.
      if (Minecraft.getInstance().screen != null) return;
      Vec3 motion = e.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6) motion = new Vec3(0.0, 0.0, 1.0);
      motion = motion.normalize();
      float progress = smooth(e.getSummonProgress(partial));
      renderPortal(e, partial, motion, progress, pose, buffers);

      pose.pushPose();
      pose.translate(motion.x * progress * 0.72, motion.y * progress * 0.72, motion.z * progress * 0.72);
      pose.mulPose(weaponRotation(e.getWeaponId(), motion));
      float scale = weaponScale(e.getWeaponId()) * (0.08F + progress * 0.92F);
      pose.scale(scale, scale, scale);
      super.render(e, yaw, partial, pose, buffers, light);
      pose.popPose();
   }

   private static void renderPortal(GilgameshGateWeaponProjectileEntity entity, float partial, Vec3 direction, float progress,
                                    PoseStack pose, MultiBufferSource buffers) {
      if (TypeMoonEffectShaders.getBabylonPortal() != null && TypeMoonEffectShaders.getBabylonPortal().getUniform("Time") != null) {
         TypeMoonEffectShaders.getBabylonPortal().getUniform("Time").set((entity.tickCount + partial) * 0.075F);
      }

      float open = progress < 0.5F ? progress / 0.5F : Math.max(0.0F, 1.0F - (progress - 0.5F) / 0.5F);
      open = smooth(Math.max(0.08F, open));
      float radius = 0.52F + weaponScale(entity.getWeaponId()) * 0.42F;
      float alpha = 0.62F + open * 0.28F;

      pose.pushPose();
      pose.translate(-direction.x * 0.08, -direction.y * 0.08, -direction.z * 0.08);
      pose.mulPose(new Quaternionf().rotationTo(
         new Vector3f(0.0F, 0.0F, 1.0F),
         new Vector3f((float)direction.x, (float)direction.y, (float)direction.z)
      ));
      pose.scale(open, open, open);
      VertexConsumer consumer = buffers.getBuffer(BabylonPortalRenderType.portal());
      drawPortalQuad(pose.last(), consumer, radius, 1.0F, 0.76F, 0.18F, alpha);
      drawPortalQuad(pose.last(), consumer, radius * 0.72F, 1.0F, 0.95F, 0.48F, alpha * 0.48F);
      pose.popPose();
   }

   private static void drawPortalQuad(Pose pose, VertexConsumer consumer, float halfSize, float r, float g, float b, float a) {
      consumer.addVertex(pose, -halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F);
      consumer.addVertex(pose,  halfSize, -halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F);
      consumer.addVertex(pose,  halfSize,  halfSize, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F);
      consumer.addVertex(pose, -halfSize,  halfSize, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F);
   }

   private static Quaternionf weaponRotation(String weaponId, Vec3 direction) {
      Vector3f sourceAxis = switch (weaponId) {
         case "durandal" -> new Vector3f(0.0F, 1.0F, 0.0F);
         case "fangtian_huaji" -> new Vector3f(0.0F, 1.0F, 0.0F);
         case "pseudo_spiral_sword" -> new Vector3f(0.0F, 0.0F, 1.0F);
         case "gae_bulg" -> new Vector3f(0.0F, 0.0F, -1.0F);
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
         case "pseudo_spiral_sword" -> 0.48F;
         case "gae_bulg" -> 0.42F;
         default -> 0.65F;
      };
   }

   private static float smooth(float value) {
      return value * value * (3.0F - 2.0F * value);
   }
}

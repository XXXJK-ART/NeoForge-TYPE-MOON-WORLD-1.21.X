package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
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
      Vec3 motion = e.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6) motion = new Vec3(0.0, 0.0, 1.0);
      motion = motion.normalize();
      float progress = smooth(e.getSummonProgress(partial));

      pose.pushPose();
      pose.translate(motion.x * progress * 0.72, motion.y * progress * 0.72, motion.z * progress * 0.72);
      pose.mulPose(weaponRotation(e.getWeaponId(), motion));
      float scale = weaponScale(e.getWeaponId()) * (0.08F + progress * 0.92F);
      pose.scale(scale, scale, scale);
      super.render(e, yaw, partial, pose, buffers, light);
      pose.popPose();
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

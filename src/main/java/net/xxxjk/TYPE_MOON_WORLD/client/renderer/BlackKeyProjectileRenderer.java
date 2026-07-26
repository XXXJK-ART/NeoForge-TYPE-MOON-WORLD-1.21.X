package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.model.BlackKeyProjectileModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.BlackKeyProjectileEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackKeyProjectileRenderer extends GeoEntityRenderer<BlackKeyProjectileEntity> {
   public BlackKeyProjectileRenderer(EntityRendererProvider.Context context) {
      super(context, new BlackKeyProjectileModel());
      withScale(1.5F);
   }

   @Override
   protected void applyRotations(BlackKeyProjectileEntity entity, PoseStack poseStack, float ageInTicks,
                                 float rotationYaw, float partialTick, float nativeScale) {
      Vec3 motion = entity.getDeltaMovement();
      if (motion.lengthSqr() < 1.0E-6) motion = new Vec3(0.0, 1.0, 0.0);
      Vector3f direction = motion.normalize().toVector3f();
      poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), direction));
   }
}

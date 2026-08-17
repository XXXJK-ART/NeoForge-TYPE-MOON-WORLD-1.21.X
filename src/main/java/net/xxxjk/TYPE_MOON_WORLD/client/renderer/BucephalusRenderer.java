package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.BucephalusModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.BucephalusEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BucephalusRenderer extends GeoEntityRenderer<BucephalusEntity> {
   public BucephalusRenderer(Context context) {
      super(context, new BucephalusModel());
   }

   @Override
   public boolean shouldRender(BucephalusEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return super.shouldRender(entity, frustum, camX, camY, camZ) || !entity.getPassengers().isEmpty();
   }
}

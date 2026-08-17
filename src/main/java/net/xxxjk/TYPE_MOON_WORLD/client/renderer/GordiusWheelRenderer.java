package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GordiusWheelModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GordiusWheelRenderer extends GeoEntityRenderer<GordiusWheelEntity> {
   public GordiusWheelRenderer(Context context) {
      super(context, new GordiusWheelModel());
   }

   @Override
   public boolean shouldRender(GordiusWheelEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return super.shouldRender(entity, frustum, camX, camY, camZ) || !entity.getPassengers().isEmpty();
   }
}

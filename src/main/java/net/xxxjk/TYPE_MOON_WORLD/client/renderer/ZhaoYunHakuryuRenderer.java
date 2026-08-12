package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.culling.Frustum;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ZhaoYunHakuryuModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class ZhaoYunHakuryuRenderer extends GeoEntityRenderer<ZhaoYunHakuryuEntity> {
   public ZhaoYunHakuryuRenderer(Context context) { super(context, new ZhaoYunHakuryuModel()); }

   @Override
   public boolean shouldRender(ZhaoYunHakuryuEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return super.shouldRender(entity, frustum, camX, camY, camZ)
         || entity.isPassenger()
         || entity.getPassengers().size() > 0;
   }
}

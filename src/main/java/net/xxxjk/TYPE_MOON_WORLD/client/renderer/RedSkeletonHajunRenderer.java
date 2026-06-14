package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.RedSkeletonHajunModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RedSkeletonHajunRenderer extends GeoEntityRenderer<RedSkeletonHajunEntity> {
   public RedSkeletonHajunRenderer(Context renderManager) {
      super(renderManager, new RedSkeletonHajunModel());
      this.withScale(1.6F);
   }
}

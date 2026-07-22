package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.xxxjk.TYPE_MOON_WORLD.client.model.RatSwarmModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class RatSwarmRenderer extends GeoEntityRenderer<RatSwarmEntity> {
   public RatSwarmRenderer(EntityRendererProvider.Context context) {
      super(context, new RatSwarmModel());
      this.shadowRadius = 0.8F;
   }
}

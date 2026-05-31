package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.RyougiShikiModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RyougiShikiRenderer extends GeoEntityRenderer<RyougiShikiEntity> {
   public RyougiShikiRenderer(Context renderManager) {
      super(renderManager, new RyougiShikiModel());
   }
}

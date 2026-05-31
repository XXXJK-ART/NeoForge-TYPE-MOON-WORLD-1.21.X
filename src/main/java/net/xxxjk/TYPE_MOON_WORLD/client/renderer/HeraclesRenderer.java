package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.HeraclesModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;

public class HeraclesRenderer extends BaseServantRenderer<HeraclesEntity> {
   public HeraclesRenderer(Context renderManager) {
      super(renderManager, new HeraclesModel(), 1.2F);
   }
}

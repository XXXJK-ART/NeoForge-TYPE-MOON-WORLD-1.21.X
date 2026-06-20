package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GawainModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;

public class GawainRenderer extends BaseServantRenderer<GawainEntity> {
   public GawainRenderer(Context renderManager) {
      super(renderManager, new GawainModel(), 1.0F);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.SasakiKojiroModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;

public class SasakiKojiroRenderer extends BaseServantRenderer<SasakiKojiroEntity> {
   public SasakiKojiroRenderer(Context renderManager) {
      super(renderManager, new SasakiKojiroModel(), 0.95F);
   }
}

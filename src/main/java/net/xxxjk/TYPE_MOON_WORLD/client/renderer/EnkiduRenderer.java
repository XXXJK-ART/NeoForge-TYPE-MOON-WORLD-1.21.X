package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.EnkiduModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;

public class EnkiduRenderer extends BaseServantRenderer<EnkiduEntity> {
   public EnkiduRenderer(Context renderManager) {
      super(renderManager, new EnkiduModel(), 0.9F);
   }
}

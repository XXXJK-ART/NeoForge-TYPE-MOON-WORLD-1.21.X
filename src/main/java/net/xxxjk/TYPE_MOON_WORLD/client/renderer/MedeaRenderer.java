package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.MedeaModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;

public class MedeaRenderer extends BaseServantRenderer<MedeaEntity> {
   public MedeaRenderer(Context renderManager) {
      super(renderManager, new MedeaModel(), 1.0F);
   }
}

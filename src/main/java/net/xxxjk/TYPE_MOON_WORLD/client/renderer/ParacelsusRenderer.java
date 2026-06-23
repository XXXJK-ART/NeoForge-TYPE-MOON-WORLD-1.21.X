package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ParacelsusModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;

public class ParacelsusRenderer extends BaseServantRenderer<ParacelsusEntity> {
   public ParacelsusRenderer(Context renderManager) {
      super(renderManager, new ParacelsusModel(), 1.0F);
   }
}

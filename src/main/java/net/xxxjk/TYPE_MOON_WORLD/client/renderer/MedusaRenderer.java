package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.MedusaModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;

public class MedusaRenderer extends BaseServantRenderer<MedusaEntity> {
   public MedusaRenderer(Context renderManager) {
      super(renderManager, new MedusaModel(), 1.0F);
   }
}

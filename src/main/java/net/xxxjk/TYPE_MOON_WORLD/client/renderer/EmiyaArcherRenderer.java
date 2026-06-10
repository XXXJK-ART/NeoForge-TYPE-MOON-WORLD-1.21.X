package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.EmiyaArcherModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;

public class EmiyaArcherRenderer extends BaseServantRenderer<EmiyaArcherEntity> {
   public EmiyaArcherRenderer(Context renderManager) {
      super(renderManager, new EmiyaArcherModel(), 0.95F);
   }
}

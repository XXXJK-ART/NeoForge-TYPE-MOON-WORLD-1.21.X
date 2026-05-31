package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.CuChulainnModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity;

public class CuChulainnRenderer extends BaseServantRenderer<CuChulainnEntity> {
   public CuChulainnRenderer(Context renderManager) {
      super(renderManager, new CuChulainnModel(), 1.0F);
   }
}

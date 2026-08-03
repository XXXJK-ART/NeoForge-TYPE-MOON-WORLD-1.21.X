package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.CasterGilgameshModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshEntity;

public final class CasterGilgameshRenderer extends BaseServantRenderer<CasterGilgameshEntity> {
   public CasterGilgameshRenderer(Context context) {
      super(context, new CasterGilgameshModel(), 0.95F);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.UshiwakamaruRiderModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruRiderEntity;

public final class UshiwakamaruRiderRenderer extends BaseServantRenderer<UshiwakamaruRiderEntity> {
   public UshiwakamaruRiderRenderer(Context context) {
      super(context, new UshiwakamaruRiderModel(), 0.88F);
   }
}

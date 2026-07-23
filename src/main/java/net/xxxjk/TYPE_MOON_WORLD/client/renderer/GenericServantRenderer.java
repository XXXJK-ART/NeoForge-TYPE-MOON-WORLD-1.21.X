package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GenericServantModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GenericServantEntity;

public final class GenericServantRenderer extends BaseServantRenderer<GenericServantEntity> {
   public GenericServantRenderer(EntityRendererProvider.Context context) {
      super(context, new GenericServantModel(), 1.0F);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.xxxjk.TYPE_MOON_WORLD.client.model.SenkoMuramasaModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SenkoMuramasaEntity;

public class SenkoMuramasaRenderer extends BaseServantRenderer<SenkoMuramasaEntity> {
   public SenkoMuramasaRenderer(EntityRendererProvider.Context context) {
      super(context, new SenkoMuramasaModel(), 1.0F);
   }
}

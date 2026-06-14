package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.OdaNobunagaModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;

public class OdaNobunagaRenderer extends BaseServantRenderer<OdaNobunagaEntity> {
   public OdaNobunagaRenderer(Context renderManager) {
      super(renderManager, new OdaNobunagaModel(), 0.88F);
   }
}

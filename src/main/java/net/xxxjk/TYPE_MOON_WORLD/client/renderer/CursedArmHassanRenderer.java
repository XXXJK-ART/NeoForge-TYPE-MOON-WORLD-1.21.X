package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.CursedArmHassanModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanEntity;

public class CursedArmHassanRenderer extends BaseServantRenderer<CursedArmHassanEntity> {
   public CursedArmHassanRenderer(Context renderManager) {
      super(renderManager, new CursedArmHassanModel(), 1.0F);
   }
}

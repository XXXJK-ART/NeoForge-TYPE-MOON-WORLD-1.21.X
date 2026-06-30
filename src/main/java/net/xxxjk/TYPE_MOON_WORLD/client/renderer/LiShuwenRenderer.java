package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.LiShuwenModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;

public class LiShuwenRenderer extends BaseServantRenderer<LiShuwenEntity> {
   public LiShuwenRenderer(Context renderManager) {
      super(renderManager, new LiShuwenModel(), 0.9F);
   }
}

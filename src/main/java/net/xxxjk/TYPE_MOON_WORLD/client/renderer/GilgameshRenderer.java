package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;

public class GilgameshRenderer extends BaseServantRenderer<GilgameshEntity> {
   public GilgameshRenderer(Context context) { super(context, new GilgameshModel(), 0.95F); }
}

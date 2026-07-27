package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ArashModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashEntity;

public final class ArashRenderer extends BaseServantRenderer<ArashEntity> {
   public ArashRenderer(EntityRendererProvider.Context context) { super(context, new ArashModel(), 0.88F); }
}

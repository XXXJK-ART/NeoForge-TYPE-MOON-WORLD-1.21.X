package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.NightingaleModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.NightingaleEntity;

public final class NightingaleRenderer extends BaseServantRenderer<NightingaleEntity> {
   public NightingaleRenderer(Context context) { super(context, new NightingaleModel(), 1.0F); }
}

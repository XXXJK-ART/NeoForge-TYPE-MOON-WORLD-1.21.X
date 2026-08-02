package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ZhaoYunRiderModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;

public final class ZhaoYunRiderRenderer extends BaseServantRenderer<ZhaoYunRiderEntity> {
   public ZhaoYunRiderRenderer(Context context) { super(context, new ZhaoYunRiderModel(), 0.9F); }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ZhaoYunHakuryuModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class ZhaoYunHakuryuRenderer extends GeoEntityRenderer<ZhaoYunHakuryuEntity> {
   public ZhaoYunHakuryuRenderer(Context context) { super(context, new ZhaoYunHakuryuModel()); }
}

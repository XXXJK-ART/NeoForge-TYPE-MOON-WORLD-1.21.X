package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.YajiaoQiangModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.YajiaoQiangItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class YajiaoQiangRenderer extends GeoItemRenderer<YajiaoQiangItem> {
   public YajiaoQiangRenderer() { super(new YajiaoQiangModel()); }
}

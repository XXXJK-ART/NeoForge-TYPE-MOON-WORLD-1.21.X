package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.SpiderCutterModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.SpiderCutterItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SpiderCutterRenderer extends GeoItemRenderer<SpiderCutterItem> {
   public SpiderCutterRenderer() {
      super(new SpiderCutterModel());
   }
}

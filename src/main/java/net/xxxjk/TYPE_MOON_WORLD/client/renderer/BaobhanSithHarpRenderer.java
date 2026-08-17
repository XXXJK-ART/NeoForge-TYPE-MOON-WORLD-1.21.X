package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.BaobhanSithHarpModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BaobhanSithHarpItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class BaobhanSithHarpRenderer extends GeoItemRenderer<BaobhanSithHarpItem> {
   public BaobhanSithHarpRenderer() {
      super(new BaobhanSithHarpModel());
   }
}

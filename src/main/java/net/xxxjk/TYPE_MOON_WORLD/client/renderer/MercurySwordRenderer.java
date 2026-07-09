package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.MercurySwordModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MercurySwordItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MercurySwordRenderer extends GeoItemRenderer<MercurySwordItem> {
   public MercurySwordRenderer() {
      super(new MercurySwordModel());
   }
}

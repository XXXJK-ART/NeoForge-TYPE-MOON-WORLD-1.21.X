package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.DirkSmallKnifeModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DirkSmallKnifeItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DirkSmallKnifeRenderer extends GeoItemRenderer<DirkSmallKnifeItem> {
   public DirkSmallKnifeRenderer() {
      super(new DirkSmallKnifeModel());
   }
}

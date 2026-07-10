package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.ThompsonContenderModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ThompsonContenderItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ThompsonContenderRenderer extends GeoItemRenderer<ThompsonContenderItem> {
   public ThompsonContenderRenderer() {
      super(new ThompsonContenderModel());
   }
}

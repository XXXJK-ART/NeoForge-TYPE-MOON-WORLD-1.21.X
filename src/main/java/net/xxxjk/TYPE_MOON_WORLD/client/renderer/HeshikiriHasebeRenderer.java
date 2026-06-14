package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.HeshikiriHasebeModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HeshikiriHasebeItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HeshikiriHasebeRenderer extends GeoItemRenderer<HeshikiriHasebeItem> {
   public HeshikiriHasebeRenderer() {
      super(new HeshikiriHasebeModel());
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.NamelessBowModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NamelessBowItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class NamelessBowRenderer extends GeoItemRenderer<NamelessBowItem> {
   public NamelessBowRenderer() {
      super(new NamelessBowModel());
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.BizenNagamitsuModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BizenNagamitsuItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BizenNagamitsuRenderer extends GeoItemRenderer<BizenNagamitsuItem> {
   public BizenNagamitsuRenderer() {
      super(new BizenNagamitsuModel());
   }
}

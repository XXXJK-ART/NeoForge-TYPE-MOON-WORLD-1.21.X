package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.GaeBulgModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GaeBulgItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GaeBulgRenderer extends GeoItemRenderer<GaeBulgItem> {
   public GaeBulgRenderer() {
      super(new GaeBulgModel());
   }
}

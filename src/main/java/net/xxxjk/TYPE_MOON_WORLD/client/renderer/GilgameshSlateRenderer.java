package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshSlateModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshSlateItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class GilgameshSlateRenderer extends GeoItemRenderer<GilgameshSlateItem> {
   public GilgameshSlateRenderer() {
      super(new GilgameshSlateModel());
   }
}

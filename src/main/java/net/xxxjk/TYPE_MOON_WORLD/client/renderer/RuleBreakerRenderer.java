package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.RuleBreakerModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RuleBreakerItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RuleBreakerRenderer extends GeoItemRenderer<RuleBreakerItem> {
   public RuleBreakerRenderer() {
      super(new RuleBreakerModel());
   }
}

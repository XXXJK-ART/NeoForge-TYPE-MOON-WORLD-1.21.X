package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.IskandarShortswordModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.IskandarShortswordItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class IskandarShortswordRenderer extends GeoItemRenderer<IskandarShortswordItem> {
   public IskandarShortswordRenderer() {
      super(new IskandarShortswordModel());
   }
}

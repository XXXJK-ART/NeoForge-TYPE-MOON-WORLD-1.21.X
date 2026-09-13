package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.BrokenExcaliburModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BrokenExcaliburItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class BrokenExcaliburRenderer extends GeoItemRenderer<BrokenExcaliburItem> {
   public BrokenExcaliburRenderer() {
      super(new BrokenExcaliburModel());
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.NamelessChainDaggerModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NamelessChainDaggerItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class NamelessChainDaggerRenderer extends GeoItemRenderer<NamelessChainDaggerItem> {
   public NamelessChainDaggerRenderer() {
      super(new NamelessChainDaggerModel());
   }
}

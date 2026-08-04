package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.OdaMatchlockCatalystModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.OdaMatchlockCatalystItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class OdaMatchlockCatalystRenderer extends GeoItemRenderer<OdaMatchlockCatalystItem> {
   public OdaMatchlockCatalystRenderer() {
      super(new OdaMatchlockCatalystModel());
   }
}

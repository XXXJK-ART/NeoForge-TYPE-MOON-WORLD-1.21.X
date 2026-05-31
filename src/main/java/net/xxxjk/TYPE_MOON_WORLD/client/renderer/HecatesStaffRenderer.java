package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.HecatesStaffModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HecatesStaffItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HecatesStaffRenderer extends GeoItemRenderer<HecatesStaffItem> {
   public HecatesStaffRenderer() {
      super(new HecatesStaffModel());
   }
}

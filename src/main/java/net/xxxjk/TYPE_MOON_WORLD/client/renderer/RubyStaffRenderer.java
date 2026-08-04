package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.RubyStaffModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RubyStaffItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RubyStaffRenderer extends GeoItemRenderer<RubyStaffItem> {
   public RubyStaffRenderer() {
      super(new RubyStaffModel());
   }
}

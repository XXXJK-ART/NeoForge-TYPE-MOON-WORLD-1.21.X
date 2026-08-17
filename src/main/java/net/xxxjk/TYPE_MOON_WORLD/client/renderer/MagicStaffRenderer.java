package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.MagicStaffModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicStaffItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MagicStaffRenderer extends GeoItemRenderer<MagicStaffItem> {
   public MagicStaffRenderer() {
      super(new MagicStaffModel());
   }
}

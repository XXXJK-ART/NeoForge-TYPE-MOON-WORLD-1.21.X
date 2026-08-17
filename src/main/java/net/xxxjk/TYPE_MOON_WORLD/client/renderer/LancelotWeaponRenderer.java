package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.LancelotWeaponModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.LancelotWeaponItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class LancelotWeaponRenderer extends GeoItemRenderer<LancelotWeaponItem> {
   public LancelotWeaponRenderer() {
      super(new LancelotWeaponModel());
   }
}

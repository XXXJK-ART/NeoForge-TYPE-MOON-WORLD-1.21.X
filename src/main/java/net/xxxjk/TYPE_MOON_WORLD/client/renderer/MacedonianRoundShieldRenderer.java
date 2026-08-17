package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.MacedonianRoundShieldModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MacedonianRoundShieldItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class MacedonianRoundShieldRenderer extends GeoItemRenderer<MacedonianRoundShieldItem> {
   public MacedonianRoundShieldRenderer() {
      super(new MacedonianRoundShieldModel());
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.MacedonianSpearModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MacedonianSpearItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class MacedonianSpearRenderer extends GeoItemRenderer<MacedonianSpearItem> {
   public MacedonianSpearRenderer() {
      super(new MacedonianSpearModel());
   }
}

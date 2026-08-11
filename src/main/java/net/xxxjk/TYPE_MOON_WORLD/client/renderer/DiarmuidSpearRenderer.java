package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.DiarmuidSpearModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DiarmuidSpearRenderer extends GeoItemRenderer<DiarmuidSpearItem> {
   public DiarmuidSpearRenderer() {
      super(new DiarmuidSpearModel());
   }
}

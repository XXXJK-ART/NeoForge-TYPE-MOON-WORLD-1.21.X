package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.OkitaKatanaRelicModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.OkitaKatanaRelicItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class OkitaKatanaRelicRenderer extends GeoItemRenderer<OkitaKatanaRelicItem> {
   public OkitaKatanaRelicRenderer() {
      super(new OkitaKatanaRelicModel());
   }
}

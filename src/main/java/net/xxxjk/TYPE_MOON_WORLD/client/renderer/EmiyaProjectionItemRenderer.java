package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.EmiyaProjectionItemModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.EmiyaProjectionItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class EmiyaProjectionItemRenderer extends GeoItemRenderer<EmiyaProjectionItem> {
   public EmiyaProjectionItemRenderer() {
      super(new EmiyaProjectionItemModel());
   }
}

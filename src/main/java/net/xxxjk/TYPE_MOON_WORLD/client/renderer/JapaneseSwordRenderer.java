package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.JapaneseSwordModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.JapaneseSwordItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class JapaneseSwordRenderer extends GeoItemRenderer<JapaneseSwordItem> {
   public JapaneseSwordRenderer() {
      super(new JapaneseSwordModel());
   }
}

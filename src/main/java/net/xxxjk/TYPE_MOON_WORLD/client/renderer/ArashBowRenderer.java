package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.ArashBowModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ArashBowItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ArashBowRenderer extends GeoItemRenderer<ArashBowItem> {
   public ArashBowRenderer() { super(new ArashBowModel()); }
}

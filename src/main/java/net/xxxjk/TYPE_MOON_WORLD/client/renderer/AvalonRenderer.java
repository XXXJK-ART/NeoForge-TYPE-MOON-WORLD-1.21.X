package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.AvalonModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AvalonRenderer extends GeoItemRenderer<AvalonItem> {
    public AvalonRenderer() {
        super(new AvalonModel());
    }
}

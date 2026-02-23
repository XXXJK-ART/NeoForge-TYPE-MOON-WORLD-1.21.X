package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.ExcaliburModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ExcaliburRenderer extends GeoItemRenderer<ExcaliburItem> {
    public ExcaliburRenderer() {
        super(new ExcaliburModel());
    }
}

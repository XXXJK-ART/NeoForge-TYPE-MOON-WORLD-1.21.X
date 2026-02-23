package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.ExcaliburGoldenModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburGoldenItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ExcaliburGoldenRenderer extends GeoItemRenderer<ExcaliburGoldenItem> {
    public ExcaliburGoldenRenderer() {
        super(new ExcaliburGoldenModel());
    }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.MuramasaModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MuramasaItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MuramasaRenderer extends GeoItemRenderer<MuramasaItem> {
    public MuramasaRenderer() {
        super(new MuramasaModel());
    }
}

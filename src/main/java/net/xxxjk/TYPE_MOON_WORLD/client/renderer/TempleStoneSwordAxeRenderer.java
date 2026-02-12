package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.TempleStoneSwordAxeModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TempleStoneSwordAxeRenderer extends GeoItemRenderer<TempleStoneSwordAxeItem> {
    public TempleStoneSwordAxeRenderer() {
        super(new TempleStoneSwordAxeModel());
    }
}

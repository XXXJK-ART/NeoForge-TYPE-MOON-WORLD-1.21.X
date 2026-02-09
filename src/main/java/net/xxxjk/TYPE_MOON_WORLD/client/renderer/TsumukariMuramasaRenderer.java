package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.TsumukariMuramasaModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TsumukariMuramasaItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TsumukariMuramasaRenderer extends GeoItemRenderer<TsumukariMuramasaItem> {
    public TsumukariMuramasaRenderer() {
        super(new TsumukariMuramasaModel());
    }
}

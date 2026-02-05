package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.RedswordModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RedswordItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RedswordRenderer extends GeoItemRenderer<RedswordItem> {
    public RedswordRenderer() {
        super(new RedswordModel());
    }
}

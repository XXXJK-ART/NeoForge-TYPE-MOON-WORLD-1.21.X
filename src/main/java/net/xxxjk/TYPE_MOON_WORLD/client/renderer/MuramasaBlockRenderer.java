package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.block.entity.MuramasaBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.model.MuramasaBlockModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MuramasaBlockRenderer extends GeoBlockRenderer<MuramasaBlockEntity> {
    public MuramasaBlockRenderer() {
        super(new MuramasaBlockModel());
    }
}

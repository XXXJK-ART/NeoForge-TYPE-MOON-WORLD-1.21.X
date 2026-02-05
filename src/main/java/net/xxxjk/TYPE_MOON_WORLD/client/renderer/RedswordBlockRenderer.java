package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.block.entity.RedswordBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.model.RedswordBlockModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class RedswordBlockRenderer extends GeoBlockRenderer<RedswordBlockEntity> {
    public RedswordBlockRenderer() {
        super(new RedswordBlockModel());
    }
}

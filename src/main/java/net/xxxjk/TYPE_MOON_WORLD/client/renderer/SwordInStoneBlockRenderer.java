package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordInStoneBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.model.SwordInStoneBlockModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class SwordInStoneBlockRenderer extends GeoBlockRenderer<SwordInStoneBlockEntity> {
   public SwordInStoneBlockRenderer() {
      super(new SwordInStoneBlockModel());
   }
}

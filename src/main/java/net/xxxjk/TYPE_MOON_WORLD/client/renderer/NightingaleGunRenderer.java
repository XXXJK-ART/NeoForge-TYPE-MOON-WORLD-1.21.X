package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.NightingaleGunModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NightingaleGunItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class NightingaleGunRenderer extends GeoItemRenderer<NightingaleGunItem> {
   public NightingaleGunRenderer() { super(new NightingaleGunModel()); }
}

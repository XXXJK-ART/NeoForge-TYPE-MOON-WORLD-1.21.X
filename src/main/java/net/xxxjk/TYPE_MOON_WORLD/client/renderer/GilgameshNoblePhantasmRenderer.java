package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.GilgameshNoblePhantasmModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GilgameshNoblePhantasmRenderer extends GeoItemRenderer<GilgameshNoblePhantasmItem> {
   public GilgameshNoblePhantasmRenderer() { super(new GilgameshNoblePhantasmModel()); }

}

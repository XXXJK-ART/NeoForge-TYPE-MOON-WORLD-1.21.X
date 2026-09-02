package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.xxxjk.TYPE_MOON_WORLD.client.model.JeanneAlterCursedLanceItemModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.jeanne.JeanneAlterCursedLanceItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class JeanneAlterCursedLanceItemRenderer extends GeoItemRenderer<JeanneAlterCursedLanceItem> {
   public JeanneAlterCursedLanceItemRenderer() { super(new JeanneAlterCursedLanceItemModel()); }
}

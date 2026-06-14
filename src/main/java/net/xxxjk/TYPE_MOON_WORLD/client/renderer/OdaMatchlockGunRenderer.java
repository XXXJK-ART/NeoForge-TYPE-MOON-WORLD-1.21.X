package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.OdaMatchlockGunModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OdaMatchlockGunRenderer extends GeoEntityRenderer<OdaMatchlockGunEntity> {
   public OdaMatchlockGunRenderer(Context renderManager) {
      super(renderManager, new OdaMatchlockGunModel());
      this.withScale(0.9F);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.MedusaPegasusModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MedusaPegasusRenderer extends GeoEntityRenderer<MedusaPegasusEntity> {
   public MedusaPegasusRenderer(Context context) {
      super(context, new MedusaPegasusModel());
   }
}

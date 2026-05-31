package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import software.bernie.geckolib.model.GeoModel;

public class MedusaPegasusModel extends GeoModel<MedusaPegasusEntity> {
   @Override
   public ResourceLocation getModelResource(MedusaPegasusEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/medusa_pegasus.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(MedusaPegasusEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/medusa_pegasus.png");
   }

   @Override
   public ResourceLocation getAnimationResource(MedusaPegasusEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/medusa_pegasus.animation.json");
   }
}

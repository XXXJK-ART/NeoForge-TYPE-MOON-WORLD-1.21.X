package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;
import software.bernie.geckolib.model.GeoModel;

public class MysteriousSwordsmanModel extends GeoModel<MysteriousSwordsmanEntity> {
   @Override public ResourceLocation getModelResource(MysteriousSwordsmanEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/sasaki_kojiro.geo.json");
   }

   @Override public ResourceLocation getTextureResource(MysteriousSwordsmanEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/mysterious_swordsman.png");
   }

   @Override public ResourceLocation getAnimationResource(MysteriousSwordsmanEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/sasaki_kojiro.animation.json");
   }
}

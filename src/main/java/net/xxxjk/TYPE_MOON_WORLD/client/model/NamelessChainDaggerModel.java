package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NamelessChainDaggerItem;
import software.bernie.geckolib.model.GeoModel;

public class NamelessChainDaggerModel extends GeoModel<NamelessChainDaggerItem> {
   @Override
   public ResourceLocation getModelResource(NamelessChainDaggerItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/nameless_chain_dagger.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(NamelessChainDaggerItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/nameless_chain_dagger.png");
   }

   @Override
   public ResourceLocation getAnimationResource(NamelessChainDaggerItem object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/nameless_chain_dagger.animation.json");
   }
}

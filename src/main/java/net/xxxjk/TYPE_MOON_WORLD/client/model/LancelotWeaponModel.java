package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.LancelotWeaponItem;
import software.bernie.geckolib.model.GeoModel;

public class LancelotWeaponModel extends GeoModel<LancelotWeaponItem> {
   @Override
   public ResourceLocation getModelResource(LancelotWeaponItem object) {
      String id = object == null ? "aroundight" : object.weaponType().id();
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/" + id + ".geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(LancelotWeaponItem object) {
      String id = object == null ? "aroundight" : object.weaponType().id();
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/" + id + ".png");
   }

   @Override
   public ResourceLocation getAnimationResource(LancelotWeaponItem object) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/gae_bulg.animation.json");
   }
}

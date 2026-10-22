package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import software.bernie.geckolib.model.GeoModel;

/** Separate NPC-only hair model. It never participates in player equipment rendering. */
public final class ServantHairArmorModel extends GeoModel<ServantCardArmorItem> {
   @Override
   public ResourceLocation getModelResource(ServantCardArmorItem item) {
      return resource("geo/servant_hair_" + item.servantId() + ".geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(ServantCardArmorItem item) {
      return resource("textures/entity/servant_hair_" + item.servantId() + ".png");
   }

   @Override
   public ResourceLocation getAnimationResource(ServantCardArmorItem item) {
      return resource("animations/empty.animation.json");
   }

   private static ResourceLocation resource(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.BlackKeyProjectileEntity;
import software.bernie.geckolib.model.GeoModel;

public class BlackKeyProjectileModel extends GeoModel<BlackKeyProjectileEntity> {
   @Override public ResourceLocation getModelResource(BlackKeyProjectileEntity entity) { return resource("geo/black_key.geo.json"); }
   @Override public ResourceLocation getTextureResource(BlackKeyProjectileEntity entity) { return resource("textures/item/black_key.png"); }
   @Override public ResourceLocation getAnimationResource(BlackKeyProjectileEntity entity) { return resource("animations/black_key.animation.json"); }

   private static ResourceLocation resource(String path) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", path);
   }
}

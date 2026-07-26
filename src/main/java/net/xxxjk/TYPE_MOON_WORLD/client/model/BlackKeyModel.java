package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;
import software.bernie.geckolib.model.GeoModel;

public class BlackKeyModel extends GeoModel<BlackKeyItem> {
   @Override public ResourceLocation getModelResource(BlackKeyItem item) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/black_key.geo.json"); }
   @Override public ResourceLocation getTextureResource(BlackKeyItem item) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/black_key.png"); }
   @Override public ResourceLocation getAnimationResource(BlackKeyItem item) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/black_key.animation.json"); }
}

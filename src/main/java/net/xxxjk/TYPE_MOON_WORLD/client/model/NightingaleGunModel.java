package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NightingaleGunItem;
import software.bernie.geckolib.model.GeoModel;

public final class NightingaleGunModel extends GeoModel<NightingaleGunItem> {
   @Override public ResourceLocation getModelResource(NightingaleGunItem item) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/nightingale_gun.geo.json"); }
   @Override public ResourceLocation getTextureResource(NightingaleGunItem item) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/nightingale_gun.png"); }
   @Override public ResourceLocation getAnimationResource(NightingaleGunItem item) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/nightingale_gun.animation.json"); }
}

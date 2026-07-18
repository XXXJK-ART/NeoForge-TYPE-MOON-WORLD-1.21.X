package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import software.bernie.geckolib.model.GeoModel;

public class GilgameshEaModel extends GeoModel<GilgameshEaBeamEntity> {
   @Override public ResourceLocation getModelResource(GilgameshEaBeamEntity e) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/gilgamesh_ea.geo.json"); }
   @Override public ResourceLocation getTextureResource(GilgameshEaBeamEntity e) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/gilgamesh_ea.png"); }
   @Override public ResourceLocation getAnimationResource(GilgameshEaBeamEntity e) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/gilgamesh_ea.animation.json"); }
}

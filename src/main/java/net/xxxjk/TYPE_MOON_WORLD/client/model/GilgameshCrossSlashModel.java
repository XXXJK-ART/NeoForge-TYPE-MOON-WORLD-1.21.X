package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshCrossSlashEntity;
import software.bernie.geckolib.model.GeoModel;

public class GilgameshCrossSlashModel extends GeoModel<GilgameshCrossSlashEntity> {
   private String name(GilgameshCrossSlashEntity e) { return e.getSlashType() == GilgameshCrossSlashEntity.SlashType.SULSAGANA ? "sulsagana" : "igalima"; }
   @Override public ResourceLocation getModelResource(GilgameshCrossSlashEntity e) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/" + name(e) + ".geo.json"); }
   @Override public ResourceLocation getTextureResource(GilgameshCrossSlashEntity e) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/" + name(e) + ".png"); }
   @Override public ResourceLocation getAnimationResource(GilgameshCrossSlashEntity e) { return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/empty.animation.json"); }
}

package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.jeanne.JeanneAlterCursedLanceItem;
import software.bernie.geckolib.model.GeoModel;

public final class JeanneAlterCursedLanceItemModel extends GeoModel<JeanneAlterCursedLanceItem> {
   @Override public ResourceLocation getModelResource(JeanneAlterCursedLanceItem item) { return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/jeanne_alter_cursed_lance.geo.json"); }
   @Override public ResourceLocation getTextureResource(JeanneAlterCursedLanceItem item) { return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/jeanne_alter_cursed_lance.png"); }
   @Override public ResourceLocation getAnimationResource(JeanneAlterCursedLanceItem item) { return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/jeanne_alter_cursed_lance.animation.json"); }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import software.bernie.geckolib.model.GeoModel;

public final class JeanneAlterSwordModel extends GeoModel<JeanneAlterSwordItem> {
   @Override
   public ResourceLocation getModelResource(JeanneAlterSwordItem animatable) {
      return id("geo/jeanne_alter_sword.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(JeanneAlterSwordItem animatable) {
      return id("textures/item/jeanne_alter_sword.png");
   }

   @Override
   public ResourceLocation getAnimationResource(JeanneAlterSwordItem animatable) {
      return id("animations/jeanne_alter_sword.animation.json");
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}

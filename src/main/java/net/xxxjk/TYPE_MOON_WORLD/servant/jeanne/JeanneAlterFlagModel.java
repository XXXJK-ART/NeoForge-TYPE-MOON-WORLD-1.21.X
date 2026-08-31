package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import software.bernie.geckolib.model.GeoModel;

public final class JeanneAlterFlagModel extends GeoModel<JeanneAlterFlagItem> {
   @Override
   public ResourceLocation getModelResource(JeanneAlterFlagItem animatable) {
      return id("geo/jeanne_alter_flag.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(JeanneAlterFlagItem animatable) {
      return id("textures/item/jeanne_alter_flag.png");
   }

   @Override
   public ResourceLocation getAnimationResource(JeanneAlterFlagItem animatable) {
      return id("animations/jeanne_alter_flag.animation.json");
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}

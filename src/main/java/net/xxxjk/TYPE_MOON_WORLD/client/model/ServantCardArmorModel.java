package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import software.bernie.geckolib.model.GeoModel;

public class ServantCardArmorModel extends GeoModel<ServantCardArmorItem> {
   private static final ResourceLocation EMIYA_MODEL = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/servant_card_emiya_archer.geo.json");
   private static final ResourceLocation EMIYA_TEXTURE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/models/armor/servant_card_emiya_archer.png");
   private static final ResourceLocation EMIYA_ANIMATION = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/servant_card_emiya_archer.animation.json");

   @Override
   public ResourceLocation getModelResource(ServantCardArmorItem animatable) {
      return EMIYA_MODEL;
   }

   @Override
   public ResourceLocation getTextureResource(ServantCardArmorItem animatable) {
      return EMIYA_TEXTURE;
   }

   @Override
   public ResourceLocation getAnimationResource(ServantCardArmorItem animatable) {
      return EMIYA_ANIMATION;
   }
}

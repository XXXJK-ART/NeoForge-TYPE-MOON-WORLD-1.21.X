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
      String servantId = animatable == null ? "" : animatable.servantId();
      return hasDedicatedArmor(servantId)
         ? ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/servant_card_" + servantId + ".geo.json")
         : EMIYA_MODEL;
   }

   @Override
   public ResourceLocation getTextureResource(ServantCardArmorItem animatable) {
      String servantId = animatable == null ? "" : animatable.servantId();
      return hasDedicatedArmor(servantId)
         ? ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/models/armor/servant_card_" + servantId + ".png")
         : EMIYA_TEXTURE;
   }

   @Override
   public ResourceLocation getAnimationResource(ServantCardArmorItem animatable) {
      String servantId = animatable == null ? "" : animatable.servantId();
      return hasDedicatedArmor(servantId)
         ? ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/servant_card_" + servantId + ".animation.json")
         : EMIYA_ANIMATION;
   }

   private static boolean hasDedicatedArmor(String servantId) {
      return switch (servantId) {
         case "emiya_archer", "enkidu", "cu_chulainn", "medea",
            "artoria_pendragon", "sasaki_kojiro", "medusa", "cursed_arm_hassan", "shadow_hassan", "heracles",
            "gilgamesh", "gawain", "paracelsus", "li_shuwen", "oda_nobunaga", "ushiwakamaru_rider" -> true;
         case "fanatic_assassin", "arash" -> true;
         default -> false;
      };
   }
}

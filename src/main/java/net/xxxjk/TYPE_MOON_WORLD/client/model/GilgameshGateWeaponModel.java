package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import software.bernie.geckolib.model.GeoModel;

public class GilgameshGateWeaponModel extends GeoModel<GilgameshGateWeaponProjectileEntity> {
   private static String normalized(String id) {
      return switch (id) {
         case "gram", "harpe", "vajra", "fangtian_huaji", "pseudo_spiral_sword", "gae_bulg" -> id;
         default -> "durandal";
      };
   }
   @Override public ResourceLocation getModelResource(GilgameshGateWeaponProjectileEntity e) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/" + normalized(e.getWeaponId()) + ".geo.json");
   }
   @Override public ResourceLocation getTextureResource(GilgameshGateWeaponProjectileEntity e) {
      String id = normalized(e.getWeaponId());
      String folder = "pseudo_spiral_sword".equals(id) || "gae_bulg".equals(id) ? "item" : "entity";
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/" + folder + "/" + id + ".png");
   }
   @Override public ResourceLocation getAnimationResource(GilgameshGateWeaponProjectileEntity e) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/empty.animation.json");
   }
}

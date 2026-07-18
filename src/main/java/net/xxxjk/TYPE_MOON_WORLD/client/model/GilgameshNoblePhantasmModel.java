package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem;
import software.bernie.geckolib.model.GeoModel;

public class GilgameshNoblePhantasmModel extends GeoModel<GilgameshNoblePhantasmItem> {
   private String asset(GilgameshNoblePhantasmItem item) {
      return switch (item.modelId()) {
         case "ea" -> "gilgamesh_ea";
         default -> item.modelId();
      };
   }
   @Override public ResourceLocation getModelResource(GilgameshNoblePhantasmItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/" + asset(item) + ".geo.json");
   }
   @Override public ResourceLocation getTextureResource(GilgameshNoblePhantasmItem item) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/" + asset(item) + ".png");
   }
   @Override public ResourceLocation getAnimationResource(GilgameshNoblePhantasmItem item) {
      String animation = switch (item.modelId()) {
         case "ea" -> "gilgamesh_ea";
         default -> "empty";
      };
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/" + animation + ".animation.json");
   }
}

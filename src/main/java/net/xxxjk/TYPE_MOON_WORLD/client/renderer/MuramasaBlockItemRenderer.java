package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MuramasaBlockItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

@SuppressWarnings("deprecation")
public class MuramasaBlockItemRenderer extends GeoItemRenderer<MuramasaBlockItem> {
   public MuramasaBlockItemRenderer() {
      super(new GeoModel<MuramasaBlockItem>() {
         public ResourceLocation getModelResource(MuramasaBlockItem object) {
            return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/red_sword_block.geo.json");
         }

         public ResourceLocation getTextureResource(MuramasaBlockItem object) {
            return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/block/red_sword_block.png");
         }

         public ResourceLocation getAnimationResource(MuramasaBlockItem object) {
            return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/red_sword_block.animation.json");
         }
      });
   }
}

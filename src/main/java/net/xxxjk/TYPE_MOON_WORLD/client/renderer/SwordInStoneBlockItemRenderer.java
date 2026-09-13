package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.SwordInStoneBlockItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SwordInStoneBlockItemRenderer extends GeoItemRenderer<SwordInStoneBlockItem> {
   public SwordInStoneBlockItemRenderer() {
      super(new GeoModel<SwordInStoneBlockItem>() {
         @Override
         public ResourceLocation getModelResource(SwordInStoneBlockItem object) {
            return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/sword_in_stone.geo.json");
         }

         @Override
         public ResourceLocation getTextureResource(SwordInStoneBlockItem object) {
            return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/block/sword_in_stone.png");
         }

         @Override
         public ResourceLocation getAnimationResource(SwordInStoneBlockItem object) {
            return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/sword_in_stone.animation.json");
         }
      });
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity;
import software.bernie.geckolib.cache.object.GeoBone;

public class CuChulainnModel extends BaseServantModel<CuChulainnEntity> {
   public CuChulainnModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/cu_chulainn.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/cu_chulainn.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/cu_chulainn.animation.json")
      );
   }

   @Override
   protected void applySpawnPoseFallback(CuChulainnEntity entity) {
      GeoBone root = this.getAnimationProcessor().getBone("bone");
      if (root != null) {
         root.setPosY(-5.0F);
         root.setScaleX(0.8F);
         root.setScaleY(0.8F);
         root.setScaleZ(0.8F);
      }
   }
}

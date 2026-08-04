package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import software.bernie.geckolib.model.GeoModel;

public final class ZhaoYunHakuryuModel extends GeoModel<ZhaoYunHakuryuEntity> {
   @Override public ResourceLocation getModelResource(ZhaoYunHakuryuEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/zhao_yun_hakuryu.geo.json");
   }
   @Override public ResourceLocation getTextureResource(ZhaoYunHakuryuEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/zhao_yun_hakuryu.png");
   }
   @Override public ResourceLocation getAnimationResource(ZhaoYunHakuryuEntity entity) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/zhao_yun_hakuryu.animation.json");
   }
}

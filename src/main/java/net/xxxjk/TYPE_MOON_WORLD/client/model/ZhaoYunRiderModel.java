package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;

public final class ZhaoYunRiderModel extends BaseServantModel<ZhaoYunRiderEntity> {
   public ZhaoYunRiderModel() {
      super(ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/zhao_yun_rider.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/zhao_yun_rider.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/zhao_yun_rider.animation.json"));
   }
}

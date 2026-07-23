package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity;
import software.bernie.geckolib.model.GeoModel;

public final class RatSwarmModel extends GeoModel<RatSwarmEntity> {
   @Override
   public ResourceLocation getModelResource(RatSwarmEntity entity) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/rat_swarm.geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(RatSwarmEntity entity) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/rat_swarm.png");
   }

   @Override
   public ResourceLocation getAnimationResource(RatSwarmEntity entity) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/rat_swarm.animation.json");
   }
}

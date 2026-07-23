package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GenericServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import software.bernie.geckolib.model.GeoModel;

/** Resolves addon model resources directly from the synchronized servant definition. */
public final class GenericServantModel extends GeoModel<GenericServantEntity> {
   private static final ResourceLocation EMPTY_GEO = ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/empty.geo.json");
   private static final ResourceLocation EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/empty.png");
   private static final ResourceLocation EMPTY_ANIMATION = ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/empty.animation.json");

   @Override public ResourceLocation getModelResource(GenericServantEntity entity) {
      return path(entity, 0, EMPTY_GEO);
   }
   @Override public ResourceLocation getTextureResource(GenericServantEntity entity) {
      return path(entity, 1, EMPTY_TEXTURE);
   }
   @Override public ResourceLocation getAnimationResource(GenericServantEntity entity) {
      return path(entity, 2, EMPTY_ANIMATION);
   }

   private static ResourceLocation path(GenericServantEntity entity, int kind, ResourceLocation fallback) {
      ServantDefinition definition = entity.getDefinition();
      if (definition == null) return fallback;
      String value = kind == 0 ? definition.modelGeometryPath() : kind == 1 ? definition.texturePath() : definition.animationPath();
      ResourceLocation parsed = value == null ? null : ResourceLocation.tryParse(value);
      return parsed == null ? fallback : parsed;
   }
}

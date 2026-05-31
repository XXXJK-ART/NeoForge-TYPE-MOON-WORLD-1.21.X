package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;
import software.bernie.geckolib.cache.object.GeoBone;

public class MedeaModel extends BaseServantModel<MedeaEntity> {
   public MedeaModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/medea.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/medea.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/medea.animation.json")
      );
   }

   @Override
   protected void applySpawnPoseFallback(MedeaEntity entity) {
      GeoBone root = this.getAnimationProcessor().getBone("bone");
      if (root != null) {
         root.setPosY(-8.0F);
         root.setScaleX(0.7F);
         root.setScaleY(0.7F);
         root.setScaleZ(0.7F);
      }
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class MysteriousSwordsmanModel extends GeoModel<MysteriousSwordsmanEntity> {
   @Override public ResourceLocation getModelResource(MysteriousSwordsmanEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/servant_card_sasaki_kojiro.geo.json");
   }

   @Override public ResourceLocation getTextureResource(MysteriousSwordsmanEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/mysterious_swordsman.png");
   }

   @Override public ResourceLocation getAnimationResource(MysteriousSwordsmanEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/servant_card_sasaki_kojiro.animation.json");
   }

   @Override
   public void setCustomAnimations(MysteriousSwordsmanEntity entity, long instanceId, AnimationState<MysteriousSwordsmanEntity> state) {
      if (entity.tickCount > 2) return;

      GeoBone root = this.getAnimationProcessor().getBone("bone");
      if (root != null) {
         root.setPosY(-6.0F);
         root.setScaleX(0.7F);
         root.setScaleY(0.7F);
         root.setScaleZ(0.7F);
      }

      setScale("body", 1.0F, 1.1F, 1.1F);
      setScale("bone4", 1.0F, 0.9F, 1.0F);
      setScale("right arm", 1.1F, 1.0F, 1.0F);
      setScale("left arm", 1.1F, 1.0F, 1.0F);
      setLegPose("left_leg");
      setLegPose("right_leg");
      setScale("bone5", 1.1F, 1.1F, 1.0F);
   }

   private void setLegPose(String boneName) {
      GeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setPosY(-1.0F);
         bone.setScaleX(1.2F);
         bone.setScaleY(1.1F);
         bone.setScaleZ(1.0F);
      }
   }

   private void setScale(String boneName, float x, float y, float z) {
      GeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setScaleX(x);
         bone.setScaleY(y);
         bone.setScaleZ(z);
      }
   }
}

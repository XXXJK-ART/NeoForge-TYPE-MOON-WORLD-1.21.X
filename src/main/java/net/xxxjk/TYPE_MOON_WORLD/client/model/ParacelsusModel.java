package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class ParacelsusModel extends BaseServantModel<ParacelsusEntity> {
   public ParacelsusModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/paracelsus.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/paracelsus.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/paracelsus.animation.json")
      );
   }

   @Override
   public void setCustomAnimations(ParacelsusEntity entity, long instanceId, AnimationState<ParacelsusEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      int phase = entity.getCombatPhase();
      boolean phaseTwoOrMore = phase >= 2;
      boolean phaseThree = phase >= 3;
      setBoneVisible("clothes", phase < 3);
      setLegMotionScale("left_leg", phaseThree ? 0.68F : phaseTwoOrMore ? 0.82F : 0.9F);
      setLegMotionScale("right_leg", phaseThree ? 0.68F : phaseTwoOrMore ? 0.82F : 0.9F);
      GeoBone body = this.getAnimationProcessor().getBone("body");
      if (body != null && phaseThree) {
         body.setRotX(body.getRotX() * 0.96F);
         body.setRotZ(body.getRotZ() * 0.96F);
      }
   }

   private void setBoneVisible(String name, boolean visible) {
      GeoBone bone = this.getAnimationProcessor().getBone(name);
      if (bone != null) {
         float scale = visible ? 1.0F : 0.0F;
         bone.setScaleX(scale);
         bone.setScaleY(scale);
         bone.setScaleZ(scale);
      }
   }

   private void setLegMotionScale(String name, float scale) {
      GeoBone bone = this.getAnimationProcessor().getBone(name);
      if (bone != null) {
         bone.setRotX(bone.getRotX() * scale);
         bone.setRotY(bone.getRotY() * scale);
         bone.setRotZ(bone.getRotZ() * scale);
         bone.setPosY(Mth.clamp(bone.getPosY(), -2.0F, 2.0F));
      }
   }
}

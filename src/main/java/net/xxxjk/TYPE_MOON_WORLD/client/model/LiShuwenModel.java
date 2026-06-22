package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class LiShuwenModel extends BaseServantModel<LiShuwenEntity> {
   public LiShuwenModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/li_shuwen.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/li_shuwen.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/li_shuwen.animation.json")
      );
   }

   @Override
   public void setCustomAnimations(LiShuwenEntity entity, long instanceId, AnimationState<LiShuwenEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      int phase = entity.getCombatPhase();
      setBoneVisible("galasses", phase < 2);
      boolean coatVisible = phase < 3;
      setBoneVisible("coat", coatVisible);
      setBoneVisible("cotton", coatVisible);
      setBoneVisible("sleeve", coatVisible);
      setBoneVisible("sleeve2", coatVisible);
      if (entity.isWuErDaTargeting()) {
         GeoBone body = this.getAnimationProcessor().getBone("body");
         if (body != null) {
            body.setScaleX(1.04F);
            body.setScaleY(1.02F);
            body.setScaleZ(1.04F);
         }
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
}

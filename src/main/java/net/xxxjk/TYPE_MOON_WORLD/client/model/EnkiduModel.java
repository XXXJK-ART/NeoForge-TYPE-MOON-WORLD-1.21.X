package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class EnkiduModel extends BaseServantModel<EnkiduEntity> {
   public EnkiduModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/enkidu.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/enkidu.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/enkidu.animation.json")
      );
   }

   @Override
   public void setCustomAnimations(EnkiduEntity entity, long instanceId, AnimationState<EnkiduEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      EntityModelData entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
      if (entityData == null) {
         return;
      }

      float pitchRad = Mth.clamp(entityData.headPitch(), -40.0F, 40.0F) * (float)(Math.PI / 180.0);
      GeoBone hair1 = this.getAnimationProcessor().getBone("hair1");
      GeoBone hair2 = this.getAnimationProcessor().getBone("hair2");
      if (hair1 != null) {
         hair1.setRotX(-pitchRad * 0.55F);
      }
      if (hair2 != null) {
         hair2.setRotX(-pitchRad * 0.85F);
      }
   }
}

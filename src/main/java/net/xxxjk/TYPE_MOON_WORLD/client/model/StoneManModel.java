package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

@SuppressWarnings("deprecation")
public class StoneManModel extends GeoModel<StoneManEntity> {
   private static final float BASE_HEAD_PITCH_DEG = -2.0F;

   public ResourceLocation getModelResource(StoneManEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/stone_man.geo.json");
   }

   public ResourceLocation getTextureResource(StoneManEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/stone_man.png");
   }

   public ResourceLocation getAnimationResource(StoneManEntity object) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/stone_man.animation.json");
   }

   public void setCustomAnimations(StoneManEntity animatable, long instanceId, AnimationState<StoneManEntity> animationState) {
      GeoBone head = this.getAnimationProcessor().getBone("head");
      if (head != null && animatable.getAttackAnimTicks() <= 0) {
         EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
         if (entityData != null) {
            float yawDeg = Mth.clamp(entityData.netHeadYaw(), -45.0F, 45.0F);
            float pitchDeg = Mth.clamp(entityData.headPitch(), -25.0F, 25.0F);
            head.setRotY(yawDeg * (float) (Math.PI / 180.0));
            head.setRotX((-2.0F + pitchDeg) * (float) (Math.PI / 180.0));
         }
      }
   }
}

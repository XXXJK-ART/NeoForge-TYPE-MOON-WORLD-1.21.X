package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class BaseServantModel<T extends ServantEntity> extends GeoModel<T> {
   private final ResourceLocation modelResource;
   private final ResourceLocation textureResource;
   private final ResourceLocation animationResource;

   public BaseServantModel(ResourceLocation modelResource, ResourceLocation textureResource, ResourceLocation animationResource) {
      this.modelResource = modelResource;
      this.textureResource = textureResource;
      this.animationResource = animationResource;
   }

   @Override
   public ResourceLocation getModelResource(T entity) {
      return this.modelResource;
   }

   @Override
   public ResourceLocation getTextureResource(T entity) {
      return this.textureResource;
   }

   @Override
   public ResourceLocation getAnimationResource(T entity) {
      return this.animationResource;
   }

   @Override
   public void setCustomAnimations(T entity, long instanceId, AnimationState<T> state) {
      if (entity.tickCount <= 2) {
         this.applySpawnPoseFallback(entity);
      }

      GeoBone head = this.getAnimationProcessor().getBone("head");
      if (head != null) {
         EntityModelData entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
         float yawDeg = Mth.clamp(entityData.netHeadYaw(), -40.0F, 40.0F);
         head.setRotY(yawDeg * (float) (Math.PI / 180.0));
         head.setRotX(entityData.headPitch() * (float) (Math.PI / 180.0));
      }
   }

   protected void applySpawnPoseFallback(T entity) {
   }
}

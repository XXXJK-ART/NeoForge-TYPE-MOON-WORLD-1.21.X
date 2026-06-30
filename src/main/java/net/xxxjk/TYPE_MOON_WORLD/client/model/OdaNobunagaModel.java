package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class OdaNobunagaModel extends BaseServantModel<OdaNobunagaEntity> {
   public OdaNobunagaModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/oda_nobunaga.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/oda_nobunaga.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/oda_nobunaga.animation.json")
      );
   }

   @Override
   public void setCustomAnimations(OdaNobunagaEntity entity, long instanceId, AnimationState<OdaNobunagaEntity> state) {
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

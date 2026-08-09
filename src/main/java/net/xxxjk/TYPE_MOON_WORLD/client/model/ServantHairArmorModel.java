package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;

/** Separate NPC-only hair model. It never participates in player equipment rendering. */
public final class ServantHairArmorModel extends GeoModel<ServantCardArmorItem> {
   @Override
   public ResourceLocation getModelResource(ServantCardArmorItem item) {
      return resource("geo/servant_hair_" + item.servantId() + ".geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(ServantCardArmorItem item) {
      return resource("textures/entity/servant_hair_" + item.servantId() + ".png");
   }

   @Override
   public ResourceLocation getAnimationResource(ServantCardArmorItem item) {
      return resource("animations/empty.animation.json");
   }

   @Override
   public void setCustomAnimations(ServantCardArmorItem item, long instanceId,
                                   AnimationState<ServantCardArmorItem> state) {
      EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
      if (data == null) {
         return;
      }
      GeoBone head = getAnimationProcessor().getBone("armorHead");
      if (head != null) {
         head.setRotY(Mth.clamp(data.netHeadYaw(), -40.0F, 40.0F) * Mth.DEG_TO_RAD);
         head.setRotX(Mth.clamp(data.headPitch(), -40.0F, 40.0F) * Mth.DEG_TO_RAD);
      }
      float pitch = Mth.clamp(data.headPitch(), -40.0F, 40.0F) * Mth.DEG_TO_RAD;
      counterRotate("hair", pitch);
      counterRotate("hair1", pitch);
      counterRotate("hair2", pitch);
      counterRotate("bone4", pitch);
      counterRotate("bone24", pitch);
   }

   private void counterRotate(String name, float pitch) {
      GeoBone bone = getAnimationProcessor().getBone(name);
      if (bone != null) {
         bone.setRotX(-pitch);
      }
   }

   private static ResourceLocation resource(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }
}

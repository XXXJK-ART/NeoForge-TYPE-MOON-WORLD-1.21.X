package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import software.bernie.geckolib.model.GeoModel;

public class DiarmuidSpearModel extends GeoModel<DiarmuidSpearItem> {
   @Override
   public void setCustomAnimations(DiarmuidSpearItem object, long instanceId, software.bernie.geckolib.animation.AnimationState<DiarmuidSpearItem> state) {
      super.setCustomAnimations(object, instanceId, state);
      var root = getAnimationProcessor().getBone("bone");
      // The model's long axis is local Z and the root pivot is the unchanged grip.
      if (root != null) root.setScaleZ(object.spearType() == DiarmuidSpearItem.SpearType.GAE_BUIDHE
         ? (float)net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction.YELLOW_MODEL_LENGTH : 1.0F);
   }
   @Override
   public ResourceLocation getModelResource(DiarmuidSpearItem object) {
      String id = object == null ? "gae_dearg" : object.spearType().id();
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/" + id + ".geo.json");
   }

   @Override
   public ResourceLocation getTextureResource(DiarmuidSpearItem object) {
      String id = object == null ? "gae_dearg" : object.spearType().id();
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/item/" + id + ".png");
   }

   @Override
   public ResourceLocation getAnimationResource(DiarmuidSpearItem object) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/gae_bulg.animation.json");
   }
}

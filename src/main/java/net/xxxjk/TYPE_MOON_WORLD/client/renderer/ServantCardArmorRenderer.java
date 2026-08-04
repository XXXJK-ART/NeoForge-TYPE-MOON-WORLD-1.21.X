package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.world.entity.EquipmentSlot;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ServantCardArmorModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ServantCardArmorRenderer extends GeoArmorRenderer<ServantCardArmorItem> {
   public ServantCardArmorRenderer() {
      super(new ServantCardArmorModel());
      withScale(0.95F, 0.95F);
   }

   @Override
   protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
      setAllBonesVisible(false);
      boolean medea = this.currentStack != null
         && this.currentStack.getItem() instanceof ServantCardArmorItem armor
         && "medea".equals(armor.servantId());
      if (currentSlot == EquipmentSlot.CHEST) {
         setBoneVisible(this.body, true);
         setBoneVisible(this.rightArm, true);
         setBoneVisible(this.leftArm, true);
      } else if (currentSlot == EquipmentSlot.HEAD) {
         setBoneVisible(this.head, true);
      } else if (currentSlot == EquipmentSlot.LEGS) {
         if (medea) {
            return;
         }
         setBoneVisible(this.rightLeg, true);
         setBoneVisible(this.leftLeg, true);
      }
   }

   @Override
   public GeoBone getHeadBone(GeoModel<ServantCardArmorItem> model) {
      return model.getBone("armorHead").orElse(null);
   }

   @Override
   public GeoBone getBodyBone(GeoModel<ServantCardArmorItem> model) {
      return model.getBone("armorBody").orElse(null);
   }

   @Override
   public GeoBone getRightArmBone(GeoModel<ServantCardArmorItem> model) {
      return model.getBone("armorRightArm").orElse(null);
   }

   @Override
   public GeoBone getLeftArmBone(GeoModel<ServantCardArmorItem> model) {
      return model.getBone("armorLeftArm").orElse(null);
   }

   @Override
   public GeoBone getRightLegBone(GeoModel<ServantCardArmorItem> model) {
      return model.getBone("armorRightLeg").orElse(null);
   }

   @Override
   public GeoBone getLeftLegBone(GeoModel<ServantCardArmorItem> model) {
      return model.getBone("armorLeftLeg").orElse(null);
   }
}

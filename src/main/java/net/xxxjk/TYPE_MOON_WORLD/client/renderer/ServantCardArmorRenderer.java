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
   }

   @Override
   protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
      setAllBonesVisible(false);
      if (currentSlot == EquipmentSlot.CHEST) {
         setBoneVisible(this.body, true);
         setBoneVisible(this.rightArm, true);
         setBoneVisible(this.leftArm, true);
      } else if (currentSlot == EquipmentSlot.LEGS) {
         setBoneVisible(this.rightLeg, true);
         setBoneVisible(this.leftLeg, true);
      }
   }

   @Override
   public GeoBone getHeadBone(GeoModel<ServantCardArmorItem> model) {
      return model.getBone("armorHead").orElse(null);
   }
}

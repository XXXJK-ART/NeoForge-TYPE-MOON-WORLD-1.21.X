package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.world.entity.EquipmentSlot;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ServantHairArmorModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib armor renderer used only for the second, NPC-only hair pass. */
public final class ServantHairArmorRenderer extends ServantCardArmorRenderer {
   public ServantHairArmorRenderer() {
      super(new ServantHairArmorModel());
   }

   private ServantHairArmorRenderer(GeoModel<ServantCardArmorItem> model) {
      super(model);
   }

   @Override
   protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
      setAllBonesVisible(false);
      if (currentSlot == EquipmentSlot.HEAD) {
         setBoneVisible(this.head, true);
         revealBoneTree(this.head);
      }
   }

   @Override
   public GeoBone getHeadBone(GeoModel<ServantCardArmorItem> model) {
      return model.getBone("armorHead").orElse(null);
   }
}

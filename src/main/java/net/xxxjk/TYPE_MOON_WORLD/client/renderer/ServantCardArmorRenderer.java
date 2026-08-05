package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ServantCardArmorModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ServantCardArmorRenderer extends GeoArmorRenderer<ServantCardArmorItem> {
   public ServantCardArmorRenderer() {
      super(new ServantCardArmorModel());
      withScale(0.95F, 0.95F);
   }

   @Override
   public void prepForRender(Entity entity, ItemStack itemStack, EquipmentSlot equipmentSlot,
                             HumanoidModel<?> original, MultiBufferSource bufferSource,
                             float partialTick, float limbSwing, float limbSwingAmount,
                             float netHeadYaw, float headPitch) {
      super.prepForRender(entity, itemStack, equipmentSlot, original, bufferSource, partialTick,
         limbSwing, limbSwingAmount, netHeadYaw, headPitch);
      applyArmorSlotVisibility();
   }

   @Override
   public void actuallyRender(PoseStack poseStack, ServantCardArmorItem animatable, BakedGeoModel model,
                              @Nullable RenderType renderType, MultiBufferSource bufferSource,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick,
                              int packedLight, int packedOverlay, int colour) {
      // ServantEntity is also a GeoEntity. GeckoLib skips its default
      // slot-visibility pass for GeoEntities, so apply our armor-slot
      // visibility explicitly before every armor render.
      applyArmorSlotVisibility();
      super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
         isReRender, partialTick, packedLight, packedOverlay, colour);
   }

   private void applyArmorSlotVisibility() {
      if (this.currentSlot != null) {
         applyBoneVisibilityBySlot(this.currentSlot);
      }
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

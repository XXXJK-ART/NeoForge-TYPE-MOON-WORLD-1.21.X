package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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

   @Override
   public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                 int packedLight, int packedOverlay, int colour) {
      super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
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
         revealBoneTree(this.head);
         applyHeadEquipmentVisibility();
      } else if (currentSlot == EquipmentSlot.LEGS) {
         if (medea) {
            return;
         }
         setBoneVisible(this.rightLeg, true);
         setBoneVisible(this.leftLeg, true);
      }
   }

   private void applyHeadEquipmentVisibility() {
      // NPC servants keep their complete head model.  Only a player wearing
      // a servant card should avoid rendering the hair twice over the skin.
      if (!(this.currentEntity instanceof Player)) {
         return;
      }
      if (!(this.currentStack.getItem() instanceof ServantCardArmorItem armor)) {
         return;
      }
      String servantId = armor.servantId();
      switch (servantId) {
         case "gilgamesh_caster", "li_shuwen" -> {
            // These dedicated head models contain only the actual headwear:
            // Caster Gilgamesh's headpiece and Li Shuwen's glasses.
         }
         case "medusa" -> {
            hideBone("hair1");
            hideBone("hair2");
         }
         case "oda_nobunaga" -> {
            // Oda's independent head Geo is hair-only.  The complete armor
            // Geo is used here so bone18 remains while the duplicate hair is
            // hidden.
            hideBone("hair1");
            hideBone("hair2");
         }
         default -> setBoneVisible(this.head, false);
      }
   }

   private void hideBone(String name) {
      this.getGeoModel().getBone(name).ifPresent(bone -> bone.setHidden(true));
   }

   private void revealBoneTree(@Nullable GeoBone bone) {
      if (bone == null) {
         return;
      }
      bone.setHidden(false);
      bone.setChildrenHidden(false);
      for (GeoBone child : bone.getChildBones()) {
         revealBoneTree(child);
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

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ServantCardArmorModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ServantCardArmorRenderer extends GeoArmorRenderer<ServantCardArmorItem> {
   private boolean renderingLayeredHairPass;

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
      boolean layeredHair = !isReRender && usesLayeredHairTexture(animatable);
      List<BoneHiddenState> hairStates = layeredHair
         ? setHidden(true, "hair", "hair1", "hair2")
         : List.of();
      try {
         super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
            isReRender, partialTick, packedLight, packedOverlay, colour);
      } finally {
         restoreHidden(hairStates);
      }

      if (layeredHair) {
         List<BoneHiddenState> headwearStates = switch (animatable.servantId()) {
            case "medusa" -> setHidden(true, "eye mask");
            case "oda_nobunaga" -> setHidden(true, "bone18");
            default -> List.of();
         };
         ResourceLocation hairTexture = ResourceLocation.fromNamespaceAndPath(
            TYPE_MOON_WORLD.MOD_ID,
            "textures/models/armor/servant_card_" + animatable.servantId() + "_head.png"
         );
         RenderType hairRenderType = RenderType.entityCutoutNoCull(hairTexture);
         VertexConsumer hairBuffer = bufferSource.getBuffer(hairRenderType);
         this.renderingLayeredHairPass = true;
         try {
            super.actuallyRender(poseStack, animatable, model, hairRenderType, bufferSource, hairBuffer,
               true, partialTick, packedLight, packedOverlay, colour);
         } finally {
            this.renderingLayeredHairPass = false;
            restoreHidden(headwearStates);
         }
      }
   }

   @Override
   public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                 int packedLight, int packedOverlay, int colour) {
      if (this.renderingLayeredHairPass
         && this.animatable != null
         && "oda_nobunaga".equals(this.animatable.servantId())
         && "hair1".equals(bone.getName())) {
         if (bone.isHidden()) return;

         // The complete Oda model has four extra UV-zero guide planes in
         // hair1. They are absent from the dedicated hair model and pick up
         // stray pixels when rendered with the independent hair texture.
         List<GeoCube> cubes = bone.getCubes();
         int texturedHairCubeCount = Math.min(2, cubes.size());
         for (int i = 0; i < texturedHairCubeCount; i++) {
            poseStack.pushPose();
            renderCube(poseStack, cubes.get(i), buffer, packedLight, packedOverlay, colour);
            poseStack.popPose();
         }
         return;
      }

      super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
   }

   private boolean usesLayeredHairTexture(ServantCardArmorItem animatable) {
      if (this.currentSlot != EquipmentSlot.HEAD || animatable == null) return false;
      return switch (animatable.servantId()) {
         case "enkidu", "medusa", "oda_nobunaga", "paracelsus" -> true;
         default -> false;
      };
   }

   private List<BoneHiddenState> setHidden(boolean hidden, String... boneNames) {
      List<BoneHiddenState> states = new ArrayList<>(boneNames.length);
      for (String boneName : boneNames) {
         this.getGeoModel().getBone(boneName).ifPresent(bone -> {
            states.add(new BoneHiddenState(bone, bone.isHidden()));
            bone.setHidden(hidden);
         });
      }
      return states;
   }

   private static void restoreHidden(List<BoneHiddenState> states) {
      for (BoneHiddenState state : states) {
         state.bone().setHidden(state.hidden());
      }
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

   private record BoneHiddenState(GeoBone bone, boolean hidden) { }
}

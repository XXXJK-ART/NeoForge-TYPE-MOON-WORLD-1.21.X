package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ServantCardArmorModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ServantCardArmorRenderer extends GeoArmorRenderer<ServantCardArmorItem> {
   private ServantHairArmorRenderer hairRenderer;

   public ServantCardArmorRenderer() {
      this(new ServantCardArmorModel());
   }

   protected ServantCardArmorRenderer(GeoModel<ServantCardArmorItem> model) {
      super(model);
      withScale(0.95F, 0.95F);
   }

   @Override
   public RenderType getRenderType(ServantCardArmorItem animatable, ResourceLocation texture,
                                   @Nullable MultiBufferSource bufferSource, float partialTick) {
      if (this.currentEntity instanceof ServantEntity servant
         && ServantClipRenderHelper.shouldClip(servant, partialTick)) {
         return ServantClipRenderHelper.renderType(servant, texture, partialTick);
      }
      return super.getRenderType(animatable, texture, bufferSource, partialTick);
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
      if (!isReRender && shouldRenderNpcHair()) {
         if (this.hairRenderer == null) {
            this.hairRenderer = new ServantHairArmorRenderer();
         }
         this.hairRenderer.prepForRender(this.currentEntity, this.currentStack, EquipmentSlot.HEAD,
            this.baseModel, bufferSource, partialTick, limbSwing, limbSwingAmount, netHeadYaw, headPitch);
         this.hairRenderer.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, colour);
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
         revealBoneTree(this.head);
         applyHeadEquipmentVisibility();
      } else if (currentSlot == EquipmentSlot.LEGS) {
         if (medea) {
            return;
         }
         setBoneVisible(this.rightLeg, true);
         setBoneVisible(this.leftLeg, true);
      } else if (currentSlot == EquipmentSlot.FEET) {
         setBoneVisible(this.rightBoot, true);
         setBoneVisible(this.leftBoot, true);
      }
   }

   private void applyHeadEquipmentVisibility() {
      // NPC servants keep their complete head model.  Only a player wearing
      // a servant card should avoid rendering the hair twice over the skin.
      if (!(this.currentStack.getItem() instanceof ServantCardArmorItem armor)) {
         return;
      }
      String servantId = armor.servantId();
      switch (servantId) {
         case "enkidu", "medusa", "oda_nobunaga" -> {
            hideBone("hair1");
            hideBone("hair2");
            hideBone("bone11");
            hideBone("bone14");
            hideBone("bone5");
            hideBone("bone15");
         }
         case "paracelsus" -> hideBone("hair");
         case "sasaki_kojiro" -> hideBone("bone5");
         case "ushiwakamaru_rider" -> hideBone("bone24");
         case "zhao_yun_rider" -> hideBone("bone4");
         default -> { }
      }
   }

   private void hideBone(String name) {
      this.getGeoModel().getBone(name).ifPresent(bone -> bone.setHidden(true));
   }

   protected void revealBoneTree(@Nullable GeoBone bone) {
      if (bone == null) {
         return;
      }
      bone.setHidden(false);
      bone.setChildrenHidden(false);
      for (GeoBone child : bone.getChildBones()) {
         revealBoneTree(child);
      }
   }

   private boolean shouldRenderNpcHair() {
      if (!(this.currentEntity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)
         || this instanceof ServantHairArmorRenderer
         || this.currentEntity instanceof Player
         || this.currentSlot != EquipmentSlot.HEAD
         || !(this.currentStack.getItem() instanceof ServantCardArmorItem armor)) {
         return false;
      }
      return switch (armor.servantId()) {
         case "enkidu", "medusa", "oda_nobunaga", "paracelsus", "sasaki_kojiro",
            "ushiwakamaru_rider", "zhao_yun_rider", "baobhan_sith" -> true;
         default -> false;
      };
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

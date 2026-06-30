package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwSkyGearEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public class UbwSkyGearEntityRenderer extends EntityRenderer<UbwSkyGearEntity> {
   private final EmiyaProjectionItemRenderer itemRenderer = new EmiyaProjectionItemRenderer();

   public UbwSkyGearEntityRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(UbwSkyGearEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
      poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getYRot()));
      float scale = entity.getGearScale();
      poseStack.scale(scale, scale, scale);
      this.itemRenderer.renderByItem(stackFor(entity.getVariant()), ItemDisplayContext.NONE, poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private ItemStack stackFor(int variant) {
      return switch (variant) {
         case 1 -> new ItemStack((net.minecraft.world.level.ItemLike)ModItems.UBW_METAL_2.get());
         case 2 -> new ItemStack((net.minecraft.world.level.ItemLike)ModItems.UBW_METAL_3.get());
         default -> new ItemStack((net.minecraft.world.level.ItemLike)ModItems.UBW_METAL_1.get());
      };
   }

   @Override
   public ResourceLocation getTextureLocation(UbwSkyGearEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

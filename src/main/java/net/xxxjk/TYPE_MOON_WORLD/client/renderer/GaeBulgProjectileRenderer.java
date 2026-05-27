package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;

public class GaeBulgProjectileRenderer extends EntityRenderer<GaeBulgProjectileEntity> {
   public GaeBulgProjectileRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(GaeBulgProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float ryaw = 90.0F + entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
      float rpitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;
      ItemStack itemStack = entity.getItem();

      poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-rpitch));
      poseStack.translate(-0.59, -0.59, 0.0);
      poseStack.scale(1.75F, 1.75F, 1.75F);
      if (!itemStack.isEmpty()) {
         BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, entity.level(), (LivingEntity)null, entity.getId());
         Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
      }
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(GaeBulgProjectileEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

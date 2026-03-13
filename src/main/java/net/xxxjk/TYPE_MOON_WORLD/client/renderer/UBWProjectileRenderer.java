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
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;

public class UBWProjectileRenderer extends EntityRenderer<UBWProjectileEntity> {
   public UBWProjectileRenderer(Context context) {
      super(context);
   }

   public void render(UBWProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float scale = 1.75F;
      float ryaw = 90.0F + entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
      float rpitch = 135.0F - entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;
      ItemStack itemStack = entity.getItem();
      if (!itemStack.isEmpty() && itemStack.getItem() instanceof NoblePhantasmItem) {
         rpitch -= 45.0F;
      }

      poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
      poseStack.mulPose(Axis.ZP.rotationDegrees(rpitch));
      poseStack.translate(-0.59, -0.59, 0.0);
      poseStack.scale(scale, scale, scale);
      if (itemStack.isEmpty()) {
         poseStack.popPose();
      } else {
         BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, entity.level(), (LivingEntity)null, entity.getId());

         try {
            Minecraft.getInstance()
               .getItemRenderer()
               .render(itemStack, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
         } catch (Exception var13) {
         }

         poseStack.popPose();
         super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      }
   }

   public ResourceLocation getTextureLocation(UBWProjectileEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

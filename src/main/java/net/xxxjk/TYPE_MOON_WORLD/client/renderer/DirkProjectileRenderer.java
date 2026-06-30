package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity;

public class DirkProjectileRenderer extends EntityRenderer<DirkProjectileEntity> {
   private final DirkSmallKnifeRenderer itemRenderer;

   public DirkProjectileRenderer(Context context) {
      super(context);
      this.itemRenderer = new DirkSmallKnifeRenderer();
   }

   @Override
   public void render(DirkProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float ryaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
      float rpitch = Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot());
      ItemStack itemStack = entity.getItem();
      poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-rpitch));
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.scale(0.4F, 0.4F, 0.4F);
      if (!itemStack.isEmpty()) {
         this.itemRenderer.renderByItem(itemStack, ItemDisplayContext.NONE, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
      }
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(DirkProjectileEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

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
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;

public class GaeBulgProjectileRenderer extends EntityRenderer<GaeBulgProjectileEntity> {
   private final GaeBulgRenderer itemRenderer;

   public GaeBulgProjectileRenderer(Context context) {
      super(context);
      this.itemRenderer = new GaeBulgRenderer();
   }

   @Override
   public void render(GaeBulgProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float ryaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
      float rpitch = Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot());
      ItemStack itemStack = entity.getItem();

      // Gae Bulg's spearhead points toward local -Z in the geo model, so flip 180 degrees after facing motion.
      poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-rpitch));
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.scale(0.95F, 0.95F, 0.95F);
      if (!itemStack.isEmpty()) {
         this.itemRenderer.renderByItem(itemStack, ItemDisplayContext.NONE, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
      }
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(GaeBulgProjectileEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

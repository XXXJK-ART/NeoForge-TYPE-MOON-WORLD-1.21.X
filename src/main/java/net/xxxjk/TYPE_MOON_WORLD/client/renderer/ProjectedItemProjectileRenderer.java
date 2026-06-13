package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.EmiyaProjectionItem;

public class ProjectedItemProjectileRenderer<T extends ThrowableItemProjectile> extends EntityRenderer<T> {
   private final EmiyaProjectionItemRenderer projectionItemRenderer = new EmiyaProjectionItemRenderer();
   private final ItemRenderer vanillaItemRenderer;

   public ProjectedItemProjectileRenderer(Context context) {
      super(context);
      this.vanillaItemRenderer = context.getItemRenderer();
   }

   @Override
   public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
      poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
      ItemStack stack = entity.getItem();
      if (stack.getItem() instanceof EmiyaProjectionItem) {
         this.projectionItemRenderer.renderByItem(stack, ItemDisplayContext.NONE, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
      } else {
         this.vanillaItemRenderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
      }
      poseStack.popPose();
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(T entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

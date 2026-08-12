package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
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
      float ryaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
      float rpitch = Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot());
      ItemStack stack = entity.getItem();
      if (entity instanceof EnkiduEarthWeaponProjectileEntity) {
         poseStack.mulPose(Axis.YP.rotationDegrees(90.0F + ryaw));
         poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F - rpitch));
         poseStack.translate(-0.59, -0.59, 0.0);
         poseStack.scale(1.55F, 1.55F, 1.55F);
         this.vanillaItemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
         poseStack.popPose();
         super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
         return;
      }
      if (entity instanceof EmiyaThrownWeaponEntity thrown && thrown.isPiercingImpact()) {
         poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
         poseStack.mulPose(Axis.XP.rotationDegrees(-rpitch));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         if (thrown.isLancelotIronRodProjectile()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
         }
         poseStack.scale(1.15F, 1.15F, 1.15F);
         this.vanillaItemRenderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
         poseStack.popPose();
         super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
         return;
      }
      poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-rpitch));
      if (stack.getItem() instanceof EmiyaProjectionItem) {
         if (isKanshouBakuya(stack)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees((entity.tickCount + partialTicks) * 42.0F));
         }
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

   private static boolean isKanshouBakuya(ItemStack stack) {
      return stack.is(ModItems.GAN_JIANG.get())
         || stack.is(ModItems.MO_YE.get())
         || stack.is(ModItems.GAN_JIANG_OVEREDGE.get())
         || stack.is(ModItems.MO_YE_OVEREDGE.get());
   }
}

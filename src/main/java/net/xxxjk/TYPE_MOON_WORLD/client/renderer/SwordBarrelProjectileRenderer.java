package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.component.DataComponents;
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
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;

public class SwordBarrelProjectileRenderer extends EntityRenderer<SwordBarrelProjectileEntity> {
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public SwordBarrelProjectileRenderer(Context context) {
      super(context);
   }

   public void render(SwordBarrelProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float scale = 1.75F;
      float time = entity.tickCount + partialTicks;
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
         ItemStack renderStack = itemStack.copy();
         renderStack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
         renderStack.remove(DataComponents.ENCHANTMENTS);
         BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(renderStack, entity.level(), (LivingEntity)null, entity.getId());

         try {
            Minecraft.getInstance()
               .getItemRenderer()
               .render(renderStack, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
         } catch (Exception var13) {
         }

         poseStack.popPose();
         if (!entity.isHovering() && !entity.tracePos.isEmpty()) {
            ProjectileVisualEffectHelper.renderRibbonTrail(
               entity.tracePos,
               entity.getPosition(partialTicks),
               Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition(),
               poseStack,
               buffer,
               TRAIL_TEXTURE,
               0.18F,
               MagicCircuitColorHelper.COLOR_SWORD,
               0.6F
            );
         }
         super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      }
   }

   public ResourceLocation getTextureLocation(SwordBarrelProjectileEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

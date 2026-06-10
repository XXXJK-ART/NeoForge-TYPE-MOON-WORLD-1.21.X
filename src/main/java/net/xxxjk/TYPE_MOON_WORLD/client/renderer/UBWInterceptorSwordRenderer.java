package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;

public class UBWInterceptorSwordRenderer extends EntityRenderer<UBWInterceptorSwordEntity> {
   private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public UBWInterceptorSwordRenderer(Context context) {
      super(context);
   }

   public void render(UBWInterceptorSwordEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      float ryaw = 90.0F + entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
      float rpitch = 135.0F - entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;
      poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
      poseStack.mulPose(Axis.ZP.rotationDegrees(rpitch));
      poseStack.translate(-0.59, -0.59, 0.0);
      poseStack.scale(1.55F, 1.55F, 1.55F);

      ItemStack renderStack = entity.getItem().copy();
      renderStack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
      renderStack.remove(DataComponents.ENCHANTMENTS);
      BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(renderStack, entity.level(), (LivingEntity)null, entity.getId());
      Minecraft.getInstance()
         .getItemRenderer()
         .render(renderStack, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
      poseStack.popPose();

      if (!entity.tracePos.isEmpty()) {
         ProjectileVisualEffectHelper.renderRibbonTrail(
            entity.tracePos,
            entity.getPosition(partialTicks),
            Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition(),
            poseStack,
            buffer,
            TRAIL_TEXTURE,
            0.12F,
            MagicCircuitColorHelper.COLOR_SWORD,
            0.45F
         );
      }

      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   public ResourceLocation getTextureLocation(UBWInterceptorSwordEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

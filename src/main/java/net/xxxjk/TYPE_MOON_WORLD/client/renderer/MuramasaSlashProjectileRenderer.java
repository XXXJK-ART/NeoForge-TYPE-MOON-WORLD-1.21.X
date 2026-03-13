package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.MuramasaSlashProjectileEntity;

public class MuramasaSlashProjectileRenderer extends EntityRenderer<MuramasaSlashProjectileEntity> {
   public MuramasaSlashProjectileRenderer(Context context) {
      super(context);
   }

   public void render(MuramasaSlashProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   public ResourceLocation getTextureLocation(MuramasaSlashProjectileEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}

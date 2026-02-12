package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.MuramasaSlashProjectileEntity;
import net.minecraft.world.inventory.InventoryMenu;

public class MuramasaSlashProjectileRenderer extends EntityRenderer<MuramasaSlashProjectileEntity> {

    public MuramasaSlashProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MuramasaSlashProjectileEntity entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        // No model, just particles handled in entity tick
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MuramasaSlashProjectileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

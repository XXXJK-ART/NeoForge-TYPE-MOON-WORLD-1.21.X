package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;

@SuppressWarnings({"null", "deprecation", "unchecked"})
public class SwordBarrelProjectileRenderer extends EntityRenderer<SwordBarrelProjectileEntity> {

    public SwordBarrelProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SwordBarrelProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float scale = 1.75F;
        
        // Calculate rotation
        float ryaw = 90.0F + entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
        float rpitch = 135.0F - entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;

        // Apply Noble Phantasm specific rotation (tip forward)
        ItemStack itemStack = entity.getItem();
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem) {
            // For 3D Geo models, they are often horizontal by default.
            // We need to rotate them to point forward.
            // Based on current ryaw/rpitch, the default 135/90 usually points standard items.
            // We add a 45 degree tilt to make them straight.
            rpitch -= 45.0F;
        }

        // Apply rotation
        poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rpitch));

        // Translation and Scale
        // Consistent with BrokenPhantasmRenderer: -0.59, -0.59, 0.0
        poseStack.translate(-0.59, -0.59, 0.0F);
        poseStack.scale(scale, scale, scale);

        if (itemStack.isEmpty()) {
            poseStack.popPose();
            return;
        }

        BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, entity.level(), (LivingEntity)null, entity.getId());
        
        try {
            Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
        } catch (Exception e) {
        }
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordBarrelProjectileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

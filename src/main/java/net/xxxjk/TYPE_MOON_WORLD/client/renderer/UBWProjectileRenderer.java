package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;

@SuppressWarnings({"null", "deprecation", "unchecked"})
public class UBWProjectileRenderer extends EntityRenderer<UBWProjectileEntity> {

    public UBWProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(UBWProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float scale = 1.5F; 
        
        float ryaw = entity.getYRot();
        // Standard Sword Model (FIXED) is 45 degrees (Tip Top-Right).
        // Rotate Z by 135 to make Tip point Down (-90 relative to 45 is -135? No, wait.)
        // Based on ProjectedSwordRenderer (which uses ~135 for Down), we use 135.
        // -135 was Horizontal. 135 is 90 deg from -135 (225), so it should be Vertical.
        float rpitch = 135.0F;

        // Apply rotation
        poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rpitch));

        // Scale
        poseStack.scale(scale, scale, scale);

        // Get Item
        ItemStack itemStack = entity.getItem();
        if (itemStack.isEmpty()) {
            poseStack.popPose();
            return;
        }

        BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, entity.level(), (LivingEntity)null, entity.getId());
        
        try {
            // Render item with FIXED context to allow custom rotation
            Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.FIXED, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
        } catch (Exception e) {
            // Silent catch
        }
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(UBWProjectileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

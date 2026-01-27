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
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;

public class BrokenPhantasmRenderer extends EntityRenderer<BrokenPhantasmProjectileEntity> {

    public BrokenPhantasmRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BrokenPhantasmProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float scale = 1.75F;
        
        // Calculate rotation
        // 修复反编译的变量名错误: 使用Mojang映射的字段名
        float ryaw = 90.0F + entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
        float rpitch = 135.0F - entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;

        // Apply rotation
        // 使用Axis类替代RenderUtils.getQuaternion
        poseStack.mulPose(Axis.YP.rotationDegrees(ryaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rpitch));

        // Translation and Scale
        poseStack.translate(-0.59, -0.59, 0.0F);
        poseStack.scale(scale, scale, scale);

        // Get Item
        ItemStack itemStack = entity.getItem();
        if (itemStack.isEmpty()) {
            poseStack.popPose();
            return;
        }

        BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, entity.level(), (LivingEntity)null, entity.getId());
        
        try {
            // Render item
            Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedModel);
        } catch (Exception e) {
            // Silent catch as requested
        }
        
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BrokenPhantasmProjectileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

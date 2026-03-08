package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGander;

public class GanderProjectileRenderer extends EntityRenderer<GanderProjectileEntity> {
    private final ItemRenderer itemRenderer;
    private final float baseScale;
    private final boolean fullBright;

    public GanderProjectileRenderer(EntityRendererProvider.Context context) {
        this(context, 1.0F, true);
    }

    public GanderProjectileRenderer(EntityRendererProvider.Context context, float baseScale, boolean fullBright) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.baseScale = baseScale;
        this.fullBright = fullBright;
    }

    @Override
    public void render(GanderProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float visualScale = entity.getVisualScale();
        float finalScale = this.baseScale * visualScale;
        int light = this.fullBright ? LightTexture.FULL_BRIGHT : packedLight;

        poseStack.pushPose();
        if (entity.isChargingPreview()) {
            LivingEntity anchorSource = null;
            Entity owner = entity.getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                anchorSource = livingOwner;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && owner != null && owner.getId() == minecraft.player.getId()) {
                anchorSource = minecraft.player;
            }
            if (anchorSource != null) {
                Vec3 anchor = MagicGander.getChargeAnchor(anchorSource);
                Vec3 renderPos = entity.getPosition(partialTicks);
                Vec3 delta = anchor.subtract(renderPos);
                poseStack.translate(delta.x, delta.y, delta.z);
            }
        }
        // Scale around model origin to avoid direction-dependent drift while charging/rotating.
        poseStack.scale(finalScale, finalScale, finalScale);
        this.itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.FIXED,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GanderProjectileEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}

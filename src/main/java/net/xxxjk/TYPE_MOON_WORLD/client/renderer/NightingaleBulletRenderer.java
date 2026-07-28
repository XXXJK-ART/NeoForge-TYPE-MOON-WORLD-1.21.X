package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.NightingaleBulletEntity;

public final class NightingaleBulletRenderer extends EntityRenderer<NightingaleBulletEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/item/bullet.png");
   private static final ResourceLocation TRAIL = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

   public NightingaleBulletRenderer(Context context) { super(context); }

   @Override
   public void render(NightingaleBulletEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
      ProjectileVisualEffectHelper.renderRibbonTrail(entity.tracePos, entity.getPosition(partialTick),
         this.entityRenderDispatcher.camera.getPosition(), poseStack, buffer, TRAIL, 0.045F, 0xFFE6A0, 0.5F);
      super.render(entity, yaw, partialTick, poseStack, buffer, light);
   }

   @Override public ResourceLocation getTextureLocation(NightingaleBulletEntity entity) { return TEXTURE; }
}

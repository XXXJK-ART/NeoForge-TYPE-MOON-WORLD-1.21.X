package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorseEntity;

public final class ApocalypseHorseRenderer extends EntityRenderer<ApocalypseHorseEntity> {
   private SkeletonHorse proxy;

   public ApocalypseHorseRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.75F;
   }

   @Override
   public void render(ApocalypseHorseEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
      if (this.proxy == null && Minecraft.getInstance().level != null) {
         this.proxy = net.minecraft.world.entity.EntityType.SKELETON_HORSE.create(Minecraft.getInstance().level);
      }
      if (this.proxy == null) return;
      this.proxy.setYRot(entity.getYRot());
      this.proxy.setXRot(entity.getXRot());
      this.proxy.yRotO = entity.yRotO;
      this.proxy.xRotO = entity.xRotO;
      this.proxy.yBodyRot = entity.yBodyRot;
      this.proxy.yBodyRotO = entity.yBodyRotO;
      this.proxy.walkAnimation.setSpeed(entity.walkAnimation.speed());
      Minecraft.getInstance().getEntityRenderDispatcher().render(this.proxy, 0.0, 0.0, 0.0, yaw, partialTick, poseStack, buffers, light);
   }

   @Override
   public ResourceLocation getTextureLocation(ApocalypseHorseEntity entity) {
      return ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_skeleton.png");
   }
}

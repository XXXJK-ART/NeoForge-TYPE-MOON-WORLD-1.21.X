package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;
import org.jetbrains.annotations.Nullable;

public final class MacedonianSoldierRenderer extends HumanoidMobRenderer<MacedonianSoldierEntity, PlayerModel<MacedonianSoldierEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "textures/entity/macedonian_soldier.png");
   private float renderPartialTick;

   public MacedonianSoldierRenderer(EntityRendererProvider.Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
      this.addLayer(new PetrifiedLivingLayer<>(this));
   }

   @Override
   public void render(MacedonianSoldierEntity entity, float yaw, float partialTick, PoseStack poseStack,
                      MultiBufferSource buffer, int packedLight) {
      float scale = entity.getVisualScale();
      float dissolve = entity.getShortServantDissolveProgress(partialTick);
      this.renderPartialTick = partialTick;
      this.model.rightArmPose = HumanoidModel.ArmPose.ITEM;
      this.model.leftArmPose = HumanoidModel.ArmPose.ITEM;
      poseStack.pushPose();
      if (entity.isShortServantDissolving()) {
         float fade = 1.0F - dissolve;
         poseStack.translate(0.0F, dissolve * 0.18F, 0.0F);
         super.render(entity, yaw, partialTick, poseStack, new FadingBufferSource(buffer, fade), packedLight);
      } else {
         poseStack.scale(scale, scale, scale);
         super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
      }
      poseStack.popPose();
      this.model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
      this.model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
   }

   @Override
   @Nullable
   protected RenderType getRenderType(MacedonianSoldierEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
      if (bodyVisible && entity.isShortServantDissolving()) {
         return ClippedEntityRenderType.servant(
            this.getTextureLocation(entity),
            entity.getShortServantDissolveProgress(this.renderPartialTick),
            ClippedEntityRenderType.ClipMode.DISSOLVE,
            -0.65F,
            Math.max(1.85F, entity.getBbHeight() + 0.2F)
         );
      }
      return super.getRenderType(entity, bodyVisible, translucent, glowing);
   }

   @Override
   protected float getFlipDegrees(MacedonianSoldierEntity livingEntity) {
      return livingEntity.isShortServantDissolving() ? 0.0F : super.getFlipDegrees(livingEntity);
   }

   @Override
   protected float getWhiteOverlayProgress(MacedonianSoldierEntity livingEntity, float partialTicks) {
      return livingEntity.isShortServantDissolving() ? 0.0F : super.getWhiteOverlayProgress(livingEntity, partialTicks);
   }

   @Override
   protected void scale(MacedonianSoldierEntity entity, PoseStack poseStack, float partialTickTime) {
      if (entity.isShortServantDissolving()) {
         float dissolve = entity.getShortServantDissolveProgress(partialTickTime);
         float scale = entity.getVisualScale() * (1.0F - dissolve * 0.16F);
         poseStack.scale(scale, scale, scale);
      }
   }

   private static final class FadingBufferSource implements MultiBufferSource {
      private final MultiBufferSource delegate;
      private final float alpha;

      private FadingBufferSource(MultiBufferSource delegate, float alpha) {
         this.delegate = delegate;
         this.alpha = Math.max(0.0F, Math.min(1.0F, alpha));
      }

      @Override
      public VertexConsumer getBuffer(RenderType renderType) {
         return new FadingVertexConsumer(this.delegate.getBuffer(renderType), this.alpha);
      }
   }

   private static final class FadingVertexConsumer implements VertexConsumer {
      private final VertexConsumer delegate;
      private final float alpha;

      private FadingVertexConsumer(VertexConsumer delegate, float alpha) {
         this.delegate = delegate;
         this.alpha = alpha;
      }

      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         this.delegate.addVertex(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         this.delegate.setColor(red, green, blue, Math.max(0, Math.min(255, Math.round(alpha * this.alpha))));
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         this.delegate.setUv(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         this.delegate.setUv1(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         this.delegate.setUv2(u, v);
         return this;
      }

      @Override
      public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
         this.delegate.setNormal(normalX, normalY, normalZ);
         return this;
      }
   }

   @Override
   public ResourceLocation getTextureLocation(MacedonianSoldierEntity entity) {
      return TEXTURE;
   }
}

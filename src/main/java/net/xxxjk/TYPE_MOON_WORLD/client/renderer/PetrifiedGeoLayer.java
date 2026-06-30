package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PetrifiedGeoLayer<T extends Entity & GeoAnimatable> extends GeoRenderLayer<T> {
   private static final ResourceLocation STONE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/stone.png");

   public PetrifiedGeoLayer(GeoEntityRenderer<T> renderer) {
      super(renderer);
   }

   @Override
   public void render(
      PoseStack poseStack,
      T animatable,
      BakedGeoModel bakedModel,
      @Nullable RenderType renderType,
      MultiBufferSource bufferSource,
      @Nullable VertexConsumer buffer,
      float partialTick,
      int packedLight,
      int packedOverlay
   ) {
      if (!(animatable instanceof LivingEntity living) || living.isInvisible() || !living.hasEffect(ModMobEffects.PETRIFIED)) {
         return;
      }

      poseStack.pushPose();
      poseStack.scale(1.01F, 1.01F, 1.01F);
      RenderType stoneRenderType = RenderType.entityCutoutNoCull(STONE_TEXTURE);
      VertexConsumer stoneBuffer = bufferSource.getBuffer(stoneRenderType);
      this.getRenderer().reRender(
         bakedModel,
         poseStack,
         bufferSource,
         animatable,
         stoneRenderType,
         stoneBuffer,
         partialTick,
         packedLight,
         packedOverlay,
         -1
      );
      poseStack.popPose();
   }
}

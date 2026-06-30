package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;

public class ArtoriaExcaliburBeamRenderer extends EntityRenderer<ArtoriaExcaliburBeamEntity> {
   private static final ResourceLocation INVISIBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/empty.png");

   public ArtoriaExcaliburBeamRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(ArtoriaExcaliburBeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(ArtoriaExcaliburBeamEntity entity) {
      return INVISIBLE_TEXTURE;
   }
}

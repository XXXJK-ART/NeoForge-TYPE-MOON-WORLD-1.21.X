package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.xxxjk.TYPE_MOON_WORLD.client.model.BlackKeyModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BlackKeyRenderer extends GeoItemRenderer<BlackKeyItem> {
   public BlackKeyRenderer() {
      super(new BlackKeyModel());
      withScale(1.5F);
   }

   @Override
   public long getInstanceId(BlackKeyItem animatable) {
      return Long.MIN_VALUE | Integer.toUnsignedLong(System.identityHashCode(getCurrentItemStack()));
   }

   @Override
   public void preRender(PoseStack poseStack, BlackKeyItem animatable, BakedGeoModel model,
                         @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                         boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
      poseStack.translate(0.0, -0.3, 0.0);
      super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
   }
}

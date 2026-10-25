package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ExcaliburGoldenModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburGoldenItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.Color;

public class ExcaliburGoldenRenderer extends GeoItemRenderer<ExcaliburGoldenItem> {
   /** The texture carries the saturated gold; white preserves its hot highlights. */
   private static final Color EMISSIVE_COLOR = Color.ofRGBA(255, 255, 255, 255);

   public ExcaliburGoldenRenderer() {
      super(new ExcaliburGoldenModel());
   }

   @Override
   public RenderType getRenderType(ExcaliburGoldenItem animatable, ResourceLocation texture,
                                  @Nullable MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucentEmissive(texture);
   }

   @Override
   public Color getRenderColor(ExcaliburGoldenItem animatable, float partialTick, int packedLight) {
      return EMISSIVE_COLOR;
   }
}

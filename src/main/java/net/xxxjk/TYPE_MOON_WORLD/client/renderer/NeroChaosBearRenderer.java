package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.PolarBear;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class NeroChaosBearRenderer extends PolarBearRenderer {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "textures/entity/nero_chaos_beast.png");

   public NeroChaosBearRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public ResourceLocation getTextureLocation(PolarBear entity) {
      return TEXTURE;
   }
}

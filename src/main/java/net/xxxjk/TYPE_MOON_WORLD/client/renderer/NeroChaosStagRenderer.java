package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.goat.Goat;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class NeroChaosStagRenderer extends GoatRenderer {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "textures/entity/nero_chaos_beast.png");

   public NeroChaosStagRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public ResourceLocation getTextureLocation(Goat entity) {
      return TEXTURE;
   }
}

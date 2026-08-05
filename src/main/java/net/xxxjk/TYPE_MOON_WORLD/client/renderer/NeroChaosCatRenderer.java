package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class NeroChaosCatRenderer extends CatRenderer {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "textures/entity/nero_chaos_beast.png");

   public NeroChaosCatRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public ResourceLocation getTextureLocation(Cat entity) {
      return TEXTURE;
   }
}

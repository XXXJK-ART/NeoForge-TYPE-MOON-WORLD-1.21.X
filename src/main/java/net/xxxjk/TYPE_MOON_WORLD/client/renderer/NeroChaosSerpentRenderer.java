package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Silverfish;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class NeroChaosSerpentRenderer extends SilverfishRenderer {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "textures/entity/nero_chaos_beast.png");

   public NeroChaosSerpentRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public ResourceLocation getTextureLocation(Silverfish entity) {
      return TEXTURE;
   }
}

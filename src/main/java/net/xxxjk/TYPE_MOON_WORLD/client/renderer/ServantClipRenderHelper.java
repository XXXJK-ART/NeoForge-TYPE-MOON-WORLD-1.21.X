package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class ServantClipRenderHelper {
   private ServantClipRenderHelper() {
   }

   public static boolean shouldClip(ServantEntity entity, float partialTick) {
      return entity.isSpiritualDissolving() || entity.getSpiritualManifestProgress(partialTick) < 1.0F;
   }

   public static RenderType renderType(ServantEntity entity, ResourceLocation texture, float partialTick) {
      float minY = -0.65F;
      float maxY = Math.max(1.85F, entity.getBbHeight() + 0.2F);
      if (entity.isSpiritualDissolving()) {
         return ClippedEntityRenderType.servant(
            texture,
            entity.getSpiritualDissolveProgress(partialTick),
            ClippedEntityRenderType.ClipMode.DISSOLVE,
            minY,
            maxY
         );
      }
      return ClippedEntityRenderType.servant(
         texture,
         entity.getSpiritualManifestProgress(partialTick),
         ClippedEntityRenderType.ClipMode.MANIFEST,
         minY,
         maxY
      );
   }
}

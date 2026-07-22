package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Parrot;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public final class PaleRiderCrowRenderer extends ParrotRenderer {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "textures/entity/pale_rider_crow.png"
   );

   public PaleRiderCrowRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.model.root().getChild("head").getChild("feather").visible = false;
      this.shadowRadius = 0.2F;
   }

   @Override
   public ResourceLocation getTextureLocation(Parrot entity) {
      return TEXTURE;
   }
}

package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GenericServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

/** Steve-style renderer for data-driven servants spawned through the summon system. */
public final class GenericServantRenderer extends HumanoidServantRenderer<GenericServantEntity> {
   public GenericServantRenderer(EntityRendererProvider.Context context) {
      super(context, GenericServantRenderer::textureFor);
   }

   private static ResourceLocation textureFor(GenericServantEntity entity) {
      ServantDefinition definition = entity.getDefinition();
      if (definition != null) {
         ResourceLocation parsed = parseTexture(definition.texturePath());
         if (parsed != null) {
            return parsed;
         }
      }

      String servantId = entity.getServantId();
      int separator = servantId == null ? -1 : servantId.indexOf(':');
      if (separator >= 0) {
         servantId = servantId.substring(separator + 1);
      }
      if (servantId == null || servantId.isBlank()) {
         return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/entity/empty.png");
      }
      return ResourceLocation.fromNamespaceAndPath(
         TYPE_MOON_WORLD.MOD_ID, "textures/entity/" + textureNameFor(servantId) + ".png");
   }

   private static String textureNameFor(String servantId) {
      return switch (servantId) {
         case "gilgamesh_caster" -> "caster_gilgamesh";
         default -> servantId;
      };
   }

   private static ResourceLocation parseTexture(String value) {
      if (value == null || value.isBlank()) {
         return null;
      }
      try {
         ResourceLocation parsed = ResourceLocation.tryParse(value.trim());
         return parsed != null && parsed.getPath().startsWith("textures/") ? parsed : null;
      } catch (RuntimeException ignored) {
         return null;
      }
   }
}

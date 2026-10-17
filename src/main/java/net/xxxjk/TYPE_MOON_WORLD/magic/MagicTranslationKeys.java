package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.typemoonworld.api.MagicIdMigration;

/** Translation keys shared by magic menus, tooltips and messages. */
public final class MagicTranslationKeys {
   private MagicTranslationKeys() {}

   public static String name(String magicId) {
      var definition = MagicDefinitionRegistry.get(magicId);
      if (definition != null && !definition.nameKey().isBlank()) return definition.nameKey();
      ResourceLocation id = id(magicId);
      return "magic." + id.getNamespace() + "." + id.getPath() + ".name";
   }

   public static String shortName(String magicId) {
      ResourceLocation id = id(magicId);
      return "key." + id.getNamespace() + ".magic." + id.getPath() + ".short";
   }

   public static String selectedName(String magicId) {
      ResourceLocation id = id(magicId);
      return "key." + id.getNamespace() + ".magic." + id.getPath() + ".selected";
   }

   public static String description(String magicId) {
      ResourceLocation id = id(magicId);
      return "magic." + id.getNamespace() + "." + id.getPath() + ".desc";
   }

   private static ResourceLocation id(String magicId) {
      String migrated = MagicIdMigration.normalize(magicId);
      if (migrated == null || migrated.isBlank()) migrated = "unknown";
      var definition = MagicDefinitionRegistry.get(migrated);
      if (definition != null) return definition.id();
      ResourceLocation id = ResourceLocation.tryParse(migrated.indexOf(':') >= 0
         ? migrated : TYPE_MOON_WORLD.MOD_ID + ":" + migrated);
      return id == null ? ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "unknown") : id;
   }
}

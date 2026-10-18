package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MagicTranslationsTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void namespacedAndLegacyIdsResolveToTheSameKeys() {
      for (String id : List.of("flight_magic", "fire_magic", "guard_boundary", "gander")) {
         assertEquals("magic.typemoonworld." + id + ".name", MagicTranslationKeys.name(id));
         assertEquals(MagicTranslationKeys.name(id), MagicTranslationKeys.name("typemoonworld:" + id));
         assertEquals(MagicTranslationKeys.shortName(id), MagicTranslationKeys.shortName("typemoonworld:" + id));
         assertEquals(MagicTranslationKeys.selectedName(id), MagicTranslationKeys.selectedName("typemoonworld:" + id));
      }
      assertEquals(MagicTranslationKeys.name("gander"), MagicTranslationKeys.name("typemoonworld:gandr"));
      assertEquals("magic.otheraddon.example.name", MagicTranslationKeys.name("otheraddon:example"));
      assertEquals("key.otheraddon.magic.example.short", MagicTranslationKeys.shortName("otheraddon:example"));
   }

   @Test
   void everyMagicHasTranslatedNamesForAllUiContexts() throws IOException {
      for (String language : List.of("zh_cn", "en_us")) {
         JsonObject translations = read(RESOURCES.resolve("assets/typemoonworld/lang/" + language + ".json"));
         for (String key : translations.keySet()) {
            if (!key.startsWith("magic.typemoonworld.") || !key.endsWith(".name")) continue;
            String id = key.substring("magic.typemoonworld.".length(), key.length() - ".name".length());
            for (String suffix : List.of("short", "selected")) {
               String contextKey = "key.typemoonworld.magic." + id + "." + suffix;
               assertTrue(translations.has(contextKey), language + " is missing " + contextKey);
               assertFalse(translations.get(contextKey).getAsString().isBlank(), contextKey);
            }
         }
         try (var definitions = Files.list(RESOURCES.resolve("data/typemoonworld/magic/definitions"))) {
            for (Path path : definitions.filter(p -> p.toString().endsWith(".json")).toList()) {
               JsonObject definition = read(path);
               if (definition.has("name_key")) {
                  String key = definition.get("name_key").getAsString();
                  assertTrue(translations.has(key), language + " is missing " + key);
               }
            }
         }
      }
   }

   private static JsonObject read(Path path) throws IOException {
      try (Reader reader = Files.newBufferedReader(path)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      }
   }
}

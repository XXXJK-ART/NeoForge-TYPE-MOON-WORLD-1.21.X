package net.xxxjk.TYPE_MOON_WORLD.talent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TalentPassiveResourceTest {
   private static final Path RESOURCES = Path.of("src", "main", "resources");

   @Test
   void talentDefinitionsAreWheelOnlyAndExcludedFromOtherAcquisitionPaths() throws IOException {
      for (String id : TalentService.IDS) {
         Path path = RESOURCES.resolve("data/typemoonworld/magic/definitions/" + id + ".json");
         try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject definition = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("typemoonworld:talent", definition.get("category").getAsString());
            assertEquals(0.0, definition.get("mana_cost").getAsDouble());
            assertEquals(0, definition.get("cooldown_ticks").getAsInt());
            assertTrue(definition.get("wheel_selectable").getAsBoolean());
            assertFalse(definition.get("learnable").getAsBoolean());
            assertFalse(definition.get("crest_allowed").getAsBoolean());
            assertFalse(definition.get("npc_allowed").getAsBoolean());
         }
      }
   }

   @Test
   void bothLanguagesContainTalentPassiveAndCommandKeys() throws IOException {
      Set<String> required = Set.of(
         "magic.typemoonworld.monstrous_strength.name",
         "magic.typemoonworld.clairvoyance.name",
         "effect.typemoonworld.monstrous_strength",
         "gui.typemoonworld.category.talent",
         "gui.typemoonworld.tab.passives",
         "gui.typemoonworld.overlay.current_talent",
         "passive.typemoonworld.divinity.name",
         "passive.typemoonworld.clairvoyance.name",
         "passive.typemoonworld.mind_eye_true.name",
         "passive.typemoonworld.mind_eye_false.name",
         "passive.typemoonworld.instinct.name",
         "command.typemoonworld.talent.granted",
         "command.typemoonworld.passive.granted"
      );
      for (String language : Set.of("zh_cn", "en_us")) {
         Path path = RESOURCES.resolve("assets/typemoonworld/lang/" + language + ".json");
         try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject translations = JsonParser.parseReader(reader).getAsJsonObject();
            for (String key : required) assertTrue(translations.has(key), language + " is missing " + key);
         }
      }
   }
}

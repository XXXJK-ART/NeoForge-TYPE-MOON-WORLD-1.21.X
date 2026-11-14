package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JeanneAlterResetResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void definitionUsesConfirmedIdentityAndParameters() throws IOException {
      JsonObject definition = json("data/typemoonworld/servant/definitions/jeanne_alter.json");
      assertEquals("human", definition.get("faction").getAsString());
      JsonObject parameters = definition.getAsJsonObject("parameters");
      assertEquals("A", parameters.get("magic").getAsString());
      assertTrue(parameters.get("magic_plus").getAsBoolean());
      assertEquals("A+", parameters.get("noble_phantasm_rank").getAsString());
      String traits = definition.getAsJsonArray("traits").toString();
      for (String trait : new String[]{"female", "servant", "humanoid", "human_kind", "chaotic", "evil", "ea_special"}) {
         assertTrue(traits.contains("\"" + trait + "\""), trait);
      }
      assertFalse(traits.contains("living_human"));
      assertFalse(traits.contains("costume"));
   }

   @Test
   void playerCardLeavesPassiveSkillSlotsEmptyAndKeepsActiveInputs() throws IOException {
      JsonObject card = json("data/typemoonworld/servant/cards/jeanne_alter.json");
      String actions = card.getAsJsonArray("actions").toString();
      assertTrue(actions.contains("jeanne_alter_fire_pillar"));
      assertTrue(actions.contains("jeanne_alter_cursed_lance"));
      assertTrue(actions.contains("jeanne_alter_inferno_body"));
      assertTrue(actions.contains("jeanne_alter_ephemeral_dream_a"));
      assertFalse(actions.contains("jeanne_alter_self_modification_ex"));
      assertFalse(actions.contains("jeanne_alter_dragon_witch_ex"));
   }

   private static JsonObject json(String relativePath) throws IOException {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relativePath), StandardCharsets.UTF_8)).getAsJsonObject();
   }
}

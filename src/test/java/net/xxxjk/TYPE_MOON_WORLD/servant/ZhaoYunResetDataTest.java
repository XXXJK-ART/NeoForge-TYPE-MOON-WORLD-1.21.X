package net.xxxjk.TYPE_MOON_WORLD.servant;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZhaoYunResetDataTest {
   private static final Path ROOT = Path.of("src/main/resources/data/typemoonworld/servant");

   @Test
   void definitionUsesTheNewZhaoYunIdentityAndParameters() throws Exception {
      JsonObject definition = read(ROOT.resolve("definitions/zhao_yun_rider.json"));
      assertEquals("human", definition.get("faction").getAsString());
      assertEquals("rider", definition.get("class_type").getAsString());

      Set<String> traits = strings(definition.getAsJsonArray("traits"));
      assertTrue(traits.containsAll(Set.of("male", "lawful", "good", "humanoid", "dragon", "beloved", "ea_special")));
      assertFalse(traits.contains("order"));
      assertFalse(traits.contains("shu_han"));
      assertFalse(traits.contains("five_tigers"));

      JsonObject parameters = definition.getAsJsonObject("parameters");
      assertEquals("A", parameters.get("strength").getAsString());
      assertEquals("B", parameters.get("endurance").getAsString());
      assertTrue(parameters.get("endurance_plus").getAsBoolean());
      assertEquals("A", parameters.get("agility").getAsString());
      assertEquals("D", parameters.get("magic").getAsString());
      assertEquals("B", parameters.get("luck").getAsString());
      assertEquals("B+", parameters.get("noble_phantasm_rank").getAsString());

      Set<String> skills = strings(definition.getAsJsonArray("skills"));
      assertTrue(skills.containsAll(Set.of("magic_resistance_d", "riding_a", "independent_action_c_plus")));
      assertFalse(skills.contains("riding_a_plus"));
      assertFalse(skills.contains("independent_action_c"));
      assertEquals(Set.of("changbanpo_no_ikki_gake", "qinggang_sword"),
         strings(definition.getAsJsonArray("noble_phantasms")));
   }

   @Test
   void skillAndNoblePhantasmRanksUseTheNewDataContracts() throws Exception {
      JsonObject riding = read(ROOT.resolve("skills/riding_a.json"));
      assertEquals("A", riding.get("rank").getAsString());
      assertEquals(0.5, riding.getAsJsonArray("effects").get(0).getAsJsonObject()
         .getAsJsonObject("params").get("speed_bonus").getAsDouble(), 1.0E-9);

      JsonObject independent = read(ROOT.resolve("skills/independent_action_c_plus.json"));
      assertEquals("C++", independent.get("rank").getAsString());
      assertTrue(independent.getAsJsonArray("effects").isEmpty());

      JsonObject sword = read(ROOT.resolve("noble_phantasms/qinggang_sword.json"));
      JsonObject charge = read(ROOT.resolve("noble_phantasms/changbanpo_no_ikki_gake.json"));
      assertEquals("anti_unit", sword.get("type").getAsString());
      assertEquals("C", sword.get("rank").getAsString());
      assertEquals("anti_unit", charge.get("type").getAsString());
      assertEquals("EX", charge.get("rank").getAsString());
   }

   private static JsonObject read(Path path) throws Exception {
      return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
   }

   private static Set<String> strings(JsonArray values) {
      Set<String> result = new HashSet<>();
      values.forEach(value -> result.add(value.getAsString()));
      return result;
   }
}

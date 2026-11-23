package net.xxxjk.TYPE_MOON_WORLD.servant.baobhan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import org.junit.jupiter.api.Test;

class BaobhanSithResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void identityMatchesTheArcherBaselineAndAllTraitsResolve() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/baobhan_sith.json");
      assertEquals("archer", definition.get("class_type").getAsString());
      assertEquals("fairy", definition.get("faction").getAsString());
      JsonObject parameters = definition.getAsJsonObject("parameters");
      assertEquals("A", parameters.get("strength").getAsString());
      assertEquals("C", parameters.get("endurance").getAsString());
      assertEquals("A", parameters.get("agility").getAsString());
      assertEquals("B", parameters.get("magic").getAsString());
      assertEquals("D", parameters.get("luck").getAsString());
      assertEquals("E", parameters.get("noble_phantasm_rank").getAsString());

      List<String> traits = definition.getAsJsonArray("traits").asList().stream()
         .map(element -> element.getAsString()).toList();
      List<String> expectedTraits = List.of("servant", "female", "earth", "heaven_or_earth_servant", "chaotic", "evil",
         "humanoid", "riding", "demonic", "fairy", "seven_knights");
      assertEquals(expectedTraits.size(), traits.size());
      for (String key : expectedTraits) {
         assertTrue(traits.contains(key), key);
         assertTrue(ServantTraitTag.findByKey(key).isPresent(), key + " must resolve as a real trait");
      }
   }

   @Test
   void fetchFailnaughtUsesThePlayerCardCostBaseline() throws Exception {
      JsonObject np = json("data/typemoonworld/servant/noble_phantasms/fetch_failnaught.json");
      assertEquals("E", np.get("rank").getAsString());
      assertEquals(35, np.get("mp_cost").getAsInt());
      JsonObject effectParams = np.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params");
      assertTrue(effectParams.isEmpty());
   }

   @Test
   void curseDamageRemainsArmorAndDefensePiercingAndGuaranteedToHit() throws Exception {
      String curseDamage = "typemoonworld:baobhan_sith_curse";
      assertTrue(json("data/minecraft/tags/damage_type/bypasses_armor.json").getAsJsonArray("values").asList()
         .stream().anyMatch(value -> curseDamage.equals(value.getAsString())));
      assertTrue(json("data/typemoonworld/tags/damage_type/fanatic_bypasses_defenses.json").getAsJsonArray("values").asList()
         .stream().anyMatch(value -> curseDamage.equals(value.getAsString())));
      assertTrue(json("data/typemoonworld/tags/damage_type/fanatic_guaranteed_hits.json").getAsJsonArray("values").asList()
         .stream().anyMatch(value -> curseDamage.equals(value.getAsString())));
   }

   private static JsonObject json(String relativePath) throws Exception {
      try (var reader = Files.newBufferedReader(RESOURCES.resolve(relativePath))) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      }
   }
}

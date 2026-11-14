package net.xxxjk.TYPE_MOON_WORLD.servant.lishuwen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LiShuwenResourcesTest {
   private static JsonObject json(String path) throws Exception {
      return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/typemoonworld/servant", path))).getAsJsonObject();
   }

   @Test void identityUsesHumanNeutralEvilAndRecognizedTraits() throws Exception {
      var definition = json("definitions/li_shuwen.json");
      assertEquals("human", definition.get("faction").getAsString());
      var traits = definition.getAsJsonArray("traits").asList().stream().map(e -> e.getAsString()).toList();
      assertTrue(traits.containsAll(List.of("servant", "male", "human", "humanoid", "human_kind",
         "seven_knights", "neutral_alignment", "evil", "ea_special")));
      for (String trait : traits) assertTrue(ServantTraitTag.findByKey(trait).isPresent(), trait);
      for (String obsolete : List.of("lawful", "earth", "living_human")) assertFalse(traits.contains(obsolete));
      var params = definition.getAsJsonObject("parameters");
      assertEquals("C", params.get("strength").getAsString());
      assertEquals("D", params.get("endurance").getAsString());
      assertEquals("A", params.get("agility").getAsString());
      assertTrue(params.get("agility_plus").getAsBoolean());
      assertEquals("E", params.get("magic").getAsString());
      assertEquals("E", params.get("luck").getAsString());
      assertEquals("", params.get("noble_phantasm_rank").getAsString());
      assertEquals("wu_er_da", definition.get("noble_phantasm").getAsString());
   }

   @Test void skillsKeepTheirRanksAndExistingNoblePhantasmHasNoRank() throws Exception {
      String[][] skills = {{"seasoned_a_plus", "A+", "passive"},
         {"chinese_martial_arts_a_triple_plus", "A+++", "passive"},
         {"sphere_boundary_extreme_a_minus", "A-", "active"}, {"yin_yang_crossing_b", "B", "active"}};
      for (String[] skill : skills) {
         var resource = json("skills/" + skill[0] + ".json");
         assertEquals(skill[1], resource.get("rank").getAsString());
         assertEquals(skill[2], resource.get("type").getAsString());
      }
      assertEquals("中国武术（八极拳） A+++", json("skills/chinese_martial_arts_a_triple_plus.json").get("display_name_zh").getAsString());
      assertEquals("", json("noble_phantasms/wu_er_da.json").get("rank").getAsString());
   }
}

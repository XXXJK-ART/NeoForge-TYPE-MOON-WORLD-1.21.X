package net.xxxjk.TYPE_MOON_WORLD.servant.muramasa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MuramasaSwordTechnique;
import org.junit.jupiter.api.Test;

class SenkoMuramasaResetResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void definitionUsesResetIdentityParametersAndBothNoblePhantasms() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/senko_muramasa.json");
      assertEquals("human", definition.get("faction").getAsString());
      assertEquals(Set.of("servant", "humanoid", "living_human", "human", "human_kind", "male", "saber",
         "chaotic", "balanced", "seven_knights"), definition.getAsJsonArray("traits").asList().stream()
         .map(element -> element.getAsString()).collect(Collectors.toSet()));

      JsonObject parameters = definition.getAsJsonObject("parameters");
      assertEquals("B", parameters.get("strength").getAsString());
      assertEquals("A", parameters.get("endurance").getAsString());
      assertEquals("D", parameters.get("agility").getAsString());
      assertEquals("E", parameters.get("magic").getAsString());
      assertEquals("B", parameters.get("luck").getAsString());
      assertEquals("A+", parameters.get("noble_phantasm_rank").getAsString());
      assertEquals(Set.of("muramasa_no_gen_kensai", "muramasa_tsumukari"),
         definition.getAsJsonArray("noble_phantasms").asList().stream()
            .map(element -> element.getAsString()).collect(Collectors.toSet()));
      assertEquals("A+", json("data/typemoonworld/servant/noble_phantasms/muramasa_no_gen_kensai.json")
         .get("rank").getAsString());
      assertEquals("EX", json("data/typemoonworld/servant/noble_phantasms/muramasa_tsumukari.json")
         .get("rank").getAsString());
   }

   @Test
   void crouchingSelectsMuramasaSwordTechniquesWithoutReplacingStandingActions() throws Exception {
      assertEquals("muramasa_trial_slash", ServantCardSkillLayout.actionFor("senko_muramasa", 2, false).effectId());
      assertEquals("muramasa_downward_slash", ServantCardSkillLayout.actionFor("senko_muramasa", 2, true).effectId());
      assertEquals("muramasa_projection_volley", ServantCardSkillLayout.actionFor("senko_muramasa", 5, false).effectId());
      assertEquals("muramasa_thrust", ServantCardSkillLayout.actionFor("senko_muramasa", 5, true).effectId());
      assertEquals("muramasa_sword_field", ServantCardSkillLayout.actionFor("senko_muramasa", 8, false).effectId());
      assertEquals("muramasa_spinning_slash", ServantCardSkillLayout.actionFor("senko_muramasa", 8, true).effectId());
      assertEquals("muramasa_no_gen_kensai", ServantCardSkillLayout.actionFor("senko_muramasa", 9, true).effectId());

      JsonObject card = json("data/typemoonworld/servant/cards/senko_muramasa.json");
      assertEquals(10, card.getAsJsonArray("actions").size());
      assertTrue(card.toString().contains("muramasa_trial_slash"));
      assertTrue(card.toString().contains("muramasa_projection_volley"));
      assertTrue(card.toString().contains("muramasa_sword_field"));
   }

   @Test
   void swordTechniquesHaveFocusedRangesAndNpcRotationIncludesThem() {
      assertEquals(3.0, MuramasaSwordTechnique.DOWNWARD_SLASH.range());
      assertEquals(4.5, MuramasaSwordTechnique.THRUST.range());
      assertEquals(4.0, MuramasaSwordTechnique.SPINNING_SLASH.range());
      assertEquals(1.15, MuramasaSwordTechnique.DOWNWARD_SLASH.damageMultiplier());
      assertEquals(1.35, MuramasaSwordTechnique.THRUST.damageMultiplier());
      assertEquals(0.75, MuramasaSwordTechnique.SPINNING_SLASH.damageMultiplier());
      assertFalse(MuramasaSwordTechnique.DOWNWARD_SLASH.isArea());
      assertTrue(MuramasaSwordTechnique.SPINNING_SLASH.isArea());
      assertEquals(3, MuramasaSwordTechnique.values().length);
   }

   private static JsonObject json(String path) throws Exception {
      try (var reader = Files.newBufferedReader(RESOURCES.resolve(path), StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      }
   }
}

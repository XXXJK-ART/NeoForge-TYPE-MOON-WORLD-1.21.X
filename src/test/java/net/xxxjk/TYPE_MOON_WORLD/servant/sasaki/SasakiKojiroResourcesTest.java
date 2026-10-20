package net.xxxjk.TYPE_MOON_WORLD.servant.sasaki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SasakiKojiroResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void definitionMatchesTheFgoBaselineAndHasNoNoblePhantasm() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/sasaki_kojiro.json");
      assertEquals("assassin", definition.get("class_type").getAsString());
      assertEquals("human", definition.get("faction").getAsString());
      assertEquals("age_of_sail", definition.get("era").getAsString());
      assertEquals("", definition.get("noble_phantasm").getAsString());
      JsonObject params = definition.getAsJsonObject("parameters");
      assertEquals("C", params.get("strength").getAsString());
      assertEquals("E", params.get("endurance").getAsString());
      assertEquals("A", params.get("agility").getAsString());
      assertTrue(params.get("agility_plus").getAsBoolean());
      assertEquals("E", params.get("magic").getAsString());
      assertEquals("A", params.get("luck").getAsString());
      assertEquals("", params.get("noble_phantasm_rank").getAsString());
      String traits = definition.getAsJsonArray("traits").toString();
      for (String trait : List.of("male", "human", "humanoid", "human_kind", "servant", "seven_knights", "neutral_alignment", "evil")) {
         assertTrue(traits.contains("\"" + trait + "\""), trait);
      }
      assertFalse(traits.contains("chaotic"));
      assertFalse(traits.contains("living_human"));
      assertEquals("", definition.getAsJsonObject("model").get("geometry").getAsString());
   }

   @Test
   void passiveRanksAndCardAnimationResourcesAreDefined() throws Exception {
      JsonObject mindEye = json("data/typemoonworld/servant/skills/mindseye_a.json");
      assertEquals("passive", mindEye.get("type").getAsString());
      assertTrue(mindEye.getAsJsonObject("defense").get("evasion").getAsBoolean());
      assertEquals("passive", json("data/typemoonworld/servant/skills/souwa_expertise_b.json").get("type").getAsString());
      assertEquals("active", json("data/typemoonworld/servant/skills/stealth_d.json").get("type").getAsString());
      assertEquals(18, json("data/typemoonworld/servant/skills/stealth_d.json").get("mp_cost").getAsInt());
      assertEquals(80, json("data/typemoonworld/servant/skills/spiritualization_b_plus.json").get("duration_ticks").getAsInt());

      for (SasakiKojiroAction action : SasakiKojiroAction.values()) {
         JsonObject animation = json("assets/typemoonworld/player_animations/" + action.playerAnimationPath() + ".json");
         assertEquals(action.playerAnimationPath(), animation.get("name").getAsString());
         assertTrue(animation.getAsJsonObject("emote").getAsJsonArray("moves").size() >= 3, action.name());
      }
      JsonObject sourceAnimation = json("assets/typemoonworld/animations/sasaki_tsubame_afterimages.animation.json")
         .getAsJsonObject("animations").getAsJsonObject("yanfan");
      assertEquals(0.625, sourceAnimation.get("animation_length").getAsDouble(), 0.0001);
      JsonObject sourceBones = sourceAnimation.getAsJsonObject("bones");
      for (String bone : List.of("Waist", "Waist2", "Waist3", "jian", "jian2", "jian3")) {
         assertTrue(sourceBones.has(bone), bone);
      }
      JsonObject tsubameEffect = json("assets/typemoonworld/effects/sasaki_tsubame.json");
      assertEquals(3, tsubameEffect.getAsJsonArray("emitters").size());
      for (var emitter : tsubameEffect.getAsJsonArray("emitters")) {
         JsonObject effect = emitter.getAsJsonObject();
         assertEquals("BILLBOARD", effect.get("renderer").getAsString());
         assertTrue(effect.get("enable_trail").getAsBoolean());
         assertTrue(effect.getAsJsonArray("velocity").get(2).getAsDouble() > 0.0);
         assertEquals("typemoonworld:textures/particle/sasaki_tsubame_slash.png",
            effect.getAsJsonObject("material").get("texture").getAsString());
      }
      BufferedImage blade = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/particle/sasaki_blade_arc.png").toFile());
      assertNotNull(blade);
      BufferedImage tsubameSlash = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/particle/sasaki_tsubame_slash.png").toFile());
      assertNotNull(tsubameSlash);
      JsonObject actions = json("data/typemoonworld/servant/actions/sasaki_kojiro.json");
      assertFalse(actions.toString().contains("tsubame_gaeshi"));
      assertFalse(Files.readString(RESOURCES.resolve("data/minecraft/tags/damage_type/bypasses_invulnerability.json"))
         .contains("typemoonworld:ganryu_tsubame"));
   }

   private static JsonObject json(String path) throws Exception {
      Path file = RESOURCES.resolve(path);
      try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      }
   }
}

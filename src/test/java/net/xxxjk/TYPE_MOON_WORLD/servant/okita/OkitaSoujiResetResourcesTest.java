package net.xxxjk.TYPE_MOON_WORLD.servant.okita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardActionPreconditions;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberTechniqueService;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OkitaSoujiResetResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void definitionUsesTheHaoriBoostedParametersAndDistinctIdentityTraits() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/okita_souji_saber.json");
      JsonObject parameters = definition.getAsJsonObject("parameters");
      assertEquals("B", parameters.get("strength").getAsString());
      assertEquals("D", parameters.get("endurance").getAsString());
      assertEquals("A++", parameters.get("agility").getAsString());
      assertEquals("E", parameters.get("magic").getAsString());
      assertEquals("D", parameters.get("luck").getAsString());
      assertEquals("C", parameters.get("noble_phantasm_rank").getAsString());

      String traits = definition.getAsJsonArray("traits").toString();
      for (String trait : new String[]{"human", "human_kind", "female", "servant", "humanoid", "riding", "shinsengumi", "neutral_alignment", "balanced"}) {
         assertTrue(traits.contains("\"" + trait + "\""), trait);
      }
      assertFalse(traits.contains("living_human"));
      assertFalse(traits.contains("weak_constitution"));
      assertTrue(definition.getAsJsonArray("skills").toString().contains("\"mindseye_a\""));
      assertEquals(ServantTraitTag.SHINSENGUMI, ServantTraitTag.fromKey("shinsengumi"));
      assertEquals(ServantTraitTag.NEUTRAL, ServantTraitTag.fromKey("neutral_alignment"));

      ServantParams params = ServantParams.of("D", false, "B", false, "A++", false, "E", false, "D", false);
      assertEquals(1.08, params.movementSpeed(), 1.0E-9);
   }

   @Test
   void flagOfSincerityUsesTwelveMaleServantMembersWithFixedRankDistribution() throws Exception {
      JsonObject np = json("data/typemoonworld/servant/noble_phantasms/flag_of_sincerity.json");
      assertEquals(12, np.getAsJsonArray("effects").get(0).getAsJsonObject()
         .getAsJsonObject("params").get("count").getAsInt());
      assertTrue(np.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params")
         .get("pool_decrements_on_death").getAsBoolean());
      assertFalse(np.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params")
         .get("pool_decrements_on_timeout").getAsBoolean());
      assertTrue(np.get("description").getAsString().contains("timeout does not"));
      assertTrue(source("servant/entity/OkitaSoujiSaberCombatHelper.java")
         .contains("FLAG_POOL_MAX = OkitaShinsengumiEntity.FLAG_MEMBER_COUNT"));
      assertTrue(source("servant/card/ServantCardOkitaSoujiSaberSkills.java")
         .contains("FLAG_COUNT = OkitaShinsengumiEntity.FLAG_MEMBER_COUNT"));

      String traits = source("entity/OkitaShinsengumiEntity.java");
      assertFalse(traits.substring(traits.indexOf("SERVANT_TRAITS"), traits.indexOf("@Nullable private UUID"))
         .contains("ServantTraitTag.FEMALE"));
      for (String trait : new String[]{"servant", "humanoid", "human", "human_kind", "male", "saber",
         "riding", "shinsengumi", "neutral_alignment", "balanced"}) {
         assertTrue(traits.contains("ServantTraitTag." + switch (trait) {
            case "neutral_alignment" -> "NEUTRAL";
            default -> trait.toUpperCase(java.util.Locale.ROOT);
         }), trait);
      }
   }

   @Test
   void shinsengumiPanelsAndCombatUseTheAgreedRanksAndVanillaInvulnerability() throws Exception {
      assertEquals(200.0, StatRank.D.toMaxHealth());
      assertEquals(10.0, StatRank.D.toAttackDamage());
      assertEquals(0.24, StatRank.D.toMovementSpeed(), 1.0E-9);
      assertEquals(300.0, StatRank.C.toMaxHealth());
      assertEquals(15.0, StatRank.C.toAttackDamage());
      assertEquals(0.28, StatRank.C.toMovementSpeed(), 1.0E-8);

      String entity = source("entity/OkitaShinsengumiEntity.java");
      assertTrue(entity.contains("this.formationIndex == 0 ? StatRank.C : StatRank.D"));
      assertTrue(entity.contains("ServantCardTraitKeys"));
      assertTrue(entity.contains("setFemale(false)"));
      String combat = source("martial/KendoNpcCombatController.java");
      assertFalse(combat.contains("target.invulnerableTime = 0"));
   }

   @Test
   void haoriIsPassiveAndNpHasThreeFixedHitsAndTheRequestedCooldown() throws Exception {
      JsonObject haori = json("data/typemoonworld/servant/skills/oath_haori_c.json");
      assertEquals("passive", haori.get("type").getAsString());
      JsonObject rankBonus = haori.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params");
      assertEquals("B", rankBonus.get("strength_rank").getAsString());
      assertEquals("D", rankBonus.get("endurance_rank").getAsString());
      assertEquals("A++", rankBonus.get("agility_rank").getAsString());
      assertEquals("kiku_ichimonji_norimune", rankBonus.get("weapon_upgrade").getAsString());

      JsonObject np = json("data/typemoonworld/servant/noble_phantasms/mumyoudan_zuki.json");
      assertEquals("", np.get("rank").getAsString());
      assertEquals(3600, np.get("cooldown_ticks").getAsInt());
      assertEquals(10, np.get("windup_ticks").getAsInt());
      JsonObject effect = np.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params");
      assertEquals(3, effect.get("hits").getAsInt());
      assertEquals(150.0, effect.get("fixed_damage_per_hit").getAsDouble());
      assertFalse(effect.has("damage_scale_per_hit"));
   }

   @Test
   void playerCardReservesNoSlotForMindEyeOrThePassiveHaori() throws Exception {
      JsonObject card = json("data/typemoonworld/servant/cards/okita_souji_saber.json");
      JsonObject[] actions = new JsonObject[10];
      for (var element : card.getAsJsonArray("actions")) {
         JsonObject action = element.getAsJsonObject();
         actions[action.get("slot").getAsInt()] = action;
      }
      assertEquals("typemoonworld:okita_thrust", actions[3].get("action").getAsString());
      assertEquals("typemoonworld:okita_downward_slash", actions[4].get("action").getAsString());
      assertEquals("typemoonworld:okita_mumyoudan_zuki", actions[8].get("action").getAsString());
      assertEquals("typemoonworld:okita_flag_of_sincerity", actions[9].get("action").getAsString());
      assertFalse(card.toString().contains("mind_eye"));
      assertFalse(card.toString().contains("oath_haori"));

      ServantCardSkillAction downwardSlash = ServantCardSkillLayout.actionFor("okita_souji_saber", 4, false);
      assertEquals("okita_downward_slash", downwardSlash.effectId());
      assertEquals(8.0, downwardSlash.mpCost());
      assertEquals(60, downwardSlash.cooldownTicks());
   }

   @Test
   void onlyMumyoudanRequiresAimAndItsAnimationAndHitFramesStayAligned() throws Exception {
      for (String action : new String[]{"okita_shukuchi", "okita_ichimonji", "okita_kaifuu", "okita_thrust",
         "okita_downward_slash", "okita_feigned_retreat", "okita_stance_break", "okita_shinsengumi_command",
         "okita_flag_of_sincerity"}) {
         assertFalse(ServantCardActionPreconditions.requiresLookTarget(action), action);
      }
      assertTrue(ServantCardActionPreconditions.requiresLookTarget("okita_mumyoudan_zuki"));
      assertEquals(OkitaSoujiSaberTechniqueService.MUMYOUDAN_CAST_RANGE,
         ServantCardActionPreconditions.targetRangeFor("okita_mumyoudan_zuki"));
      assertEquals(10, OkitaSoujiSaberTechniqueService.MUMYOUDAN_WINDUP_TICKS);
      JsonObject baked = json("assets/typemoonworld/player_animations/okita_mumyoudan_zuki.json");
      JsonObject source = json("assets/typemoonworld/animations/okita_souji_saber_source.animation.json")
         .getAsJsonObject("animations").getAsJsonObject("mumyoudan_zuki");
      int[] extensions = source.getAsJsonObject("bones").getAsJsonObject("Right Arm").getAsJsonObject("position")
         .entrySet().stream().filter(key -> key.getValue().getAsJsonArray().get(2).getAsDouble() == -9.0)
         .mapToInt(key -> (int)Math.ceil(Double.parseDouble(key.getKey()) * baked.get("motion_ticks").getAsInt()
            / baked.get("source_length").getAsDouble())).sorted().toArray();
      assertEquals(3, extensions.length);
      assertArrayEquals(extensions, OkitaSoujiSaberTechniqueService.mumyoudanStrikeTicks());
      assertEquals("okita_mumyoudan_zuki", SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI.playerAnimationPath());
   }

   @Test
   void allOkitaVoiceTracksAreRegisteredAndPackaged() throws Exception {
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      assertEquals(10, sounds.getAsJsonObject("okita_souji_saber_voice_attack").getAsJsonArray("sounds").size());
      assertEquals(3, sounds.getAsJsonObject("okita_souji_saber_voice_fail").getAsJsonArray("sounds").size());
      assertEquals(3, sounds.getAsJsonObject("okita_souji_saber_voice_victory").getAsJsonArray("sounds").size());
      for (String event : new String[]{"okita_souji_saber_voice_battle_start", "okita_souji_saber_voice_flag"}) {
         assertEquals(1, sounds.getAsJsonObject(event).getAsJsonArray("sounds").size(), event);
      }
      for (String file : new String[]{"np.ogg", "victory1.ogg", "victory2.ogg", "victory3.ogg", "fail1.ogg", "fail2.ogg", "fail3.ogg",
         "attack4.ogg", "attack5.ogg", "attack6.ogg", "attack7.ogg", "attack8.ogg", "attack9.ogg", "attack10.ogg",
         "battle_start1.ogg", "flag_of_sincerity.ogg"}) {
         Path voice = RESOURCES.resolve("assets/typemoonworld/sounds/voice/okita_souji_saber").resolve(file);
         assertTrue(Files.isRegularFile(voice) && Files.size(voice) > 0, file);
      }
   }

   private static JsonObject json(String path) throws Exception {
      try (var reader = Files.newBufferedReader(RESOURCES.resolve(path), StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      }
   }

   private static String source(String path) throws Exception {
      return Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD").resolve(path), StandardCharsets.UTF_8);
   }

}

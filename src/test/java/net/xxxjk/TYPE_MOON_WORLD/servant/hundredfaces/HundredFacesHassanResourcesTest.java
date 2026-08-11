package net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class HundredFacesHassanResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java");

   @Test
   void definitionUsesPlannedNpcParametersAndTraits() throws IOException {
      JsonObject definition = json("data/typemoonworld/servant/definitions/hundred_faces_hassan.json");
      JsonObject params = definition.getAsJsonObject("parameters");
      assertEquals("C", params.get("strength").getAsString());
      assertEquals("D", params.get("endurance").getAsString());
      assertEquals("A", params.get("agility").getAsString());
      assertEquals("C", params.get("magic").getAsString());
      assertEquals("E", params.get("luck").getAsString());
      assertEquals("assassin", definition.get("class_type").getAsString());
      assertEquals("zabaniya_hundred_faces", definition.get("noble_phantasm").getAsString());
      String actions = definition.getAsJsonObject("specialization").getAsJsonArray("combat_actions").toString();
      for (String action : List.of("dirk_throw", "assassin_stab", "shadow_step", "knife_feint", "shadow_lunge")) {
         assertTrue(actions.contains(action), action);
      }
      String traits = definition.getAsJsonArray("traits").toString();
      for (String trait : List.of("servant", "humanoid", "assassin", "lawful", "evil", "human",
         "hassan_order", "old_man_of_the_mountain", "hundred_faces")) {
         assertTrue(traits.contains(trait), trait);
      }
   }

   @Test
   void resourcesAndRegistrationsExist() throws IOException {
      for (String path : List.of(
         "data/typemoonworld/servant/ai/hundred_faces_hassan_ai.json",
         "data/typemoonworld/servant/actions/hundred_faces_hassan.json",
         "data/typemoonworld/servant/skills/presence_concealment_a_plus_hundred_faces.json",
         "data/typemoonworld/servant/skills/encyclopedia_a_plus.json",
         "data/typemoonworld/servant/skills/battle_retreat_b.json",
         "data/typemoonworld/servant/noble_phantasms/zabaniya_hundred_faces.json",
         "assets/typemoonworld/textures/entity/hundred_faces_hassan.png",
         "assets/typemoonworld/models/item/hundred_faces_hassan_spawn_egg.json",
         "assets/typemoonworld/models/item/servant_card_hundred_faces_hassan.json",
         "assets/typemoonworld/textures/item/card_faces_3d/servant/hundred_faces_hassan_card.png",
         "assets/typemoonworld/models/item/servant_card_hundred_faces_hassan_head.json",
         "assets/typemoonworld/models/item/servant_card_hundred_faces_hassan_chest.json",
         "assets/typemoonworld/models/item/servant_card_hundred_faces_hassan_legs.json",
         "assets/typemoonworld/geo/servant_card_hundred_faces_hassan.geo.json",
         "assets/typemoonworld/animations/servant_card_hundred_faces_hassan.animation.json",
         "assets/typemoonworld/textures/models/armor/servant_card_hundred_faces_hassan.png",
         "assets/typemoonworld/textures/item/servant_card_armor/hundred_faces_hassan_head.png",
         "assets/typemoonworld/textures/item/servant_card_armor/hundred_faces_hassan_chest.png",
         "assets/typemoonworld/textures/item/servant_card_armor/hundred_faces_hassan_legs.png"
      )) assertTrue(Files.isRegularFile(RESOURCES.resolve(path)), path);

      String entities = readJava("net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java");
      String items = readJava("net/xxxjk/TYPE_MOON_WORLD/item/ModItems.java");
      String factory = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/BuiltinServantEntityFactory.java");
      String renderer = readJava("net/xxxjk/TYPE_MOON_WORLD/client/TypeMoonWorldClientEvents.java");
      assertTrue(entities.contains("\"hundred_faces_hassan\""));
      assertTrue(entities.contains("\"hundred_faces_hassan_persona\""));
      assertTrue(items.contains("HUNDRED_FACES_HASSAN_SPAWN_EGG"));
      assertTrue(items.contains("SERVANT_CARD_HUNDRED_FACES_HASSAN"));
      assertTrue(items.contains("SERVANT_CARD_HUNDRED_FACES_HASSAN_HEAD"));
      assertTrue(items.contains("SERVANT_CARD_HUNDRED_FACES_HASSAN_CHEST"));
      assertTrue(items.contains("SERVANT_CARD_HUNDRED_FACES_HASSAN_LEGS"));
      assertTrue(factory.contains("HUNDRED_FACES_HASSAN.get().create"));
      assertTrue(renderer.contains("HumanoidServantRenderer<>(context, \"hundred_faces_hassan\")"));
      assertTrue(readJava("net/xxxjk/TYPE_MOON_WORLD/init/ModCreativeModeTabs.java").contains("SERVANT_CARD_HUNDRED_FACES_HASSAN"));
   }

   @Test
   void entityNamesOmitClassButSpawnEggsKeepClass() throws IOException {
      JsonObject zh = jsonAsset("assets/typemoonworld/lang/zh_cn.json");
      JsonObject en = jsonAsset("assets/typemoonworld/lang/en_us.json");
      assertEquals("百貌哈桑", zh.get("entity.typemoonworld.hundred_faces_hassan").getAsString());
      assertEquals("狂信子", zh.get("entity.typemoonworld.fanatic_assassin").getAsString());
      assertEquals("百貌哈桑（Assassin）刷怪蛋", zh.get("item.typemoonworld.hundred_faces_hassan_spawn_egg").getAsString());
      assertEquals("狂信子（Assassin）刷怪蛋", zh.get("item.typemoonworld.fanatic_assassin_spawn_egg").getAsString());
      assertEquals("Hassan of the Hundred Faces", en.get("entity.typemoonworld.hundred_faces_hassan").getAsString());
      assertEquals("Fanatic", en.get("entity.typemoonworld.fanatic_assassin").getAsString());
      assertEquals("Hassan of the Hundred Faces (Assassin) Spawn Egg", en.get("item.typemoonworld.hundred_faces_hassan_spawn_egg").getAsString());
      assertEquals("Fanatic (Assassin) Spawn Egg", en.get("item.typemoonworld.fanatic_assassin_spawn_egg").getAsString());
   }

   @Test
   void personaImplementationPreservesNoMindLinkAndNonDecayingAttackSpeed() throws IOException {
      String rules = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/hundredfaces/HundredFacesHassanRules.java");
      String persona = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/HundredFacesHassanPersonaEntity.java");
      String helper = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/hundredfaces/HundredFacesHassanCombatHelper.java");
      assertTrue(rules.contains("PERSONA_ATTACK_DAMAGE = 5.0"));
      assertTrue(rules.contains("PERSONA_MOVEMENT_SPEED = 0.36"));
      assertTrue(rules.contains("PERSONA_MIN_HEALTH = 20.0"));
      assertTrue(rules.contains("PERSONA_DEFENSE_RECOVERY_MULTIPLIER = 0.5"));
      assertTrue(rules.contains("TAG_TOTAL_SPLIT_COUNT"));
      assertTrue(rules.contains("mainHealthForSplitCount"));
      assertTrue(rules.contains("mainAttackDamageForSplitCount"));
      assertTrue(persona.contains("resolveLocalTarget"));
      assertTrue(persona.contains("targetUuid"));
      assertTrue(persona.contains("personaAttackDamageForCount"));
      assertTrue(rules.contains("MAX_SUMMON_BATCH = 10"));
      assertTrue(helper.contains("affordableSummonCount"));
      assertTrue(helper.contains("countOwnedPersonas"));
      assertTrue(helper.contains("rescaleOwnedPersonas"));
      assertTrue(helper.contains("getTotalSplitCount"));
      String combat = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/combat/ServantCombatSystem.java");
      assertTrue(combat.contains("HundredFacesHassanPersonaEntity"));
      assertTrue(combat.contains("HundredFacesHassanEntity"));
      assertTrue(combat.contains("PERSONA_DEFENSE_RECOVERY_MULTIPLIER"));
   }

   @Test
   void assassinPersonalCombatActionsAreWired() throws IOException {
      String definition = Files.readString(RESOURCES.resolve("data/typemoonworld/servant/definitions/hundred_faces_hassan.json"), StandardCharsets.UTF_8);
      String actions = Files.readString(RESOURCES.resolve("data/typemoonworld/servant/actions/hundred_faces_hassan.json"), StandardCharsets.UTF_8);
      String helper = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/hundredfaces/HundredFacesHassanCombatHelper.java");
      String entity = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/HundredFacesHassanEntity.java");
      String persona = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/HundredFacesHassanPersonaEntity.java");
      for (String action : List.of("dirk_throw", "assassin_stab", "shadow_step", "knife_feint", "shadow_lunge")) {
         assertTrue(definition.contains(action), action);
         assertTrue(actions.contains("hundred_faces/" + action), action);
      }
      assertTrue(helper.contains("DirkProjectileEntity"));
      assertTrue(helper.contains("tryPersonaCombatSkill"));
      assertTrue(helper.contains("applyAssassinFootwork"));
      assertTrue(helper.contains("tryReactiveDodge"));
      assertTrue(entity.contains("DIRK_SMALL_KNIFE"));
      assertTrue(entity.contains("tryReactiveDodge(this, source)"));
      assertTrue(persona.contains("DIRK_SMALL_KNIFE"));
   }

   @Test
   void copiedTexturesAreReadablePngs() throws IOException {
      assertNotNull(ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/entity/hundred_faces_hassan.png").toFile()));
      var card = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/card_faces_3d/servant/hundred_faces_hassan_card.png").toFile());
      assertNotNull(card);
      assertEquals(292, card.getWidth());
      assertEquals(500, card.getHeight());
      assertNotNull(ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/models/armor/servant_card_hundred_faces_hassan.png").toFile()));
      assertNotNull(ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/servant_card_armor/hundred_faces_hassan_head.png").toFile()));
      assertNotNull(ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/servant_card_armor/hundred_faces_hassan_chest.png").toFile()));
      assertNotNull(ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/servant_card_armor/hundred_faces_hassan_legs.png").toFile()));
   }

   @Test
   void servantCardPlayerControlSystemsAreWired() throws IOException {
      String layout = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardSkillLayout.java");
      String transform = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardTransformManager.java");
      String skills = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardHundredFacesHassanSkills.java");
      String persona = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/HundredFacesHassanPersonaEntity.java");
      String network = readJava("net/xxxjk/TYPE_MOON_WORLD/TYPE_MOON_WORLD.java");
      String client = readJava("net/xxxjk/TYPE_MOON_WORLD/client/ClientPacketHandler.java");
      String hud = readJava("net/xxxjk/TYPE_MOON_WORLD/client/screens/ServantCardHud.java");
      String events = readJava("net/xxxjk/TYPE_MOON_WORLD/event/ModPlayerEventHandler.java");
      String loadout = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardLoadoutManager.java");
      for (String action : List.of("hundred_faces_summon", "hundred_faces_summon_menu", "hundred_faces_command",
         "hundred_faces_single_command", "hundred_faces_switch", "hundred_faces_concealment")) {
         assertTrue(layout.contains(action), action);
         assertTrue(transform.contains(action), action);
      }
      assertTrue(skills.contains("MAX_SUMMON_BATCH"));
      assertTrue(skills.contains("MP_PER_PERSONA"));
      assertTrue(skills.contains("COMMAND_RECALL"));
      assertTrue(skills.contains("COMMAND_SCATTER"));
      assertTrue(skills.contains("COMMAND_ATTACK_TOGGLE"));
      assertTrue(skills.contains("totalSplitCount"));
      assertTrue(skills.contains("applyBodySplitAttributes"));
      assertTrue(persona.contains("initialize(ServerPlayer owner"));
      assertTrue(persona.contains("getOwnerPlayer"));
      assertTrue(persona.contains("notifyPersonaDeath"));
      for (String message : List.of("HundredFacesOpenScreenMessage", "HundredFacesSummonMessage",
         "HundredFacesCommandMessage", "HundredFacesSwitchMessage", "HundredFacesStateMessage")) {
         assertTrue(network.contains(message), message);
      }
      assertTrue(client.contains("openHundredFacesScreen"));
      assertTrue(hud.contains("drawHundredFacesStatus"));
      assertTrue(events.contains("handleHundredFacesPersonaCommand"));
      assertTrue(loadout.contains("\"hundred_faces_hassan\" -> off = stack(ModItems.DIRK_SMALL_KNIFE.get())")
         || loadout.contains("\"cursed_arm_hassan\", \"hundred_faces_hassan\" -> off = stack(ModItems.DIRK_SMALL_KNIFE.get())"));
   }

   private static JsonObject json(String path) throws IOException {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(path), StandardCharsets.UTF_8)).getAsJsonObject();
   }

   private static JsonObject jsonAsset(String path) throws IOException {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(path), StandardCharsets.UTF_8)).getAsJsonObject();
   }

   private static String readJava(String path) throws IOException {
      return Files.readString(JAVA.resolve(path), StandardCharsets.UTF_8);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.servant.iskandar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class IskandarResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void definitionMatchesRiderPanelAndArmyContract() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/iskandar.json");
      assertEquals("iskandar", definition.get("id").getAsString());
      assertEquals("rider", definition.get("class_type").getAsString());

      JsonObject parameters = definition.getAsJsonObject("parameters");
      assertEquals("B", parameters.get("strength").getAsString());
      assertEquals("A", parameters.get("endurance").getAsString());
      assertEquals("D", parameters.get("agility").getAsString());
      assertEquals("C", parameters.get("magic").getAsString());
      assertEquals("A", parameters.get("luck").getAsString());
      assertTrue(parameters.get("luck_plus").getAsBoolean());

      assertEquals("ionioi_hetairoi", definition.get("noble_phantasm").getAsString());
      assertTrue(definition.getAsJsonArray("noble_phantasms").toString().contains("gordius_wheel"));
      for (String skill : List.of("magic_resistance_d", "riding_a_plus", "divinity_c",
         "leadership_a_iskandar", "military_tactics_b_iskandar")) {
         assertTrue(definition.getAsJsonArray("skills").toString().contains(skill), skill);
      }

      JsonObject army = json("data/typemoonworld/servant/noble_phantasms/ionioi_hetairoi.json");
      JsonObject params = army.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params");
      assertEquals(10000, params.get("total_soldiers").getAsInt());
      assertEquals(200, params.get("active_cap").getAsInt());
      assertEquals(8000, params.get("collapse_after_deaths").getAsInt());
      assertEquals(10000, params.get("end_after_deaths").getAsInt());
      assertEquals(5, params.get("upkeep_mp_per_second").getAsInt());
   }

   @Test
   void registrationsResourcesAndAudioArePresent() throws Exception {
      String entities = Files.readString(JAVA.resolve("init/ModEntities.java"));
      String items = Files.readString(JAVA.resolve("item/ModItems.java"));
      String client = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      String factory = Files.readString(JAVA.resolve("servant/entity/BuiltinServantEntityFactory.java"));

      for (String id : List.of("ISKANDAR", "BUCEPHALUS", "GORDIUS_WHEEL", "MACEDONIAN_SOLDIER")) {
         assertTrue(entities.contains(" " + id + " ="), id);
      }
      assertTrue(items.contains("ISKANDAR_SPAWN_EGG"));
      assertTrue(items.contains("MACEDONIAN_SOLDIER_SPAWN_EGG"));
      assertTrue(items.contains("MACEDONIAN_SPEAR"));
      assertTrue(items.contains("MACEDONIAN_ROUND_SHIELD"));
      assertTrue(client.contains("ModEntities.ISKANDAR.get()"));
      assertTrue(client.contains("ModEntities.BUCEPHALUS.get()"));
      assertTrue(client.contains("ModEntities.GORDIUS_WHEEL.get()"));
      assertTrue(client.contains("ModEntities.MACEDONIAN_SOLDIER.get()"));
      assertTrue(factory.contains("IskandarEntity.SERVANT_KEY.equals(servantId.getPath())"));

      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String event : List.of("iskandar_voice_attack", "iskandar_voice_fail",
         "iskandar_voice_victory", "iskandar_voice_ionioi")) {
         assertTrue(sounds.has(event), event);
      }
      for (String file : List.of("np.ogg", "attack1.ogg", "attack2.ogg", "attack3.ogg",
         "fail1.ogg", "fail2.ogg", "fail3.ogg", "fail4.ogg",
         "victory1.ogg", "victory2.ogg", "victory3.ogg", "victory4.ogg")) {
         assertTrue(Files.size(RESOURCES.resolve("assets/typemoonworld/sounds/voice/iskandar/" + file)) > 4, file);
      }
   }

   @Test
   void noIskandarServantCardWasAdded() throws Exception {
      String registry = Files.readString(JAVA.resolve("servant/card/ServantCardRegistry.java"));
      assertFalse(registry.contains("\"iskandar\""));
      assertFalse(Files.exists(RESOURCES.resolve("assets/typemoonworld/textures/item/servant_cards/iskandar_card.png")));
   }

   @Test
   void soldiersRespectFormationDelayAndStandaloneSpawnsStayAlive() throws Exception {
      String soldier = Files.readString(JAVA.resolve("entity/MacedonianSoldierEntity.java"));
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      assertTrue(iskandar.contains("IONIOI_FORMATION_DELAY_TICKS = 2 * 20"));
      assertTrue(soldier.contains("actionStartTick"));
      assertTrue(soldier.contains("if (this.iskandarUuid == null)"));
      assertTrue(soldier.contains("level.getGameTime() < this.actionStartTick"));
      assertTrue(soldier.contains("IonioiHetairoiActionStartTick"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
   }
}

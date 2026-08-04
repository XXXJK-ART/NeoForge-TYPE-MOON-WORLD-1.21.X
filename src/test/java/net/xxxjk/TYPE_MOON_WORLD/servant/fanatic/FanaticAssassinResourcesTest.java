package net.xxxjk.TYPE_MOON_WORLD.servant.fanatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class FanaticAssassinResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void definitionAndAiMatchTheNpcContract() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/fanatic_assassin.json");
      JsonObject params = definition.getAsJsonObject("parameters");
      assertEquals("B", params.get("endurance").getAsString());
      assertEquals("C", params.get("strength").getAsString());
      assertEquals("A", params.get("agility").getAsString());
      assertEquals("C", params.get("magic").getAsString());
      assertEquals("D", params.get("luck").getAsString());
      assertEquals("phantasmal_lineage", definition.get("noble_phantasm").getAsString());
      assertEquals(8.0, json("data/typemoonworld/servant/ai/fanatic_assassin_ai.json")
         .getAsJsonObject("movement").get("follow_distance").getAsDouble());
      String definitionText = definition.toString();
      for (String trait : List.of("female", "assassin", "lawful", "good", "earth", "hassan_order", "fanatic", "divine_technique_mimic")) {
         assertTrue(definitionText.contains(trait), trait);
      }
   }

   @Test
   void modelTextureAndRequiredAnimationsAreValid() throws Exception {
      JsonObject definitionModel = json("data/typemoonworld/servant/definitions/fanatic_assassin.json")
         .getAsJsonObject("model");
      assertEquals("", definitionModel.get("geometry").getAsString());
      assertEquals("", definitionModel.get("animation").getAsString());
      var texture = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/entity/fanatic_assassin.png").toFile());
      assertNotNull(texture);
      assertEquals(64, texture.getWidth());
      assertEquals(64, texture.getHeight());
      String client = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      assertTrue(client.contains("ModEntities.FANATIC_ASSASSIN.get(), context -> new HumanoidServantRenderer<>(context, \"fanatic_assassin\")"));
      assertTrue(Files.notExists(JAVA.resolve("client/model/FanaticAssassinModel.java")));
      assertTrue(Files.notExists(JAVA.resolve("client/renderer/FanaticAssassinRenderer.java")));
      assertTrue(Files.notExists(RESOURCES.resolve("assets/typemoonworld/geo/fanatic_assassin.geo.json")));
      assertTrue(Files.notExists(RESOURCES.resolve("assets/typemoonworld/animations/fanatic_assassin.animation.json")));
   }

   @Test
   void registrationsDamageTypesAndSpawnEggExist() throws Exception {
      String entities = Files.readString(JAVA.resolve("init/ModEntities.java"));
      String items = Files.readString(JAVA.resolve("item/ModItems.java"));
      String effects = Files.readString(JAVA.resolve("init/ModMobEffects.java"));
      assertTrue(entities.contains("FANATIC_ASSASSIN_JINN"));
      assertTrue(entities.contains("FANATIC_ASSASSIN"));
      assertTrue(items.contains("FANATIC_ASSASSIN_SPAWN_EGG"));
      assertTrue(effects.contains("FANATIC_WOUNDED"));
      assertTrue(effects.contains("FANATIC_CIRCUIT_DISRUPTION"));
      assertTrue(effects.contains("FANATIC_TOXIN"));
      for (String name : List.of("heartbeat", "marrow", "computer", "toxin", "jinn")) {
         assertTrue(Files.isRegularFile(RESOURCES.resolve("data/typemoonworld/damage_type/fanatic_" + name + ".json")), name);
      }
      assertTrue(Files.readString(RESOURCES.resolve("data/minecraft/tags/damage_type/bypasses_armor.json"))
         .contains("typemoonworld:fanatic_heartbeat"));
      assertTrue(Files.readString(RESOURCES.resolve("data/minecraft/tags/damage_type/bypasses_armor.json"))
         .contains("typemoonworld:fanatic_computer"));
      assertTrue(Files.readString(RESOURCES.resolve("data/minecraft/tags/damage_type/is_explosion.json"))
         .contains("typemoonworld:fanatic_computer"));
      assertTrue(Files.isRegularFile(RESOURCES.resolve("data/typemoonworld/damage_type/fanatic_computer_splash.json")));
      assertTrue(Files.readString(RESOURCES.resolve("data/typemoonworld/tags/damage_type/fanatic_guaranteed_hits.json"))
         .contains("typemoonworld:fanatic_marrow"));
      assertTrue(Files.readString(RESOURCES.resolve("data/typemoonworld/tags/damage_type/fanatic_bypasses_defenses.json"))
         .contains("typemoonworld:fanatic_computer"));
   }

   @Test
   void combatDecisionOrderAndPersistenceAreExplicit() throws Exception {
      String combat = Files.readString(JAVA.resolve("servant/fanatic/FanaticAssassinCombatHelper.java"));
      int computer = combat.indexOf("COMPUTER_RANGE");
      int temperature = combat.indexOf("TEMPERATURE_MP");
      int marrow = combat.indexOf("nearbyEnemies.size() >= 3");
      int heartbeat = combat.indexOf("HEARTBEAT_RANGE");
      int nerves = combat.indexOf("!lineOfSight");
      int jinn = combat.indexOf("!hasOwnedJinn");
      int hair = combat.indexOf("HAIR_RANGE");
      int toxin = combat.indexOf("!entity.isToxinStanceActive()");
      assertTrue(computer < temperature && temperature < marrow && marrow < heartbeat);
      assertTrue(heartbeat < nerves && nerves < jinn && jinn < hair && hair < toxin);
      assertTrue(combat.contains("FanaticLast"));
      assertTrue(combat.contains("RETREATING"));
      String entity = Files.readString(JAVA.resolve("servant/entity/FanaticAssassinEntity.java"));
      assertTrue(entity.contains("FanaticTechniqueUntil"));
      assertTrue(entity.contains("addAdditionalSaveData"));
      String jinnEntity = Files.readString(JAVA.resolve("servant/entity/FanaticAssassinJinnEntity.java"));
      assertTrue(jinnEntity.contains("FanaticJinnOwner"));
      assertTrue(jinnEntity.contains("FanaticJinnExpires"));
      assertTrue(jinnEntity.contains("this.nextAttackTick = now + 20L"));
      assertTrue(jinnEntity.contains("this.nextAttackTick = now + 40L"));
   }

   @Test
   void allVoiceEventsReferenceValidOggFiles() throws Exception {
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String event : List.of("attack", "encounter", "fail", "victory", "heartbeat", "nerves", "marrow", "hair", "computer_temperature")) {
         assertTrue(sounds.has("fanatic_assassin_voice_" + event), event);
      }
      for (String file : List.of("attack1.ogg", "attack2.ogg", "encounter.ogg", "fail.ogg", "victory.ogg",
         "heartbeat.ogg", "nerves.ogg", "marrow.ogg", "hair.ogg", "computer_temperature.ogg")) {
         Path path = RESOURCES.resolve("assets/typemoonworld/sounds/voice/fanatic_assassin/" + file);
         byte[] bytes = Files.readAllBytes(path);
         assertTrue(bytes.length > 4, file);
         assertEquals("OggS", new String(bytes, 0, 4, StandardCharsets.US_ASCII), file);
      }
   }

   @Test
   void allEightTechniquesHaveDistinctLayeredEffects() throws Exception {
      String combat = Files.readString(JAVA.resolve("servant/fanatic/FanaticAssassinCombatHelper.java"));
      for (String method : List.of("spawnNervesFx", "spawnHairFx", "spawnMarrowFx", "spawnComputerFx",
         "spawnHeartbeatFx", "spawnTemperatureActivation", "spawnToxinActivation", "spawnJinnSummonFx")) {
         assertTrue(combat.contains("void " + method), method);
      }
      for (String primitive : List.of("spawnLine", "spawnCurve", "spawnSphere", "spawnOrientedRing",
         "spawnBodySpiral", "spawnTerrainGrid")) {
         assertTrue(combat.contains("void " + primitive), primitive);
      }
      assertTrue(combat.contains("SENSE_PURPLE"));
      assertTrue(combat.contains("HAIR_SILVER"));
      assertTrue(combat.contains("MARROW_PURPLE"));
      assertTrue(combat.contains("COMPUTER_RED"));
      assertTrue(combat.contains("HEART_PURPLE"));
      assertTrue(combat.contains("CRYSTAL"));
      assertTrue(combat.contains("TOXIN_BRIGHT"));
      assertTrue(combat.contains("JINN_GRAY"));
      assertTrue(!combat.contains("VFXServerEffects"));

      String jinn = Files.readString(JAVA.resolve("servant/entity/FanaticAssassinJinnEntity.java"));
      assertTrue(jinn.contains("spawnJinnTransitionFx"));
      assertTrue(jinn.contains("spawnJinnAura"));
      assertTrue(Files.notExists(JAVA.resolve("client/renderer/FanaticAssassinRenderer.java")));
   }

   private static JsonObject json(String path) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(path))).getAsJsonObject();
   }
}

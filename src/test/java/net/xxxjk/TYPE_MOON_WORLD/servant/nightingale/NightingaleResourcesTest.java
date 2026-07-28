package net.xxxjk.TYPE_MOON_WORLD.servant.nightingale;

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

class NightingaleResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void definitionAiSkillsAndNoblePhantasmParse() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/nightingale.json");
      JsonObject params = definition.getAsJsonObject("parameters");
      assertEquals("A", params.get("endurance").getAsString()); assertTrue(params.get("endurance_plus").getAsBoolean());
      assertEquals("D", params.get("magic").getAsString()); assertTrue(params.get("magic_plus").getAsBoolean());
      assertEquals("nightingale_pledge", definition.get("noble_phantasm").getAsString());
      for (String trait : List.of("servant", "female", "berserker", "human", "humanoid", "living_human", "human_kind", "lawful", "good", "nurse", "lamp_angel", "crimean_angel")) {
         assertTrue(definition.getAsJsonArray("traits").toString().contains(trait), trait);
      }
      JsonObject ai = json("data/typemoonworld/servant/ai/nightingale_ai.json");
      assertEquals(5.0, ai.getAsJsonObject("movement").get("follow_distance").getAsDouble());
      assertEquals(10.0, ai.getAsJsonObject("movement").get("wander_radius").getAsDouble());
      assertEquals(13.0, ai.getAsJsonObject("combat").get("preferred_attack_distance").getAsDouble());
      assertEquals(0.85, ai.getAsJsonObject("command").get("base_obedience_rate").getAsDouble());
      for (String file : List.of("mad_enhancement_ex_nightingale", "steel_nursing_a", "human_understanding_a", "angel_cry_ex")) {
         assertEquals(file, json("data/typemoonworld/servant/skills/" + file + ".json").get("id").getAsString());
      }
      JsonObject np = json("data/typemoonworld/servant/noble_phantasms/nightingale_pledge.json");
      assertEquals(50, np.get("mp_cost").getAsInt()); assertEquals(20, np.get("windup_ticks").getAsInt());
      assertEquals(100, np.get("duration_ticks").getAsInt());
      json("data/typemoonworld/servant/actions/nightingale.json");
      json("data/typemoonworld/tags/entity_type/nightingale_humanoids.json");
      json("data/typemoonworld/damage_type/nightingale_healing_reversal.json");
   }

   @Test
   void modelsAnimationsTexturesAndItemModelArePresent() throws Exception {
      JsonObject bodyGeo = json("assets/typemoonworld/geo/nightingale.geo.json");
      JsonObject gunGeo = json("assets/typemoonworld/geo/nightingale_gun.geo.json");
      assertEquals("geometry.nightingale", bodyGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
         .getAsJsonObject("description").get("identifier").getAsString());
      assertEquals("geometry.nightingale_gun", gunGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
         .getAsJsonObject("description").get("identifier").getAsString());
      json("assets/typemoonworld/models/item/nightingale_gun.json");
      json("assets/typemoonworld/models/item/nightingale_spawn_egg.json");
      JsonObject animations = json("assets/typemoonworld/animations/nightingale.animation.json").getAsJsonObject("animations");
      for (String action : List.of("idle", "walk", "shoot", "heal", "buff", "noble_phantasm")) {
         assertTrue(animations.has("animation.nightingale." + action), action);
         JsonObject root = animations.getAsJsonObject("animation.nightingale." + action).getAsJsonObject("bones").getAsJsonObject("bone");
         assertEquals(-8.0, root.getAsJsonArray("position").get(1).getAsDouble());
         assertEquals(0.7, root.getAsJsonArray("scale").get(0).getAsDouble());
      }
      var body = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/entity/nightingale.png").toFile());
      var gun = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/nightingale_gun.png").toFile());
      assertNotNull(body); assertEquals(128, body.getWidth()); assertEquals(128, body.getHeight());
      assertNotNull(gun); assertEquals(32, gun.getWidth()); assertEquals(32, gun.getHeight());
   }

   @Test
   void tenOggFilesAndFourSoundPoolsAreRegistered() throws Exception {
      Path voice = RESOURCES.resolve("assets/typemoonworld/sounds/voice/nightingale");
      List<String> files = List.of("np", "attack1", "attack2", "attack3", "fail1", "fail2", "victory1", "victory2", "victory3", "victory4");
      for (String name : files) {
         byte[] bytes = Files.readAllBytes(voice.resolve(name + ".ogg"));
         assertTrue(bytes.length > 1000, name); assertEquals("OggS", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
      }
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String pool : List.of("attack", "fail", "victory", "np")) assertTrue(sounds.has("nightingale_voice_" + pool), pool);
      assertEquals(3, sounds.getAsJsonObject("nightingale_voice_attack").getAsJsonArray("sounds").size());
      assertEquals(2, sounds.getAsJsonObject("nightingale_voice_fail").getAsJsonArray("sounds").size());
      assertEquals(4, sounds.getAsJsonObject("nightingale_voice_victory").getAsJsonArray("sounds").size());
      assertTrue(sounds.getAsJsonObject("nightingale_voice_np").getAsJsonArray("sounds").get(0).getAsJsonObject().get("stream").getAsBoolean());
   }

   @Test
   void registrationsRendererLanguagesAndDamageHooksExist() throws Exception {
      String entities = Files.readString(JAVA.resolve("init/ModEntities.java"));
      String items = Files.readString(JAVA.resolve("item/ModItems.java"));
      String client = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      String events = Files.readString(JAVA.resolve("event/NightingaleEvents.java"));
      assertTrue(entities.contains("NIGHTINGALE =") && entities.contains("NIGHTINGALE_BULLET ="));
      assertTrue(items.contains("NIGHTINGALE_GUN") && items.contains("NIGHTINGALE_SPAWN_EGG"));
      assertTrue(items.contains("SERVANT_CARD_NIGHTINGALE")
         && items.contains("SERVANT_CARD_NIGHTINGALE_CHEST") && items.contains("SERVANT_CARD_NIGHTINGALE_LEGS"));
      assertTrue(client.contains("NightingaleRenderer::new") && client.contains("NightingaleBulletRenderer::new"));
      assertTrue(events.contains("EventPriority.HIGHEST") && events.contains("EventPriority.LOWEST"));
      for (String lang : List.of("en_us", "zh_cn")) {
         JsonObject language = json("assets/typemoonworld/lang/" + lang + ".json");
         for (String key : List.of("entity.typemoonworld.nightingale", "item.typemoonworld.nightingale_gun",
            "item.typemoonworld.nightingale_spawn_egg", "item.typemoonworld.servant_card_nightingale",
            "skill.typemoonworld.servant_card.nightingale_steel_nursing",
            "skill.typemoonworld.servant_card.nightingale_angel_cry",
            "skill.typemoonworld.servant_card.nightingale_pledge")) {
            assertTrue(language.has(key), lang + ":" + key);
         }
      }
   }

   @Test
   void servantCardArtAndArmorMeetTheResourceContract() throws Exception {
      Path face2dPath = RESOURCES.resolve("assets/typemoonworld/textures/item/servant_cards/nightingale_card.png");
      Path face3dPath = RESOURCES.resolve("assets/typemoonworld/textures/item/card_faces_3d/servant/nightingale_card.png");
      var face2d = ImageIO.read(face2dPath.toFile());
      var face3d = ImageIO.read(face3dPath.toFile());
      assertEquals(181, face2d.getWidth()); assertEquals(256, face2d.getHeight());
      assertEquals(292, face3d.getWidth()); assertEquals(500, face3d.getHeight());
      assertTrue(Files.size(face2dPath) <= 150 * 1024, "2D Nightingale card face is too large");
      assertTrue(Files.size(face3dPath) <= 400 * 1024, "3D Nightingale card face is too large");

      JsonObject card = json("assets/typemoonworld/models/item/servant_card_nightingale.json");
      assertEquals("typemoonworld:item/servant_card_backs/berserker",
         card.getAsJsonObject("textures").get("back").getAsString());
      json("assets/typemoonworld/models/item/servant_card_nightingale_chest.json");
      json("assets/typemoonworld/models/item/servant_card_nightingale_legs.json");

      JsonObject armorGeo = json("assets/typemoonworld/geo/servant_card_nightingale.geo.json");
      assertEquals("geometry.servant_card_nightingale", armorGeo.getAsJsonArray("minecraft:geometry").get(0)
         .getAsJsonObject().getAsJsonObject("description").get("identifier").getAsString());
      var armorTexture = ImageIO.read(RESOURCES.resolve(
         "assets/typemoonworld/textures/models/armor/servant_card_nightingale.png").toFile());
      assertEquals(128, armorTexture.getWidth()); assertEquals(128, armorTexture.getHeight());
      assertTrue(json("assets/typemoonworld/animations/servant_card_nightingale.animation.json")
         .getAsJsonObject("animations").has("1"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative), StandardCharsets.UTF_8)).getAsJsonObject();
   }
}

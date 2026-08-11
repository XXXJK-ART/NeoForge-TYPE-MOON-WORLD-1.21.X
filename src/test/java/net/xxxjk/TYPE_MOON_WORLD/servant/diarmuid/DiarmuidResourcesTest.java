package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DiarmuidResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java");

   @Test
   void definitionAndDataResourcesExist() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/diarmuid_ua_duibhne.json");
      assertEquals("diarmuid_ua_duibhne", definition.get("id").getAsString());
      assertEquals("lancer", definition.get("class_type").getAsString());
      assertEquals("B", definition.getAsJsonObject("parameters").get("strength").getAsString());
      assertEquals("C", definition.getAsJsonObject("parameters").get("endurance").getAsString());
      assertTrue(definition.getAsJsonObject("parameters").get("agility_plus").getAsBoolean());
      for (String trait : new String[]{"servant", "humanoid", "living_human", "lancer", "male", "lawful", "neutral", "earth", "fianna_knights", "knight"}) {
         assertTrue(definition.getAsJsonArray("traits").contains(JsonParser.parseString("\"" + trait + "\"")), trait);
      }
      for (String path : new String[]{
         "data/typemoonworld/servant/ai/diarmuid_ua_duibhne_ai.json",
         "data/typemoonworld/servant/actions/diarmuid_ua_duibhne.json",
         "data/typemoonworld/servant/skills/love_spot_c.json",
         "data/typemoonworld/servant/skills/knight_strategy_b.json",
         "data/typemoonworld/servant/skills/mana_burst_jump_a.json",
         "assets/typemoonworld/textures/entity/diarmuid_ua_duibhne.png",
         "assets/typemoonworld/models/item/diarmuid_ua_duibhne_spawn_egg.json",
         "assets/typemoonworld/geo/servant_card_diarmuid_ua_duibhne.geo.json",
         "assets/typemoonworld/textures/models/armor/servant_card_diarmuid_ua_duibhne.png",
         "assets/typemoonworld/geo/gae_dearg.geo.json",
         "assets/typemoonworld/geo/gae_buidhe.geo.json",
         "assets/typemoonworld/textures/item/gae_dearg.png",
         "assets/typemoonworld/textures/item/gae_buidhe.png"
      }) {
         assertTrue(Files.isRegularFile(RESOURCES.resolve(path)), path);
      }
   }

   @Test
   void javaRegistrationsAndLanguageKeysExist() throws Exception {
      String entities = readJava("net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java");
      String items = readJava("net/xxxjk/TYPE_MOON_WORLD/item/ModItems.java");
      String eventBus = readJava("net/xxxjk/TYPE_MOON_WORLD/init/ModEventBusEvents.java");
      String renderer = readJava("net/xxxjk/TYPE_MOON_WORLD/client/TypeMoonWorldClientEvents.java");
      String factory = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/BuiltinServantEntityFactory.java");
      String entity = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/DiarmuidUaDuibhneEntity.java");
      String ai = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/diarmuid/DiarmuidCombatAi.java");
      assertTrue(entities.contains("\"diarmuid_ua_duibhne\""));
      assertTrue(items.contains("DIARMUID_UA_DUIBHNE_SPAWN_EGG"));
      assertTrue(items.contains("GAE_DEARG"));
      assertTrue(items.contains("GAE_BUIDHE"));
      assertTrue(items.contains("SERVANT_CARD_DIARMUID_UA_DUIBHNE_FEET"));
      assertTrue(eventBus.contains("DIARMUID_UA_DUIBHNE"));
      assertTrue(renderer.contains("HumanoidServantRenderer<>(context, \"diarmuid_ua_duibhne\")"));
      assertTrue(factory.contains("DiarmuidUaDuibhneEntity.SERVANT_KEY"));
      assertTrue(entity.contains("DiarmuidCombatAi.tick(this, level)"));
      assertTrue(entity.contains("DiarmuidCombatAi.consumePreferredRedRose"));
      assertTrue(ai.contains("chooseRedRose"));
      assertTrue(ai.contains("kiteAway"));
      assertTrue(ai.contains("sideStep"));

      JsonObject zh = json("assets/typemoonworld/lang/zh_cn.json");
      JsonObject en = json("assets/typemoonworld/lang/en_us.json");
      assertEquals("迪尔姆德", zh.get("entity.typemoonworld.diarmuid_ua_duibhne").getAsString());
      assertEquals("Diarmuid Ua Duibhne", en.get("entity.typemoonworld.diarmuid_ua_duibhne").getAsString());
      assertTrue(zh.get("item.typemoonworld.diarmuid_ua_duibhne_spawn_egg").getAsString().contains("Lancer"));
      assertTrue(en.get("item.typemoonworld.diarmuid_ua_duibhne_spawn_egg").getAsString().contains("Lancer"));
   }

   @Test
   void voiceAndPreviewAssetsAreAvailable() throws Exception {
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String key : new String[]{
         "hundred_faces_hassan_voice_attack", "hundred_faces_hassan_voice_fail", "hundred_faces_hassan_voice_victory", "hundred_faces_hassan_voice_np",
         "diarmuid_ua_duibhne_voice_attack", "diarmuid_ua_duibhne_voice_fail", "diarmuid_ua_duibhne_voice_victory", "diarmuid_ua_duibhne_voice_np"
      }) {
         assertTrue(sounds.has(key), key);
      }
      for (String path : new String[]{
         "assets/typemoonworld/sounds/voice/hundred_faces_hassan/np.ogg",
         "assets/typemoonworld/sounds/voice/hundred_faces_hassan/attack1.ogg",
         "assets/typemoonworld/sounds/voice/diarmuid_ua_duibhne/np.ogg",
         "assets/typemoonworld/sounds/voice/diarmuid_ua_duibhne/attack1.ogg"
      }) {
         assertTrue(Files.size(RESOURCES.resolve(path)) > 0, path);
      }
      for (String path : new String[]{
         "assets/typemoonworld/textures/item/servant_card_armor/diarmuid_ua_duibhne_head.png",
         "assets/typemoonworld/textures/item/servant_card_armor/diarmuid_ua_duibhne_chest.png",
         "assets/typemoonworld/textures/item/servant_card_armor/diarmuid_ua_duibhne_legs.png",
         "assets/typemoonworld/textures/item/servant_card_armor/diarmuid_ua_duibhne_feet.png"
      }) {
         var image = ImageIO.read(RESOURCES.resolve(path).toFile());
         assertNotNull(image, path);
         assertTrue(image.getWidth() <= 128 && image.getHeight() <= 128, path);
      }
   }

   @Test
   void dualSpearRulesAreDocumentedInRuntimeHelper() throws Exception {
      assertEquals(100, DiarmuidCombatHelper.SPEAR_MAX_DURABILITY);
      assertEquals(5, DiarmuidCombatHelper.MAX_YELLOW_ROSE_STACKS);
      assertEquals(0.10, DiarmuidCombatHelper.MAX_HEALTH_REDUCTION_PER_STACK, 1.0E-9);
      assertEquals(1.0F, DiarmuidCombatHelper.DIRECT_DAMAGE_PER_STACK, 1.0E-6F);
      String helper = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/diarmuid/DiarmuidCombatHelper.java");
      String events = readJava("net/xxxjk/TYPE_MOON_WORLD/event/DiarmuidEvents.java");
      assertTrue(helper.contains("RhoAiasEntity"));
      assertTrue(helper.contains("REINFORCEMENT_SELF_DEFENSE"));
      assertTrue(helper.contains("clearCursesFromOwner"));
      assertTrue(events.contains("AttackEntityEvent"));
      assertTrue(events.contains("PlayerInteractEvent"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative), StandardCharsets.UTF_8)).getAsJsonObject();
   }

   private static String readJava(String relative) throws Exception {
      return Files.readString(JAVA.resolve(relative), StandardCharsets.UTF_8);
   }
}

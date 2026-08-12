package net.xxxjk.TYPE_MOON_WORLD.servant.lancelot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import org.junit.jupiter.api.Test;

class LancelotBerserkerResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java");

   @Test
   void definitionAndDataResourcesExist() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/lancelot_berserker.json");
      assertEquals("lancelot_berserker", definition.get("id").getAsString());
      assertEquals("berserker", definition.get("class_type").getAsString());
      assertEquals("A", definition.getAsJsonObject("parameters").get("strength").getAsString());
      assertEquals("A", definition.getAsJsonObject("parameters").get("endurance").getAsString());
      assertEquals("A", definition.getAsJsonObject("parameters").get("agility").getAsString());
      assertTrue(definition.getAsJsonObject("parameters").get("agility_plus").getAsBoolean());
      for (String trait : new String[]{"servant", "humanoid", "living_human", "berserker", "male", "lawful", "mad", "earth", "round_table", "knight_of_the_lake"}) {
         assertTrue(definition.getAsJsonArray("traits").contains(JsonParser.parseString("\"" + trait + "\"")), trait);
      }

      for (String path : new String[]{
         "data/typemoonworld/servant/ai/lancelot_berserker_ai.json",
         "data/typemoonworld/servant/actions/lancelot_berserker.json",
         "data/typemoonworld/servant/skills/mad_enhancement_c.json",
         "data/typemoonworld/servant/skills/magic_resistance_e.json",
         "data/typemoonworld/servant/skills/eternal_arms_mastership_a_plus.json",
         "data/typemoonworld/servant/skills/blessing_of_fairy_a.json",
         "data/typemoonworld/servant/skills/mana_reversal_a.json",
         "data/typemoonworld/servant/noble_phantasms/knight_of_owner.json",
         "data/typemoonworld/servant/noble_phantasms/aroundight.json",
         "assets/typemoonworld/textures/entity/lancelot_berserker.png",
         "assets/typemoonworld/geo/servant_card_lancelot_berserker.geo.json",
         "assets/typemoonworld/textures/models/armor/servant_card_lancelot_berserker.png",
         "assets/typemoonworld/geo/aroundight.geo.json",
         "assets/typemoonworld/geo/lancelot_iron_rod.geo.json",
         "assets/typemoonworld/textures/item/aroundight.png",
         "assets/typemoonworld/textures/item/lancelot_iron_rod.png",
         "assets/typemoonworld/textures/misc/knight_of_owner_glint.png",
         "assets/typemoonworld/models/item/lancelot_berserker_spawn_egg.json"
      }) {
         assertTrue(Files.isRegularFile(RESOURCES.resolve(path)), path);
      }
   }

   @Test
   void javaRegistrationsAndDedicatedAiAreConnected() throws Exception {
      String entities = readJava("net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java");
      String items = readJava("net/xxxjk/TYPE_MOON_WORLD/item/ModItems.java");
      String renderer = readJava("net/xxxjk/TYPE_MOON_WORLD/client/TypeMoonWorldClientEvents.java");
      String factory = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/BuiltinServantEntityFactory.java");
      String entity = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/LancelotBerserkerEntity.java");
      String ai = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/lancelot/LancelotBerserkerCombatAi.java");
      String helper = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/lancelot/LancelotCombatHelper.java");
      String glint = readJava("net/xxxjk/TYPE_MOON_WORLD/client/renderer/ReinforcementRenderType.java");
      String mixin = readJava("net/xxxjk/TYPE_MOON_WORLD/mixin/ItemRendererMixin.java");
      String sounds = readJava("net/xxxjk/TYPE_MOON_WORLD/init/ModSounds.java");

      assertTrue(entities.contains("\"lancelot_berserker\""));
      assertTrue(renderer.contains("HumanoidServantRenderer<>(context, \"lancelot_berserker\")"));
      assertTrue(factory.contains("LancelotBerserkerEntity.SERVANT_KEY"));
      assertTrue(items.contains("AROUNDIGHT"));
      assertTrue(items.contains("LANCELOT_IRON_ROD"));
      assertTrue(items.contains("SERVANT_CARD_LANCELOT_BERSERKER_HEAD"));
      assertTrue(items.contains("LANCELOT_BERSERKER_SPAWN_EGG"));
      assertTrue(sounds.contains("LANCELOT_BERSERKER_VOICE_ATTACK"));
      assertTrue(entity.contains("LancelotBerserkerCombatAi.tick(this, level)"));
      assertTrue(entity.contains("LancelotCombatHelper.tick(this, level)"));
      assertTrue(entity.contains("ServantSprintCollisionHelper.tickNpcSprintCollision(this)"));
      assertTrue(ai.contains("isPhaseTwo"));
      assertTrue(ai.contains("tryDrawAroundight"));
      assertTrue(ai.contains("throwHeldWeapon"));
      assertTrue(!ai.toLowerCase().contains("probing"));
      assertTrue(helper.contains("maybeInterceptProjectile"));
      assertTrue(helper.contains("GilgameshGateWeaponProjectileEntity"));
      assertTrue(helper.contains("new EmiyaThrownWeaponEntity"));
      assertTrue(glint.contains("knight_of_owner_glint.png"));
      assertTrue(glint.contains("setupStaticGlintTexturing"));
      assertTrue(mixin.contains("shouldUseKnightOfOwnerGlint"));
   }

   @Test
   void knightOfOwnerAndAroundightRulesAreDocumentedInRuntimeHelper() {
      assertEquals(2.0, LancelotCombatHelper.KNIGHT_OF_OWNER_MP_COST, 1.0E-9);
      assertEquals(50.0, LancelotCombatHelper.AROUNDIGHT_DRAW_MP_COST, 1.0E-9);
      assertEquals(5.0, LancelotCombatHelper.AROUNDIGHT_DRAIN_MP, 1.0E-9);
      assertEquals(20, LancelotCombatHelper.AROUNDIGHT_DRAIN_INTERVAL);
      assertEquals(8, LancelotCombatHelper.PROJECTILE_INTERCEPT_COOLDOWN);
      assertEquals(ServantTraitTag.MAD, ServantTraitTag.fromKey("mad"));
      assertEquals(ServantTraitTag.KNIGHT_OF_THE_LAKE, ServantTraitTag.fromKey("knight_of_the_lake"));
   }

   @Test
   void soundsLanguageAndPreviewAssetsAreAvailable() throws Exception {
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String key : new String[]{
         "lancelot_berserker_voice_attack",
         "lancelot_berserker_voice_roar",
         "lancelot_berserker_voice_fail",
         "lancelot_berserker_voice_victory",
         "lancelot_berserker_voice_np"
      }) {
         assertTrue(sounds.has(key), key);
      }
      for (String file : new String[]{"attack1", "attack2", "roar", "fail", "victory", "np"}) {
         Path path = RESOURCES.resolve("assets/typemoonworld/sounds/voice/lancelot_berserker/" + file + ".ogg");
         assertTrue(Files.size(path) > 0L, file);
         assertEquals("OggS", new String(Files.readAllBytes(path), 0, 4, StandardCharsets.US_ASCII), file);
      }
      for (String slot : new String[]{"head", "chest", "legs", "feet"}) {
         Path path = RESOURCES.resolve("assets/typemoonworld/textures/item/servant_card_armor/lancelot_berserker_" + slot + ".png");
         var image = ImageIO.read(path.toFile());
         assertNotNull(image, slot);
         assertTrue(image.getWidth() <= 128 && image.getHeight() <= 128, slot);
      }

      JsonObject zh = json("assets/typemoonworld/lang/zh_cn.json");
      JsonObject en = json("assets/typemoonworld/lang/en_us.json");
      assertEquals("兰斯洛特", zh.get("entity.typemoonworld.lancelot_berserker").getAsString());
      assertTrue(zh.get("item.typemoonworld.lancelot_berserker_spawn_egg").getAsString().contains("Berserker"));
      assertEquals("Lancelot", en.get("entity.typemoonworld.lancelot_berserker").getAsString());
      assertTrue(en.get("item.typemoonworld.lancelot_berserker_spawn_egg").getAsString().contains("Berserker"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative), StandardCharsets.UTF_8)).getAsJsonObject();
   }

   private static String readJava(String relative) throws Exception {
      return Files.readString(JAVA.resolve(relative), StandardCharsets.UTF_8);
   }
}

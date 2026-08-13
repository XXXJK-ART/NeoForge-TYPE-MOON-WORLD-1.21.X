package net.xxxjk.TYPE_MOON_WORLD.servant.lancelot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
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
         "assets/typemoonworld/models/item/lancelot_berserker_spawn_egg.json",
         "assets/typemoonworld/models/item/servant_card_lancelot_berserker.json",
         "assets/typemoonworld/textures/item/card_faces_3d/servant/lancelot_berserker_card.png"
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
      String gateEntity = readJava("net/xxxjk/TYPE_MOON_WORLD/entity/GilgameshGateWeaponProjectileEntity.java");
      String gateRenderer = readJava("net/xxxjk/TYPE_MOON_WORLD/client/renderer/GilgameshGateWeaponRenderer.java");
      String glint = readJava("net/xxxjk/TYPE_MOON_WORLD/client/renderer/ReinforcementRenderType.java");
      String mixin = readJava("net/xxxjk/TYPE_MOON_WORLD/mixin/ItemRendererMixin.java");
      String sounds = readJava("net/xxxjk/TYPE_MOON_WORLD/init/ModSounds.java");
      String layout = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardSkillLayout.java");
      String transform = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardTransformManager.java");
      String cardSkills = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardLancelotBerserkerSkills.java");
      String masterProtection = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantMasterProtection.java");
      String commonEvents = readJava("net/xxxjk/TYPE_MOON_WORLD/event/CommonEvents.java");

      assertTrue(entities.contains("\"lancelot_berserker\""));
      assertTrue(renderer.contains("HumanoidServantRenderer<>(context, \"lancelot_berserker\")"));
      assertTrue(factory.contains("LancelotBerserkerEntity.SERVANT_KEY"));
      assertTrue(items.contains("AROUNDIGHT"));
      assertTrue(items.contains("LANCELOT_IRON_ROD"));
      assertTrue(items.contains("SERVANT_CARD_LANCELOT_BERSERKER = registerServantCard(\"lancelot_berserker\")"));
      assertTrue(items.contains("SERVANT_CARD_LANCELOT_BERSERKER_HEAD"));
      assertTrue(!items.contains("SERVANT_CARD_LANCELOT_BERSERKER_FEET"));
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
      assertTrue(helper.contains("AbstractArrow"));
      assertTrue(helper.contains("ThrownTrident"));
      assertTrue(helper.contains("new EmiyaThrownWeaponEntity"));
      assertTrue(helper.contains("spawnKnightOfOwnerGateCounter"));
      assertTrue(helper.contains("DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true"));
      assertTrue(gateEntity.contains("SOURCE_STYLE_LANCELOT"));
      assertTrue(gateRenderer.contains("knightOfOwnerEntityGlint3d"));
      assertTrue(glint.contains("knight_of_owner_glint.png"));
      assertTrue(glint.contains("setupStaticGlintTexturing"));
      assertTrue(glint.contains("TRANSLUCENT_TRANSPARENCY"));
      assertTrue(mixin.contains("shouldUseKnightOfOwnerGlint"));
      assertTrue(layout.contains("case \"lancelot_berserker\""));
      assertTrue(layout.contains("case 8 -> new ServantCardSkillAction(\"Knight of Owner\""));
      assertTrue(layout.contains("case 9 -> new ServantCardSkillAction(\"Aroundight\""));
      assertTrue(layout.contains("\"lancelot_knight_of_owner\", 10.0, 40"));
      assertTrue(layout.contains("\"lancelot_aroundight\", 50.0, 1200"));
      assertTrue(transform.contains("ServantCardLancelotBerserkerSkills.initialize(player, vars)"));
      assertTrue(transform.contains("ServantCardLancelotBerserkerSkills.performAroundight(player, vars)"));
      assertTrue(cardSkills.contains("maybeInterceptProjectile"));
      assertTrue(cardSkills.contains("tickLancelotSprintCollisionBreak(player)"));
      assertTrue(cardSkills.contains("10.0F"));
      assertTrue(cardSkills.contains("tryFeralRushCollision"));
      assertTrue(cardSkills.contains("new int[]{1, 2, 3, 4}"));
      assertTrue(cardSkills.contains("AbstractArrow"));
      assertTrue(cardSkills.contains("ThrownTrident"));
      assertTrue(cardSkills.contains("LancelotCombatHelper.applyWeaponHit"));
      assertTrue(cardSkills.contains("onAroundightToss(ItemTossEvent event)"));
      assertTrue(cardSkills.contains("remainingKnightOfOwnerCapacity(player)"));
      assertTrue(cardSkills.contains("KNIGHT_OF_OWNER_MP_COST"));
      assertTrue(!cardSkills.contains("if (!isHoldingAroundight(player)) {"));
      assertTrue(masterProtection.contains("isProtectedMasterDamage"));
      assertTrue(masterProtection.contains("projectile.getOwner()"));
      assertTrue(commonEvents.contains("ServantMasterProtection.isProtectedMasterDamage"));
      assertTrue(commonEvents.contains("NoblePhantasmDamageClassifier.isNoblePhantasmDamage"));
      assertNull(ServantCardSkillLayout.actionFor("lancelot_berserker", 6, false));
      assertNull(ServantCardSkillLayout.actionFor("lancelot_berserker", 7, false));
   }

   @Test
   void knightOfOwnerAndAroundightRulesAreDocumentedInRuntimeHelper() {
      assertEquals(10.0, LancelotCombatHelper.KNIGHT_OF_OWNER_MP_COST, 1.0E-9);
      assertEquals(30, LancelotCombatHelper.KNIGHT_OF_OWNER_ITEM_LIMIT);
      assertEquals(50.0, LancelotCombatHelper.AROUNDIGHT_DRAW_MP_COST, 1.0E-9);
      assertEquals(5.0, LancelotCombatHelper.AROUNDIGHT_DRAIN_MP, 1.0E-9);
      assertEquals(20, LancelotCombatHelper.AROUNDIGHT_DRAIN_INTERVAL);
      assertEquals(2.0, LancelotCombatHelper.AROUNDIGHT_CORE_ATTRIBUTE_MULTIPLIER, 1.0E-9);
      assertEquals(2, LancelotCombatHelper.PROJECTILE_INTERCEPT_COOLDOWN);
      assertEquals(2.0, LancelotCombatHelper.eternalArmsMastershipRecoveryMultiplier(), 1.0E-9);
      assertEquals(2.0, LancelotCombatHelper.capEternalArmsRecoveryMultiplier(4.0), 1.0E-9);
      assertEquals(0.30F, LancelotCombatHelper.ETERNAL_ARMS_MASTERSHIP_BYPASS_DODGE_CHANCE, 1.0E-6F);
      assertEquals(0.30F, LancelotCombatHelper.ETERNAL_ARMS_MASTERSHIP_BYPASS_GUARD_CHANCE, 1.0E-6F);
      assertEquals(0.30F, LancelotCombatHelper.capEternalArmsBypassChance(0.95F), 1.0E-6F);
      assertEquals(ServantTraitTag.MAD, ServantTraitTag.fromKey("mad"));
      assertEquals(ServantTraitTag.KNIGHT_OF_THE_LAKE, ServantTraitTag.fromKey("knight_of_the_lake"));
   }

   @Test
   void knightOfOwnerCountersKeepOriginalGateWeaponItem() throws Exception {
      String helper = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/lancelot/LancelotCombatHelper.java");
      for (String mapping : new String[]{
         "case \"durandal\" -> new ItemStack(ModItems.GILGAMESH_DURANDAL.get())",
         "case \"gram\" -> new ItemStack(ModItems.GILGAMESH_GRAM.get())",
         "case \"harpe\" -> new ItemStack(ModItems.GILGAMESH_HARPE.get())",
         "case \"vajra\" -> new ItemStack(ModItems.GILGAMESH_VAJRA.get())",
         "case \"fangtian_huaji\" -> new ItemStack(ModItems.GILGAMESH_FANGTIAN_HUAJI.get())",
         "case \"pseudo_spiral_sword\" -> new ItemStack(ModItems.GILGAMESH_SPIRAL_SWORD.get())",
         "case \"gae_bulg\" -> new ItemStack(ModItems.GILGAMESH_GAE_BULG.get())"
      }) {
         assertTrue(helper.contains(mapping), mapping);
      }
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
      for (String slot : new String[]{"head", "chest", "legs"}) {
         Path path = RESOURCES.resolve("assets/typemoonworld/textures/item/servant_card_armor/lancelot_berserker_" + slot + ".png");
         var image = ImageIO.read(path.toFile());
         assertNotNull(image, slot);
         assertTrue(image.getWidth() > 0 && image.getHeight() > 0, slot);
      }
      assertTrue(!Files.exists(RESOURCES.resolve("assets/typemoonworld/models/item/servant_card_lancelot_berserker_feet.json")));
      assertTrue(!Files.exists(RESOURCES.resolve("assets/typemoonworld/textures/item/servant_card_armor/lancelot_berserker_feet.png")));
      var card = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/card_faces_3d/servant/lancelot_berserker_card.png").toFile());
      assertNotNull(card);
      assertEquals(292, card.getWidth());
      assertEquals(500, card.getHeight());

      JsonObject zh = json("assets/typemoonworld/lang/zh_cn.json");
      JsonObject en = json("assets/typemoonworld/lang/en_us.json");
      assertEquals("兰斯洛特", zh.get("entity.typemoonworld.lancelot_berserker").getAsString());
      assertEquals("兰斯洛特（Berserker）", zh.get("item.typemoonworld.servant_card_lancelot_berserker").getAsString());
      assertEquals("骑士不死于徒手", zh.get("skill.typemoonworld.servant_card.lancelot_knight_of_owner").getAsString());
      assertTrue(zh.get("item.typemoonworld.lancelot_berserker_spawn_egg").getAsString().contains("Berserker"));
      assertEquals("Lancelot", en.get("entity.typemoonworld.lancelot_berserker").getAsString());
      assertEquals("Lancelot (Berserker)", en.get("item.typemoonworld.servant_card_lancelot_berserker").getAsString());
      assertEquals("Knight of Owner", en.get("skill.typemoonworld.servant_card.lancelot_knight_of_owner").getAsString());
      assertTrue(en.get("item.typemoonworld.lancelot_berserker_spawn_egg").getAsString().contains("Berserker"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative), StandardCharsets.UTF_8)).getAsJsonObject();
   }

   private static String readJava(String relative) throws Exception {
      return Files.readString(JAVA.resolve(relative), StandardCharsets.UTF_8);
   }
}

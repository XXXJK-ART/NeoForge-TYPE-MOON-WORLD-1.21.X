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
      assertEquals("迪尔姆德·奥迪那", definition.get("display_name_zh").getAsString());
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
      String armorModel = readJava("net/xxxjk/TYPE_MOON_WORLD/client/model/ServantCardArmorModel.java");
      String armorRenderer = readJava("net/xxxjk/TYPE_MOON_WORLD/client/renderer/ServantCardArmorRenderer.java");
      String creativeTabs = readJava("net/xxxjk/TYPE_MOON_WORLD/init/ModCreativeModeTabs.java");
      String skillLayout = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardSkillLayout.java");
      String transformManager = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardTransformManager.java");
      String loadoutManager = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardLoadoutManager.java");
      String basicAttack = readJava("net/xxxjk/TYPE_MOON_WORLD/network/ServantCardBasicAttackMessage.java");
      String spearItem = readJava("net/xxxjk/TYPE_MOON_WORLD/item/custom/DiarmuidSpearItem.java");
      String cardSkills = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardDiarmuidSkills.java");
      String keyMappings = readJava("net/xxxjk/TYPE_MOON_WORLD/init/TypeMoonWorldModKeyMappings.java");
      assertTrue(entities.contains("\"diarmuid_ua_duibhne\""));
      assertTrue(items.contains("SERVANT_CARD_DIARMUID_UA_DUIBHNE = registerServantCard(\"diarmuid_ua_duibhne\")"));
      assertTrue(items.contains("DIARMUID_UA_DUIBHNE_SPAWN_EGG"));
      assertTrue(items.contains("GAE_DEARG"));
      assertTrue(items.contains("GAE_BUIDHE"));
      assertTrue(!items.contains("SERVANT_CARD_DIARMUID_UA_DUIBHNE_HEAD"));
      assertTrue(items.contains("SERVANT_CARD_DIARMUID_UA_DUIBHNE_FEET"));
      assertTrue(creativeTabs.contains("SERVANT_CARD_DIARMUID_UA_DUIBHNE"));
      assertTrue(eventBus.contains("DIARMUID_UA_DUIBHNE"));
      assertTrue(renderer.contains("HumanoidServantRenderer<>(context, \"diarmuid_ua_duibhne\")"));
      assertTrue(factory.contains("DiarmuidUaDuibhneEntity.SERVANT_KEY"));
      assertTrue(entity.contains("super.customServerAiStep();"));
      assertTrue(entity.contains("DiarmuidCombatAi.tick(this, level)"));
      assertTrue(entity.contains("DiarmuidCombatAi.consumePreferredRedRose"));
      assertTrue(!entity.contains("BASE_MOVEMENT_SPEED"));
      assertTrue(!entity.contains("BURST_MOVEMENT_SPEED"));
      assertTrue(ai.contains("chooseRedRose"));
      assertTrue(ai.contains("kiteAway"));
      assertTrue(ai.contains("sideStep"));
      assertTrue(!ai.contains("entity.doHurtTarget(target)"));
      assertTrue(ai.contains("spawnCombatAura"));
      assertTrue(ai.contains("spawnStrategyVfx"));
      assertTrue(ai.contains("spawnJumpBurstVfx"));
      assertTrue(ai.contains("spawnRepositionVfx"));
      assertTrue(armorModel.contains("applyDiarmuidArmorFit"));
      assertTrue(armorModel.contains("setScale(\"armorLeftArm\", 1.5F, 1.5F, 1.5F"));
      assertTrue(armorModel.contains("setScale(\"armorRightArm\", 1.5F, 1.5F, 1.5F"));
      assertTrue(armorModel.contains("setScale(\"armorLeftLeg\", 1.5F, 1.5F, 1.5F"));
      assertTrue(armorModel.contains("setScale(\"armorRightLeg\", 1.5F, 1.5F, 1.5F"));
      assertTrue(armorModel.contains("setScale(\"armorLeftBoot\", 1.5F, 1.5F, 1.5F"));
      assertTrue(!armorModel.contains("setScale(\"armorBody\""));
      assertTrue(!armorModel.contains("setScaleAndOffset"));
      assertTrue(!armorRenderer.contains("DIARMUID_EXTREMITY_SCALE"));
      assertTrue(!armorRenderer.contains("shouldScaleDiarmuidExtremity"));
      assertTrue(skillLayout.contains("case \"diarmuid_ua_duibhne\""));
      for (String skillId : new String[]{
         "diarmuid_twin_spear_combo", "diarmuid_red_rose_focus", "diarmuid_yellow_rose_focus",
         "diarmuid_knight_strategy", "diarmuid_mana_burst_jump", "diarmuid_flower_step", "diarmuid_disengage",
         "diarmuid_dual_wield"
      }) {
         assertTrue(skillLayout.contains(skillId), skillId);
         assertTrue(transformManager.contains(skillId), skillId);
      }
      assertTrue(transformManager.contains("ServantCardDiarmuidSkills.initialize(player)"));
      assertTrue(transformManager.contains("ServantCardDiarmuidSkills.clear(player)"));
      assertTrue(transformManager.contains("EquipmentSlot.HEAD, ItemStack.EMPTY"));
      assertTrue(loadoutManager.contains("case \"diarmuid_ua_duibhne\""));
      assertTrue(loadoutManager.contains("GAE_DEARG"));
      assertTrue(loadoutManager.contains("GAE_BUIDHE"));
      assertTrue(basicAttack.contains("performOffhandSpearThrust"));
      assertTrue(basicAttack.contains("performMainhandSpearThrust"));
      assertTrue(basicAttack.contains("isDualWieldActive"));
      assertTrue(spearItem.contains("ServantCardDiarmuidSkills.applySpearItemHit"));
      assertTrue(cardSkills.contains("DiarmuidCombatHelper.applyRedRoseHit"));
      assertTrue(cardSkills.contains("DiarmuidCombatHelper.applyYellowRoseHit"));
      assertTrue(cardSkills.contains("ACTION_MODE_DUAL_WIELD"));
      assertTrue(cardSkills.contains("player.swing(hand, true)"));
      assertTrue(keyMappings.contains("isClientDiarmuidDualWieldActive"));
      assertTrue(keyMappings.contains("new ServantCardBasicAttackMessage(true)"));
      assertTrue(keyMappings.contains("new ServantCardBasicAttackMessage(false)"));
      assertTrue(keyMappings.contains("player.swing(InteractionHand.MAIN_HAND)"));
      assertTrue(keyMappings.contains("player.swing(InteractionHand.OFF_HAND)"));

      JsonObject zh = json("assets/typemoonworld/lang/zh_cn.json");
      JsonObject en = json("assets/typemoonworld/lang/en_us.json");
      assertEquals("迪尔姆德·奥迪那", zh.get("entity.typemoonworld.diarmuid_ua_duibhne").getAsString());
      assertEquals("Diarmuid Ua Duibhne", en.get("entity.typemoonworld.diarmuid_ua_duibhne").getAsString());
      assertTrue(zh.get("item.typemoonworld.diarmuid_ua_duibhne_spawn_egg").getAsString().contains("Lancer"));
      assertTrue(en.get("item.typemoonworld.diarmuid_ua_duibhne_spawn_egg").getAsString().contains("Lancer"));
      assertTrue(zh.has("item.typemoonworld.servant_card_diarmuid_ua_duibhne"));
      assertTrue(en.has("item.typemoonworld.servant_card_diarmuid_ua_duibhne"));
      assertTrue(zh.has("skill.typemoonworld.servant_card.diarmuid_dual_wield"));
      assertTrue(en.has("skill.typemoonworld.servant_card.diarmuid_dual_wield"));
      assertTrue(zh.has("message.typemoonworld.diarmuid.dual_wield.on"));
      assertTrue(en.has("message.typemoonworld.diarmuid.dual_wield.on"));
   }

   @Test
   void servantCardResourcesAreAvailable() throws Exception {
      Path cardTexture = RESOURCES.resolve("assets/typemoonworld/textures/item/card_faces_3d/servant/diarmuid_ua_duibhne_card.png");
      var cardImage = ImageIO.read(cardTexture.toFile());
      assertNotNull(cardImage);
      assertEquals(292, cardImage.getWidth());
      assertEquals(500, cardImage.getHeight());
      assertTrue(Files.size(cardTexture) > 0L);
      assertTrue(Files.size(cardTexture) < 600_000L);

      JsonObject model = json("assets/typemoonworld/models/item/servant_card_diarmuid_ua_duibhne.json");
      assertEquals("typemoonworld:item/template/card_3d", model.get("parent").getAsString());
      JsonObject textures = model.getAsJsonObject("textures");
      assertEquals("typemoonworld:item/card_faces_3d/servant/diarmuid_ua_duibhne_card", textures.get("front").getAsString());
      assertEquals("typemoonworld:item/servant_card_backs/lancer", textures.get("back").getAsString());
      assertTrue(!Files.exists(RESOURCES.resolve("assets/typemoonworld/models/item/servant_card_diarmuid_ua_duibhne_head.json")));
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
         "assets/typemoonworld/textures/item/servant_card_armor/diarmuid_ua_duibhne_chest.png",
         "assets/typemoonworld/textures/item/servant_card_armor/diarmuid_ua_duibhne_legs.png",
         "assets/typemoonworld/textures/item/servant_card_armor/diarmuid_ua_duibhne_feet.png"
      }) {
         var image = ImageIO.read(RESOURCES.resolve(path).toFile());
         assertNotNull(image, path);
         assertTrue(image.getWidth() <= 128 && image.getHeight() <= 128, path);
      }
      JsonObject armorGeo = json("assets/typemoonworld/geo/servant_card_diarmuid_ua_duibhne.geo.json");
      JsonObject leftArm = null;
      for (var element : armorGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
         JsonObject bone = element.getAsJsonObject();
         if ("armorLeftArm".equals(bone.get("name").getAsString())) {
            leftArm = bone;
            break;
         }
      }
      assertNotNull(leftArm);
      var pivot = leftArm.getAsJsonArray("pivot");
      assertEquals(5.0, pivot.get(0).getAsDouble(), 1.0E-6);
      assertEquals(22.0, pivot.get(1).getAsDouble(), 1.0E-6);
      assertEquals(0.0, pivot.get(2).getAsDouble(), 1.0E-6);
   }

   @Test
   void dualSpearRulesAreDocumentedInRuntimeHelper() throws Exception {
      assertEquals(200, DiarmuidCombatHelper.SPEAR_MAX_DURABILITY);
      assertEquals(5, DiarmuidCombatHelper.MAX_YELLOW_ROSE_STACKS);
      assertEquals(100, DiarmuidCombatHelper.YELLOW_ROSE_DAMAGE_INTERVAL_TICKS);
      assertEquals(0.10, DiarmuidCombatHelper.MAX_HEALTH_REDUCTION_PER_STACK, 1.0E-9);
      assertEquals(1.0F, DiarmuidCombatHelper.DIRECT_DAMAGE_PER_STACK, 1.0E-6F);
      assertEquals(0.10, DiarmuidCombatHelper.LIMB_DISABLE_CHANCE, 1.0E-9);
      assertEquals(0.50, DiarmuidCombatHelper.FULL_STACK_LIMB_DISABLE_CHANCE, 1.0E-9);
      String helper = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/diarmuid/DiarmuidCombatHelper.java");
      String events = readJava("net/xxxjk/TYPE_MOON_WORLD/event/DiarmuidEvents.java");
      String items = readJava("net/xxxjk/TYPE_MOON_WORLD/item/ModItems.java");
      assertTrue(helper.contains("RhoAiasEntity"));
      assertTrue(helper.contains("REINFORCEMENT_SELF_DEFENSE"));
      assertTrue(helper.contains("clearCursesFromOwner"));
      assertTrue(helper.contains("CURSE_NEXT_DAMAGE_TICK_TAG"));
      assertTrue(helper.contains("now + YELLOW_ROSE_DAMAGE_INTERVAL_TICKS"));
      assertTrue(helper.contains("limbDisableChance(stacks)"));
      assertTrue(helper.contains("createSpearStack"));
      assertTrue(helper.contains("syncSpearItemToOwner"));
      assertTrue(helper.contains("RED_ROSE_DUST"));
      assertTrue(helper.contains("YELLOW_ROSE_DUST"));
      assertTrue(helper.contains("spawnRedRoseHitVfx"));
      assertTrue(helper.contains("spawnCurseTickVfx"));
      assertTrue(helper.contains("spawnSpearBreakVfx"));
      assertTrue(events.contains("AttackEntityEvent"));
      assertTrue(events.contains("PlayerInteractEvent"));
      assertTrue(items.contains("\"gae_dearg_range\"),\r\n                                            2.0")
         || items.contains("\"gae_dearg_range\"),\n                                            2.0"));
      assertTrue(items.contains("\"gae_buidhe_range\"),\r\n                                            2.0")
         || items.contains("\"gae_buidhe_range\"),\n                                            2.0"));
      assertTrue(items.contains("new Item.Properties().durability(200)"));
      assertTrue(readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardDiarmuidSkills.java").contains("stack.hurtAndBreak(1, player"));
      String cardSkills = readJava("net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardDiarmuidSkills.java");
      String basicAttackPacket = readJava("net/xxxjk/TYPE_MOON_WORLD/network/ServantCardBasicAttackMessage.java");
      assertTrue(cardSkills.contains("MAINHAND_ATTACK_READY_TAG"));
      assertTrue(cardSkills.contains("OFFHAND_ATTACK_READY_TAG"));
      assertTrue(cardSkills.contains("DUAL_WIELD_HAND_ATTACK_COOLDOWN_TICKS"));
      assertTrue(cardSkills.contains("tryStartHandAttack(player, InteractionHand.MAIN_HAND)"));
      assertTrue(cardSkills.contains("tryStartHandAttack(player, InteractionHand.OFF_HAND)"));
      assertTrue(basicAttackPacket.contains("servant_card_diarmuid_mainhand_attack"));
      assertTrue(basicAttackPacket.contains("servant_card_diarmuid_offhand_attack"));
      assertTrue(readJava("net/xxxjk/TYPE_MOON_WORLD/servant/entity/DiarmuidUaDuibhneEntity.java").contains("currentMain"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative), StandardCharsets.UTF_8)).getAsJsonObject();
   }

   private static String readJava(String relative) throws Exception {
      return Files.readString(JAVA.resolve(relative), StandardCharsets.UTF_8);
   }
}

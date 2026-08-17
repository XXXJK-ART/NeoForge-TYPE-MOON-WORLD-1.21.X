package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshSlateItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardCasterGilgameshSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshCombatHelper;
import org.junit.jupiter.api.Test;

class CasterGilgameshResourcesTest {
   private static final Path ROOT = Path.of("src/main/resources");

   @Test
   void definitionUsesNpcOnlyCasterLoadoutAndSteveSkin() throws Exception {
      JsonObject definition = readJson("data/typemoonworld/servant/definitions/gilgamesh_caster.json");

      assertEquals("gilgamesh_caster", definition.get("id").getAsString());
      assertEquals("Gilgamesh", definition.get("display_name").getAsString());
      assertEquals("吉尔伽美什", definition.get("display_name_zh").getAsString());
      assertEquals("gilgamesh_caster_ai", definition.get("ai_config").getAsString());
      assertEquals("royal_cannon", definition.get("noble_phantasm").getAsString());
      assertEquals("typemoonworld:gilgamesh_slate",
         definition.getAsJsonObject("specialization").get("default_weapon").getAsString());

      JsonObject model = definition.getAsJsonObject("model");
      assertEquals("", model.get("geometry").getAsString());
      assertEquals("typemoonworld:textures/entity/caster_gilgamesh.png", model.get("texture").getAsString());
      assertEquals("", model.get("animation").getAsString());
   }

   @Test
   void royalCannonResourcesMatchRuntimeLimits() throws Exception {
      JsonObject noblePhantasm = readJson("data/typemoonworld/servant/noble_phantasms/royal_cannon.json");
      JsonObject ai = readJson("data/typemoonworld/servant/ai/gilgamesh_caster_ai.json");

      assertEquals("B", noblePhantasm.get("rank").getAsString());
      assertEquals(CasterGilgameshCombatHelper.CANNON_SHOTS_PER_ROUND, noblePhantasm.get("mp_cost").getAsInt());
      assertEquals(0, noblePhantasm.get("cooldown_ticks").getAsInt());
      assertEquals(CasterGilgameshCombatHelper.STARTING_AMMO, 500);
      assertEquals(CasterGilgameshCombatHelper.MAX_AMMO, 5000);
      assertEquals(30, CasterGilgameshCombatHelper.CANNON_SHOTS_PER_ROUND);
      assertEquals(45.0F, CasterGilgameshCombatHelper.CANNON_DAMAGE_PER_SHOT);
      assertEquals(3.0F, CasterGilgameshCombatHelper.CANNON_EXPLOSION_RADIUS);
      assertEquals("medium", ai.getAsJsonObject("tactical").get("maximum_terrain_impact").getAsString());
   }

   @Test
   void modelSlateAndVoiceResourcesExist() throws Exception {
      List<String> resources = List.of(
         "assets/typemoonworld/textures/entity/caster_gilgamesh.png",
         "assets/typemoonworld/geo/gilgamesh_slate.geo.json",
         "assets/typemoonworld/textures/item/gilgamesh_slate.png",
         "assets/typemoonworld/models/item/gilgamesh_slate.json",
         "assets/typemoonworld/models/item/gilgamesh_caster_spawn_egg.json",
         "assets/typemoonworld/models/item/servant_card_gilgamesh_caster.json",
         "assets/typemoonworld/models/item/servant_card_gilgamesh_caster_head.json",
         "assets/typemoonworld/models/item/servant_card_gilgamesh_caster_chest.json",
         "assets/typemoonworld/models/item/servant_card_gilgamesh_caster_legs.json",
         "assets/typemoonworld/geo/servant_card_gilgamesh_caster.geo.json",
         "assets/typemoonworld/geo/servant_card_gilgamesh_caster_head.geo.json",
         "assets/typemoonworld/animations/servant_card_gilgamesh_caster.animation.json",
         "assets/typemoonworld/textures/models/armor/servant_card_gilgamesh_caster.png",
         "assets/typemoonworld/textures/item/card_faces_3d/servant/gilgamesh_caster_card.png",
         "assets/typemoonworld/textures/item/servant_card_armor/gilgamesh_caster_head.png",
         "assets/typemoonworld/textures/item/servant_card_armor/gilgamesh_caster_chest.png",
         "assets/typemoonworld/textures/item/servant_card_armor/gilgamesh_caster_legs.png",
         "assets/typemoonworld/sounds.json"
      );
      for (String resource : resources) {
         assertTrue(Files.isRegularFile(ROOT.resolve(resource)), resource);
      }
      JsonObject armorGeo = readJson("assets/typemoonworld/geo/servant_card_gilgamesh_caster.geo.json");
      String armorGeoText = armorGeo.toString();
      assertFalse(armorGeoText.contains("caster_head"));
      assertTrue(armorGeoText.contains("armorBody"));
      assertTrue(armorGeoText.contains("armorRightArm"));
      JsonObject headGeo = readJson("assets/typemoonworld/geo/servant_card_gilgamesh_caster_head.geo.json");
      assertTrue(headGeo.toString().contains("caster_head"));
      String armorModel = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/model/ServantCardArmorModel.java"));
      assertTrue(armorModel.contains("hasDedicatedHeadModel(servantId)"));
      assertTrue(armorModel.contains("\"gilgamesh_caster\""));
      assertTrue(armorModel.contains("geo/servant_card_\" + servantId + \"_head.geo.json"));
      assertTrue(armorModel.contains("textures/models/armor/servant_card_\" + servantId + \"_head.png"));
      assertTrue(armorModel.contains("animations/empty.animation.json"));

      for (String voice : List.of("np", "shot", "attack1", "attack2", "fail1", "fail2",
         "mongrel", "victory1", "victory2")) {
         assertTrue(Files.isRegularFile(ROOT.resolve(
            "assets/typemoonworld/sounds/voice/caster_gilgamesh/" + voice + ".ogg")), voice);
      }
   }

   @Test
   void cannonRuntimeStateNamesAreStable() {
      assertFalse(CasterGilgameshCombatHelper.AMMO_TAG.isBlank());
      assertFalse(CasterGilgameshCombatHelper.FIRING_TAG.isBlank());
      assertEquals("RoyalCannonAmmo", CasterGilgameshCombatHelper.AMMO_TAG);
      assertEquals("RoyalCannonFiring", CasterGilgameshCombatHelper.FIRING_TAG);
   }

   @Test
   void casterGilgameshMeleeModeUsesFangtianAndRestoresSlate() throws Exception {
      assertEquals(80, CasterGilgameshCombatHelper.MELEE_DURATION_TICKS);
      assertEquals(300, CasterGilgameshCombatHelper.MELEE_REUSE_TICKS);
      String helper = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/CasterGilgameshCombatHelper.java"));
      String entity = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/CasterGilgameshEntity.java"));
      assertTrue(helper.contains("ModItems.GILGAMESH_FANGTIAN_HUAJI"));
      assertTrue(helper.contains("ModItems.GILGAMESH_SLATE"));
      assertTrue(helper.contains("tickIndependentMelee"));
      assertTrue(entity.contains("CasterGilgameshCombatHelper.isMeleeMode(this)"));
      assertTrue(entity.contains("25.0F"));
   }

   @Test
   void specialStatesRunBeforeTacticalArbitration() throws Exception {
      String servant = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/ServantEntity.java"));
      String service = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/ai/ServantSpecialStateService.java"));
      assertTrue(servant.contains("ServantSpecialStateService.tickBeforeAi(this)"));
      for (String type : List.of("ArtoriaPendragonEntity", "CasterGilgameshEntity", "GilgameshEntity",
         "EnkiduEntity", "MedusaEntity", "CursedArmHassanEntity", "EmiyaArcherEntity",
         "OdaNobunagaEntity", "PaleRiderEntity")) {
         assertTrue(service.contains("instanceof " + type), type);
      }
   }

   @Test
   void slatePlayerShotRulesMatchDesign() {
      assertEquals(5, GilgameshSlateItem.CROUCH_SHOT_COUNT);
      assertEquals(5.0F, GilgameshSlateItem.MIN_SHOT_DAMAGE);
      assertEquals(10.0F, GilgameshSlateItem.MAX_SHOT_DAMAGE);
   }

   @Test
   void casterGilgameshServantCardItemsStayOutOfMainCreativeTab() throws Exception {
      String tabs = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModCreativeModeTabs.java"));
      String mainTab = tabs.substring(tabs.indexOf("TYPE_MOON_WORLD_TAB"));
      assertTrue(mainTab.contains("ModItems.GILGAMESH_SLATE"));
      assertFalse(mainTab.contains("SERVANT_CARD_GILGAMESH_CASTER"));
   }

   @Test
   void servantCardCasterGilgameshCannonRulesMatchNpcVolleyShape() {
      assertEquals(CasterGilgameshCombatHelper.AMMO_TAG, ServantCardCasterGilgameshSkills.AMMO_TAG);
      assertEquals(CasterGilgameshCombatHelper.FIRING_TAG, ServantCardCasterGilgameshSkills.FIRING_TAG);
      assertEquals(CasterGilgameshCombatHelper.STARTING_AMMO, ServantCardCasterGilgameshSkills.STARTING_AMMO);
      assertEquals(CasterGilgameshCombatHelper.MAX_AMMO, ServantCardCasterGilgameshSkills.MAX_AMMO);
      assertEquals(30, ServantCardCasterGilgameshSkills.CANNON_SHOTS_PER_ROUND);
      assertEquals(1.20F, ServantCardCasterGilgameshSkills.WAND_DOMINION_MULTIPLIER);
   }

   private static JsonObject readJson(String relativePath) throws Exception {
      return JsonParser.parseString(Files.readString(ROOT.resolve(relativePath))).getAsJsonObject();
   }
}

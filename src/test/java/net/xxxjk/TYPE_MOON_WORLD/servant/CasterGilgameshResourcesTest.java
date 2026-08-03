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
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshCombatHelper;
import org.junit.jupiter.api.Test;

class CasterGilgameshResourcesTest {
   private static final Path ROOT = Path.of("src/main/resources");

   @Test
   void definitionUsesNpcOnlyCasterLoadoutAndFlightAnimation() throws Exception {
      JsonObject definition = readJson("data/typemoonworld/servant/definitions/gilgamesh_caster.json");

      assertEquals("gilgamesh_caster", definition.get("id").getAsString());
      assertEquals("Gilgamesh", definition.get("display_name").getAsString());
      assertEquals("吉尔伽美什", definition.get("display_name_zh").getAsString());
      assertEquals("gilgamesh_caster_ai", definition.get("ai_config").getAsString());
      assertEquals("royal_cannon", definition.get("noble_phantasm").getAsString());
      assertEquals("typemoonworld:gilgamesh_slate",
         definition.getAsJsonObject("specialization").get("default_weapon").getAsString());

      JsonObject animations = definition.getAsJsonObject("model").getAsJsonObject("animations");
      assertEquals("animation.caster_gilgamesh.idle", animations.get("idle").getAsString());
      assertEquals("animation.caster_gilgamesh.walk", animations.get("walk").getAsString());
      assertEquals("animation.caster_gilgamesh.fly", animations.get("fly").getAsString());
      assertEquals("animation.caster_gilgamesh.float_idle", animations.get("float_idle").getAsString());

      JsonObject animationFile = readJson("assets/typemoonworld/animations/caster_gilgamesh.animation.json");
      JsonObject animationKeys = animationFile.getAsJsonObject("animations");
      assertTrue(animationKeys.has("animation.caster_gilgamesh.standing"));
      assertTrue(animationKeys.has("animation.caster_gilgamesh.idle"));
      assertTrue(animationKeys.has("animation.caster_gilgamesh.walk"));
      assertTrue(animationKeys.has("animation.caster_gilgamesh.fly"));
      assertTrue(animationKeys.has("animation.caster_gilgamesh.float_idle"));
   }

   @Test
   void royalCannonResourcesMatchRuntimeLimits() throws Exception {
      JsonObject noblePhantasm = readJson("data/typemoonworld/servant/noble_phantasms/royal_cannon.json");

      assertEquals("B", noblePhantasm.get("rank").getAsString());
      assertEquals(30, noblePhantasm.get("mp_cost").getAsInt());
      assertEquals(0, noblePhantasm.get("cooldown_ticks").getAsInt());
      assertEquals(CasterGilgameshCombatHelper.STARTING_AMMO, 500);
      assertEquals(CasterGilgameshCombatHelper.MAX_AMMO, 5000);
   }

   @Test
   void modelSlateAndVoiceResourcesExist() {
      List<String> resources = List.of(
         "assets/typemoonworld/geo/caster_gilgamesh.geo.json",
         "assets/typemoonworld/textures/entity/caster_gilgamesh.png",
         "assets/typemoonworld/animations/caster_gilgamesh.animation.json",
         "assets/typemoonworld/geo/gilgamesh_slate.geo.json",
         "assets/typemoonworld/textures/item/gilgamesh_slate.png",
         "assets/typemoonworld/models/item/gilgamesh_slate.json",
         "assets/typemoonworld/models/item/gilgamesh_caster_spawn_egg.json",
         "assets/typemoonworld/sounds.json"
      );
      for (String resource : resources) {
         assertTrue(Files.isRegularFile(ROOT.resolve(resource)), resource);
      }

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
   void slatePlayerShotRulesMatchDesign() {
      assertEquals(5, GilgameshSlateItem.CROUCH_SHOT_COUNT);
      assertEquals(5.0F, GilgameshSlateItem.MIN_SHOT_DAMAGE);
      assertEquals(10.0F, GilgameshSlateItem.MAX_SHOT_DAMAGE);
   }

   private static JsonObject readJson(String relativePath) throws Exception {
      return JsonParser.parseString(Files.readString(ROOT.resolve(relativePath))).getAsJsonObject();
   }
}

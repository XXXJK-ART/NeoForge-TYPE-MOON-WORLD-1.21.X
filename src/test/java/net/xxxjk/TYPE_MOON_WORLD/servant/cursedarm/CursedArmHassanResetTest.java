package net.xxxjk.TYPE_MOON_WORLD.servant.cursedarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardActionPreconditions;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiActionDescriptor;
import org.junit.jupiter.api.Test;

class CursedArmHassanResetTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void definitionUsesConfirmedIdentityAndRanks() throws Exception {
      var definition = JsonParser.parseString(Files.readString(
         RESOURCES.resolve("data/typemoonworld/servant/definitions/cursed_arm_hassan.json"), StandardCharsets.UTF_8)).getAsJsonObject();
      var traits = definition.getAsJsonArray("traits").toString();
      assertTrue(traits.contains("human_kind"));
      assertTrue(traits.contains("seven_knights"));
      assertFalse(traits.contains("living_human"));
      assertEquals("C", definition.getAsJsonObject("parameters").get("noble_phantasm_rank").getAsString());
      assertEquals("zabaniya_cursed_arm", definition.get("noble_phantasm").getAsString());
      assertEquals(List.of("presence_concealment_a_plus_cursed_arm", "throwing_dagger_b", "self_modification_c", "wind_protection_a"),
         definition.getAsJsonArray("skills").asList().stream().map(e -> e.getAsString()).toList());
   }

   @Test
   void cardHasAllConfirmedActionSlots() {
      String[] ids = {"hassan_concealment", "hassan_quick_stab", "hassan_dagger", "dirk_throw", "shadow_step", "shadow_lunge", "knife_feint", "elbow_strike", "trip_sweep", "zabaniya"};
      double[] mp = {18, 0, 0, 8, 10, 7, 5, 6, 8, 80};
      int[] cooldown = {0, 30, 40, 40, 120, 85, 70, 80, 160, 1400};
      for (int slot = 0; slot <= 9; slot++) {
         ServantCardSkillAction action = ServantCardSkillLayout.actionFor("cursed_arm_hassan", slot, false);
         assertEquals(ids[slot], action.effectId(), "slot " + slot);
         assertEquals(mp[slot], action.mpCost(), "slot " + slot);
         assertEquals(cooldown[slot], action.cooldownTicks(), "slot " + slot);
      }
   }

   @Test
   void closeRangeActionsDoNotRequireAimedTargetsAndSingleDaggerIsTenDamage() throws Exception {
      assertFalse(ServantCardActionPreconditions.requiresLookTarget("dirk_throw"));
      assertFalse(ServantCardActionPreconditions.requiresLookTarget("elbow_strike"));
      assertFalse(ServantCardActionPreconditions.requiresLookTarget("trip_sweep"));
      String helper = Files.readString(RESOURCES.getParent().getParent().resolve("main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/CursedArmHassanCombatHelper.java"), StandardCharsets.UTF_8);
      String card = Files.readString(RESOURCES.getParent().getParent().resolve("main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardHassanSkills.java"), StandardCharsets.UTF_8);
      assertTrue(helper.contains("projectile.setDamage(10.0F)"));
      assertTrue(card.contains("projectile.setDamage(10.0F)"));
      String item = Files.readString(RESOURCES.getParent().getParent().resolve("main/java/net/xxxjk/TYPE_MOON_WORLD/item/ModItems.java"), StandardCharsets.UTF_8);
      assertTrue(item.contains("stacksTo(1).durability(16)"));
   }

   @Test
   void sharedRulesContainThreeStagesEightTickInterruptAndWindProtection() throws Exception {
      String helper = Files.readString(RESOURCES.getParent().getParent().resolve("main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/CursedArmHassanCombatHelper.java"), StandardCharsets.UTF_8);
      String card = Files.readString(RESOURCES.getParent().getParent().resolve("main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardHassanSkills.java"), StandardCharsets.UTF_8);
      assertTrue(helper.contains("WIND_DODGE_MAX_CHARGES = 3"));
      assertTrue(helper.contains("WIND_DODGE_RESTORE_TICKS = 400"));
      assertTrue(helper.contains("now + 8L"));
      assertTrue(helper.contains("HassanZabaniyaService.resolve(entity, target, 55.0F, true)"));
      assertTrue(card.contains("level >= 3"));
      assertTrue(card.contains("level * 50.0"));
      assertTrue(card.contains("level * 5.0"));
      assertTrue(card.contains("now + 8L"));
   }

   @Test
   void npcActionDescriptorsAndNoblePhantasmDecode() throws Exception {
      var actions = JsonParser.parseString(Files.readString(
         RESOURCES.resolve("data/typemoonworld/servant/actions/cursed_arm_hassan.json"), StandardCharsets.UTF_8)).getAsJsonObject();
      for (var action : actions.getAsJsonArray("actions")) {
         AiActionDescriptor.CODEC.parse(JsonOps.INSTANCE, action).resultOrPartial(error -> {
            throw new AssertionError(error);
         }).orElseThrow();
      }
      var noblePhantasm = JsonParser.parseString(Files.readString(
         RESOURCES.resolve("data/typemoonworld/servant/noble_phantasms/zabaniya_cursed_arm.json"), StandardCharsets.UTF_8));
      var parsed = net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.CODEC
         .parse(JsonOps.INSTANCE, noblePhantasm).resultOrPartial(error -> { throw new AssertionError(error); }).orElseThrow();
      assertEquals("C", parsed.rank());
      assertEquals(80, parsed.mpCost());
      assertEquals(7.0, parsed.range());
   }

   @Test
   void activeSkillsAndBoundCoreMessagesAreTranslatedInEveryLocale() throws Exception {
      for (String locale : List.of("en_us", "zh_cn", "zh_tw", "ja_jp", "ko_kr", "ru_ru")) {
         var lang = JsonParser.parseString(Files.readString(RESOURCES.resolve("assets/typemoonworld/lang/" + locale + ".json"))).getAsJsonObject();
         for (int slot = 0; slot <= 9; slot++) {
            var action = ServantCardSkillLayout.actionFor("cursed_arm_hassan", slot, false);
            if (action != null) assertTrue(lang.has("skill.typemoonworld.servant_card." + action.effectId()), locale + ": " + action.effectId());
         }
         for (String suffix : List.of("extracted", "heart_taken", "remaining", "expired", "evolved", "concealment_on", "concealment_off"))
            assertTrue(lang.has("message.typemoonworld.hassan." + suffix), locale + ": " + suffix);
      }
      var evolution = JsonParser.parseString(Files.readString(RESOURCES.resolve("data/typemoonworld/servant/skills/self_modification_c.json"))).getAsJsonObject();
      assertEquals("passive", evolution.get("type").getAsString());
      assertEquals(0, evolution.get("mp_cost").getAsInt());
   }

   @Test
   void voiceEventsAndCoreModelsResolveExistingAssets() throws Exception {
      var sounds = JsonParser.parseString(Files.readString(RESOURCES.resolve("assets/typemoonworld/sounds.json"))).getAsJsonObject();
      for (String event : List.of("attack", "fail", "victory", "zabaniya", "zabaniya_short")) {
         for (var sound : sounds.getAsJsonObject("cursed_arm_hassan_voice_" + event).getAsJsonArray("sounds")) {
            String path = sound.getAsJsonObject().get("name").getAsString().split(":", 2)[1];
            assertTrue(Files.size(RESOURCES.resolve("assets/typemoonworld/sounds/" + path + ".ogg")) > 1000, path);
         }
      }
      for (String item : List.of("heart", "mechanical_core", "divine_clay")) {
         var model = JsonParser.parseString(Files.readString(RESOURCES.resolve("assets/typemoonworld/models/item/zabaniya_" + item + ".json"))).getAsJsonObject();
         String[] texture = model.getAsJsonObject("textures").get("layer0").getAsString().split(":", 2);
         if ("typemoonworld".equals(texture[0])) assertTrue(Files.exists(RESOURCES.resolve("assets/typemoonworld/textures/" + texture[1] + ".png")));
      }
   }
}

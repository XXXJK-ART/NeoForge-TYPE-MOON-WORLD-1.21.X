package net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class NeroChaosResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void neroDefinitionAndSkinAreComplete() throws Exception {
      JsonObject definition = JsonParser.parseString(Files.readString(
         RESOURCES.resolve("data/typemoonworld/dead_apostle/definitions/nero_chaos.json"))).getAsJsonObject();
      assertEquals("nero_chaos", definition.get("id").getAsString());
      assertEquals(400.0, definition.get("max_health").getAsDouble());
      assertEquals(20.0, definition.get("attack_damage").getAsDouble());
      assertEquals(170.0, definition.get("poise_max").getAsDouble());
      assertEquals(0.35, definition.get("block_reduction").getAsDouble());
      assertEquals(0.72, definition.get("dodge_chance").getAsDouble());
      assertEquals(0.90, definition.get("urgent_dodge_chance").getAsDouble());

      BufferedImage neroSkin = ImageIO.read(RESOURCES.resolve(
         "assets/typemoonworld/textures/entity/nero_chaos.png").toFile());
      BufferedImage beastSkin = ImageIO.read(RESOURCES.resolve(
         "assets/typemoonworld/textures/entity/nero_chaos_beast.png").toFile());
      assertNotNull(neroSkin);
      assertNotNull(beastSkin);
      assertEquals(64, neroSkin.getWidth());
      assertEquals(64, neroSkin.getHeight());
      assertEquals(32, beastSkin.getWidth());
      assertEquals(32, beastSkin.getHeight());
   }

   @Test
   void registrationAndLanguageWiringIsPresent() throws Exception {
      String entities = Files.readString(JAVA.resolve("init/ModEntities.java"));
      String attributes = Files.readString(JAVA.resolve("init/ModEventBusEvents.java"));
      String client = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      String items = Files.readString(JAVA.resolve("item/ModItems.java"));
      String creativeTab = Files.readString(JAVA.resolve("init/ModCreativeModeTabs.java"));
      String zh = Files.readString(RESOURCES.resolve("assets/typemoonworld/lang/zh_cn.json"),
         StandardCharsets.UTF_8);

      for (String id : new String[]{"NERO_CHAOS", "NERO_CHAOS_HOUND", "NERO_CHAOS_SERPENT",
         "NERO_CHAOS_STAG", "NERO_CHAOS_BIRD", "NERO_CHAOS_BEAR", "NERO_CHAOS_CAT", "NERO_CHAOS_BAT"}) {
         assertTrue(entities.contains(id));
         assertTrue(attributes.contains(id));
         assertTrue(client.contains(id));
      }
      assertTrue(items.contains("NERO_CHAOS_SPAWN_EGG"));
      assertTrue(creativeTab.contains("NERO_CHAOS_SPAWN_EGG"));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos\": \"尼禄·卡欧斯\""));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos_hound\": \"尼禄·卡欧斯之兽\""));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos_serpent\": \"尼禄·卡欧斯之蛇\""));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos_stag\": \"尼禄·卡欧斯之鹿\""));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos_bird\": \"尼禄·卡欧斯之鸟\""));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos_bear\": \"尼禄·卡欧斯之熊\""));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos_cat\": \"尼禄·卡欧斯之猫\""));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos_bat\": \"尼禄·卡欧斯之蝠\""));
      assertTrue(zh.contains("\"item.typemoonworld.nero_chaos_spawn_egg\": \"尼禄·卡欧斯（死徒二十七祖第十席）\""));
      assertTrue(Files.exists(RESOURCES.resolve(
         "assets/typemoonworld/models/item/nero_chaos_spawn_egg.json")));
   }

   @Test
   void lowTierDeadApostlesDoNotEnterTheHighTierCombatSystem() throws Exception {
      String system = Files.readString(JAVA.resolve("combat/deadapostle/DeadApostleCombatSystem.java"));
      assertTrue(system.contains("NeroChaosEntity"));
      assertTrue(system.contains("NeroChaosBeastLogic"));
      assertTrue(system.contains("tryConsumeLifeAndRevive"));
      assertTrue(!system.contains("DeadApostleEntity"));
      assertTrue(!system.contains("ServantEntity"));
      assertTrue(Files.exists(RESOURCES.resolve(
         "data/typemoonworld/dead_apostle/definitions/nero_chaos.json")));
   }

   @Test
   void damagedNeroRegroupsBeastsInsteadOfDiscardingThem() throws Exception {
      String system = Files.readString(JAVA.resolve("combat/deadapostle/DeadApostleCombatSystem.java"));
      String logic = Files.readString(JAVA.resolve("entity/deadapostle/NeroChaosBeastLogic.java"));

      assertTrue(system.contains("LivingDamageEvent.Post"));
      assertTrue(system.contains("event.getNewDamage() > 0.0F"));
      assertTrue(system.contains("NeroChaosBeastLogic.regroupOwnedBeasts(nero)"));
      assertTrue(logic.contains("owner.getPersistentData().putLong(NeroChaosEntity.TAG_BEAST_REGROUP_UNTIL, until);"));
      assertTrue(logic.contains("mob.setTarget(null);"));
      assertTrue(logic.contains("returnToOwner(mob, owner, kind(mob));"));

      String regroupBody = logic.substring(logic.indexOf("public static void regroupOwnedBeasts"));
      regroupBody = regroupBody.substring(0, regroupBody.indexOf("private static LivingEntity findTarget"));
      assertTrue(!regroupBody.contains(".discard("));
      assertTrue(!regroupBody.contains(".remove("));
   }

   @Test
   void neroBeastLivesAreStoredReleasedReabsorbedAndInherited() throws Exception {
      String nero = Files.readString(JAVA.resolve("entity/deadapostle/NeroChaosEntity.java"));
      String logic = Files.readString(JAVA.resolve("entity/deadapostle/NeroChaosBeastLogic.java"));
      String system = Files.readString(JAVA.resolve("combat/deadapostle/DeadApostleCombatSystem.java"));

      assertTrue(nero.contains("setRemainingLives(NeroChaosRules.consumeLife(getRemainingLives()))"));
      assertTrue(nero.contains("public void reabsorbBeast(Mob beast)"));
      assertTrue(nero.contains("setRemainingLives(NeroChaosRules.restoreLife(getRemainingLives()))"));
      assertTrue(nero.contains("public void queueBeastRevival()"));
      assertTrue(nero.contains("NeroChaosRules.BEAST_REVIVAL_DELAY_TICKS"));
      assertTrue(nero.contains("public boolean spawnSuccessorFromOwnedBeast()"));
      assertTrue(nero.contains("transferOwnedBeastsToSuccessor"));

      assertTrue(logic.contains("owner.reabsorbBeast(beast);"));
      assertTrue(logic.contains("owner.queueBeastRevival();"));
      assertTrue(system.contains("nero.spawnSuccessorFromOwnedBeast()"));
   }
}

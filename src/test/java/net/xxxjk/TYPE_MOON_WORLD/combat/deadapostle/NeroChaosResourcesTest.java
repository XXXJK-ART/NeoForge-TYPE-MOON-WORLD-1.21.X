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
         "NERO_CHAOS_STAG", "NERO_CHAOS_BIRD"}) {
         assertTrue(entities.contains(id));
         assertTrue(attributes.contains(id));
         assertTrue(client.contains(id));
      }
      assertTrue(items.contains("NERO_CHAOS_SPAWN_EGG"));
      assertTrue(creativeTab.contains("NERO_CHAOS_SPAWN_EGG"));
      assertTrue(zh.contains("\"entity.typemoonworld.nero_chaos\": \"尼禄·卡欧斯\""));
      assertTrue(zh.contains("\"item.typemoonworld.nero_chaos_spawn_egg\": \"尼禄·卡欧斯（死徒二十七祖第十席）\""));
      assertTrue(Files.exists(RESOURCES.resolve(
         "assets/typemoonworld/models/item/nero_chaos_spawn_egg.json")));
   }

   @Test
   void lowTierDeadApostlesDoNotEnterTheHighTierCombatSystem() throws Exception {
      String system = Files.readString(JAVA.resolve("combat/deadapostle/DeadApostleCombatSystem.java"));
      assertTrue(system.contains("NeroChaosEntity"));
      assertTrue(system.contains("NeroChaosBeastLogic"));
      assertTrue(!system.contains("DeadApostleEntity"));
      assertTrue(!system.contains("ServantEntity"));
      assertTrue(Files.exists(RESOURCES.resolve(
         "data/typemoonworld/dead_apostle/definitions/nero_chaos.json")));
   }
}

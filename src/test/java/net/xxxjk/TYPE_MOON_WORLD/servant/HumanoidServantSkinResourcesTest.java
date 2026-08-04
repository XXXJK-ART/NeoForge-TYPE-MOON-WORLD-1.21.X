package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class HumanoidServantSkinResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   private record HumanoidServant(String entityField, String servantId, String textureName) {}

   private static final List<HumanoidServant> CONVERTED = List.of(
      new HumanoidServant("ARASH", "arash", "arash"),
      new HumanoidServant("ARTORIA_PENDRAGON", "artoria_pendragon", "artoria_pendragon"),
      new HumanoidServant("CU_CHULAINN", "cu_chulainn", "cu_chulainn"),
      new HumanoidServant("GILGAMESH_CASTER", "gilgamesh_caster", "caster_gilgamesh"),
      new HumanoidServant("EMIYA_ARCHER", "emiya_archer", "emiya_archer"),
      new HumanoidServant("ENKIDU", "enkidu", "enkidu"),
      new HumanoidServant("FANATIC_ASSASSIN", "fanatic_assassin", "fanatic_assassin"),
      new HumanoidServant("NIGHTINGALE", "nightingale", "nightingale"),
      new HumanoidServant("GAWAIN", "gawain", "gawain"),
      new HumanoidServant("GILGAMESH", "gilgamesh", "gilgamesh"),
      new HumanoidServant("LI_SHUWEN", "li_shuwen", "li_shuwen"),
      new HumanoidServant("MEDEA", "medea", "medea"),
      new HumanoidServant("MEDUSA", "medusa", "medusa"),
      new HumanoidServant("ODA_NOBUNAGA", "oda_nobunaga", "oda_nobunaga"),
      new HumanoidServant("PARACELSUS", "paracelsus", "paracelsus"),
      new HumanoidServant("SASAKI_KOJIRO", "sasaki_kojiro", "sasaki_kojiro"),
      new HumanoidServant("SENKO_MURAMASA", "senko_muramasa", "senko_muramasa"),
      new HumanoidServant("USHIWAKAMARU_RIDER", "ushiwakamaru_rider", "ushiwakamaru_rider"),
      new HumanoidServant("ZHAO_YUN_RIDER", "zhao_yun_rider", "zhao_yun_rider")
   );

   @Test
   void convertedServantsUseSteveSkinsAndGenericRenderer() throws Exception {
      String client = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      String servantEntity = Files.readString(JAVA.resolve("servant/entity/ServantEntity.java"));

      for (HumanoidServant servant : CONVERTED) {
         var texture = ImageIO.read(RESOURCES.resolve(
            "assets/typemoonworld/textures/entity/" + servant.textureName() + ".png").toFile());
         assertNotNull(texture, servant.servantId());
         assertEquals(texture.getWidth(), texture.getHeight(), servant.servantId());
         assertEquals(0, texture.getWidth() % 64, servant.servantId());

         assertTrue(client.contains("ModEntities." + servant.entityField()
            + ".get(), context -> new HumanoidServantRenderer<>(context, \"" + servant.textureName() + "\")"),
            servant.servantId());
         assertTrue(servantEntity.contains("\"" + servant.servantId() + "\""), servant.servantId());
      }

      assertTrue(client.contains("ModEntities.CURSED_ARM_HASSAN.get(), CursedArmHassanRenderer::new"));
      assertTrue(client.contains("ModEntities.HERACLES.get(), HeraclesRenderer::new"));
   }

   @Test
   void convertedServantsNoLongerReferenceOrShipOldGeckoBodyResources() throws Exception {
      for (HumanoidServant servant : CONVERTED) {
         var model = JsonParser.parseString(Files.readString(RESOURCES.resolve(
            "data/typemoonworld/servant/definitions/" + servant.servantId() + ".json")))
            .getAsJsonObject().getAsJsonObject("model");
         assertEquals("", model.get("geometry").getAsString(), servant.servantId());
         assertEquals("typemoonworld:textures/entity/" + servant.textureName() + ".png",
            model.get("texture").getAsString(), servant.servantId());
         assertEquals("", model.get("animation").getAsString(), servant.servantId());
         assertTrue(!model.has("animations"), servant.servantId());

         String resourceName = "gilgamesh_caster".equals(servant.servantId()) ? "caster_gilgamesh" : servant.servantId();
         assertTrue(Files.notExists(RESOURCES.resolve(
            "assets/typemoonworld/geo/" + resourceName + ".geo.json")), servant.servantId());
         assertTrue(Files.notExists(RESOURCES.resolve(
            "assets/typemoonworld/animations/" + resourceName + ".animation.json")), servant.servantId());
      }
   }

   @Test
   void generatedHairHelmetsAndLongHairCounterRotationAreWired() throws Exception {
      List<String> hairHelmets = List.of(
         "artoria_pendragon", "sasaki_kojiro", "enkidu", "ushiwakamaru_rider",
         "oda_nobunaga", "medusa", "zhao_yun_rider", "paracelsus", "gilgamesh_caster"
      );
      for (String servantId : hairHelmets) {
         assertTrue(Files.isRegularFile(RESOURCES.resolve(
            "assets/typemoonworld/models/item/servant_card_" + servantId + "_head.json")), servantId);
         assertTrue(Files.isRegularFile(RESOURCES.resolve(
            "assets/typemoonworld/textures/item/servant_card_armor/" + servantId + "_head.png")), servantId);
         assertTrue(Files.isRegularFile(RESOURCES.resolve(
            "assets/typemoonworld/textures/models/armor/servant_card_" + servantId + "_head.png")), servantId);
      }

      String armorModel = Files.readString(JAVA.resolve("client/model/ServantCardArmorModel.java"));
      assertTrue(armorModel.contains(
         "case \"enkidu\", \"medusa\", \"oda_nobunaga\", \"paracelsus\", \"gilgamesh_caster\" -> true"));
      assertTrue(armorModel.contains("counterRotateHair(\"hair1\", pitchRad, 0.55F)"));
      assertTrue(armorModel.contains("counterRotateHair(\"hair2\", pitchRad, 0.85F)"));
      assertTrue(armorModel.contains("counterRotateHair(\"bone4\", pitchRad, 0.75F)"));

      String armorItem = Files.readString(JAVA.resolve("item/custom/ServantCardArmorItem.java"));
      assertTrue(armorItem.contains("\"enkidu\","));

      String armorRenderer = Files.readString(JAVA.resolve("client/renderer/ServantCardArmorRenderer.java"));
      assertTrue(armorRenderer.contains("withScale(1.02F, 1.02F)"));

      String humanoidRenderer = Files.readString(JAVA.resolve("client/renderer/HumanoidServantRenderer.java"));
      assertTrue(humanoidRenderer.contains("case \"oda_nobunaga\" -> 0.84F"));
      assertTrue(humanoidRenderer.contains("case \"enkidu\" -> 0.88F"));
   }
}

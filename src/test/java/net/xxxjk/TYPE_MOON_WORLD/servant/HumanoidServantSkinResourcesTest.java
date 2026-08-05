package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class HumanoidServantSkinResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");
   private static final Pattern LITERAL_RESOURCE_LOCATION = Pattern.compile(
      "ResourceLocation\\.fromNamespaceAndPath\\(\"typemoonworld\",\\s*\"([^\"]+\\.(?:geo|animation)\\.json|textures/[^\"]+?\\.png)\"\\)"
   );

   private record HumanoidServant(String entityField, String servantId, String textureName) {}

   private static int firstNonWhitespaceByte(byte[] bytes) {
      for (byte b : bytes) {
         int value = b & 0xFF;
         if (value != ' ' && value != '\n' && value != '\r' && value != '\t') {
            return value;
         }
      }
      return -1;
   }

   private static int countMatches(String text, Pattern pattern) {
      int count = 0;
      var matcher = pattern.matcher(text);
      while (matcher.find()) {
         count++;
      }
      return count;
   }

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
   void humanoidNpcArmorIsAppliedForAllSpawnEntryPointsAndRenderedPerSlot() throws Exception {
      String commonEvents = Files.readString(JAVA.resolve("event/CommonEvents.java"));
      assertTrue(commonEvents.contains("onEntityJoin(EntityJoinLevelEvent event)"));
      assertTrue(commonEvents.contains("event.getEntity() instanceof ServantEntity servant"));
      assertTrue(commonEvents.contains("servant.ensureDefaultNpcServantCardArmor(false)"));
      assertTrue(commonEvents.contains("servant.ensureDefaultNpcServantCardArmor(true)"));

      String servantEntity = Files.readString(JAVA.resolve("servant/entity/ServantEntity.java"));
      assertTrue(servantEntity.contains("public void ensureDefaultNpcServantCardArmor(boolean forceClientSync)"));
      assertTrue(servantEntity.contains("this.tickCount == 1"));
      assertTrue(servantEntity.contains("this.setItemSlot(slot, ItemStack.EMPTY)"));
      assertTrue(servantEntity.contains("this.setItemSlot(slot, current)"));
      assertTrue(servantEntity.contains("equipNpcServantCardArmorSlot(EquipmentSlot.CHEST, forceClientSync)"));
      assertTrue(servantEntity.contains("equipNpcServantCardArmorSlot(EquipmentSlot.LEGS, forceClientSync)"));

      String armorItem = Files.readString(JAVA.resolve("item/custom/ServantCardArmorItem.java"));
      assertTrue(armorItem.contains("EnumMap<EquipmentSlot, ServantCardArmorRenderer>"));
      assertTrue(armorItem.contains("computeIfAbsent(slot"));
   }

   @Test
   void servantCardArmorGeoFilesAreValidJson() throws Exception {
      Path geoRoot = RESOURCES.resolve("assets/typemoonworld/geo");
      try (var paths = Files.list(geoRoot)) {
         List<Path> geoFiles = paths
            .filter(path -> path.getFileName().toString().endsWith(".geo.json"))
            .toList();
         assertFalse(geoFiles.isEmpty());
         for (Path geoFile : geoFiles) {
            JsonParser.parseString(Files.readString(geoFile));
         }
      }
   }

   @Test
   void resourceJsonFilesStartWithStrictJsonBytes() throws Exception {
      try (var paths = Files.walk(RESOURCES)) {
         paths.filter(path -> path.getFileName().toString().endsWith(".json"))
            .forEach(path -> {
               try {
                  byte[] bytes = Files.readAllBytes(path);
                  assertFalse(bytes.length >= 3
                     && bytes[0] == (byte)0xEF
                     && bytes[1] == (byte)0xBB
                     && bytes[2] == (byte)0xBF, path + " has a UTF-8 BOM");
                  assertFalse(bytes.length >= 2
                     && ((bytes[0] == (byte)0xFF && bytes[1] == (byte)0xFE)
                        || (bytes[0] == (byte)0xFE && bytes[1] == (byte)0xFF)),
                     path + " has a UTF-16 BOM");
                  for (int i = 0; i < Math.min(bytes.length, 128); i++) {
                     assertFalse(bytes[i] == 0, path + " contains a NUL byte near the start");
                  }
                  int first = firstNonWhitespaceByte(bytes);
                  assertTrue(first == '{' || first == '[', path + " does not start with JSON");
               } catch (Exception exception) {
                  throw new AssertionError(path.toString(), exception);
               }
            });
      }
   }

   @Test
   void literalJavaGeoModelResourcesExist() throws Exception {
      try (var paths = Files.walk(JAVA)) {
         paths.filter(path -> path.getFileName().toString().endsWith(".java")).forEach(path -> {
            try {
               var matcher = LITERAL_RESOURCE_LOCATION.matcher(Files.readString(path));
               while (matcher.find()) {
                  Path resource = RESOURCES.resolve("assets/typemoonworld").resolve(matcher.group(1));
                  assertTrue(Files.isRegularFile(resource), path + " -> " + matcher.group(1));
               }
            } catch (Exception exception) {
               throw new AssertionError(path.toString(), exception);
            }
         });
      }
   }

   @Test
   void packagedResourcePathsUseMinecraftSafeCharacters() throws Exception {
      for (String rootName : List.of("assets", "data")) {
         Path root = RESOURCES.resolve(rootName);
         try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
               String relative = root.relativize(path).toString().replace('\\', '/');
               assertTrue(relative.matches("[a-z0-9_./-]+"), rootName + "/" + relative);
            });
         }
      }

      for (String slot : List.of("head", "chest", "legs", "feet")) {
         Path model = RESOURCES.resolve(
            "assets/typemoonworld/models/item/servant_armor_generic_" + slot + ".json");
         assertTrue(Files.isRegularFile(model), slot);
         JsonParser.parseString(Files.readString(model));
      }

      Path itemModels = RESOURCES.resolve("assets/typemoonworld/models/item");
      try (var paths = Files.list(itemModels)) {
         paths.filter(path -> path.getFileName().toString().matches("servant_card_.+_(head|chest|legs)\\.json"))
            .forEach(path -> {
               try {
                  String fileName = path.getFileName().toString();
                  String slot = fileName.substring(fileName.lastIndexOf('_') + 1, fileName.length() - ".json".length());
                  var model = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                  String parent = model.get("parent").getAsString();
                  if (parent.equals("typemoonworld:item/servant_armor_generic_" + slot)) {
                     assertEquals("servant_card_cursed_arm_hassan_legs.json", fileName);
                     return;
                  }
                  assertEquals("minecraft:item/generated", parent, fileName);
                  String layer0 = model.getAsJsonObject("textures").get("layer0").getAsString();
                  assertTrue(layer0.startsWith("typemoonworld:item/servant_card_armor/"), fileName);
                  Path texture = RESOURCES.resolve("assets/typemoonworld/textures")
                     .resolve(layer0.substring("typemoonworld:".length()) + ".png");
                  assertTrue(Files.isRegularFile(texture), fileName);
                  var image = ImageIO.read(texture.toFile());
                  assertTrue(image.getWidth() <= 128 && image.getHeight() <= 128,
                     fileName + " preview is " + image.getWidth() + "x" + image.getHeight());
               } catch (Exception exception) {
                  throw new AssertionError(path.toString(), exception);
               }
            });
      }

      Path effects = RESOURCES.resolve("assets/typemoonworld/effects");
      try (var paths = Files.walk(effects)) {
         paths.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
            try {
               String json = Files.readString(path);
               assertFalse(json.contains("\"type\": \"enchant\""), path.toString());
               assertFalse(json.contains("\"type\": \"electric_spark\""), path.toString());
               assertFalse(json.contains("\"type\": \"block\""), path.toString());
               assertFalse(json.contains("\"block\": \"minecraft:"), path.toString());
               assertFalse(json.contains("\"from\":"), path.toString());
               assertFalse(json.contains("\"to\":"), path.toString());
            } catch (Exception exception) {
               throw new AssertionError(path.toString(), exception);
            }
         });
      }
   }

   @Test
   void generatedHairHelmetsAndLongHairCounterRotationAreWired() throws Exception {
      List<String> hairHelmets = List.of(
         "artoria_pendragon", "sasaki_kojiro", "enkidu", "ushiwakamaru_rider",
         "oda_nobunaga", "medusa", "zhao_yun_rider", "paracelsus", "li_shuwen"
      );
      for (String servantId : hairHelmets) {
         Path itemHead = RESOURCES.resolve(
            "assets/typemoonworld/textures/item/servant_card_armor/" + servantId + "_head.png");
         Path modelHead = RESOURCES.resolve(
            "assets/typemoonworld/textures/models/armor/servant_card_" + servantId + "_head.png");
         Path headGeo = RESOURCES.resolve(
            "assets/typemoonworld/geo/servant_card_" + servantId + "_head.geo.json");
         Path armor = RESOURCES.resolve(
            "assets/typemoonworld/textures/models/armor/servant_card_" + servantId + ".png");
         assertTrue(Files.isRegularFile(RESOURCES.resolve(
            "assets/typemoonworld/models/item/servant_card_" + servantId + "_head.json")), servantId);
         assertTrue(Files.isRegularFile(itemHead), servantId);
         assertTrue(Files.isRegularFile(modelHead), servantId);
         assertTrue(Files.isRegularFile(headGeo), servantId);
         assertTrue(Files.mismatch(modelHead, armor) != -1, servantId);
         var texture = ImageIO.read(modelHead.toFile());
         var description = JsonParser.parseString(Files.readString(headGeo)).getAsJsonObject()
            .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonObject("description");
         assertEquals(description.get("texture_width").getAsInt(), texture.getWidth(), servantId);
         assertEquals(description.get("texture_height").getAsInt(), texture.getHeight(), servantId);
      }

      String armorModel = Files.readString(JAVA.resolve("client/model/ServantCardArmorModel.java"));
      assertTrue(armorModel.contains("_head.geo.json"));
      assertTrue(armorModel.contains("hasDedicatedHeadModel(servantId)"));
      assertTrue(armorModel.contains("usesDedicatedHeadModel(servantId)"));
      assertTrue(armorModel.contains("return EMPTY_ANIMATION;"));
      assertTrue(armorModel.contains(
         "case \"artoria_pendragon\", \"enkidu\", \"medusa\", \"oda_nobunaga\", \"paracelsus\","));
      assertTrue(armorModel.contains(
         "\"sasaki_kojiro\", \"ushiwakamaru_rider\", \"zhao_yun_rider\", \"li_shuwen\" -> true"));
      assertTrue(armorModel.contains(
         "case \"enkidu\", \"medusa\", \"oda_nobunaga\", \"paracelsus\" -> true"));
      assertFalse(armorModel.contains(
         "case \"enkidu\", \"medusa\", \"oda_nobunaga\", \"paracelsus\", \"gilgamesh_caster\" -> true"));
      assertTrue(armorModel.contains(
         "return hasDedicatedHeadModel(servantId) || \"gilgamesh_caster\".equals(servantId);"));
      assertFalse(armorModel.contains("\"gilgamesh_caster\" -> true"));
      assertTrue(armorModel.contains("counterRotateHair(\"hair\", pitchRad, 1.25F)"));
      assertTrue(armorModel.contains("counterRotateHair(\"hair1\", pitchRad, 1.35F)"));
      assertTrue(armorModel.contains("counterRotateHair(\"hair2\", pitchRad, 1.35F)"));
      assertTrue(armorModel.contains("counterRotateHair(\"bone4\", pitchRad, 1.25F)"));

      String armorItem = Files.readString(JAVA.resolve("item/custom/ServantCardArmorItem.java"));
      assertTrue(armorItem.contains("\"enkidu\","));

      String armorRenderer = Files.readString(JAVA.resolve("client/renderer/ServantCardArmorRenderer.java"));
      assertTrue(armorRenderer.contains("withScale(0.95F, 0.95F)"));

      String medusaGeo = Files.readString(RESOURCES.resolve(
         "assets/typemoonworld/geo/servant_card_medusa.geo.json"));
      assertTrue(medusaGeo.contains("\"eye mask\""));
      assertTrue(medusaGeo.contains("23.48868"));

      String liShuwenGeo = Files.readString(RESOURCES.resolve(
         "assets/typemoonworld/geo/servant_card_li_shuwen.geo.json"));
      assertTrue(liShuwenGeo.contains("\"galasses\""));
      assertTrue(liShuwenGeo.contains("26.47306"));
      assertTrue(liShuwenGeo.contains("-3.95026"));
      assertTrue(liShuwenGeo.contains("-5.11692"));

      String liShuwenHeadGeo = Files.readString(RESOURCES.resolve(
         "assets/typemoonworld/geo/servant_card_li_shuwen_head.geo.json"));
      assertTrue(liShuwenHeadGeo.contains("-3.95026"));
      assertTrue(liShuwenHeadGeo.contains("-5.11692"));
   }

   @Test
   void hairHelmetTexturesStaySeparateAndClean() throws Exception {
      var artoriaHead = ImageIO.read(RESOURCES.resolve(
         "assets/typemoonworld/textures/models/armor/servant_card_artoria_pendragon_head.png").toFile());
      int artoriaOpaque = 0;
      for (int y = 0; y < artoriaHead.getHeight(); y++) {
         for (int x = 0; x < artoriaHead.getWidth(); x++) {
            int argb = artoriaHead.getRGB(x, y);
            int alpha = (argb >>> 24) & 0xFF;
            int red = (argb >>> 16) & 0xFF;
            int green = (argb >>> 8) & 0xFF;
            int blue = argb & 0xFF;
            if (alpha == 0) {
               assertEquals(0, red, x + "," + y);
               assertEquals(0, green, x + "," + y);
               assertEquals(0, blue, x + "," + y);
               continue;
            }
            artoriaOpaque++;
            assertFalse(blue > Math.max(red, green) + 20, x + "," + y);
         }
      }
      assertEquals(24, artoriaOpaque);

      var artoriaGeo = JsonParser.parseString(Files.readString(RESOURCES.resolve(
         "assets/typemoonworld/geo/servant_card_artoria_pendragon_head.geo.json"))).getAsJsonObject();
      var artoriaGeometry = artoriaGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
      assertEquals("geometry.artoria_ahoge", artoriaGeometry.getAsJsonObject("description")
         .get("identifier").getAsString());
      var ahogeCubes = artoriaGeometry
         .getAsJsonArray("bones").get(1).getAsJsonObject().getAsJsonArray("cubes");
      assertEquals(1, ahogeCubes.size());
      var ahogeCube = ahogeCubes.get(0).getAsJsonObject();
      var ahogeSize = ahogeCube.getAsJsonArray("size");
      assertEquals(6, ahogeSize.get(0).getAsInt());
      assertEquals(5, ahogeSize.get(1).getAsInt());
      assertEquals(0, ahogeSize.get(2).getAsInt());

      var artoriaArmorGeo = JsonParser.parseString(Files.readString(RESOURCES.resolve(
         "assets/typemoonworld/geo/servant_card_artoria_pendragon.geo.json"))).getAsJsonObject();
      var artoriaArmorBones = artoriaArmorGeo.getAsJsonArray("minecraft:geometry")
         .get(0).getAsJsonObject().getAsJsonArray("bones");
      for (var boneElement : artoriaArmorBones) {
         var bone = boneElement.getAsJsonObject();
         if ("armorHead".equals(bone.get("name").getAsString())) {
            assertFalse(bone.has("cubes"));
         }
      }

      var odaHead = ImageIO.read(RESOURCES.resolve(
         "assets/typemoonworld/textures/models/armor/servant_card_oda_nobunaga_head.png").toFile());
      for (int y = 0; y < odaHead.getHeight(); y++) {
         for (int x = 0; x < odaHead.getWidth(); x++) {
            int argb = odaHead.getRGB(x, y);
            int alpha = (argb >>> 24) & 0xFF;
            if (alpha == 0) continue;
            int red = (argb >>> 16) & 0xFF;
            int green = (argb >>> 8) & 0xFF;
            int blue = argb & 0xFF;
            int max = Math.max(red, Math.max(green, blue));
            int min = Math.min(red, Math.min(green, blue));
            assertTrue(max <= 70 && max - min <= 18, x + "," + y);
         }
      }

      var odaGeo = JsonParser.parseString(Files.readString(RESOURCES.resolve(
         "assets/typemoonworld/geo/servant_card_oda_nobunaga_head.geo.json"))).getAsJsonObject();
      var odaBones = odaGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
         .getAsJsonArray("bones");
      for (var boneElement : odaBones) {
         var bone = boneElement.getAsJsonObject();
         if (!"hair1".equals(bone.get("name").getAsString()) || !bone.has("cubes")) continue;
         for (var cubeElement : bone.getAsJsonArray("cubes")) {
            var cube = cubeElement.getAsJsonObject();
            var size = cube.getAsJsonArray("size");
            var uv = cube.getAsJsonArray("uv");
            boolean zeroSize = size.get(0).getAsDouble() == 0.0
               || size.get(1).getAsDouble() == 0.0
               || size.get(2).getAsDouble() == 0.0;
            assertFalse(zeroSize && uv != null && uv.size() == 2
               && uv.get(0).getAsInt() == 0 && uv.get(1).getAsInt() == 0);
         }
      }
   }

   @Test
   void humanoidServantsUseApproximateOriginalHeightScale() throws Exception {
      String renderer = Files.readString(JAVA.resolve("client/renderer/HumanoidServantRenderer.java"));
      assertTrue(renderer.contains("case \"oda_nobunaga\" -> 0.800F"));
      assertTrue(renderer.contains("case \"artoria_pendragon\" -> 0.811F"));
      assertTrue(renderer.contains("case \"fanatic_assassin\", \"medea\" -> 0.858F"));
      assertTrue(renderer.contains("case \"nightingale\" -> 0.868F"));
      assertTrue(renderer.contains("case \"li_shuwen\" -> 0.874F"));
      assertTrue(renderer.contains("case \"senko_muramasa\" -> 0.879F"));
      assertTrue(renderer.contains("case \"ushiwakamaru_rider\" -> 0.884F"));
      assertTrue(renderer.contains("case \"medusa\" -> 0.905F"));
      assertTrue(renderer.contains("case \"emiya_archer\" -> 0.921F"));
      assertTrue(renderer.contains("case \"sasaki_kojiro\" -> 0.926F"));
      assertTrue(renderer.contains("case \"gawain\" -> 0.947F"));
      assertTrue(renderer.contains("case \"enkidu\", \"gilgamesh\", \"gilgamesh_caster\" -> 0.958F"));
      assertTrue(renderer.contains("case \"paracelsus\" -> 0.963F"));
      assertTrue(renderer.contains("case \"zhao_yun_rider\" -> 0.968F"));
      assertTrue(renderer.contains("case \"arash\", \"cu_chulainn\" -> 0.974F"));
   }

   @Test
   void shaderSensitiveRenderStateUsesOfficialBuffersAndClearsThreadLocals() throws Exception {
      String mixins = Files.readString(RESOURCES.resolve("typemoonworld.mixins.json"));
      assertFalse(mixins.contains("RenderBuffersMixin"));
      assertTrue(Files.notExists(JAVA.resolve("mixin/client/RenderBuffersMixin.java")));

      String clientEvents = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      assertTrue(clientEvents.contains("RegisterRenderBuffersEvent"));
      assertTrue(clientEvents.contains("event.registerRenderBuffer(renderType)"));

      String reinforcement = Files.readString(JAVA.resolve("client/renderer/ReinforcementRenderType.java"));
      assertTrue(reinforcement.contains("public static RenderType[] glintTypes()"));

      String armorMixin = Files.readString(JAVA.resolve("mixin/client/HumanoidArmorLayerMixin.java"));
      assertTrue(armorMixin.contains("CURRENT_ARMOR_STACK.remove();"));
      assertTrue(armorMixin.contains("finally"));

      String itemMixin = Files.readString(JAVA.resolve("mixin/ItemRendererMixin.java"));
      assertTrue(itemMixin.contains("TARGET_STACK.remove();"));
      assertTrue(itemMixin.contains("TARGET_IS_GUI_3D.remove();"));
   }

   @Test
   void clientRendererRegistrationHasNoDuplicateEntityOrBlockEntries() throws Exception {
      String registrations = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java")) + "\n"
         + Files.readString(JAVA.resolve("client/ClientModEventSubscriber.java"));
      Pattern entityPattern = Pattern.compile("registerEntityRenderer\\(ModEntities\\.([A-Z0-9_]+)\\.get\\(\\)");
      Pattern blockPattern = Pattern.compile("registerBlockEntityRenderer\\(ModBlockEntities\\.([A-Z0-9_]+)\\.get\\(\\)");
      var entityMatcher = entityPattern.matcher(registrations);
      while (entityMatcher.find()) {
         String field = entityMatcher.group(1);
         assertEquals(1, countMatches(registrations, Pattern.compile(
            "registerEntityRenderer\\(ModEntities\\." + field + "\\.get\\(\\)")), field);
      }
      var blockMatcher = blockPattern.matcher(registrations);
      while (blockMatcher.find()) {
         String field = blockMatcher.group(1);
         assertEquals(1, countMatches(registrations, Pattern.compile(
            "registerBlockEntityRenderer\\(ModBlockEntities\\." + field + "\\.get\\(\\)")), field);
      }

      String genericRenderer = Files.readString(JAVA.resolve("client/renderer/GenericServantRenderer.java"));
      assertTrue(genericRenderer.contains("case \"gilgamesh_caster\" -> \"caster_gilgamesh\""));
   }

   @Test
   void gilgameshProjectileShieldAndCasterBuffCleanupAreThrottled() throws Exception {
      String gilgamesh = Files.readString(JAVA.resolve("servant/entity/GilgameshCombatHelper.java"));
      assertTrue(gilgamesh.contains("DIVINE_SHIELD_SCAN_INTERVAL = 5"));
      assertTrue(gilgamesh.contains("LAST_DIVINE_SHIELD_SCAN"));
      assertTrue(gilgamesh.contains("entity.tickCount - data.getInt(LAST_DIVINE_SHIELD_SCAN)"));

      String caster = Files.readString(JAVA.resolve("servant/entity/CasterGilgameshCombatHelper.java"));
      assertTrue(caster.contains("DIVINE_SHIELD_SCAN_INTERVAL = 5"));
      assertTrue(caster.contains("LAST_DIVINE_SHIELD_SCAN"));
      assertTrue(caster.contains("long leaderUntil = data.getLong(LEADER_UNTIL);"));
      assertTrue(caster.contains("if (leaderUntil > 0L && now >= leaderUntil)"));
      assertTrue(caster.contains("data.remove(LEADER_UNTIL);"));
      assertTrue(caster.contains("if (returnUntil > 0L && now >= returnUntil)"));
      assertTrue(caster.contains("data.remove(RETURN_UNTIL);"));
   }

   @Test
   void npcArmorSyncAndTacticalScansStayLowFrequency() throws Exception {
      String servantEntity = Files.readString(JAVA.resolve("servant/entity/ServantEntity.java"));
      assertTrue(servantEntity.contains("this.equipNpcServantCardArmor(this.tickCount % 200 == 0)"));

      String maneuver = Files.readString(JAVA.resolve("servant/ai/ServantManeuverService.java"));
      assertEquals(1, countMatches(maneuver, Pattern.compile("getEntitiesOfClass\\(LivingEntity\\.class")));
      assertTrue(maneuver.contains("List<LivingEntity> nearbyAllies"));
      assertTrue(maneuver.contains("countAlliesNear(nearbyAllies, safe, 3.0)"));

      String link = Files.readString(JAVA.resolve("servant/card/MasterServantLinkService.java"));
      assertTrue(link.contains("if (player.tickCount % 20 == 0) removeLegacyServantPenalties(player);"));
   }

   @Test
   void compatibilityCleanupAvoidsNoopTicksAndStaleStaticState() throws Exception {
      String chant = Files.readString(JAVA.resolve("magic/unlimited_blade_works/ChantHandler.java"));
      assertFalse(chant.contains("UBWInstanceManager.processPendingDeletions(player.getServer())"));
      assertTrue(chant.contains("MagicSwordBarrelFullOpen.tick(player, vars);"));

      String swordBarrel = Files.readString(JAVA.resolve("magic/unlimited_blade_works/MagicSwordBarrelFullOpen.java"));
      assertTrue(swordBarrel.contains("public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars)"));

      String ubwManager = Files.readString(JAVA.resolve("magic/unlimited_blade_works/UBWInstanceManager.java"));
      assertTrue(ubwManager.contains("ServerStoppingEvent"));
      assertTrue(ubwManager.contains("public static void clearAllInstances(MinecraftServer server)"));
      assertTrue(ubwManager.contains("OWNER_TICKETS.clear();"));
      assertTrue(ubwManager.contains("OWNER_DIMENSIONS.clear();"));
      assertTrue(ubwManager.contains("DIMENSION_OWNERS.clear();"));

      String commonEvents = Files.readString(JAVA.resolve("event/CommonEvents.java"));
      assertTrue(commonEvents.contains("public static void onLevelUnload(LevelEvent.Unload event)"));
      assertTrue(commonEvents.contains("SUGGESTED_MOB_IDS_BY_DIMENSION.remove(key);"));
      assertTrue(commonEvents.contains("SERVANT_IDS_BY_DIMENSION.remove(key);"));
      assertTrue(commonEvents.contains("SHIKI_IDS_BY_DIMENSION.remove(key);"));
      assertTrue(commonEvents.contains("SERVANT_IDS_BY_DIMENSION.remove(dimensionKey(level), ids);"));
      assertTrue(commonEvents.contains("SHIKI_IDS_BY_DIMENSION.remove(dimensionKey(level), ids);"));
      assertTrue(commonEvents.contains("SUGGESTED_MOB_IDS_BY_DIMENSION.remove(key, ids);"));
   }
}

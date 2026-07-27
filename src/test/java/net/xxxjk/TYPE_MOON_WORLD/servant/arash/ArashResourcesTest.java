package net.xxxjk.TYPE_MOON_WORLD.servant.arash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ArashResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void definitionAiAndTraitsMatchContract() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/arash.json");
      JsonObject params = definition.getAsJsonObject("parameters");
      assertEquals("A", params.get("endurance").getAsString());
      assertEquals("B", params.get("strength").getAsString());
      assertEquals("B", params.get("agility").getAsString());
      assertTrue(params.get("agility_plus").getAsBoolean());
      assertEquals("E", params.get("magic").getAsString());
      assertEquals("D", params.get("luck").getAsString());
      assertEquals("stella_arash", definition.get("noble_phantasm").getAsString());
      for (String trait : List.of("servant", "male", "archer", "human", "humanoid", "chaotic", "balanced", "earth", "seven_knights", "ea_special")) {
         assertTrue(definition.getAsJsonArray("traits").toString().contains(trait), trait);
      }
      JsonObject ai = json("data/typemoonworld/servant/ai/arash_ai.json");
      assertEquals(12.0, ai.getAsJsonObject("movement").get("wander_radius").getAsDouble());
      assertTrue(ai.getAsJsonObject("movement").get("likes_high_places").getAsBoolean());
      assertEquals(0.0, ai.getAsJsonObject("combat").get("melee_preference").getAsDouble());
      assertEquals(0.95, ai.getAsJsonObject("command").get("base_obedience_rate").getAsDouble());
   }

   @Test
   void modelTexturesBowAndAnimationsAreValid() throws Exception {
      JsonObject bodyGeo = json("assets/typemoonworld/geo/arash.geo.json");
      JsonObject bodyDescription = bodyGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonObject("description");
      assertEquals("geometry.arash", bodyDescription.get("identifier").getAsString());
      assertEquals(128, bodyDescription.get("texture_width").getAsInt());
      assertEquals(128, bodyDescription.get("texture_height").getAsInt());
      JsonObject bowGeo = json("assets/typemoonworld/geo/arash_bow.geo.json");
      assertEquals("geometry.arash_bow", bowGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
         .getAsJsonObject("description").get("identifier").getAsString());
      var body = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/entity/arash.png").toFile());
      var bow = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/arash_bow.png").toFile());
      assertNotNull(body); assertNotNull(bow);
      assertEquals(128, body.getWidth()); assertEquals(128, body.getHeight());
      assertEquals(64, bow.getWidth()); assertEquals(64, bow.getHeight());
      JsonObject animations = json("assets/typemoonworld/animations/arash.animation.json").getAsJsonObject("animations");
      for (String key : List.of("idle", "walk", "bow_shot", "arrow_rain", "energy_small", "energy_large", "stella_chant", "stella_release")) {
         assertTrue(animations.has("animation.arash." + key), key);
      }
      JsonObject root = animations.getAsJsonObject("animation.arash.idle").getAsJsonObject("bones").getAsJsonObject("bone");
      assertEquals(-3.0, root.getAsJsonArray("position").get(1).getAsDouble());
      assertEquals(0.88, root.getAsJsonArray("scale").get(0).getAsDouble());
   }

   @Test
   void registrationsAndEveryVoiceAssetExist() throws Exception {
      String entities = Files.readString(JAVA.resolve("init/ModEntities.java"));
      String items = Files.readString(JAVA.resolve("item/ModItems.java"));
      assertTrue(entities.contains("ARASH_PARTICLE_ARROW"));
      assertTrue(entities.contains("ARASH_STELLA_CONTROLLER"));
      assertTrue(entities.contains("ARASH ="));
      assertTrue(items.contains("ARASH_BOW"));
      assertTrue(items.contains("ARASH_SPAWN_EGG"));
      assertEquals("minecraft:item/template_spawn_egg",
         json("assets/typemoonworld/models/item/arash_spawn_egg.json").get("parent").getAsString());
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String event : List.of("attack", "fail", "victory", "stella")) assertTrue(sounds.has("arash_voice_" + event), event);
      for (String file : List.of("attack1.ogg", "attack2.ogg", "attack3.ogg", "fail1.ogg", "fail2.ogg", "victory1.ogg", "victory2.ogg", "stella.ogg")) {
         byte[] bytes = Files.readAllBytes(RESOURCES.resolve("assets/typemoonworld/sounds/voice/arash/" + file));
         assertTrue(bytes.length > 4, file);
         assertEquals("OggS", new String(bytes, 0, 4, StandardCharsets.US_ASCII), file);
         String container = new String(bytes, StandardCharsets.ISO_8859_1);
         assertTrue(container.contains("vorbis"), file + " is missing Vorbis audio");
         assertTrue(!container.contains("theora"), file + " must not contain a video stream");
      }
   }

   @Test
   void servantCardAssetsMeetTheApprovedContract() throws Exception {
      String items = Files.readString(JAVA.resolve("item/ModItems.java"));
      String registry = Files.readString(JAVA.resolve("servant/card/ServantCardRegistry.java"));
      String loadout = Files.readString(JAVA.resolve("servant/card/ServantCardLoadoutManager.java"));
      assertTrue(items.contains("SERVANT_CARD_ARASH"));
      assertTrue(items.contains("SERVANT_CARD_ARASH_CHEST"));
      assertTrue(items.contains("SERVANT_CARD_ARASH_LEGS"));
      assertTrue(registry.contains("new Entry(\"arash\""));
      assertTrue(loadout.contains("case \"arash\" -> main = stack(ModItems.ARASH_BOW.get())"));

      Path face2dPath = RESOURCES.resolve("assets/typemoonworld/textures/item/servant_cards/arash_card.png");
      Path face3dPath = RESOURCES.resolve("assets/typemoonworld/textures/item/card_faces_3d/servant/arash_card.png");
      var face2d = ImageIO.read(face2dPath.toFile());
      var face3d = ImageIO.read(face3dPath.toFile());
      assertEquals(181, face2d.getWidth()); assertEquals(256, face2d.getHeight());
      assertEquals(292, face3d.getWidth()); assertEquals(500, face3d.getHeight());
      assertTrue(Files.size(face2dPath) <= 150 * 1024, "2D card face is too large");
      assertTrue(Files.size(face3dPath) <= 400 * 1024, "3D card face is too large");

      JsonObject model = json("assets/typemoonworld/models/item/servant_card_arash.json");
      assertEquals("typemoonworld:item/servant_card_backs/archer",
         model.getAsJsonObject("textures").get("back").getAsString());
      JsonObject armorGeo = json("assets/typemoonworld/geo/servant_card_arash.geo.json");
      JsonObject description = armorGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonObject("description");
      assertEquals("geometry.servant_card_arash", description.get("identifier").getAsString());
      var armorTexture = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/models/armor/servant_card_arash.png").toFile());
      assertEquals(128, armorTexture.getWidth()); assertEquals(128, armorTexture.getHeight());
      assertTrue(json("assets/typemoonworld/animations/servant_card_arash.animation.json")
         .getAsJsonObject("animations").has("1"));
   }

   @Test
   void stellaImplementationContainsSafetyPersistenceAndTerrainContracts() throws Exception {
      String controller = Files.readString(JAVA.resolve("entity/ArashStellaControllerEntity.java"));
      assertTrue(controller.contains("STELLA_FLIGHT_TICKS"));
      assertTrue(controller.contains("STELLA_PRELOAD_GRACE_TICKS"));
      assertTrue(controller.contains("CHUNK_TICKETS.forceChunk"));
      assertTrue(controller.contains("hasFriendlyInBlastPath"));
      assertTrue(controller.contains("insideWorldBorder"));
      assertTrue(controller.contains("putLongArray(\"Chunks\""));
      String terrain = Files.readString(JAVA.resolve("world/terrain/DeferredTerrainDestruction.java"));
      assertTrue(terrain.contains("queueAdvancingCylinder"));
      assertTrue(terrain.contains("currentSide * currentSide + currentY * currentY > radiusSqr"));
      assertTrue(terrain.contains("scarDepthAt"));
      assertTrue(terrain.contains("MOTION_BLOCKING_NO_LEAVES"));
      assertTrue(terrain.contains("queueExpandingSphere"));
      for (String protectedBlock : List.of("NETHER_PORTAL", "END_PORTAL", "COMMAND_BLOCK", "STRUCTURE_BLOCK", "JIGSAW")) {
         assertTrue(terrain.contains(protectedBlock), protectedBlock);
      }
   }

   @Test
   void npcAiContainsApproachOrbitFlankAndCrossoverMovement() throws Exception {
      JsonObject ai = json("data/typemoonworld/servant/ai/arash_ai.json");
      assertEquals(32.0, ai.getAsJsonObject("combat").get("preferred_attack_distance").getAsDouble());
      String combat = Files.readString(JAVA.resolve("servant/entity/ArashCombatHelper.java"));
      assertTrue(combat.contains("tacticalMovement"));
      assertTrue(combat.contains("beginCrossover"));
      assertTrue(combat.contains("findTacticalPosition"));
      assertTrue(combat.contains("applyMobileStrafe"));
      assertTrue(!combat.contains("doHurtTarget"));
   }

   @Test
   void playerBowUsesInstantAndTwoStageLoadedChunkArrows() throws Exception {
      String bow = Files.readString(JAVA.resolve("item/custom/ArashBowItem.java"));
      String arrow = Files.readString(JAVA.resolve("entity/ArashParticleArrowEntity.java"));
      String client = Files.readString(JAVA.resolve("client/ServantCardClientEvents.java"));
      assertTrue(bow.contains("fireBasicArrow(serverLevel, player)"));
      assertTrue(bow.contains("CHARGED_ARROW_TICKS = 40"));
      assertTrue(bow.contains("HEAVY_CHARGED_ARROW_TICKS = 80"));
      assertTrue(bow.contains("performBowChargedArrowNoCooldown"));
      assertTrue(!bow.contains("triggerAction(serverPlayer, slot)"));
      assertTrue(arrow.contains("level.hasChunkAt(BlockPos.containing"));
      assertTrue(!arrow.contains("maxLifeTicks"));
      assertTrue(!arrow.contains("tickCount >"));
      assertTrue(client.contains("ModItems.ARASH_BOW"));
      assertTrue(client.contains("float magnification = 2.0F + zoomSteps"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
   }
}

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
   void doubledStoutEffectsAreSharedByNpcAndCard() throws Exception {
      String rules = Files.readString(JAVA.resolve("servant/entity/ArashCombatRules.java"));
      String npc = Files.readString(JAVA.resolve("servant/entity/ArashEntity.java"));
      String card = Files.readString(JAVA.resolve("servant/card/ServantCardArashSkills.java"));
      String npcCombat = Files.readString(JAVA.resolve("servant/combat/ServantCombatSystem.java"));
      String cardDefense = Files.readString(JAVA.resolve("servant/card/ServantCardDefenseHandler.java"));
      String commonSkills = Files.readString(JAVA.resolve("servant/skill/CommonServantSkills.java"));
      assertTrue(rules.contains("STOUT_DAMAGE_MULTIPLIER = 0.70F"));
      assertTrue(npc.contains("amount *= ArashCombatRules.STOUT_DAMAGE_MULTIPLIER"));
      assertTrue(card.contains("event.getAmount() * (float)ArashCombatRules.STOUT_DAMAGE_MULTIPLIER"));
      assertTrue(npcCombat.contains("entity instanceof ArashEntity"));
      assertTrue(npcCombat.contains("ArashCombatRules.boostedDefenseRecovery"));
      assertTrue(npcCombat.contains("ArashCombatRules.boostedPoiseRecovery"));
      assertTrue(cardDefense.contains("\"arash\".equals(vars.servant_card_id)"));
      assertTrue(cardDefense.contains("ArashCombatRules.boostedDefenseRecovery"));
      assertTrue(cardDefense.contains("ArashCombatRules.boostedPoiseRecovery"));
      assertTrue(commonSkills.contains("StoutDefenseRecoveryMultiplier"));
      assertTrue(commonSkills.contains("StoutPoiseRecoveryMultiplier"));
      JsonObject skill = json("data/typemoonworld/servant/skills/stout_ex_arash.json");
      assertTrue(skill.get("description_zh").getAsString().contains("防御恢复效率"));
      assertTrue(skill.get("description_zh").getAsString().contains("韧度恢复效率"));
   }

   @Test
   void npcUsesSteveSkinAndNoLongerUsesGeckoModel() throws Exception {
      String renderer = Files.readString(JAVA.resolve("client/renderer/HumanoidServantRenderer.java"));
      String client = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      String definition = Files.readString(RESOURCES.resolve("data/typemoonworld/servant/definitions/arash.json"));
      assertTrue(renderer.contains("HumanoidMobRenderer"));
      assertTrue(renderer.contains("ModelLayers.PLAYER"));
      assertTrue(renderer.contains("HumanoidArmorLayer"));
      assertTrue(client.contains("ModEntities.ARASH.get(), context -> new HumanoidServantRenderer<>(context, \"arash\")"));
      assertEquals("", JsonParser.parseString(definition).getAsJsonObject().getAsJsonObject("model").get("geometry").getAsString());
      assertEquals("", JsonParser.parseString(definition).getAsJsonObject().getAsJsonObject("model").get("animation").getAsString());
      assertTrue(Files.notExists(JAVA.resolve("client/renderer/ArashRenderer.java")));
      assertTrue(Files.notExists(RESOURCES.resolve("assets/typemoonworld/geo/arash.geo.json")));
      assertTrue(Files.notExists(RESOURCES.resolve("assets/typemoonworld/animations/arash.animation.json")));
      var body = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/entity/arash.png").toFile());
      assertNotNull(body);
      assertEquals(64, body.getWidth());
      assertEquals(64, body.getHeight());
      JsonObject bowGeo = json("assets/typemoonworld/geo/arash_bow.geo.json");
      assertEquals("geometry.arash_bow", bowGeo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
         .getAsJsonObject("description").get("identifier").getAsString());
      var bow = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/arash_bow.png").toFile());
      assertNotNull(bow);
      assertEquals(64, bow.getWidth()); assertEquals(64, bow.getHeight());
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
      for (String event : List.of("attack", "fail", "victory", "stella", "stella_short")) {
         assertTrue(sounds.has("arash_voice_" + event), event);
      }
      for (String file : List.of("attack1.ogg", "attack2.ogg", "attack3.ogg", "fail1.ogg", "fail2.ogg",
         "victory1.ogg", "victory2.ogg", "stella.ogg", "stella_short.ogg")) {
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
      int releaseStart = controller.indexOf("private void release(ServerLevel level, LivingEntity caster)");
      int releaseEnd = controller.indexOf("public void forceReleaseForGameTest()", releaseStart);
      assertTrue(releaseStart >= 0 && releaseEnd > releaseStart);
      assertTrue(!controller.substring(releaseStart, releaseEnd).contains("stopChantSound"),
         "the 38.83 second Stella voice must continue after the 35 second release");
      assertTrue(controller.contains("profile.flightTicks()"));
      assertTrue(controller.contains("STELLA_PRELOAD_GRACE_TICKS"));
      assertTrue(controller.contains("CHUNK_TICKETS.forceChunk"));
      assertTrue(controller.contains("hasFriendlyInBlastPath"));
      assertTrue(controller.contains("insideWorldBorder"));
      assertTrue(controller.contains("putLongArray(\"Chunks\""));
      assertTrue(controller.contains("putLongArray(\"ForcedChunks\""));
      assertTrue(controller.contains("shouldFinishLongStellaVoice"));
      assertTrue(controller.contains("stopLongChantSound"));
      assertTrue(controller.contains("playShortStella"));
      assertTrue(controller.contains("skyRiftHits"));
      assertTrue(controller.contains("horizontalDistanceToSegment"));
      assertTrue(controller.contains("level.getMaxBuildHeight()"));
      assertTrue(controller.contains("Heightmap.Types.WORLD_SURFACE"));
      assertTrue(controller.contains("profile.terrainRadius()"));
      assertTrue(controller.contains("worldBorderEndpoint"));
      assertTrue(controller.contains("distanceToWorldBorder"));
      assertTrue(controller.contains("worldBorderLength"));
      assertTrue(controller.contains("WorldBorderLength"));
      assertTrue(controller.contains("WorldBorder"));
      assertTrue(controller.contains("sealAt(profile.length())"));
      String controls = Files.readString(JAVA.resolve("init/TypeMoonWorldModKeyMappings.java"));
      String manager = Files.readString(JAVA.resolve("servant/card/ServantCardTransformManager.java"));
      assertTrue(controls.contains("\"arash\".equals(vars.servant_card_id) && slot == 9"));
      assertTrue(manager.contains("ServantCardArashSkills.requestPlayerStellaRelease(player)"));
      String cardSkills = Files.readString(JAVA.resolve("servant/card/ServantCardArashSkills.java"));
      assertTrue(cardSkills.contains("player.setPos(anchorX, anchorY, anchorZ)"));
      assertTrue(cardSkills.contains("player.setDeltaMovement(Vec3.ZERO)"));
      assertTrue(cardSkills.contains("data.putInt(REFUND_ARROWS, vars.servant_card_arash_arrow_stock)"));
      assertTrue(cardSkills.contains("vars.servant_card_arash_arrow_stock = data.getInt(REFUND_ARROWS)"));
      assertTrue(cardSkills.contains("data.remove(REFUND_ARROWS)"));
      String terrain = Files.readString(JAVA.resolve("world/terrain/DeferredTerrainDestruction.java"));
      assertTrue(terrain.contains("sealAt(double distance)"));
      assertTrue(terrain.contains("sealedDistance"));
      assertTrue(terrain.contains("queueAdvancingCylinder"));
      assertTrue(terrain.contains("queueAdvancingSkyRift"));
      assertTrue(terrain.contains("currentSide * currentSide + currentY * currentY > radiusSqr"));
      assertTrue(terrain.contains("scarDepthAt"));
      assertTrue(terrain.contains("MOTION_BLOCKING_NO_LEAVES"));
      assertTrue(terrain.contains("WORLD_SURFACE"));
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
      assertTrue(!combat.contains("consumeArrow"));
      assertTrue(!combat.contains("ARROW_REFILL_MANA"));
      assertTrue(combat.contains("now >= arash.getPersistentData().getLong(TAG_NEXT_NORMAL)"));
      assertTrue(combat.contains("lockAttackFacing"));
      assertTrue(combat.contains("arash.faceVector(offset)"));
      assertTrue(combat.contains("consumeCraftedArrows(ArashCombatRules.ARROW_RAIN_COST)"));
      assertTrue(combat.contains("consumeCraftedArrows(ArashCombatRules.NORMAL_ARROW_COST)"));
      String entity = Files.readString(JAVA.resolve("servant/entity/ArashEntity.java"));
      assertTrue(entity.contains("INITIAL_ARROW_COUNT"));
      assertTrue(entity.contains("tickArrowCreation"));
      assertTrue(entity.contains("ARROW_CREATION_THRESHOLD"));
      String commonSkills = Files.readString(JAVA.resolve("servant/skill/CommonServantSkills.java"));
      assertTrue(!commonSkills.contains("ArashVirtualArrows"));
   }

   @Test
   void playerBowUsesInstantAndTwoStageLoadedChunkArrows() throws Exception {
      String bow = Files.readString(JAVA.resolve("item/custom/ArashBowItem.java"));
      String arrow = Files.readString(JAVA.resolve("entity/ArashParticleArrowEntity.java"));
      String aim = Files.readString(JAVA.resolve("servant/entity/ArashAimHelper.java"));
      String combat = Files.readString(JAVA.resolve("servant/entity/ArashCombatHelper.java"));
      String skills = Files.readString(JAVA.resolve("servant/card/ServantCardArashSkills.java"));
      String client = Files.readString(JAVA.resolve("client/ServantCardClientEvents.java"));
      assertTrue(bow.contains("fireBasicArrow(serverLevel, player)"));
      assertTrue(bow.contains("CHARGED_ARROW_TICKS = 40"));
      assertTrue(bow.contains("HEAVY_CHARGED_ARROW_TICKS = 60"));
      assertTrue(bow.contains("performBowChargedArrowNoCooldown"));
      assertTrue(!bow.contains("triggerAction(serverPlayer, slot)"));
      assertTrue(arrow.contains("level.hasChunkAt(BlockPos.containing"));
      assertTrue(arrow.contains("MAX_VISIBLE_FLIGHT_DISTANCE = 128.0"));
      assertTrue(arrow.contains("distanceTraveled + this.getDeltaMovement().length()"));
      assertTrue(arrow.contains("addFlightParticle(GREEN, this.position())"));
      assertTrue(arrow.contains("addFlightParticle(heavy ? HEAVY_RED : CHARGED_GREEN, center)"));
      assertTrue(arrow.contains("addParticle(particle, false"),
         "Arash arrow trails must stop outside the client's normal visible particle distance");
      assertTrue(arrow.contains("LARGE_ENERGY ? HEAVY_RED : GREEN"));
      assertTrue(entitiesForTest().contains("clientTrackingRange(8)"));
      assertTrue(arrow.contains("terrainDestructionRadius(this.getVariant())"));
      assertTrue(arrow.contains("queueExpandingSphere(level, impactPosition, terrainRadius, null)"));
      assertTrue(arrow.contains("terrain.advanceTo(terrainRadius)"));
      assertTrue(arrow.contains("terrain.seal()"));
      assertTrue(bow.contains("ArashAimHelper.autoAimDirection"));
      assertTrue(skills.contains("EntityUtils.getRayTraceTarget(player, ArashAimHelper.AUTO_AIM_RANGE)"));
      assertTrue(skills.contains("ArashAimHelper.findTargetNearPoint"));
      assertTrue(skills.contains("lookedAt.getLocation()"));
      assertTrue(skills.contains("ArashAimHelper.autoAimDirection"));
      assertTrue(skills.contains("CROUCH_RAIN_ARROW_COUNT"));
      assertTrue(skills.contains("CROUCH_RAIN_SPREAD_RADIUS"));
      assertTrue(combat.contains("ArashAimHelper.leadDirection"));
      assertTrue(combat.contains("ArashAimHelper.predictionOffset"));
      assertTrue(aim.contains("AUTO_AIM_ANGLE_DEGREES = 8.0"));
      assertTrue(aim.contains("ARROW_RAIN_ASSIST_RADIUS = 12.0"));
      assertTrue(aim.contains("target.getDeltaMovement()"));
      assertTrue(client.contains("ModItems.ARASH_BOW"));
      assertTrue(client.contains("float magnification = 2.0F + zoomSteps"));
      String hud = Files.readString(JAVA.resolve("client/screens/ServantCardHud.java"));
      assertTrue(hud.contains("drawArashArrows"));
      assertTrue(hud.contains("servant_card_arash_arrow_stock"));
      String variables = Files.readString(JAVA.resolve("network/TypeMoonWorldModVariables.java"));
      assertTrue(variables.contains("servant_card_arash_arrow_stock"));
      assertTrue(variables.contains("arashArrowStock"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
   }

   private static String entitiesForTest() throws Exception {
      return Files.readString(JAVA.resolve("init/ModEntities.java"));
   }
}

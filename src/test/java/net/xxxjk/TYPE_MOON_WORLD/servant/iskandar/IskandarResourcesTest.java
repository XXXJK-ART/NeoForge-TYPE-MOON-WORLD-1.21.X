package net.xxxjk.TYPE_MOON_WORLD.servant.iskandar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class IskandarResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");
   private static final Path JAVA = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void definitionMatchesRiderPanelAndArmyContract() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/iskandar.json");
      assertEquals("iskandar", definition.get("id").getAsString());
      assertEquals("rider", definition.get("class_type").getAsString());

      JsonObject parameters = definition.getAsJsonObject("parameters");
      assertEquals("B", parameters.get("strength").getAsString());
      assertEquals("A", parameters.get("endurance").getAsString());
      assertEquals("D", parameters.get("agility").getAsString());
      assertEquals("C", parameters.get("magic").getAsString());
      assertEquals("A", parameters.get("luck").getAsString());
      assertTrue(parameters.get("luck_plus").getAsBoolean());

      assertEquals("ionioi_hetairoi", definition.get("noble_phantasm").getAsString());
      assertTrue(definition.getAsJsonArray("noble_phantasms").toString().contains("gordius_wheel"));
      assertEquals("typemoonworld:geo/iskandar.geo.json",
         definition.getAsJsonObject("model").get("geometry").getAsString());
      assertEquals("typemoonworld:animations/iskandar.animation.json",
         definition.getAsJsonObject("model").get("animation").getAsString());
      assertEquals("typemoonworld:iskandar_shortsword",
         definition.getAsJsonObject("specialization").get("default_weapon").getAsString());
      for (String skill : List.of("magic_resistance_d", "riding_a_plus", "divinity_c",
         "leadership_a_iskandar", "military_tactics_b_iskandar")) {
         assertTrue(definition.getAsJsonArray("skills").toString().contains(skill), skill);
      }

      JsonObject army = json("data/typemoonworld/servant/noble_phantasms/ionioi_hetairoi.json");
      JsonObject params = army.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params");
      assertEquals(10000, params.get("total_soldiers").getAsInt());
      assertEquals(400, params.get("active_cap").getAsInt());
      assertEquals(49, params.get("reality_marble_priority").getAsInt());
      assertEquals(8000, params.get("collapse_after_deaths").getAsInt());
      assertEquals(10000, params.get("end_after_deaths").getAsInt());
      assertEquals(5, params.get("upkeep_mp_per_second").getAsInt());
      assertEquals(60, army.get("mp_cost").getAsInt());
   }

   @Test
   void ionioiHetairoiUsesARealDesertRealityMarbleDimension() throws Exception {
      JsonObject dimension = json("data/typemoonworld/dimension/ionioi_hetairoi.json");
      assertEquals("typemoonworld:ionioi_hetairoi", dimension.get("type").getAsString());
      assertEquals("typemoonworld:ionioi_hetairoi_noise_settings",
         dimension.getAsJsonObject("generator").get("settings").getAsString());
      assertEquals("typemoonworld:ionioi_hetairoi_biome",
         dimension.getAsJsonObject("generator").getAsJsonObject("biome_source").get("biome").getAsString());

      JsonObject dimensionType = json("data/typemoonworld/dimension_type/ionioi_hetairoi.json");
      assertEquals("typemoonworld:ionioi_hetairoi", dimensionType.get("effects").getAsString());
      assertTrue(dimensionType.get("has_skylight").getAsBoolean());
      assertEquals(6000, dimensionType.get("fixed_time").getAsInt());

      String dimensions = Files.readString(JAVA.resolve("world/dimension/ModDimensions.java"));
      String client = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      assertTrue(dimensions.contains("IONIOI_HETAIROI_KEY"));
      assertTrue(dimensions.contains("isIonioiHetairoiDimension"));
      assertTrue(client.contains("IonioiHetairoiDimensionEffects"));
      assertTrue(iskandar.contains("DimensionTransition(ionioiLevel"));
      assertTrue(iskandar.contains("TAG_IONIOI_RETURN_DIM"));
      assertTrue(iskandar.contains("returnIonioiTargets(level)"));

      JsonObject biome = json("data/typemoonworld/worldgen/biome/ionioi_hetairoi_biome.json");
      JsonObject effects = biome.getAsJsonObject("effects");
      assertEquals(16771494, effects.get("fog_color").getAsInt());
      assertEquals(16770234, effects.get("sky_color").getAsInt());

      String noise = Files.readString(RESOURCES.resolve("data/typemoonworld/worldgen/noise_settings/ionioi_hetairoi_noise_settings.json"));
      assertTrue(noise.contains("\"Name\": \"minecraft:sand\""));
      assertTrue(noise.contains("\"Name\": \"minecraft:sandstone\""));
      assertTrue(noise.contains("\"Name\": \"minecraft:smooth_sandstone\""));
      assertTrue(noise.contains("\"Name\": \"minecraft:cut_sandstone\""));
      assertFalse(noise.contains("red_sandstone"));
      assertTrue(noise.contains("\"secondary_depth_range\": 4"));

      String effectsClass = Files.readString(JAVA.resolve("client/world/IonioiHetairoiDimensionEffects.java"));
      assertTrue(effectsClass.contains("sunHaze"));
      assertTrue(effectsClass.contains("return false;"));
      assertTrue(effectsClass.contains("litFog.scale(0.78)"));
      String commonEvents = Files.readString(JAVA.resolve("event/CommonEvents.java"));
      assertFalse(commonEvents.contains("shouldBlockUnexpectedIonioiSpawn"));
      assertFalse(commonEvents.contains("ModDimensions.isIonioiHetairoiDimension"));
      assertFalse(commonEvents.contains("IONIOI_TARGET_OWNER_TAG"));
   }

   @Test
   void ionioiHetairoiUsesOffscreenSettlementWhenNoPlayerIsPulled() throws Exception {
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      assertTrue(iskandar.contains("pulled.stream().noneMatch(ServerPlayer.class::isInstance)"));
      assertTrue(iskandar.contains("startOffscreenIonioiDuel"));
      assertTrue(iskandar.contains("finishOffscreenIonioiDuel"));
      assertTrue(iskandar.contains("TAG_IONIOI_OFFSCREEN_DUEL"));
      assertTrue(iskandar.contains("hideForIonioiOffscreenDuel"));
   }

   @Test
   void registrationsResourcesAndAudioArePresent() throws Exception {
      String entities = Files.readString(JAVA.resolve("init/ModEntities.java"));
      String items = Files.readString(JAVA.resolve("item/ModItems.java"));
      String client = Files.readString(JAVA.resolve("client/TypeMoonWorldClientEvents.java"));
      String factory = Files.readString(JAVA.resolve("servant/entity/BuiltinServantEntityFactory.java"));

      for (String id : List.of("ISKANDAR", "BUCEPHALUS", "GORDIUS_WHEEL", "MACEDONIAN_SOLDIER", "MACEDONIAN_SPEAR_PROJECTILE")) {
         assertTrue(entities.contains(" " + id + " ="), id);
      }
      assertTrue(items.contains("ISKANDAR_SPAWN_EGG"));
      assertTrue(items.contains("MACEDONIAN_SOLDIER_SPAWN_EGG"));
      assertTrue(items.contains("MACEDONIAN_SPEAR"));
      assertTrue(items.contains("MACEDONIAN_ROUND_SHIELD"));
      assertTrue(items.contains("ISKANDAR_SHORTSWORD"));
      assertTrue(items.contains("new IskandarShortswordItem"));
      assertTrue(items.contains("SERVANT_CARD_ISKANDAR = registerServantCard(\"iskandar\")"));
      assertTrue(items.contains("SERVANT_CARD_ISKANDAR_CHEST"));
      assertTrue(items.contains("SERVANT_CARD_ISKANDAR_LEGS"));
      assertTrue(items.contains("SERVANT_CARD_ISKANDAR_FEET"));
      assertTrue(client.contains("ModEntities.ISKANDAR.get()"));
      assertTrue(client.contains("ModEntities.BUCEPHALUS.get()"));
      assertTrue(client.contains("ModEntities.GORDIUS_WHEEL.get()"));
      assertTrue(client.contains("ModEntities.MACEDONIAN_SOLDIER.get()"));
      assertTrue(client.contains("ModEntities.MACEDONIAN_SPEAR_PROJECTILE.get()"));
      assertTrue(factory.contains("IskandarEntity.SERVANT_KEY.equals(servantId.getPath())"));

      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String event : List.of("iskandar_voice_attack", "iskandar_voice_fail",
         "iskandar_voice_victory", "iskandar_voice_ionioi")) {
         assertTrue(sounds.has(event), event);
      }
      for (String file : List.of("np.ogg", "attack1.ogg", "attack2.ogg", "attack3.ogg",
         "fail1.ogg", "fail2.ogg", "fail3.ogg", "fail4.ogg",
         "victory1.ogg", "victory2.ogg", "victory3.ogg", "victory4.ogg")) {
         assertTrue(Files.size(RESOURCES.resolve("assets/typemoonworld/sounds/voice/iskandar/" + file)) > 4, file);
      }
      for (String file : List.of(
         "assets/typemoonworld/textures/item/iskandar_shortsword.png",
         "assets/typemoonworld/textures/item/servant_card_armor/iskandar_chest.png",
         "assets/typemoonworld/textures/item/servant_card_armor/iskandar_legs.png",
         "assets/typemoonworld/textures/item/servant_card_armor/iskandar_feet.png",
         "assets/typemoonworld/textures/models/armor/servant_card_iskandar.png",
         "assets/typemoonworld/geo/servant_card_iskandar.geo.json",
         "assets/typemoonworld/geo/iskandar.geo.json",
         "assets/typemoonworld/geo/iskandar_shortsword.geo.json",
         "assets/typemoonworld/animations/servant_card_iskandar.animation.json",
         "assets/typemoonworld/animations/iskandar.animation.json",
         "assets/typemoonworld/animations/iskandar_shortsword.animation.json",
         "assets/typemoonworld/models/item/iskandar_shortsword.json",
         "assets/typemoonworld/models/item/servant_card_iskandar.json",
         "assets/typemoonworld/textures/item/card_faces_3d/servant/iskandar_card.png",
         "assets/typemoonworld/models/item/servant_card_iskandar_chest.json",
         "assets/typemoonworld/models/item/servant_card_iskandar_legs.json",
         "assets/typemoonworld/models/item/servant_card_iskandar_feet.json")) {
         assertTrue(Files.exists(RESOURCES.resolve(file)), file);
      }
      String swordModel = Files.readString(RESOURCES.resolve("assets/typemoonworld/models/item/iskandar_shortsword.json"));
      String swordGeoModel = Files.readString(JAVA.resolve("client/model/IskandarShortswordModel.java"));
      String swordRenderer = Files.readString(JAVA.resolve("client/renderer/IskandarShortswordRenderer.java"));
      assertTrue(swordModel.contains("\"parent\": \"builtin/entity\""));
      assertFalse(swordModel.contains("minecraft:item/generated"));
      assertTrue(swordGeoModel.contains("geo/iskandar_shortsword.geo.json"));
      assertTrue(swordGeoModel.contains("textures/item/iskandar_shortsword.png"));
      assertTrue(swordRenderer.contains("extends GeoItemRenderer<IskandarShortswordItem>"));
   }

   @Test
   void iskandarServantCardResourcesAndSkillDispatchArePresent() throws Exception {
      String registry = Files.readString(JAVA.resolve("servant/card/ServantCardRegistry.java"));
      String manager = Files.readString(JAVA.resolve("servant/card/ServantCardTransformManager.java"));
      String layout = Files.readString(JAVA.resolve("servant/card/ServantCardSkillLayout.java"));
      String loadout = Files.readString(JAVA.resolve("servant/card/ServantCardLoadoutManager.java"));
      String skills = Files.readString(JAVA.resolve("servant/card/ServantCardIskandarSkills.java"));
      String zh = Files.readString(RESOURCES.resolve("assets/typemoonworld/lang/zh_cn.json"));
      String en = Files.readString(RESOURCES.resolve("assets/typemoonworld/lang/en_us.json"));
      String model = Files.readString(RESOURCES.resolve("assets/typemoonworld/models/item/servant_card_iskandar.json"));
      BufferedImage card = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/item/card_faces_3d/servant/iskandar_card.png").toFile());

      assertTrue(registry.contains("new Entry(\"iskandar\""));
      assertTrue(manager.contains("ServantCardIskandarSkills.initialize(player)"));
      assertTrue(manager.contains("ServantCardIskandarSkills.tick(player, vars)"));
      assertTrue(manager.contains("ServantCardIskandarSkills.clear(player)"));
      assertTrue(manager.contains("player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY)"));
      assertTrue(loadout.contains("case \"iskandar\" -> main = stack(ModItems.ISKANDAR_SHORTSWORD.get())"));
      assertTrue(model.contains("\"back\": \"typemoonworld:item/servant_card_backs/rider\""));
      assertEquals(292, card.getWidth());
      assertEquals(500, card.getHeight());
      assertTrue(layout.contains("case 7 -> new ServantCardSkillAction(\"Charge\", \"iskandar_charge\", 20.0, 40)"));
      for (String id : List.of("iskandar_bucephalus", "iskandar_royal_sword_assault",
         "iskandar_conqueror_order", "iskandar_thunder_call", "iskandar_battlefield_stride",
         "iskandar_vanguard_summon", "iskandar_kingly_war_cry", "iskandar_charge",
         "iskandar_gordius_wheel", "iskandar_ionioi_hetairoi")) {
         assertTrue(layout.contains(id), id);
         assertTrue(manager.contains("ServantCardIskandarSkills.perform"), id);
         assertTrue(zh.contains("skill.typemoonworld.servant_card." + id), id);
         assertTrue(en.contains("skill.typemoonworld.servant_card." + id), id);
      }
      assertTrue(skills.contains("TAG_BUCEPHALUS_HP"));
      assertTrue(skills.contains("TAG_GORDIUS_HP"));
      assertTrue(skills.contains("bindCardOwner(player)"));
      assertTrue(skills.contains("performCharge(level, player, 32.0F, 3.2)"));
      assertTrue(skills.contains("performCharge(level, player, 42.0F, 4.2)"));
      assertTrue(skills.contains("restoreStoredMountAfterIonioi"));
      assertTrue(skills.contains("spawnCardIonioiFormation"));
      assertTrue(skills.contains("CARD_IONIOI_ACTIVE_CAP = 400"));
      assertTrue(skills.contains("returnCardIonioiTargets"));
      assertTrue(skills.contains("initializeForServantCardIonioi"));
      assertTrue(skills.contains("TAG_IONIOI_DEATHS"));
      assertTrue(skills.contains("TAG_IONIOI_SEED"));
      assertTrue(skills.contains("TAG_IONIOI_SESSION"));
      assertTrue(skills.contains("TAG_IONIOI_TARGET_SESSION"));
      assertTrue(skills.contains("isPulledByCurrentCardIonioi"));
      assertTrue(skills.contains("onMacedonianSoldierDeath"));
      assertTrue(skills.contains("message.typemoonworld.servant_card.iskandar_ionioi_released"));
      assertFalse(skills.contains("message.typemoonworld.servant_card.ubw_released"));
      assertTrue(skills.contains("isIonioiActiveOrInside(ServerPlayer player)"));
      assertTrue(skills.contains("rescueIonioiEntity(movedPlayer, armyEntry)"));
      assertTrue(skills.contains("scheduleIonioiEntryRescue(movedPlayer, armyEntry)"));
      assertTrue(skills.contains("safeEntryUpward"));
      assertTrue(skills.contains("cleanupCardIonioiAfterExit"));
      assertTrue(skills.contains("MacedonianSoldierEntity.clearFormationCache(ownerId)"));
      assertTrue(skills.contains("player.teleportTo(returnLevel, returnPos.x, returnPos.y, returnPos.z"));
      assertFalse(skills.contains("player.changeDimension(new DimensionTransition(returnLevel"));
      assertFalse(skills.contains("prepareIonioiEntry"));
      assertFalse(skills.contains("Blocks.SANDSTONE.defaultBlockState()"));
      assertTrue(skills.contains("hasActiveGordiusWheel"));
      assertTrue(skills.contains("endIonioi(player, true)"));
      assertTrue(zh.contains("message.typemoonworld.servant_card.iskandar_ionioi_released"));
      assertTrue(en.contains("message.typemoonworld.servant_card.iskandar_ionioi_released"));
      assertFalse(skills.contains("ModEntities.ISKANDAR.get().create(source)"));
      assertFalse(skills.contains("TAG_IONIOI_PROXY_UUID"));
      assertTrue(skills.contains("IONIOI_UPKEEP_MP_PER_SECOND = 5.0"));
      assertTrue(manager.contains("\"iskandar_ionioi_hetairoi\".equals(action.effectId()) && ServantCardIskandarSkills.isIonioiActiveOrInside(player)"));
      assertTrue(manager.contains("\"iskandar_gordius_wheel\".equals(action.effectId()) && ServantCardIskandarSkills.hasActiveGordiusWheel(player)"));
   }

   @Test
   void soldiersRespectFormationDelayAndStandaloneSpawnsStayAlive() throws Exception {
      String soldier = Files.readString(JAVA.resolve("entity/MacedonianSoldierEntity.java"));
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      assertTrue(iskandar.contains("IONIOI_FORMATION_DELAY_TICKS = 2 * 20"));
      assertTrue(soldier.contains("actionStartTick"));
      assertTrue(soldier.contains("if (this.iskandarUuid == null)"));
      assertTrue(soldier.contains("level.getGameTime() < this.actionStartTick"));
      assertTrue(soldier.contains("IonioiHetairoiActionStartTick"));
   }

   @Test
   void gordiusWheelUsesStableRearBodyAnchorWithoutPassengerThrowingLag() throws Exception {
      String wheel = Files.readString(JAVA.resolve("entity/GordiusWheelEntity.java"));
      String mount = Files.readString(JAVA.resolve("entity/IskandarMountEntity.java"));
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      String model = Files.readString(JAVA.resolve("client/model/GordiusWheelModel.java"));
      assertTrue(wheel.contains("tickRearBodyPhysics"));
      assertTrue(wheel.contains("super.tick();\r\n      tickRearBodyPhysics();")
         || wheel.contains("super.tick();\n      tickRearBodyPhysics();"));
      assertTrue(wheel.contains("Vec3 desired = getIdealRearBodyAnchor();"));
      assertTrue(wheel.contains("return getIdealRearBodyAnchor();"));
      assertFalse(wheel.contains("lateralLag"));
      assertFalse(wheel.contains("verticalLag"));
      assertTrue(wheel.contains("getRearBodyAnchor"));
      assertTrue(wheel.contains("snapRearBodyToCurrentPosition"));
      assertTrue(wheel.contains("REAR_BODY_DISTANCE = 2.7"));
      assertTrue(wheel.contains("getRearSeatAnchor"));
      assertTrue(wheel.contains("positionChariotPassenger"));
      assertTrue(wheel.contains("getPassengerAttachmentPoint"));
      assertTrue(wheel.contains("CHARIOT_RIDER_FORWARD = 0.05"));
      assertTrue(wheel.contains("CHARIOT_RIDER_SIDE = 0.0"));
      assertTrue(wheel.contains("CHARIOT_PASSENGER_SIDE = 1.0"));
      assertTrue(wheel.contains("CHARIOT_RIDER_HEIGHT = 0.65"));
      assertTrue(wheel.contains("骑乘点"));
      assertTrue(wheel.contains("snapToNearbyGround"));
      assertTrue(wheel.contains("isNoGravity()"));
      assertTrue(wheel.contains("isFlyingMode()"));
      assertTrue(wheel.contains("setFlyingMode"));
      assertTrue(wheel.contains("DIVE_COOLDOWN_TICKS = 9 * 20"));
      assertTrue(wheel.contains("DIVE_ASCENT_TICKS = 18"));
      assertTrue(wheel.contains("DIVE_ATTACK_TICKS = 16"));
      assertTrue(wheel.contains("DIVE_DIRECT_DAMAGE = 120.0F"));
      assertTrue(wheel.contains("DIVE_IMPACT_DAMAGE = 90.0F"));
      assertTrue(wheel.contains("tickThunderStrike"));
      assertTrue(wheel.contains("THUNDER_STRIKE_RADIUS = 18.0"));
      assertTrue(wheel.contains("EntityType.LIGHTNING_BOLT.create(level)"));
      assertTrue(wheel.contains("lightning.setVisualOnly(true)"));
      assertTrue(wheel.contains("emitZeusLightningAura"));
      assertTrue(wheel.contains("emitZeusThunderStrike"));
      assertTrue(wheel.contains("ParticleTypes.FLASH"));
      assertTrue(wheel.contains("ParticleTypes.CLOUD"));
      assertTrue(wheel.contains("SoundEvents.LIGHTNING_BOLT_IMPACT"));
      assertTrue(wheel.contains("THUNDER_STRIKE_MIN_DELAY = 24"));
      assertTrue(wheel.contains("THUNDER_STRIKE_RANDOM_DELAY = 36"));
      assertTrue(wheel.contains("LIGHTNING_AURA_DAMAGE = 16.0F"));
      assertTrue(wheel.contains("THUNDER_STRIKE_DAMAGE = 72.0F"));
      assertTrue(wheel.contains("createMountAttributes(4000.0, 0.62)"));
      assertTrue(wheel.contains("damagingStrikeCount = target == null ? 0 : 2"));
      assertTrue(wheel.contains("THUNDER_STRIKE_TERRAIN_PROFILE"));
      assertTrue(wheel.contains("TerrainImpactService.impact(level, this, base.add(0.0, 0.15, 0.0), THUNDER_STRIKE_TERRAIN_PROFILE"));
      assertTrue(wheel.contains("THUNDER_ROAR_DAMAGE = 20.0F"));
      assertTrue(wheel.contains("THUNDER_ROAR_COOLDOWN_TICKS = 16 * 20"));
      assertTrue(wheel.contains("EntityUtils.isImmunePlayerTarget(entity)"));
      assertTrue(wheel.contains("zeusDamageSource"));
      assertTrue(wheel.contains("this.damageSources().mobAttack(iskandar)"));
      assertTrue(wheel.contains("damagingStrike"));
      assertTrue(wheel.contains("randomThunderStrikePosition(level, target)"));
      assertTrue(wheel.contains("spawnVisualThunderbolt(level, arcPos)"));
      assertTrue(wheel.contains("this.isCharging()"));
      assertTrue(wheel.contains("breakGordiusChargeTerrain"));
      assertTrue(wheel.contains("breakGordiusImpactTerrain"));
      assertTrue(wheel.contains("owner.isSprinting()"));
      assertTrue(wheel.contains("canBreakTerrainFor(source) && !isFlyingMode()"));
      assertTrue(wheel.contains("return !(source instanceof ServerPlayer);"));
      assertTrue(wheel.contains("isCloseEnoughForTerrainBreak(level)"));
      assertTrue(wheel.contains("FRONT_TERRAIN_ANCHOR_DISTANCE = 3.4"));
      assertTrue(wheel.contains("getFrontTerrainAnchor"));
      assertTrue(wheel.contains("Vec3 frontAnchor = getFrontTerrainAnchor(forward);"));
      assertTrue(wheel.contains("frontAnchor.add(forward.scale(0.7))"));
      assertTrue(wheel.contains("(this.tickCount % CHARGE_TERRAIN_SERVICE_INTERVAL == 0 || this.horizontalCollision)"));
      assertTrue(wheel.contains("CHARGE_TERRAIN_MAX_BLOCKS = 36"));
      assertTrue(wheel.contains("CHARGE_TERRAIN_SERVICE_INTERVAL = 4"));
      assertTrue(wheel.contains("TerrainImpactService.impactForwardBreakthrough"));
      assertTrue(wheel.contains("CHARGE_TERRAIN_PROFILE"));
      assertTrue(wheel.contains("IMPACT_TERRAIN_PROFILE"));
      assertTrue(wheel.contains("TerrainImpactService.Shape.SURFACE_HEMISPHERE"));
      assertTrue(wheel.contains("never dig below the grounded chariot's current footing"));
      assertTrue(wheel.contains("Config.terrainDestructionEnabled"));
      assertTrue(wheel.contains("ModTags.Blocks.TERRAIN_IMMUNE"));
      assertTrue(wheel.contains("isProtectedTerrain"));
      assertTrue(mount.contains("getSeatSideOffset(passengerSeat)"));
      assertTrue(mount.contains("Math.cos(yaw) * localX - Math.sin(yaw) * localZ"));
      assertFalse(iskandar.contains(".add(0.0, 1.2, 0.0)"));
      assertTrue(wheel.contains("GordiusRearX"));
      assertTrue(wheel.contains("GordiusFrontX"));
      assertTrue(wheel.contains("prevRearX"));
      assertTrue(wheel.contains("getIdealRearBodyAnchor"));
      assertTrue(wheel.contains("position().subtract(horizontalForward().scale(REAR_BODY_DISTANCE))"));
      assertTrue(model.contains("setCustomAnimations"));
      assertTrue(model.contains("MODEL_GROUND_LIFT = 8.0F"));
      assertTrue(model.contains("FRONT_SCALE = 1.5F"));
      assertTrue(model.contains("REAR_SCALE = 2.0F"));
      assertTrue(model.contains("FRONT_SCALE_GROUND_OFFSET = -4.0F"));
      assertTrue(model.contains("REAR_SCALE_GROUND_OFFSET = -8.0F"));
      assertTrue(model.contains("REAR_RENDER_MAX_HORIZONTAL_OFFSET = 24.0"));
      assertTrue(model.contains("firstBone(\"前端\", \"鍓嶇\")"));
      assertTrue(model.contains("firstBone(\"后端\", \"鍚庣\")"));
      assertTrue(model.contains("front.setScaleX(FRONT_SCALE)"));
      assertTrue(model.contains("rear.setScaleX(REAR_SCALE)"));
      assertTrue(model.contains("front.setPosY(FRONT_SCALE_GROUND_OFFSET)"));
      assertTrue(model.contains("rear.setPosY(REAR_SCALE_GROUND_OFFSET + (float)offset.y)"));
      assertTrue(model.contains("return Vec3.ZERO;"));
      assertFalse(model.contains("Mth.clamp(localX * 16.0"));
      assertTrue(model.contains("rearRenderOffset"));
      assertTrue(model.contains("Left front leg"));
      assertTrue(model.contains("Left_front_leg2"));
      assertTrue(model.contains("animateWheel(\"车轮\", wheelRot)"));
      assertTrue(model.contains("animateWheel(\"杞﹁疆\", wheelRot)"));
      String animation = Files.readString(RESOURCES.resolve("assets/typemoonworld/animations/gordius_wheel.animation.json"));
      assertTrue(animation.contains("\"vector\": [1.5, 1.5, 1.5]"));
      assertTrue(animation.contains("\"vector\": [2.0, 2.0, 2.0]"));
      assertTrue(animation.contains("\"move\""));
      assertTrue(animation.contains("\"charge\""));
      assertTrue(animation.contains("\"车轮\""));
      assertTrue(animation.contains("\"牛1\""));
      assertTrue(animation.contains("\"牛2\""));
   }

   @Test
   void mountLifecycleAndDedicatedAiMatchIskandarDesign() throws Exception {
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      String ai = Files.readString(JAVA.resolve("servant/entity/IskandarCombatHelper.java"));
      String mount = Files.readString(JAVA.resolve("entity/IskandarMountEntity.java"));
      String gordius = Files.readString(JAVA.resolve("entity/GordiusWheelEntity.java"));
      assertFalse(iskandar.contains("ensurePersistentMounts"));
      assertTrue(iskandar.contains("summonBucephalusAndRide"));
      assertTrue(iskandar.contains("summonGordiusWheelAndRide"));
      assertTrue(iskandar.contains("notifyGordiusWheelNoblePhantasm"));
      assertTrue(iskandar.contains("ServantCombatSystem.broadcastNoblePhantasmWindup(this, target, 20, false)"));
      assertTrue(iskandar.contains("ServantCombatSystem.forcePhaseAtLeast(responder, ServantCombatPhase.DECISIVE)"));
      assertTrue(iskandar.contains("discardBucephalus"));
      assertTrue(iskandar.contains("discardGordiusWheel"));
      assertTrue(iskandar.contains("TAG_GORDIUS_WHEEL_SUMMONED_ONCE"));
      assertTrue(iskandar.contains("TAG_GORDIUS_WHEEL_DESTROYED"));
      assertTrue(iskandar.contains("TAG_BUCEPHALUS_SUMMONED_ONCE"));
      assertTrue(iskandar.contains("TAG_BUCEPHALUS_READY_AFTER_WHEEL"));
      assertTrue(iskandar.contains("canSummonGordiusWheel"));
      assertTrue(iskandar.contains("!isGordiusWheelDestroyed()"));
      assertTrue(iskandar.contains("getGordiusWheel(level) == null"));
      assertTrue(iskandar.contains("onGordiusWheelDestroyed"));
      assertTrue(iskandar.contains("!this.getPersistentData().getBoolean(TAG_BUCEPHALUS_SUMMONED_ONCE)"));
      assertTrue(iskandar.contains("this.getPersistentData().putBoolean(TAG_BUCEPHALUS_SUMMONED_ONCE, true)"));
      assertFalse(iskandar.contains("TAG_BUCEPHALUS_SUMMON_COOLDOWN"));
      assertFalse(iskandar.contains("markBucephalusLost"));
      assertTrue(iskandar.contains("discardBucephalus(level, false)"));
      assertTrue(iskandar.contains("getPersistentData().putBoolean(TAG_BUCEPHALUS_READY_AFTER_WHEEL, true)"));
      assertTrue(iskandar.contains("IskandarCombatHelper.tick"));
      assertTrue(ai.contains("HIGH_HP_PHASE"));
      assertTrue(ai.contains("MID_HP_PHASE"));
      assertTrue(ai.contains("LOW_HP_PHASE"));
      assertTrue(ai.contains("shouldUseGordiusWheel"));
      assertTrue(ai.contains("tickFallbackMount"));
      assertFalse(ai.contains("dismountForWalking"));
      assertTrue(ai.contains("keepRidingBucephalus"));
      assertTrue(ai.contains("keepRidingGordiusWheel"));
      assertTrue(ai.contains("entity.isGordiusWheelDestroyed()"));
      assertTrue(ai.contains("if (!entity.isGordiusWheelDestroyed())"));
      assertTrue(gordius.contains("public void die(DamageSource cause)"));
      assertTrue(gordius.contains("iskandar.onGordiusWheelDestroyed(this)"));
      assertFalse(mount.contains("followOwnerWhenEmpty"));
      assertTrue(mount.contains("iskandar.getVehicle() != this"));
      assertTrue(mount.contains("this.discard()"));
      assertTrue(mount.contains("moveAroundTarget"));
      assertTrue(mount.contains("getCombatOrbitRadius"));
      assertTrue(mount.contains("tickCharge"));
      assertTrue(mount.contains("chargeHitTargets"));
      assertTrue(mount.contains("getMovingAnimation"));
      assertTrue(mount.contains("getChargeAnimation"));
      assertTrue(mount.contains("this.isCharging()"));
      assertTrue(mount.contains("entityData.get(MOVING) ? getMovingAnimation() : getLoopAnimation()"));
      assertTrue(mount.contains("right.scale(-strafeInput)"));
      assertTrue(iskandar.contains("this.getVehicle() instanceof IskandarMountEntity"));
      assertTrue(iskandar.contains("IONIOI_MP_COST = 60.0"));
      assertTrue(iskandar.contains("WHEEL_CHARGE_COOLDOWN = 5 * 20"));
      assertTrue(iskandar.contains("HORSE_CHARGE_COOLDOWN = 7 * 20"));
      assertTrue(iskandar.contains("tickKinglyWarCry"));
      assertTrue(iskandar.contains("mount.performCharge(level, this, 32.0F, 2.1)"));
      assertTrue(iskandar.contains("mount.performCharge(level, this, 42.0F, 3.0)"));
      assertTrue(iskandar.contains("distanceSqr >= 6.0 * 6.0"));
      String horse = Files.readString(JAVA.resolve("entity/BucephalusEntity.java"));
      String horseModel = Files.readString(JAVA.resolve("client/model/BucephalusModel.java"));
      assertTrue(horse.contains("return \"walk\""));
      assertTrue(horse.contains("return \"gallop\""));
      assertTrue(horse.contains("createMountAttributes(2000.0, 0.48)"));
      assertTrue(horse.contains("protected void followCardOwnerInput(ServerPlayer owner)"));
      assertTrue(horse.contains("float forwardInput = owner.zza"));
      assertTrue(horse.contains("float yaw = Mth.rotLerp(0.32F, this.getYRot(), owner.getYRot())"));
      assertTrue(horse.contains("this.setYRot(yaw)"));
      assertTrue(horse.contains("right.scale(-strafeInput)"));
      assertFalse(horse.contains("typemoonworld$isJumping"));
      assertFalse(horse.contains("isFlyingMode"));
      assertTrue(horseModel.contains("setCustomAnimations"));
      assertTrue(horseModel.contains("MODEL_GROUND_LIFT = 0.0F"));
      assertTrue(horseModel.contains("Left front leg"));
      String horseAnimation = Files.readString(RESOURCES.resolve("assets/typemoonworld/animations/bucephalus.animation.json"));
      assertTrue(horseAnimation.contains("\"walk\""));
      assertTrue(horseAnimation.contains("\"gallop\""));
      assertTrue(horseAnimation.contains("\"Left front leg\""));
      assertTrue(horseAnimation.contains("\"Right front leg\""));
      assertTrue(horseAnimation.contains("\"Left hind leg\""));
      assertTrue(horseAnimation.contains("\"Right hind leg\""));
   }

   @Test
   void iskandarStandsOnGordiusWheelInsteadOfUsingHumanoidRidingPose() throws Exception {
      String renderer = Files.readString(JAVA.resolve("client/renderer/HumanoidServantRenderer.java"));
      assertTrue(renderer.contains("new ServantPlayerModel<>"));
      assertTrue(renderer.contains("entity instanceof IskandarEntity && entity.getVehicle() instanceof GordiusWheelEntity"));
      assertTrue(renderer.contains("this.riding = false"));
   }

   @Test
   void ionioiHetairoiPullsTargetsImmediatelyWithWiderRange() throws Exception {
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      String ai = Files.readString(JAVA.resolve("servant/entity/IskandarCombatHelper.java"));
      assertTrue(iskandar.contains("IONIOI_PULL_RADIUS = 64.0"));
      assertTrue(iskandar.contains("IONIOI_TARGET_OFFSET_CLAMP = 48.0"));
      assertTrue(iskandar.contains("IONIOI_ARMY_ENTRY_DISTANCE = 20.0"));
      assertTrue(iskandar.contains("IONIOI_TARGET_REFRESH_TICKS = 20"));
      assertTrue(iskandar.contains("IONIOI_SOLDIER_PRUNE_TICKS = 20"));
      assertTrue(iskandar.contains("IONIOI_ACTIVE_CAP = 400"));
      assertTrue(iskandar.contains("IONIOI_MAX_REPLENISH_PER_TICK = IONIOI_FORMATION_SIZE"));
      assertTrue(iskandar.contains("formation = i / IONIOI_FORMATION_SIZE"));
      assertTrue(iskandar.contains("formation % 8 - 3.5"));
      assertTrue(iskandar.contains("formation / 8 - 0.5"));
      assertTrue(iskandar.contains("groupX"));
      assertTrue(iskandar.contains("groupZ"));
      assertTrue(iskandar.contains("List<LivingEntity> pulled = collectIonioiTargets(level, primary);"));
      assertTrue(iskandar.contains("Vec3 enemyEntry = safeIonioiEntry(ionioiLevel, randomIonioiEntry(), primary.getBbWidth(), primary.getBbHeight());"));
      assertTrue(iskandar.contains("Vec3 armyEntry = safeIonioiEntry"));
      assertTrue(iskandar.contains("LivingEntity movedPrimary = moveIonioiTargets(level, ionioiLevel, pulled, primary, primary.position(), enemyEntry, session);"));
      assertTrue(iskandar.contains("int lifetimeDeaths = Mth.clamp(data.getInt(TAG_IONIOI_DEATHS), 0, IonioiHetairoiRankPool.TOTAL_SIZE);"));
      assertTrue(iskandar.contains("data.contains(TAG_IONIOI_SEED) ? data.getLong(TAG_IONIOI_SEED) : this.getRandom().nextLong()"));
      assertTrue(iskandar.contains("data.putInt(TAG_IONIOI_NEXT_INDEX, lifetimeDeaths)"));
      assertTrue(iskandar.contains("TAG_IONIOI_TARGET_COUNT"));
      assertTrue(iskandar.contains("rememberIonioiTarget(movedLiving.getUUID())"));
      assertTrue(iskandar.contains("this.ionioiTargets.clear();"));
      assertTrue(iskandar.contains("private static Vec3 safeIonioiEntry(ServerLevel level, Vec3 pos, float width, float height)"));
      assertTrue(iskandar.contains("rescueIonioiEntity(iskandar, armyEntry)"));
      assertTrue(iskandar.contains("scheduleIonioiEntryRescue(iskandar, armyEntry)"));
      assertTrue(iskandar.contains("safeIonioiColumn(level, pos.x, pos.y, pos.z, width, height)"));
      assertFalse(iskandar.contains("prepareIonioiEntry"));
      assertFalse(iskandar.contains("Blocks.SANDSTONE.defaultBlockState()"));
      assertTrue(iskandar.contains("level.noCollision(box)"));
      assertTrue(iskandar.contains("this.getBoundingBox().inflate(IONIOI_PULL_RADIUS)"));
      assertTrue(iskandar.contains("resolveIonioiPrimaryTarget"));
      assertTrue(iskandar.contains("UBWInstanceManager.randomEntryPosition(this.getRandom())"));
      assertTrue(iskandar.contains("refreshIonioiCombatTarget(level)"));
      assertTrue(iskandar.contains("isHostileIonioiTarget"));
      assertTrue(iskandar.contains("TAG_IONIOI_SESSION"));
      assertTrue(iskandar.contains("TAG_IONIOI_TARGET_SESSION"));
      assertTrue(iskandar.contains("UUID session = UUID.randomUUID()"));
      assertTrue(iskandar.contains("isPulledByCurrentIonioi(this.getUUID(), ionioiSession(this.getPersistentData()), living)"));
      assertTrue(iskandar.contains("data.putUUID(TAG_IONIOI_TARGET_SESSION, session)"));
      assertTrue(iskandar.contains("this.getLastHurtByMob()"));
      assertTrue(iskandar.contains("!EntityUtils.isSpectatorPlayer(living)"));
      assertTrue(iskandar.contains("summonIonioiBucephalusAndRide"));
      assertTrue(iskandar.contains("batchSize = Math.min(IONIOI_MAX_REPLENISH_PER_TICK, missing)"));
      assertTrue(iskandar.contains("level.getGameTime() % IONIOI_SOLDIER_PRUNE_TICKS"));
      assertTrue(iskandar.contains("MacedonianSoldierEntity.clearFormationCache(this.getUUID())"));
      assertTrue(iskandar.contains("double distanceSqr = mount.distanceToSqr(target);"));
      assertTrue(iskandar.contains("private static double attributeValue"));
      assertFalse(iskandar.contains("!EntityUtils.isImmunePlayerTarget(living)"));
      assertTrue(ai.contains("recentIonioiAggressor"));
      assertTrue(ai.contains("entity.tickCount - entity.getLastHurtByMobTimestamp() > 200"));
      assertTrue(ai.contains("if (!entity.isGordiusWheelDestroyed())"));
      assertTrue(ai.contains("ridingBucephalus"));
      assertTrue(ai.contains("IONIOI_LOW_PHASE_WARMUP_TICKS = 8 * 20"));
      assertTrue(ai.contains("ionioiWarmupReady"));
      assertTrue(ai.contains("resetIonioiWarmup"));
      assertFalse(ai.contains("if (target == null || !target.isAlive()) {\r\n         dismountForWalking(entity);")
         || ai.contains("if (target == null || !target.isAlive()) {\n         dismountForWalking(entity);"));
      assertFalse(ai.contains("hasNearbyIonioiPlayerPullTarget"));
      assertTrue(ai.contains("phase == LOW_HP_PHASE"));
      assertTrue(ai.contains("hasSummonedFallbackMount(entity)"));
      assertTrue(ai.contains("fallbackLastStand"));
      assertTrue(ai.contains("ridingBucephalus\r\n         && phase == LOW_HP_PHASE")
         || ai.contains("ridingBucephalus\n         && phase == LOW_HP_PHASE"));
      assertFalse(ai.contains("entity.getCurrentMp() >= IskandarEntity.IONIOI_MP_COST"));
      assertFalse(ai.contains("hasNearbyIonioiPullTarget"));
   }

   @Test
   void bucephalusCanRecoverCombatMovementAfterMountSwitch() throws Exception {
      String mount = Files.readString(JAVA.resolve("entity/IskandarMountEntity.java"));
      String wheel = Files.readString(JAVA.resolve("entity/GordiusWheelEntity.java"));
      assertTrue(mount.contains("followIskandarCombatIntent(ServerLevel level, IskandarEntity iskandar)"));
      assertTrue(mount.contains("resolveCombatTarget"));
      assertTrue(mount.contains("iskandar.getLastHurtByMob()"));
      assertTrue(mount.contains("mob.getTarget() == iskandar || mob.getTarget() == this"));
      assertTrue(mount.contains("moveAroundTarget(target)"));
      assertTrue(mount.contains("isHostileIonioiTarget(target)"));
      assertTrue(mount.contains("recoverBlockedMountedMove"));
      assertTrue(mount.contains("blockedMoveTicks"));
      assertTrue(mount.contains("this.horizontalCollision"));
      assertTrue(mount.contains("this.isInWall()"));
      assertTrue(wheel.contains("super.followIskandarCombatIntent(level, iskandar)"));
      assertTrue(wheel.contains("recoverBlockedMountedMove(before, unit)"));
   }

   @Test
   void genericAiRedirectsMountedCombatTargetsToTheirLivingMounts() throws Exception {
      String utils = Files.readString(JAVA.resolve("utils/EntityUtils.java"));
      String servant = Files.readString(JAVA.resolve("servant/entity/ServantEntity.java"));
      String tactical = Files.readString(JAVA.resolve("combat/ai/NpcTacticalController.java"));
      assertTrue(utils.contains("redirectMountedCombatTarget"));
      assertTrue(utils.contains("target.getVehicle() instanceof LivingEntity mount"));
      assertTrue(utils.contains("attacker.isAlliedTo(mount) || mount.isAlliedTo(attacker)"));
      assertTrue(utils.contains("isImmunePlayerTarget(mount)"));
      assertTrue(servant.contains("EntityUtils.redirectMountedCombatTarget(this, target)"));
      assertTrue(servant.contains("this.doHurtTarget(resolvedTarget)"));
      assertTrue(tactical.contains("EntityUtils.redirectMountedCombatTarget(entity, currentTarget)"));
      assertTrue(tactical.contains("entity.setTarget(target);"));
   }

   @Test
   void macedonianGearIsOrdinaryAndSpearHasLongReach() throws Exception {
      String items = Files.readString(JAVA.resolve("item/ModItems.java"));
      assertTrue(items.contains("new MacedonianSpearItem(new Item.Properties().stacksTo(1)"));
      assertTrue(items.contains("new MacedonianRoundShieldItem(new Item.Properties().stacksTo(1))"));
      assertTrue(items.contains("\"macedonian_spear_range\""));
      assertTrue(items.contains("3.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE"));
   }

   @Test
   void macedonianSoldiersUseSteveModelWithPhalanxGearAndRandomScale() throws Exception {
      String renderer = Files.readString(JAVA.resolve("client/renderer/MacedonianSoldierRenderer.java"));
      String soldier = Files.readString(JAVA.resolve("entity/MacedonianSoldierEntity.java"));
      assertTrue(renderer.contains("extends HumanoidMobRenderer<MacedonianSoldierEntity, PlayerModel<MacedonianSoldierEntity>>"));
      assertTrue(renderer.contains("ModelLayers.PLAYER"));
      assertTrue(renderer.contains("textures/entity/macedonian_soldier.png"));
      assertTrue(renderer.contains("new ItemInHandLayer"));
      assertFalse(renderer.contains("GeoEntityRenderer"));
      assertFalse(renderer.contains("MacedonianSoldierModel"));
      assertFalse(soldier.contains("implements GeoEntity"));
      assertFalse(Files.exists(JAVA.resolve("client/model/MacedonianSoldierModel.java")));
      assertFalse(Files.exists(RESOURCES.resolve("assets/typemoonworld/geo/macedonian_soldier.geo.json")));
      assertFalse(Files.exists(RESOURCES.resolve("assets/typemoonworld/animations/macedonian_soldier.animation.json")));
      assertTrue(soldier.contains("EquipmentSlot.MAINHAND, new ItemStack(ModItems.MACEDONIAN_SPEAR.get())"));
      assertTrue(soldier.contains("EquipmentSlot.OFFHAND, new ItemStack(ModItems.MACEDONIAN_ROUND_SHIELD.get())"));
      assertTrue(soldier.contains("finalizeSpawn"));
      assertTrue(soldier.contains("this.equipPhalanxGear();"));
      assertTrue(soldier.contains("VISUAL_SCALE"));
      assertTrue(soldier.contains("0.9F + this.getRandom().nextFloat() * 0.1F"));
      assertTrue(soldier.contains("MacedonianSoldierVisualScale"));
   }

   @Test
   void macedonianSoldiersHoldFormationUntilSurroundingEnemy() throws Exception {
      String soldier = Files.readString(JAVA.resolve("entity/MacedonianSoldierEntity.java"));
      assertTrue(soldier.contains("FORMATION_ACTIVE_CAP = 400"));
      assertTrue(soldier.contains("FORMATION_SIZE = 25"));
      assertTrue(soldier.contains("FORMATION_SIDE = 5"));
      assertTrue(soldier.contains("FORMATION_GRID_WIDTH = 8"));
      assertTrue(soldier.contains("FORMATION_GRID_DEPTH = 2"));
      assertTrue(soldier.contains("FORMATION_THREAT_SCAN_RADIUS = 96.0"));
      assertTrue(soldier.contains("SPEAR_REACH_BONUS = 3.0"));
      assertTrue(soldier.contains("SPEAR_ATTACK_INTERVAL_TICKS = 12"));
      assertTrue(soldier.contains("Attributes.ATTACK_SPEED, 4.8"));
      assertTrue(soldier.contains("new MacedonianSpearAttackGoal(this, 1.18, true)"));
      assertTrue(soldier.contains("spearAttackCooldown"));
      assertTrue(soldier.contains("checkAndPerformAttack"));
      assertTrue(soldier.contains("canPerformAttack"));
      assertTrue(soldier.contains("tryDirectSpearAttack(target)"));
      assertTrue(soldier.contains("isFormationVolleyWindow()"));
      assertTrue(soldier.contains("shouldCheckPersonalSpearThisTick()"));
      assertTrue(soldier.contains("canSpearReach(target)"));
      assertTrue(soldier.contains("facePosition(target.position())"));
      assertTrue(soldier.contains("emitSpearThrustFeedback(target)"));
      assertTrue(soldier.contains("ParticleTypes.SWEEP_ATTACK"));
      assertTrue(soldier.contains("SoundEvents.PLAYER_ATTACK_SWEEP"));
      assertTrue(soldier.contains("formationSurroundSlot(owner, target)"));
      assertTrue(soldier.contains("formationTarget(owner, level)"));
      assertTrue(soldier.contains("isAssignedIonioiTarget"));
      assertTrue(soldier.contains("directNpcArmyThreat(owner)"));
      assertTrue(soldier.contains("directCardArmyThreat(owner)"));
      assertTrue(soldier.contains("isThreateningNpcArmy(owner, candidate)"));
      assertTrue(soldier.contains("isThreateningCardArmy(owner, candidate)"));
      assertTrue(soldier.contains("owner.getLastHurtMob()"));
      assertTrue(soldier.contains("owner.getLastHurtByMob()"));
      assertTrue(soldier.contains("candidate instanceof Mob mob && isFriendly(mob.getTarget())"));
      assertTrue(soldier.contains("isFriendly(candidate.getLastHurtMob())"));
      assertTrue(soldier.contains("candidate instanceof Monster"));
      assertTrue(soldier.contains("soldier.isAssignedIonioiTarget(owner, candidate)\n               || soldier.isThreateningNpcArmy(owner, candidate)"));
      assertTrue(soldier.contains("soldier.isAssignedCardIonioiTarget(owner, candidate)\n               || soldier.isThreateningCardArmy(owner, candidate)"));
      assertTrue(soldier.contains("threatTargets.add(target.getUUID())"));
      assertTrue(soldier.contains("this.targets.addAll(threatTargets)"));
      assertTrue(soldier.contains("moveInFormation(owner, null)"));
      assertTrue(soldier.contains("moveInCardFormation(owner, null)"));
      assertTrue(soldier.contains("formationNumber()"));
      assertTrue(soldier.contains("level.getGameTime() / 80L"));
      assertTrue(soldier.contains("moveDirectlyInFormation(slot, FORMATION_ASSAULT_SPEED, true)"));
      assertTrue(soldier.contains("formationSlot(owner, target)"));
      assertTrue(soldier.contains("this.setTarget(null);"));
      assertTrue(soldier.contains("double angle = Math.PI * 2.0 * formation"));
      assertTrue(soldier.contains("double radius = 3.0 + row * 0.8"));
      assertTrue(soldier.contains("FORMATION_VOLLEY_INTERVAL_TICKS = 18"));
      assertTrue(soldier.contains("FORMATION_VOLLEY_WINDOW_TICKS = 8"));
      assertTrue(soldier.contains("PERSONAL_SPEAR_CHECK_STRIDE = 3"));
      assertTrue(soldier.contains("SPEAR_FORWARD_DOT_MIN = 0.38"));
      assertTrue(soldier.contains("SPEAR_LANE_HALF_WIDTH = 1.55"));
      assertTrue(soldier.contains("formation = formationIndex / FORMATION_SIZE"));
      assertTrue(soldier.contains("localX"));
      assertTrue(soldier.contains("localZ"));
      assertTrue(soldier.contains("facePosition"));
   }

   @Test
   void macedonianSoldiersUseLightweightFormationAiAndServantGradeGuard() throws Exception {
      String soldier = Files.readString(JAVA.resolve("entity/MacedonianSoldierEntity.java"));
      String renderer = Files.readString(JAVA.resolve("client/renderer/MacedonianSoldierRenderer.java"));
      assertTrue(soldier.contains("FORMATION_TARGET_SCAN_INTERVAL = 30"));
      assertTrue(soldier.contains("FORMATION_BRAIN_STRIDE = 4"));
      assertTrue(soldier.contains("FORMATION_TARGET_CACHE"));
      assertTrue(soldier.contains("CARD_FORMATION_TARGET_CACHE"));
      assertTrue(soldier.contains("FORMATION_Y_CACHE"));
      assertTrue(soldier.contains("FormationTargetCache"));
      assertTrue(soldier.contains("clearFormationCache(UUID ownerId)"));
      assertTrue(soldier.contains("CARD_FORMATION_TARGET_CACHE.remove(ownerId)"));
      assertTrue(soldier.contains("if (this.iskandarUuid == null)"));
      assertTrue(soldier.contains("boolean tactical = net.xxxjk.TYPE_MOON_WORLD.combat.ai.NpcTacticalController.tick(this)"));
      assertTrue(soldier.contains("shouldRunFormationBrain()"));
      assertFalse(soldier.contains("NearestAttackableTargetGoal"));
      assertFalse(soldier.contains("WaterAvoidingRandomStrollGoal"));
      assertFalse(soldier.contains("RandomLookAroundGoal"));
      assertTrue(soldier.contains("moveDirectlyInFormation"));
      assertTrue(soldier.contains("formationSurroundSlot(owner, target)"));
      assertTrue(soldier.contains("formationSurroundSlot(LivingEntity owner, LivingEntity target)"));
      assertTrue(soldier.contains("FORMATION_DIRECT_PATH_REFRESH_TICKS = 12"));
      assertTrue(soldier.contains("FORMATION_IDLE_PATH_REFRESH_TICKS = 80"));
      assertTrue(soldier.contains("cachedFormationY(level"));
      assertTrue(soldier.contains("CachedFormationY"));
      assertTrue(soldier.contains("MOTION_BLOCKING_NO_LEAVES"));
      assertTrue(soldier.contains("cache.refreshCard(owner, level, this)"));
      assertTrue(soldier.contains("targetForCardFormation(owner, level, formationNumber())"));
      assertTrue(soldier.contains("fallbackCardTarget(owner, level)"));
      assertFalse(soldier.contains("findCardThreatNear(ServerPlayer owner)"));
      assertFalse(soldier.contains("refreshPathTo("));
      assertTrue(soldier.contains("getMoveControl().setWantedPosition"));
      assertTrue(soldier.contains("FORMATION_DIRECT_MOVE_STOP_DISTANCE_SQR"));
      assertTrue(soldier.contains("FORMATION_KEEP_DISTANCE = 6.4"));
      assertTrue(soldier.contains("MIN_IONIOI_MOVEMENT_SPEED = StatRank.C.toMovementSpeed()"));
      assertTrue(soldier.contains("Math.max(MIN_IONIOI_MOVEMENT_SPEED, params.movementSpeed())"));
      assertTrue(soldier.contains("FORMATION_ASSAULT_SPEED = 1.45"));
      assertTrue(soldier.contains("EMBEDDED_RECOVERY_INTERVAL = 20"));
      assertTrue(soldier.contains("recoverIfEmbedded(level)"));
      assertTrue(soldier.contains("this.isInWall()"));
      assertTrue(soldier.contains("findSafeSpawnY(level"));
      assertTrue(soldier.contains("isMarkedIonioiTarget"));
      assertTrue(soldier.contains("moveDirectlyInFormation(slot, FORMATION_ASSAULT_SPEED, true)"));
      assertTrue(soldier.contains("tryDirectSpearAttack(target)"));
      assertTrue(soldier.contains("directSpearCooldown"));
      assertTrue(soldier.contains("this.directSpearCooldown = SPEAR_ATTACK_INTERVAL_TICKS"));
      assertTrue(soldier.contains("DamageTypeTags.IS_FALL"));
      assertTrue(soldier.contains("applyMacedonianGuard"));
      assertTrue(soldier.contains("ServantCombatFormulas.blockReduction(params)"));
      assertTrue(soldier.contains("rank == StatRank.A || rank == StatRank.B ? normalReduction : normalReduction * 0.5"));
      assertTrue(soldier.contains("DamageTypeTags.BYPASSES_SHIELD"));
      assertTrue(soldier.contains("SoundEvents.SHIELD_BLOCK"));
      assertTrue(soldier.contains("SERVANT_DISSOLVE_TICKS = 10"));
      assertTrue(soldier.contains("tickDeath"));
      assertTrue(soldier.contains("isShortServantDissolving"));
      assertTrue(soldier.contains("getShortServantDissolveProgress"));
      assertTrue(renderer.contains("ClippedEntityRenderType.ClipMode.DISSOLVE"));
      assertTrue(renderer.contains("FadingBufferSource"));
      assertTrue(renderer.contains("FadingVertexConsumer"));
      assertTrue(renderer.contains("getShortServantDissolveProgress(this.renderPartialTick)"));
      assertTrue(renderer.contains("getFlipDegrees"));
      assertTrue(renderer.contains("return livingEntity.isShortServantDissolving() ? 0.0F"));
   }

   @Test
   void iskandarCorrectionsCoverNamesProtectionProjectilesAndExcaliburTiming() throws Exception {
      String zh = Files.readString(RESOURCES.resolve("assets/typemoonworld/lang/zh_cn.json"));
      String en = Files.readString(RESOURCES.resolve("assets/typemoonworld/lang/en_us.json"));
      String entities = Files.readString(JAVA.resolve("init/ModEntities.java"));
      String soldier = Files.readString(JAVA.resolve("entity/MacedonianSoldierEntity.java"));
      String protection = Files.readString(JAVA.resolve("servant/card/ServantMasterProtection.java"));
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      String helper = Files.readString(JAVA.resolve("item/custom/PlayerNoblePhantasmHelper.java"));
      String vfx = Files.readString(RESOURCES.resolve("assets/typemoonworld/effects/artoria_excalibur_beam.json"));

      assertTrue(zh.contains("\"item.typemoonworld.iskandar_shortsword\": \"塞浦路特之剑\""));
      assertTrue(en.contains("\"item.typemoonworld.iskandar_shortsword\": \"Sword of Cyprus\""));
      assertTrue(entities.contains("macedonian_spear_projectile"));
      assertTrue(soldier.contains("getSpearThrowDamage"));
      assertTrue(soldier.contains("baseAttack + weaponAttack + rankBonus"));
      assertTrue(protection.contains("IskandarEntity"));
      assertTrue(protection.contains("MacedonianSoldierEntity"));
      assertTrue(iskandar.contains("ServantMasterProtection.isProtectedMaster(this, living)"));
      assertTrue(helper.contains("artoria_excalibur_beam\", player, 150.0"));
      assertTrue(vfx.contains("\"duration\": 7.5"));
      assertFalse(vfx.contains("\"end_time\": 30.0"));
   }

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
   }
}

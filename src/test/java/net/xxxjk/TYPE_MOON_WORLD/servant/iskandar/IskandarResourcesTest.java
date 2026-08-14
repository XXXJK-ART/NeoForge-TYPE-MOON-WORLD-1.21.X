package net.xxxjk.TYPE_MOON_WORLD.servant.iskandar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
      assertEquals(200, params.get("active_cap").getAsInt());
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

      for (String id : List.of("ISKANDAR", "BUCEPHALUS", "GORDIUS_WHEEL", "MACEDONIAN_SOLDIER")) {
         assertTrue(entities.contains(" " + id + " ="), id);
      }
      assertTrue(items.contains("ISKANDAR_SPAWN_EGG"));
      assertTrue(items.contains("MACEDONIAN_SOLDIER_SPAWN_EGG"));
      assertTrue(items.contains("MACEDONIAN_SPEAR"));
      assertTrue(items.contains("MACEDONIAN_ROUND_SHIELD"));
      assertTrue(items.contains("ISKANDAR_SHORTSWORD"));
      assertTrue(items.contains("new IskandarShortswordItem"));
      assertTrue(items.contains("SERVANT_CARD_ISKANDAR_CHEST"));
      assertTrue(items.contains("SERVANT_CARD_ISKANDAR_LEGS"));
      assertTrue(items.contains("SERVANT_CARD_ISKANDAR_FEET"));
      assertFalse(items.contains("SERVANT_CARD_ISKANDAR = registerServantCard"));
      assertTrue(client.contains("ModEntities.ISKANDAR.get()"));
      assertTrue(client.contains("ModEntities.BUCEPHALUS.get()"));
      assertTrue(client.contains("ModEntities.GORDIUS_WHEEL.get()"));
      assertTrue(client.contains("ModEntities.MACEDONIAN_SOLDIER.get()"));
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
   void noIskandarServantCardWasAdded() throws Exception {
      String registry = Files.readString(JAVA.resolve("servant/card/ServantCardRegistry.java"));
      assertFalse(registry.contains("\"iskandar\""));
      assertFalse(Files.exists(RESOURCES.resolve("assets/typemoonworld/textures/item/servant_cards/iskandar_card.png")));
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
   void gordiusWheelUsesDelayedRearBodyAnchor() throws Exception {
      String wheel = Files.readString(JAVA.resolve("entity/GordiusWheelEntity.java"));
      String mount = Files.readString(JAVA.resolve("entity/IskandarMountEntity.java"));
      String iskandar = Files.readString(JAVA.resolve("servant/entity/IskandarEntity.java"));
      String model = Files.readString(JAVA.resolve("client/model/GordiusWheelModel.java"));
      assertTrue(wheel.contains("tickRearBodyPhysics"));
      assertTrue(wheel.contains("position().subtract(forward.scale(REAR_BODY_DISTANCE))"));
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
      assertTrue(wheel.contains("DIVE_DIRECT_DAMAGE = 60.0F"));
      assertTrue(wheel.contains("DIVE_IMPACT_DAMAGE = 45.0F"));
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
      assertTrue(wheel.contains("LIGHTNING_AURA_DAMAGE = 4.0F"));
      assertTrue(wheel.contains("THUNDER_STRIKE_DAMAGE = 18.0F"));
      assertTrue(wheel.contains("THUNDER_ROAR_COOLDOWN_TICKS = 16 * 20"));
      assertTrue(wheel.contains("EntityUtils.isImmunePlayerTarget(entity)"));
      assertTrue(wheel.contains("zeusDamageSource"));
      assertTrue(wheel.contains("this.damageSources().mobAttack(iskandar)"));
      assertTrue(wheel.contains("if (i > 0)"));
      assertTrue(wheel.contains("spawnVisualThunderbolt(level, arcPos)"));
      assertTrue(wheel.contains("this.isCharging()"));
      assertTrue(mount.contains("getSeatSideOffset(passengerSeat)"));
      assertTrue(mount.contains("Math.cos(yaw) * localX - Math.sin(yaw) * localZ"));
      assertFalse(iskandar.contains(".add(0.0, 1.2, 0.0)"));
      assertTrue(wheel.contains("GordiusRearX"));
      assertTrue(wheel.contains("GordiusFrontX"));
      assertTrue(wheel.contains("prevRearX"));
      assertTrue(wheel.contains("getIdealRearBodyAnchor"));
      assertTrue(model.contains("setCustomAnimations"));
      assertTrue(model.contains("MODEL_GROUND_LIFT = 8.0F"));
      assertTrue(model.contains("front.setScaleX(FULL_SCALE)"));
      assertTrue(model.contains("rearRenderOffset"));
      assertTrue(model.contains("Left front leg"));
      assertTrue(model.contains("Left_front_leg2"));
      assertTrue(model.contains("杞﹁疆"));
      String animation = Files.readString(RESOURCES.resolve("assets/typemoonworld/animations/gordius_wheel.animation.json"));
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
      assertFalse(iskandar.contains("ensurePersistentMounts"));
      assertTrue(iskandar.contains("summonBucephalusAndRide"));
      assertTrue(iskandar.contains("summonGordiusWheelAndRide"));
      assertTrue(iskandar.contains("discardBucephalus"));
      assertTrue(iskandar.contains("discardGordiusWheel"));
      assertTrue(iskandar.contains("TAG_GORDIUS_WHEEL_SUMMONED_ONCE"));
      assertTrue(iskandar.contains("TAG_BUCEPHALUS_SUMMONED_ONCE"));
      assertTrue(iskandar.contains("TAG_BUCEPHALUS_READY_AFTER_WHEEL"));
      assertTrue(iskandar.contains("canSummonGordiusWheel"));
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
      assertTrue(ai.contains("dismountForWalking"));
      assertTrue(ai.contains("keepRidingBucephalus"));
      assertTrue(ai.contains("keepRidingGordiusWheel"));
      assertTrue(ai.contains("!entity.hasSummonedGordiusWheelOnce()"));
      assertTrue(ai.contains("if (entity.hasSummonedGordiusWheelOnce())"));
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
      assertTrue(iskandar.contains("this.getVehicle() instanceof IskandarMountEntity"));
      assertTrue(iskandar.contains("IONIOI_MP_COST = 60.0"));
      assertTrue(iskandar.contains("WHEEL_CHARGE_COOLDOWN = 8 * 20"));
      assertTrue(iskandar.contains("HORSE_CHARGE_COOLDOWN = 12 * 20"));
      assertTrue(iskandar.contains("tickKinglyWarCry"));
      assertTrue(iskandar.contains("mount.performCharge(level, this, 28.0F, 1.9)"));
      assertTrue(iskandar.contains("mount.performCharge(level, this, 36.0F, 2.6)"));
      assertTrue(iskandar.contains("distanceSqr >= 6.0 * 6.0"));
      String horse = Files.readString(JAVA.resolve("entity/BucephalusEntity.java"));
      String horseModel = Files.readString(JAVA.resolve("client/model/BucephalusModel.java"));
      assertTrue(horse.contains("return \"walk\""));
      assertTrue(horse.contains("return \"gallop\""));
      assertTrue(horse.contains("createMountAttributes(2000.0, 0.48)"));
      assertFalse(horse.contains("isFlyingMode"));
      assertTrue(horseModel.contains("setCustomAnimations"));
      assertTrue(horseModel.contains("MODEL_GROUND_LIFT = 8.0F"));
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
      assertTrue(iskandar.contains("List<LivingEntity> pulled = collectIonioiTargets(level, primary);"));
      assertTrue(iskandar.contains("LivingEntity movedPrimary = moveIonioiTargets(level, ionioiLevel, pulled, primary, entry);"));
      assertTrue(iskandar.contains("this.getBoundingBox().inflate(IONIOI_PULL_RADIUS)"));
      assertTrue(iskandar.contains("resolveIonioiPrimaryTarget"));
      assertTrue(iskandar.contains("!EntityUtils.isSpectatorPlayer(living)"));
      assertFalse(iskandar.contains("!EntityUtils.isImmunePlayerTarget(living)"));
      assertTrue(ai.contains("hasNearbyIonioiPullTarget"));
      assertTrue(ai.contains("candidate instanceof Player"));
      assertTrue(ai.contains("!EntityUtils.isSpectatorPlayer(candidate)"));
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
      assertTrue(wheel.contains("super.followIskandarCombatIntent(level, iskandar)"));
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

   private static JsonObject json(String relative) throws Exception {
      return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
   }
}

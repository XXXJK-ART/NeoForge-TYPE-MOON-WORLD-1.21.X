package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashStellaControllerEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ArashBowItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardArashSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDefenseHandler;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashAimHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashEntity;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

@GameTestHolder("typemoonworld_arash")
@PrefixGameTestTemplate(false)
public final class ArashGameTests {
   private ArashGameTests() { }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void advancingTerrainUsesCircularCrossSection(GameTestHelper helper) {
      BlockPos relativeOrigin = new BlockPos(3, 7, 3);
      for (int along = 0; along <= 2; along++) {
         for (int side = -2; side <= 2; side++) {
            for (int vertical = -2; vertical <= 2; vertical++) {
               helper.setBlock(relativeOrigin.offset(along, vertical, side), Blocks.STONE);
            }
         }
      }
      BlockPos absoluteOrigin = helper.absolutePos(relativeOrigin);
      var cylinder = DeferredTerrainDestruction.queueAdvancingCylinder(helper.getLevel(),
         new Vec3(absoluteOrigin.getX() + 0.5, absoluteOrigin.getY() + 0.5, absoluteOrigin.getZ() + 0.5),
         new Vec3(1.0, 0.0, 0.0), 2.0, 2, 2, null);
      cylinder.advanceTo(2.0);
      cylinder.seal();
      helper.runAfterDelay(5, () -> {
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(relativeOrigin.offset(1, 0, 0))).isAir(),
            "Advancing cylinder did not clear its center");
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(relativeOrigin.offset(1, 0, 2))).isAir(),
            "Advancing cylinder did not clear its radius boundary");
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(relativeOrigin.offset(1, 2, 2))).is(Blocks.STONE),
            "Advancing cylinder incorrectly cleared a square-corner voxel");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void chargedArrowImpactsDestroyScaledTerrainAndPreserveBedrock(GameTestHelper helper) {
      var level = helper.getLevel();
      var owner = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 8, 8));
      owner.setNoAi(true);
      BlockPos smallImpact = new BlockPos(8, 8, 4);
      BlockPos heavyImpact = new BlockPos(8, 8, 12);
      for (int x = 4; x < 8; x++) {
         helper.setBlock(new BlockPos(x, 8, smallImpact.getZ()), Blocks.AIR);
         helper.setBlock(new BlockPos(x, 8, heavyImpact.getZ()), Blocks.AIR);
      }
      helper.setBlock(smallImpact, Blocks.STONE);
      helper.setBlock(smallImpact.above(2), Blocks.STONE);
      helper.setBlock(smallImpact.above(3), Blocks.STONE);
      helper.setBlock(smallImpact.offset(0, 0, 1), Blocks.BEDROCK);
      helper.setBlock(heavyImpact, Blocks.STONE);
      helper.setBlock(heavyImpact.above(4), Blocks.STONE);
      helper.setBlock(heavyImpact.above(5), Blocks.STONE);
      helper.setBlock(heavyImpact.offset(0, 0, 1), Blocks.BEDROCK);

      spawnTerrainTestArrow(helper, owner, smallImpact, ArashParticleArrowEntity.SMALL_ENERGY, 30.0F);
      spawnTerrainTestArrow(helper, owner, heavyImpact, ArashParticleArrowEntity.LARGE_ENERGY, 60.0F);
      helper.runAfterDelay(15, () -> {
         helper.assertTrue(level.getBlockState(helper.absolutePos(smallImpact)).isAir(),
            "Charged arrow did not destroy terrain at its impact point");
         helper.assertTrue(level.getBlockState(helper.absolutePos(smallImpact.above(2))).isAir(),
            "Charged arrow did not clear its two-block terrain radius");
         helper.assertTrue(level.getBlockState(helper.absolutePos(smallImpact.above(3))).is(Blocks.STONE),
            "Charged arrow destroyed terrain beyond its intended radius");
         helper.assertTrue(level.getBlockState(helper.absolutePos(smallImpact.offset(0, 0, 1))).is(Blocks.BEDROCK),
            "Charged arrow destroyed protected bedrock");
         helper.assertTrue(level.getBlockState(helper.absolutePos(heavyImpact.above(4))).isAir(),
            "Heavy charged arrow did not clear its four-block terrain radius");
         helper.assertTrue(level.getBlockState(helper.absolutePos(heavyImpact.above(5))).is(Blocks.STONE),
            "Heavy charged arrow destroyed terrain beyond its intended radius");
         helper.assertTrue(level.getBlockState(helper.absolutePos(heavyImpact.offset(0, 0, 1))).is(Blocks.BEDROCK),
            "Heavy charged arrow destroyed protected bedrock");
         helper.succeed();
      });
   }

   private static void spawnTerrainTestArrow(GameTestHelper helper, net.minecraft.world.entity.LivingEntity owner,
                                             BlockPos impact, int variant, float damage) {
      BlockPos absoluteImpact = helper.absolutePos(impact);
      var arrow = new ArashParticleArrowEntity(helper.getLevel(), owner, variant, damage);
      arrow.setPos(absoluteImpact.getX() - 3.5, absoluteImpact.getY() + 0.5, absoluteImpact.getZ() + 0.5);
      arrow.setDeltaMovement(2.0, 0.0, 0.0);
      helper.getLevel().addFreshEntity(arrow);
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void playerBowAutoAimLeadsMovingTargetInsideSmallCone(GameTestHelper helper) {
      var level = helper.getLevel();
      var player = helper.makeMockServerPlayerInLevel();
      BlockPos playerPosition = helper.absolutePos(new BlockPos(2, 8, 2));
      player.moveTo(playerPosition.getX() + 0.5, playerPosition.getY(), playerPosition.getZ() + 0.5,
         -90.0F, 0.0F);
      helper.assertTrue(ServantCardTransformManager.transform(player, "arash"), "Arash card transformation failed");
      var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(10, 8, 2));
      target.setNoAi(true);
      target.setDeltaMovement(0.0, 0.0, 0.18);
      Vec3 look = player.getLookAngle().normalize();
      Vec3 origin = player.getEyePosition().add(look.scale(0.65));
      helper.assertTrue(ArashAimHelper.findAutoAimTarget(player, origin, look) == target,
         "Moving target inside the eight-degree cone was not selected");
      Vec3 direct = target.getEyePosition().subtract(origin).normalize();

      ((ArashBowItem)player.getMainHandItem().getItem()).use(level, player, InteractionHand.MAIN_HAND);
      var arrows = level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(5.0), arrow -> arrow.getVariant() == ArashParticleArrowEntity.NORMAL);
      helper.assertTrue(arrows.size() == 1, "Auto-aim test did not fire exactly one normal arrow");
      Vec3 firedDirection = arrows.get(0).getDeltaMovement().normalize();
      helper.assertTrue(firedDirection.z > direct.z + 0.02,
         "Arrow launch direction did not lead the target's lateral movement");
      helper.assertTrue(ArashAimHelper.isWithinAimCone(look, firedDirection, ArashAimHelper.AUTO_AIM_ANGLE_DEGREES),
         "Auto-aim corrected the shot beyond the allowed small angle");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void playerArrowRainUsesLookedAtAreaAndNearbyEnemyPrediction(GameTestHelper helper) {
      var level = helper.getLevel();
      var player = helper.makeMockServerPlayerInLevel();
      BlockPos playerPosition = helper.absolutePos(new BlockPos(2, 8, 2));
      player.moveTo(playerPosition.getX() + 0.5, playerPosition.getY(), playerPosition.getZ() + 0.5,
         -90.0F, 0.0F);
      helper.assertTrue(ServantCardTransformManager.transform(player, "arash"), "Arash card transformation failed");
      helper.setBlock(new BlockPos(8, 9, 2), Blocks.STONE);
      var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, 8, 7));
      target.setNoAi(true);
      target.setDeltaMovement(0.0, 0.0, 0.12);

      helper.assertTrue(ServantCardArashSkills.performArrowRain(player), "Player arrow rain failed to fire");
      var rain = level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(8.0), arrow -> arrow.getVariant() == ArashParticleArrowEntity.RAIN);
      helper.assertTrue(rain.size() == ArashCombatRules.RAIN_ARROW_COUNT,
         "Player arrow rain did not create all fifty arrows");
      double averageHorizontalZ = rain.stream().map(ArashParticleArrowEntity::getDeltaMovement)
         .mapToDouble(motion -> motion.horizontalDistanceSqr() < 1.0E-8 ? 0.0
            : motion.z / Math.sqrt(motion.horizontalDistanceSqr())).average().orElse(0.0);
      helper.assertTrue(averageHorizontalZ > 0.35,
         "Arrow rain stayed on the raw look ray instead of assisting toward the nearby enemy");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void summonInitializesDedicatedArcher(GameTestHelper helper) {
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "arash");
      var entity = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(), id,
         helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(entity instanceof ArashEntity, "Arash summon did not use the dedicated entity");
      var arash = (ArashEntity)entity;
      helper.assertTrue(Math.abs(arash.getMaxHealth() - 600.0F) < 0.001F, "Stout EX did not produce 600 HP");
      helper.assertTrue(Math.abs(arash.getMaxMp() - 200.0) < 0.001, "Magic E did not produce 200 MP");
      helper.assertTrue(arash.getMainHandItem().is(ModItems.ARASH_BOW.get()), "Arash did not equip his bow");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void arashCrossesCloseEnemyInsteadOfStandingStill(GameTestHelper helper) {
      var level = helper.getLevel();
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "arash");
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(level, id,
         helper.absolutePos(new BlockPos(2, 3, 2)));
      helper.assertTrue(summoned instanceof ArashEntity, "Could not summon mobile Arash");
      var arash = (ArashEntity)summoned;
      arash.setCurrentMp(0.0);
      var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 3, 2));
      target.setNoAi(true);
      target.setInvulnerable(true);
      Vec3 start = arash.position();
      Vec3 crossingAxis = target.position().subtract(start).multiply(1.0, 0.0, 1.0).normalize();
      arash.setTarget(target);

      helper.runAfterDelay(30, () -> {
         double movement = arash.position().subtract(start).multiply(1.0, 0.0, 1.0).length();
         double crossedBy = arash.position().subtract(target.position()).dot(crossingAxis);
         helper.assertTrue(movement >= 3.0, "Arash remained stationary while firing");
         helper.assertTrue(crossedBy > 0.5, "Arash crossed by " + crossedBy + " after moving " + movement
            + " blocks and did not clear the close enemy");
         helper.assertTrue(arash.getMainHandItem().is(ModItems.ARASH_BOW.get()),
            "Arash switched away from ranged combat while repositioning");
         helper.assertTrue(arash.getCurrentMp() == 0.0,
            "Arash normal attacks unexpectedly consumed mana");
         helper.assertTrue(!arash.getPersistentData().contains("ArashVirtualArrows"),
            "Arash normal attacks still used the old mana-backed virtual arrow stock");
         Vec3 facing = arash.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
         Vec3 towardTarget = target.position().subtract(arash.position()).multiply(1.0, 0.0, 1.0).normalize();
         helper.assertTrue(facing.dot(towardTarget) > 0.95,
            "Arash did not face the enemy while firing; facing dot was " + facing.dot(towardTarget));
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 240)
   public static void particleArrowHitsAndControllerSurvivesSacrifice(GameTestHelper helper) {
      var level = helper.getLevel();
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "arash");
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(level, id,
         helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(summoned instanceof ArashEntity, "Could not summon Arash");
      var arash = (ArashEntity)summoned;
      var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 2, 2));
      target.setNoAi(true);
      float before = target.getHealth();
      var arrow = new ArashParticleArrowEntity(level, arash, ArashParticleArrowEntity.NORMAL, 11.0F);
      arrow.setPos(arash.getX(), arash.getEyeY(), arash.getZ());
      arrow.setDeltaMovement(target.getEyePosition().subtract(arrow.position()).normalize().scale(2.0));
      level.addFreshEntity(arrow);
      helper.runAfterDelay(8, () -> {
         helper.assertTrue(target.getHealth() < before, "Arash particle arrow did not damage its target");
         for (int i = 0; i < 8; i++) {
            var enemy = EntityType.ZOMBIE.create(level);
            helper.assertTrue(enemy != null, "Could not create Stella trigger enemy");
            enemy.setNoAi(true);
            enemy.moveTo(arash.getX() + 12.0 + i, arash.getY(), arash.getZ() + (i % 2), 0.0F, 0.0F);
            level.addFreshEntity(enemy);
         }
         arash.setCurrentMp(200.0);
         arash.setTarget(target);
         helper.assertTrue(ArashStellaControllerEntity.tryBegin(arash, target), "Stella did not begin with eight enemies");
         var controllers = level.getEntitiesOfClass(ArashStellaControllerEntity.class,
            arash.getBoundingBox().inflate(4.0), controller -> true);
         helper.assertTrue(controllers.size() == 1, "Stella controller was not created");
         ArashStellaControllerEntity controller = controllers.get(0);
         controller.forceReleaseForGameTest();
         helper.assertTrue(arash.isAlive(), "Arash died immediately when Stella was released");
         helper.assertTrue(arash.getPersistentData().getBoolean(ArashEntity.TAG_STELLA_SACRIFICE),
            "Arash did not enter the ten-second Stella sacrifice state");
         helper.assertTrue(controller.isAlive() && controller.isInFlight(), "Stella controller stopped with its caster");
         float releaseHealth = arash.getHealth();
         float pulseDamage = arash.getMaxHealth() * ArashCombatRules.STELLA_SACRIFICE_DAMAGE_FRACTION;
         controller.discard();
         helper.runAfterDelay(10, () -> helper.assertTrue(Math.abs(arash.getHealth() - releaseHealth) < 0.001F,
            "Arash lost Stella sacrifice health before the first one-second pulse"));
         helper.runAfterDelay(25, () -> helper.assertTrue(
            Math.abs(arash.getHealth() - Math.max(1.0F, releaseHealth - pulseDamage)) < 0.001F,
            "Arash Stella sacrifice health was " + arash.getHealth() + " instead of "
               + Math.max(1.0F, releaseHealth - pulseDamage) + " after one pulse"));
         helper.runAfterDelay(205, () -> {
            helper.assertTrue(!arash.isAlive(), "Arash survived beyond the ten-second Stella sacrifice");
            helper.succeed();
         });
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 260)
   public static void playerCardLocksRefundsAndReleasesIndependentStella(GameTestHelper helper) {
      var level = helper.getLevel();
      var player = helper.makeMockServerPlayerInLevel();
      var start = helper.absolutePos(new BlockPos(2, 3, 2));
      player.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, -90.0F, 0.0F);
      helper.assertTrue(ServantCardTransformManager.transform(player, "arash"), "Arash card transformation failed");
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      helper.assertTrue(Math.abs(player.getMaxHealth() - 600.0F) < 0.001F, "Player Arash did not receive 600 HP");
      helper.assertTrue(Math.abs(vars.servant_card_max_mana - 200.0) < 0.001, "Player Arash did not receive 200 MP");
      helper.assertTrue(player.getMainHandItem().is(ModItems.ARASH_BOW.get()), "Player Arash did not equip the dedicated bow");
      player.getPersistentData().putDouble("ServantCardCombatStamina", 0.0);
      player.getPersistentData().putDouble("ServantCardCombatPoise", 0.0);
      ServantCardDefenseHandler.tick(player, vars);
      helper.assertTrue(Math.abs(player.getPersistentData().getDouble("ServantCardCombatStamina") - 1.6) < 0.001,
         "Player Arash defense recovery did not receive the Stout EX efficiency bonus");
      helper.assertTrue(Math.abs(player.getPersistentData().getDouble("ServantCardCombatPoise") - 1.3) < 0.001,
         "Player Arash poise recovery did not receive the Stout EX efficiency bonus");

      vars.servant_card_mana = 200.0;
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Player Stella did not start");
      var first = level.getEntitiesOfClass(ArashStellaControllerEntity.class,
         player.getBoundingBox().inflate(4.0), controller -> true).get(0);
      first.abortTechnicalForGameTest();
      helper.assertTrue(!ServantCardArashSkills.isPlayerChanting(player), "Technical abort did not unlock the player");
      helper.assertTrue(Math.abs(vars.servant_card_mana - 200.0) < 0.001, "Technical abort did not refund mana");
      helper.assertTrue(vars.servant_card_np_cooldown == 0, "Technical abort did not restore cooldown");

      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Player Stella could not restart after refund");
      var controller = level.getEntitiesOfClass(ArashStellaControllerEntity.class,
         player.getBoundingBox().inflate(4.0), candidate -> true).get(0);
      var fixedDirection = controller.getDirectionForGameTest();
      double anchorX = player.getX(), anchorY = player.getY(), anchorZ = player.getZ();
      player.setPos(anchorX + 2.0, anchorY + 1.0, anchorZ);
      player.setDeltaMovement(0.5, 0.5, 0.5);
      player.setYRot(player.getYRot() + 40.0F);
      player.setXRot(25.0F);
      player.getInventory().selected = 4;
      ServantCardArashSkills.tick(player, vars);
      double horizontalDistance = Math.hypot(player.getX() - anchorX, player.getZ() - anchorZ);
      helper.assertTrue(horizontalDistance <= 0.001,
         "Stella chant did not pin the player to its horizontal anchor");
      helper.assertTrue(Math.abs(player.getY() - anchorY) <= 0.001 && player.getDeltaMovement().lengthSqr() < 1.0E-9,
         "Stella chant allowed vertical movement or retained velocity");
      helper.assertTrue(Math.abs(player.getYRot() + 90.0F) <= ServantCardArashSkills.CHANT_LOOK_TOLERANCE + 0.001,
         "Stella chant yaw exceeded its tolerance");
      helper.assertTrue(Math.abs(player.getXRot()) <= ServantCardArashSkills.CHANT_LOOK_TOLERANCE + 0.001,
         "Stella chant pitch exceeded its tolerance");
      helper.assertTrue(player.getInventory().selected == ServantCardArashSkills.STELLA_LOCKED_HOTBAR_SLOT
         && player.getMainHandItem().is(ModItems.ARASH_BOW.get()),
         "Stella chant did not force the dedicated bow into hotbar slot one");
      helper.assertTrue(controller.getDirectionForGameTest().distanceToSqr(fixedDirection) < 1.0E-9,
         "Stella direction changed after the player turned");

      controller.forceReleaseForGameTest();
      helper.assertTrue(controller.isAlive() && controller.isInFlight(), "Player Stella controller stopped with its caster");
      helper.assertTrue(!ServantCardArashSkills.isPlayerChanting(player), "Stella release did not unlock the player");
      helper.assertTrue(player.isAlive() && ServantCardArashSkills.isPlayerStellaSacrificing(player),
         "Player died immediately instead of entering the Stella sacrifice state");
      float releaseHealth = player.getHealth();
      float pulseDamage = player.getMaxHealth() * ArashCombatRules.STELLA_SACRIFICE_DAMAGE_FRACTION;
      controller.discard();
      helper.onEachTick(() -> ServantCardArashSkills.onPlayerTick(new PlayerTickEvent.Post(player)));
      helper.runAfterDelay(10, () -> helper.assertTrue(Math.abs(player.getHealth() - releaseHealth) < 0.001F,
         "Player lost Stella sacrifice health before the first one-second pulse"));
      helper.runAfterDelay(25, () -> helper.assertTrue(
         Math.abs(player.getHealth() - Math.max(1.0F, releaseHealth - pulseDamage)) < 0.001F,
         "Player Stella sacrifice health was " + player.getHealth() + " instead of "
            + Math.max(1.0F, releaseHealth - pulseDamage) + " after one pulse"));
      helper.runAfterDelay(205, () -> {
         helper.assertTrue(!ServantCardArashSkills.isPlayerStellaSacrificing(player),
            "Player Stella sacrifice did not finish after ten seconds");
         if (player.isAlive()) {
            helper.assertTrue(!vars.servant_card_transformed && player.getHealth() <= 1.0F,
               "Global card death-release rule was not preserved");
         }
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 230)
   public static void playerBowFiresInstantChargedAndLongLivedArrows(GameTestHelper helper) {
      var level = helper.getLevel();
      var player = helper.makeMockServerPlayerInLevel();
      var start = helper.absolutePos(new BlockPos(2, 3, 2));
      player.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, -90.0F, 0.0F);
      helper.assertTrue(ServantCardTransformManager.transform(player, "arash"), "Arash card transformation failed");
      helper.assertTrue(player.getMainHandItem().getItem() instanceof ArashBowItem, "Arash bow was not equipped");
      var bow = (ArashBowItem)player.getMainHandItem().getItem();
      var stack = player.getMainHandItem();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

      bow.use(level, player, InteractionHand.MAIN_HAND);
      var instant = level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(5.0), arrow -> arrow.getVariant() == ArashParticleArrowEntity.NORMAL);
      helper.assertTrue(instant.size() == 1, "Right click did not immediately fire exactly one normal arrow");
      helper.assertTrue(Math.abs(instant.get(0).getDamageForGameTest() - 10.0F) < 0.001F,
         "Instant arrow damage was not 10");

      double manaBeforeCharge = vars.servant_card_mana;
      String cooldownsBeforeCharge = vars.servant_card_skill_cooldowns;
      String cooldownEndsBeforeCharge = vars.servant_card_skill_cooldown_ends;
      bow.releaseUsing(stack, level, player, bow.getUseDuration(stack, player) - ArashBowItem.CHARGED_ARROW_TICKS);
      bow.releaseUsing(stack, level, player, bow.getUseDuration(stack, player) - ArashBowItem.HEAVY_CHARGED_ARROW_TICKS);
      var charged = level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(6.0), arrow -> arrow.getVariant() == ArashParticleArrowEntity.SMALL_ENERGY);
      var heavy = level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(6.0), arrow -> arrow.getVariant() == ArashParticleArrowEntity.LARGE_ENERGY);
      helper.assertTrue(charged.size() == 1 && Math.abs(charged.get(0).getDamageForGameTest() - 30.0F) < 0.001F,
         "Two-second charged arrow was not 30 damage");
      helper.assertTrue(heavy.size() == 1 && Math.abs(heavy.get(0).getDamageForGameTest() - 60.0F) < 0.001F,
         "Four-second heavy charged arrow was not 60 damage");
      helper.assertTrue(Math.abs(vars.servant_card_mana - (manaBeforeCharge - 28.0)) < 0.001,
         "Bow-charged arrows did not consume their normal skill MP");
      helper.assertTrue(java.util.Objects.equals(cooldownsBeforeCharge, vars.servant_card_skill_cooldowns)
            && java.util.Objects.equals(cooldownEndsBeforeCharge, vars.servant_card_skill_cooldown_ends),
         "Bow-charged arrows incorrectly changed skill cooldowns");

      java.util.List<ArashParticleArrowEntity> persistent = new java.util.ArrayList<>();
      double openSkyY = level.getSeaLevel() + 40.0;
      for (int variant : new int[]{ArashParticleArrowEntity.NORMAL, ArashParticleArrowEntity.RAIN,
         ArashParticleArrowEntity.SMALL_ENERGY, ArashParticleArrowEntity.LARGE_ENERGY}) {
         var arrow = new ArashParticleArrowEntity(level, player, variant, 10.0F);
         arrow.setPos(player.getX(), openSkyY, player.getZ());
         arrow.setDeltaMovement(variant == ArashParticleArrowEntity.RAIN ? new Vec3(0.0, 4.0, 0.0) : Vec3.ZERO);
         level.addFreshEntity(arrow);
         persistent.add(arrow);
      }
      var unloaded = new ArashParticleArrowEntity(level, player, ArashParticleArrowEntity.LARGE_ENERGY, 60.0F);
      unloaded.setPos(player.getX(), openSkyY, player.getZ());
      unloaded.setDeltaMovement(10000.0, 0.0, 0.0);
      level.addFreshEntity(unloaded);
      helper.runAfterDelay(190, () -> {
         for (ArashParticleArrowEntity arrow : persistent) {
            helper.assertTrue(arrow.isAlive(), "Arash arrow variant " + arrow.getVariant()
               + " expired without leaving loaded chunks or hitting a target");
            arrow.discard();
         }
         helper.assertTrue(!unloaded.isAlive(), "Arash arrow continued beyond the loaded chunk range");
         helper.succeed();
      });
   }
}

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
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashTacticalSkillService;
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
   public static void advancingSkyRiftClearsTunnelCoreUpwardAndPreservesEdges(GameTestHelper helper) {
      BlockPos relativeOrigin = new BlockPos(3, 7, 3);
      for (int along = 0; along <= 2; along++) {
         BlockPos center = relativeOrigin.offset(along, 0, 0);
         helper.setBlock(center, Blocks.STONE);
         helper.setBlock(center.above(1), Blocks.STONE);
         helper.setBlock(center.above(2), Blocks.STONE);
         helper.setBlock(center.above(3), Blocks.BEDROCK);
         helper.setBlock(center.above(4), Blocks.STONE);
         helper.setBlock(center.below(1), Blocks.STONE);
         helper.setBlock(center.offset(0, 2, 2), Blocks.STONE);
         helper.setBlock(center.offset(0, 2, 3), Blocks.STONE);
      }
      BlockPos absoluteOrigin = helper.absolutePos(relativeOrigin);
      var skyRift = DeferredTerrainDestruction.queueAdvancingSkyRift(helper.getLevel(),
         new Vec3(absoluteOrigin.getX() + 0.5, absoluteOrigin.getY() + 0.5, absoluteOrigin.getZ() + 0.5),
         new Vec3(1.0, 0.0, 0.0), 2.0, 2, null);
      skyRift.advanceTo(2.0);
      skyRift.seal();
      helper.runAfterDelay(20, () -> {
         BlockPos center = relativeOrigin.offset(1, 0, 0);
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(center)).isAir(),
            "Advancing sky rift did not clear the trajectory center");
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(center.above(2))).isAir(),
            "Advancing sky rift did not clear blocks above the tunnel core");
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(center.above(3))).is(Blocks.BEDROCK),
            "Advancing sky rift destroyed protected bedrock");
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(center.below(1))).is(Blocks.STONE),
            "Advancing sky rift incorrectly cleared blocks below the trajectory center");
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 2))).isAir(),
            "Advancing sky rift did not clear the main tunnel radius");
         helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(center.offset(0, 2, 3))).is(Blocks.STONE),
            "Advancing sky rift incorrectly cleared beyond the main tunnel radius");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void arashArrowImpactsBreakShallowTerrainAndProtectBedrock(GameTestHelper helper) {
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
      helper.setBlock(heavyImpact.above(2), Blocks.STONE);
      helper.setBlock(heavyImpact.above(4), Blocks.STONE);
      helper.setBlock(heavyImpact.above(5), Blocks.STONE);
      helper.setBlock(heavyImpact.offset(0, 0, 1), Blocks.BEDROCK);

      spawnTerrainTestArrow(helper, owner, smallImpact, ArashParticleArrowEntity.SMALL_ENERGY);
      spawnTerrainTestArrow(helper, owner, heavyImpact, ArashParticleArrowEntity.LARGE_ENERGY);
      helper.runAfterDelay(15, () -> {
         helper.assertTrue(level.getBlockState(helper.absolutePos(smallImpact.above(2))).isAir(),
            "Arash blast did not break terrain inside its upper hemisphere");
         helper.assertTrue(level.getBlockState(helper.absolutePos(smallImpact.offset(0, 0, 1))).is(Blocks.BEDROCK),
            "Arash terrain blast changed protected bedrock");
         helper.assertTrue(level.getBlockState(helper.absolutePos(heavyImpact.above(2))).isAir(),
            "Charged Arash blast did not break terrain in its larger hemisphere");
         helper.assertTrue(level.getBlockState(helper.absolutePos(heavyImpact.offset(0, 0, 1))).is(Blocks.BEDROCK),
            "Charged Arash terrain blast changed protected bedrock");
         helper.succeed();
      });
   }

   private static void spawnTerrainTestArrow(GameTestHelper helper, net.minecraft.world.entity.LivingEntity owner,
                                             BlockPos impact, int variant) {
      BlockPos absoluteImpact = helper.absolutePos(impact);
      var arrow = new ArashParticleArrowEntity(helper.getLevel(), owner, variant, 100.0F,
         ArashCombatRules.ARROW_MAX_CHARGE_TICKS);
      arrow.setPos(absoluteImpact.getX() - 3.5, absoluteImpact.getY() + 0.5, absoluteImpact.getZ() + 0.5);
      arrow.setDeltaMovement(2.0, 0.0, 0.0);
      helper.getLevel().addFreshEntity(arrow);
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 70)
   public static void poweredArrowLeavesAtMostTwoBlocksOfGroundDepth(GameTestHelper helper) {
      var level = helper.getLevel();
      var owner = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 12, 1));
      owner.setNoAi(true);
      BlockPos impact = new BlockPos(8, 8, 8);
      for (int x = 3; x <= 13; x++) for (int z = 3; z <= 13; z++) {
         for (int y = 5; y <= 8; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
         for (int y = 9; y <= 32; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
      }
      var arrow = new ArashParticleArrowEntity(level, owner, ArashParticleArrowEntity.NORMAL, 100.0F, 100);
      var data = new net.minecraft.nbt.CompoundTag();
      arrow.addAdditionalSaveData(data);
      data.putInt("FlightTicks", 200);
      arrow.readAdditionalSaveData(data);
      Vec3 center = Vec3.atCenterOf(helper.absolutePos(impact));
      arrow.setPos(center.add(0.0, 2.0, 0.0));
      arrow.setDeltaMovement(0.0, -2.0, 0.0);
      level.addFreshEntity(arrow);
      helper.succeedWhen(() -> {
         helper.assertTrue(level.getBlockState(helper.absolutePos(impact)).isAir(), "Impact did not break the ground surface; arrow="
            + arrow.position() + ", removed=" + arrow.isRemoved() + ", damage=" + arrow.getDamageForGameTest()
            + ", ground=" + level.getBlockState(helper.absolutePos(impact)) + ", height="
            + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
               helper.absolutePos(impact).getX(), helper.absolutePos(impact).getZ())
            + ", removable=" + net.xxxjk.TYPE_MOON_WORLD.world.terrain.CombatTerrainProtection.canRemoveWithinSurfaceDepth(level, helper.absolutePos(impact), 2)
            + ", queued=" + DeferredTerrainDestruction.totalQueuedJobs());
         helper.assertTrue(level.getBlockState(helper.absolutePos(impact.below())).isAir(), "Powered impact did not reach the second ground layer");
         helper.assertTrue(level.getBlockState(helper.absolutePos(impact.below(2))).is(Blocks.STONE), "Impact dug deeper than two ground layers");
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void playerBasicBowFiresAlongLookDirectionWithoutAimAssist(GameTestHelper helper) {
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
      var bow = (ArashBowItem)player.getMainHandItem().getItem();
      bow.use(level, player, InteractionHand.MAIN_HAND);
      bow.releaseUsing(player.getMainHandItem(), level, player, bow.getUseDuration(player.getMainHandItem(), player));
      var arrows = level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(5.0), arrow -> arrow.getVariant() == ArashParticleArrowEntity.NORMAL);
      helper.assertTrue(arrows.size() == 1, "Auto-aim test did not fire exactly one normal arrow");
      Vec3 firedDirection = arrows.get(0).getDeltaMovement().normalize();
      helper.assertTrue(firedDirection.distanceTo(look) < 0.001,
         "Basic arrow launch direction deviated from the player's look direction");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void emergencyRetreatSynchronizesThePlayerImpulse(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      player.moveTo(helper.absolutePos(new BlockPos(8, 8, 8)).getX() + 0.5,
         helper.absolutePos(new BlockPos(8, 8, 8)).getY(), helper.absolutePos(new BlockPos(8, 8, 8)).getZ() + 0.5,
         -90.0F, 0.0F);
      Vec3 look = player.getLookAngle().normalize();
      helper.assertTrue(ArashTacticalSkillService.performRetreat(player), "Emergency retreat did not execute");
      Vec3 velocity = player.getDeltaMovement();
      helper.assertTrue(velocity.x * -look.x + velocity.z * -look.z > 1.0,
         "Emergency retreat did not apply a clear backward impulse");
      helper.assertTrue(player.hurtMarked && player.hasImpulse,
         "Emergency retreat impulse was not marked for client synchronization");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void damageInterruptsChargeWithoutConsumingTheNextShot(GameTestHelper helper) {
      var level = helper.getLevel();
      // The vanilla GameTest mock hardcodes isCreative() to true; use a real survival player for damage.
      var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
         new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "arash-charge-test"), false);
      var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
         cookie.gameProfile(), cookie.clientInformation());
      var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
      new io.netty.channel.embedded.EmbeddedChannel(connection);
      level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
      player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
      var start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 8, 3)));
      player.moveTo(start.x, start.y, start.z, -90.0F, 0.0F);
      helper.assertTrue(ServantCardTransformManager.transform(player, "arash"), "Arash transformation failed");
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int stock = vars.servantCardState().arashArrowStock();
      player.getAbilities().invulnerable = false;
      player.invulnerableTime = 0;
      var bow = (ArashBowItem)player.getMainHandItem().getItem();
      var stack = player.getMainHandItem();
      bow.use(level, player, InteractionHand.MAIN_HAND);
      float healthBefore = player.getHealth();
      boolean hurt = player.hurt(level.damageSources().genericKill(), 1.0F);
      helper.assertTrue(!player.isUsingItem(), "Effective damage did not stop bow charge; hurt=" + hurt
         + ", health=" + healthBefore + " -> " + player.getHealth());
      helper.assertTrue(vars.servantCardState().arashArrowStock() == stock, "Interrupted charge consumed an arrow");
      helper.assertTrue(level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(5.0), arrow -> arrow.getOwner() == player).isEmpty(),
         "Interrupted charge fired an arrow");
      bow.use(level, player, InteractionHand.MAIN_HAND);
      bow.releaseUsing(stack, level, player, bow.getUseDuration(stack, player) - 100);
      helper.assertTrue(vars.servantCardState().arashArrowStock() == stock - 1,
         "Stale interruption state swallowed the next valid shot");
      helper.assertTrue(level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(5.0), arrow -> arrow.getOwner() == player).size() == 1,
         "Interrupted and resumed charge did not produce exactly one valid shot");
      player.discard();
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

   @GameTest(template = "ancient_temple", timeoutTicks = 130)
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
      target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(10000.0);
      target.setHealth(10000.0F);
      Vec3 start = arash.position();
      Vec3 crossingAxis = target.position().subtract(start).multiply(1.0, 0.0, 1.0).normalize();
      helper.onEachTick(() -> arash.setTarget(target));

      helper.runAfterDelay(95, () -> {
         double movement = arash.position().subtract(start).multiply(1.0, 0.0, 1.0).length();
         double crossedBy = arash.position().subtract(target.position()).dot(crossingAxis);
         helper.assertTrue(movement >= 3.0, "Arash moved only " + movement + " blocks; delta="
            + arash.getDeltaMovement() + ", crossoverUntil="
            + arash.getPersistentData().getLong("ArashCrossoverUntil") + ", nextCrossover="
            + arash.getPersistentData().getLong("ArashNextCrossover") + ", stationary="
            + arash.getPersistentData().getInt("ArashStationaryTicks") + ", target=" + arash.getTarget());
         helper.assertTrue(crossedBy > 0.5, "Arash crossed by " + crossedBy + " after moving " + movement
            + " blocks and did not clear the close enemy");
         helper.assertTrue(arash.getMainHandItem().is(ModItems.ARASH_BOW.get()),
            "Arash switched away from ranged combat while repositioning");
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
      var params = net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry.get("arash").parameters();
      double expectedStamina = net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas.staminaRegenPerSecond(params) * 2.0 / 20.0;
      double expectedPoise = net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatFormulas.poiseRegenPerSecond(params) * 2.0 / 20.0;
      helper.assertTrue(Math.abs(player.getPersistentData().getDouble("ServantCardCombatStamina") - expectedStamina) < 0.001,
         "Player Arash defense recovery did not receive the Stout EX efficiency bonus");
      helper.assertTrue(Math.abs(player.getPersistentData().getDouble("ServantCardCombatPoise") - expectedPoise) < 0.001,
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
   public static void playerBowChargesOnHoldAndFiresOnRelease(GameTestHelper helper) {
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
      var beforeRelease = level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(5.0), arrow -> arrow.getVariant() == ArashParticleArrowEntity.NORMAL);
      helper.assertTrue(beforeRelease.isEmpty(), "Right click fired before the bow was released");

      double manaBeforeCharge = vars.servant_card_mana;
      String cooldownsBeforeCharge = vars.servant_card_skill_cooldowns;
      String cooldownEndsBeforeCharge = vars.servant_card_skill_cooldown_ends;
      bow.releaseUsing(stack, level, player, bow.getUseDuration(stack, player) - ArashBowItem.CHARGED_ARROW_TICKS);
      var charged = level.getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(6.0), arrow -> arrow.getVariant() == ArashParticleArrowEntity.NORMAL);
      helper.assertTrue(charged.size() == 1 && charged.get(0).getChargeTicks() == ArashCombatRules.ARROW_MAX_CHARGE_TICKS,
         "Full bow charge was not attached to the released arrow");
      helper.assertTrue(Math.abs(charged.get(0).getDamageForGameTest() - 40.0F) < 0.001F,
         "Full-charge arrow did not start at 40 damage");
      helper.assertTrue(Math.abs(vars.servant_card_mana - manaBeforeCharge) < 0.001,
         "Bow-charged arrows incorrectly consumed MP");
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

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
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashStellaControllerEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ArashBowItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardArashSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
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
         helper.assertTrue(crossedBy > 0.5, "Arash did not cross the close enemy to open a new firing lane");
         helper.assertTrue(arash.getMainHandItem().is(ModItems.ARASH_BOW.get()),
            "Arash switched away from ranged combat while repositioning");
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
         controller.discard();
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
      double anchorX = player.getX(), anchorZ = player.getZ();
      player.setPos(anchorX + 2.0, player.getY(), anchorZ);
      player.setYRot(player.getYRot() + 40.0F);
      player.setXRot(25.0F);
      player.getInventory().selected = 4;
      ServantCardArashSkills.tick(player, vars);
      double horizontalDistance = Math.hypot(player.getX() - anchorX, player.getZ() - anchorZ);
      helper.assertTrue(horizontalDistance <= 0.001,
         "Stella chant did not pin the player to its horizontal anchor");
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
      controller.discard();
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
      for (int variant : new int[]{ArashParticleArrowEntity.NORMAL, ArashParticleArrowEntity.RAIN,
         ArashParticleArrowEntity.SMALL_ENERGY, ArashParticleArrowEntity.LARGE_ENERGY}) {
         var arrow = new ArashParticleArrowEntity(level, player, variant, 10.0F);
         arrow.setPos(player.getX(), player.getY() + 8.0, player.getZ());
         arrow.setDeltaMovement(variant == ArashParticleArrowEntity.RAIN ? new Vec3(0.0, 4.0, 0.0) : Vec3.ZERO);
         level.addFreshEntity(arrow);
         persistent.add(arrow);
      }
      var unloaded = new ArashParticleArrowEntity(level, player, ArashParticleArrowEntity.LARGE_ENERGY, 60.0F);
      unloaded.setPos(player.getX(), player.getY() + 8.0, player.getZ());
      unloaded.setDeltaMovement(10000.0, 0.0, 0.0);
      level.addFreshEntity(unloaded);
      helper.runAfterDelay(190, () -> {
         for (ArashParticleArrowEntity arrow : persistent) {
            helper.assertTrue(arrow.isAlive(), "An Arash arrow variant expired from age inside loaded chunks");
            arrow.discard();
         }
         helper.assertTrue(!unloaded.isAlive(), "Arash arrow continued beyond the loaded chunk range");
         helper.succeed();
      });
   }
}

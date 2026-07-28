package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.DefinitionSnapshotService;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicPresetRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.entity.ContenderBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

@GameTestHolder("typemoonworld")
@PrefixGameTestTemplate(false)
public final class TypeMoonWorldGameTests {
   private TypeMoonWorldGameTests() { }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void codecDefinitionsLoad(GameTestHelper helper) {
      helper.assertTrue(ServantDataRegistry.size() > 0, "servant definitions were not loaded");
      helper.assertTrue(ServantSkillDataRegistry.all().size() >= 80, "skill definitions were not loaded");
      helper.assertTrue(ServantAiDefinitionRegistry.all().size() >= 10, "AI definitions were not loaded");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantActionRegistry.all().size() == 20,
         "all 20 servant action profiles were not loaded");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void magicDefinitionsLoad(GameTestHelper helper) {
      helper.assertTrue(!MagicDefinitionRegistry.ids().isEmpty(), "magic definitions were not loaded");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void contenderBulletsConstructAfterSyncedDataInitialization(GameTestHelper helper) {
      var owner = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
      var normal = new ContenderBulletEntity(helper.getLevel(), owner, false);
      var origin = new ContenderBulletEntity(helper.getLevel(), owner, true);
      helper.assertTrue(!normal.isOriginBullet() && normal.getItem().is(ModItems.BULLET.get()),
         "normal Contender bullet was not initialized safely");
      helper.assertTrue(origin.isOriginBullet() && origin.getItem().is(ModItems.ORIGIN_BULLET.get()),
         "Origin Bullet item did not match its synchronized kind");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void deadApostlesReceivePersistentBodyScale(GameTestHelper helper) {
      var dead = ModEntities.THE_DEAD.get().create(helper.getLevel());
      helper.assertTrue(dead != null, "could not create The Dead");
      dead.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(helper.absolutePos(new BlockPos(2, 2, 2))),
         net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null);
      double scale = dead.getAttributeValue(Attributes.SCALE);
      helper.assertTrue(scale >= DeadApostleEntity.MIN_BODY_SCALE && scale <= DeadApostleEntity.MAX_BODY_SCALE,
         "dead apostle body scale was outside the configured range: " + scale);
      helper.assertTrue(dead.getPersistentData().getBoolean("TypeMoonNpcRandomScaleV1"),
         "dead apostle body scale was not marked as initialized for persistence");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void genericServantSummons(GameTestHelper helper) {
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon");
      var entity = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(), id, helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(entity != null && entity.isAlive(), "generic servant summon failed");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void ushiwakamaruRiderSummonInitializesDedicatedEntity(GameTestHelper helper) {
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "ushiwakamaru_rider");
      var entity = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(), id, helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(entity instanceof UshiwakamaruRiderEntity, "Rider summon did not use dedicated entity");
      var rider = (UshiwakamaruRiderEntity) entity;
      helper.assertTrue(rider.getMainHandItem().is(ModItems.SPIDER_CUTTER.get()), "Rider did not equip Spider Cutter");
      helper.assertTrue(rider.getCombatPhase() == 1 && rider.getMaxHealth() > 0.0F, "Rider attributes were not initialized");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 80)
   public static void ushiwakamaruEightBoatClonesMatchOwnerAndAcquireTargets(GameTestHelper helper) {
      var level = helper.getLevel();
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "ushiwakamaru_rider");
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(level, id, helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(summoned instanceof UshiwakamaruRiderEntity, "Rider summon did not use dedicated entity");
      var rider = (UshiwakamaruRiderEntity)summoned;
      rider.setCombatPhase(3);
      rider.setCurrentMp(100.0);

      var firstTarget = EntityType.ZOMBIE.create(level);
      var secondTarget = EntityType.ZOMBIE.create(level);
      helper.assertTrue(firstTarget != null && secondTarget != null, "Could not create clone targets");
      firstTarget.setNoAi(true);
      secondTarget.setNoAi(true);
      firstTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
      secondTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
      firstTarget.setHealth(1000.0F);
      secondTarget.setHealth(1000.0F);
      firstTarget.moveTo(rider.getX() + 10.0, rider.getY(), rider.getZ(), 0.0F, 0.0F);
      secondTarget.moveTo(rider.getX() - 10.0, rider.getY(), rider.getZ(), 0.0F, 0.0F);
      level.addFreshEntity(firstTarget);
      level.addFreshEntity(secondTarget);
      rider.setTarget(firstTarget);

      helper.runAfterDelay(10, () -> {
         helper.assertTrue(rider.getPersistentData().hasUUID(UshiwakamaruRiderEntity.TAG_EIGHT_BOAT_TARGET),
            "Eight-Boat Leap did not retain its original target UUID; mp=" + rider.getCurrentMp()
               + ", phase=" + rider.getCombatPhase() + ", target=" + rider.getTarget()
               + ", until=" + rider.getPersistentData().getLong("UshiwakamaruEightBoatUntil")
               + ", last=" + rider.getPersistentData().getLong("UshiwakamaruLastEightBoat"));
         helper.assertTrue(firstTarget.isAlive(), "Eight-Boat original target died before the cleanup check");
         helper.assertTrue(level.getEntity(firstTarget.getUUID()) == firstTarget,
            "Eight-Boat original target was missing from the level UUID index");
         var clones = level.getEntitiesOfClass(UshiwakamaruRiderEntity.class,
            new AABB(rider.blockPosition()).inflate(32.0), UshiwakamaruRiderEntity::isClone);
         helper.assertTrue(clones.size() == 7, "Eight-Boat Leap did not create exactly seven clones; found " + clones.size());
         var targetIds = new java.util.HashSet<java.util.UUID>();
         for (var clone : clones) {
            if (clone.getTarget() != null) targetIds.add(clone.getTarget().getUUID());
         }
         helper.assertTrue(targetIds.size() >= 2, "Clones did not acquire targets independently; locked targets: " + targetIds.size());
         for (var clone : clones) {
            helper.assertTrue(Math.abs(clone.getMaxHealth() - rider.getMaxHealth()) < 0.001F,
               "Clone max health does not match the owner");
            helper.assertTrue(Math.abs(clone.getAttributeValue(Attributes.ATTACK_DAMAGE)
               - rider.getAttributeValue(Attributes.ATTACK_DAMAGE)) < 0.001,
               "Clone attack damage does not match the owner");
            helper.assertTrue(Math.abs(clone.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()
               - rider.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()) < 0.001,
               "Clone base movement speed does not match the owner");
            helper.assertTrue(clone.getAttributeValue(Attributes.MOVEMENT_SPEED)
               > clone.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * 2.5,
               "Clone did not retain Riding and Eight-Boat Leap movement bonuses: "
                  + clone.getAttributeValue(Attributes.MOVEMENT_SPEED) + " / "
                  + clone.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers());
            helper.assertTrue(Math.abs(clone.getCurrentMp() - 80.0) < 0.001,
               "Clone consumed MP by entering the active-skill AI path");
            if (clone.getTarget() != null) targetIds.add(clone.getTarget().getUUID());
         }
         firstTarget.kill();
      });
      helper.runAfterDelay(16, () -> {
         var clones = level.getEntitiesOfClass(UshiwakamaruRiderEntity.class,
            new AABB(rider.blockPosition()).inflate(32.0), UshiwakamaruRiderEntity::isClone);
         helper.assertTrue(clones.isEmpty(), "Eight-Boat clones remained after the original target died");
         helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void serverSnapshotContainsAllSections(GameTestHelper helper) {
      String snapshot = DefinitionSnapshotService.build();
      helper.assertTrue(snapshot.contains("\"magic\"") && snapshot.contains("\"cards\"") && snapshot.contains("\"skills\"")
         && snapshot.contains("\"noble_phantasms\"") && snapshot.contains("\"ai\""), "definition snapshot is incomplete");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void transformAndMasterContract(GameTestHelper helper) {
      var masterPlayer = helper.makeMockServerPlayerInLevel();
      var servantPlayer = helper.makeMockServerPlayerInLevel();
      var servant = TypeMoonWorldApi.servantForm(servantPlayer);
      var master = TypeMoonWorldApi.master(masterPlayer);
      helper.assertTrue(servant.transform(ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon")), "servant transform failed");
      helper.assertTrue(master.activate(), "master activation failed");
      helper.assertTrue(master.bind(servantPlayer), "master contract failed");
      helper.assertTrue(master.boundServant() != null && servant.transformed(), "contract state was not persisted");
      helper.assertTrue(servant.release(), "servant release failed");
      helper.assertTrue(master.release(), "master release failed");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void entityServantContractGuardAndPersistence(GameTestHelper helper) {
      var master = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master), "master activation failed");
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon"), helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(summoned instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity,
         "entity servant summon failed");
      var servant = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)summoned;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bindEntityServant(master, servant),
         "entity servant contract failed");
      helper.assertTrue(servant.isBoundTo(master) && servant.isAlliedTo(master), "master alliance was not established");
      CompoundTag saved = new CompoundTag();
      servant.saveWithoutId(saved);
      helper.assertTrue(saved.hasUUID("EntityMaster") && saved.getUUID("EntityMaster").equals(master.getUUID()),
         "entity master UUID was not persisted");
      servant.cycleCommandMode();
      var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 2));
      master.setLastHurtByMob(attacker);
      net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalController.tick(servant);
      helper.assertTrue(servant.getTarget() == attacker, "GUARD did not prioritize the master's attacker");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.unbindEntityServant(master, servant),
         "entity servant unbind failed");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void launchedTargetIsRememberedAndPursued(GameTestHelper helper) {
      BlockPos servantPos = helper.absolutePos(new BlockPos(2, 20, 2));
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon"), servantPos);
      helper.assertTrue(summoned instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity,
         "could not create servant for pursuit test");
      var servant = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)summoned;
      var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(42, 20, 2));
      target.setNoAi(true);
      servant.setTarget(target);
      net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTargetingService.remember(
         servant, target, helper.getLevel().getGameTime());
      servant.setTarget(null);

      helper.runAfterDelay(2, () -> {
         boolean handled = net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalController.tick(servant);
         helper.assertTrue(servant.getTarget() == target,
            "servant did not recover its recently launched combat target");
         helper.assertTrue(handled && servant.isSprinting(),
            "servant did not claim the distant pursuit movement intent");
         var followAttribute = servant.getAttribute(Attributes.FOLLOW_RANGE);
         double baseFollowRange = followAttribute == null ? 0.0 : followAttribute.getBaseValue();
         helper.assertTrue(baseFollowRange >= 96.0,
            "servant base follow range was not expanded for launched targets: " + baseFollowRange);
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void terrainLowerHemisphereProtectsUpperAndBlockEntities(GameTestHelper helper) {
      var level = helper.getLevel();
      var player = helper.makeMockServerPlayerInLevel();
      BlockPos center = helper.absolutePos(new BlockPos(4, 4, 4));
      level.setBlock(center, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
      level.setBlock(center.above(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
      level.setBlock(center.offset(1, -1, 0), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
      level.setBlock(center.offset(-1, -1, 0), net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 3);
      net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService.impact(level, player, net.minecraft.world.phys.Vec3.atCenterOf(center),
         new net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile(
            net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL, 2.5, 6.0F, 0, 0),
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService.Shape.GROUND_LOWER_HEMISPHERE);
      helper.runAfterDelay(12, () -> {
         helper.assertTrue(level.getBlockState(center).isAir(), "lower hemisphere did not remove its center block");
         helper.assertTrue(level.getBlockState(center.above()).is(net.minecraft.world.level.block.Blocks.STONE),
            "lower hemisphere removed an upper-half block");
         helper.assertTrue(level.getBlockState(center.offset(1, -1, 0)).is(net.minecraft.world.level.block.Blocks.CHEST),
            "terrain impact removed a block entity");
         helper.assertTrue(level.getBlockState(center.offset(-1, -1, 0)).is(net.minecraft.world.level.block.Blocks.BEDROCK),
            "terrain impact removed bedrock");
         helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,
            new AABB(center).inflate(8.0)).isEmpty(), "terrain debris created FallingBlockEntity instances");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void ordinarySelfImpactOnlyBreaksOneFloorLayer(GameTestHelper helper) {
      var level = helper.getLevel();
      var player = helper.makeMockServerPlayerInLevel();
      BlockPos top = helper.absolutePos(new BlockPos(5, 5, 5));
      for (int depth = 0; depth < 3; depth++) {
         level.setBlock(top.below(depth), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
      }
      player.teleportTo(top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5);
      net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService.impact(level, player,
         player.position().add(0.0, 0.2, 0.0),
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
            net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL),
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService.Shape.GROUND_LOWER_HEMISPHERE);
      helper.runAfterDelay(10, () -> {
         helper.assertTrue(level.getBlockState(top).isAir(), "ordinary self impact did not break the top floor layer");
         helper.assertTrue(level.getBlockState(top.below()).is(net.minecraft.world.level.block.Blocks.STONE),
            "ordinary self impact broke more than one block below the caster");
         helper.assertTrue(level.getBlockState(top.below(2)).is(net.minecraft.world.level.block.Blocks.STONE),
            "ordinary self impact drilled through the protected lower floor");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void mediumImpactLaunchesBoundedPhysicalBlocks(GameTestHelper helper) {
      var level = helper.getLevel();
      var player = helper.makeMockServerPlayerInLevel();
      BlockPos center = helper.absolutePos(new BlockPos(5, 6, 5));
      for (int x = -3; x <= 3; x++) {
         for (int z = -3; z <= 3; z++) {
            level.setBlock(center.offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(center.offset(x, 1, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
         }
      }
      player.teleportTo(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
      net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService.impact(level, player,
         player.position().add(0.0, 0.2, 0.0),
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.of(
            net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.MEDIUM),
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService.Shape.GROUND_LOWER_HEMISPHERE);
      helper.runAfterDelay(6, () -> {
         var physical = level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,
            new AABB(center).inflate(16.0),
            net.xxxjk.TYPE_MOON_WORLD.world.terrain.PhysicalTerrainDebrisService::isPhysicalDebris);
         helper.assertTrue(!physical.isEmpty(), "medium terrain impact launched no real falling-block debris");
         helper.assertTrue(physical.size() <= 4, "medium terrain impact exceeded its per-impact physical debris cap");
         helper.assertTrue(physical.stream().noneMatch(entity -> entity.dropItem),
            "physical terrain debris was allowed to create item drops");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 190)
   public static void eaAndExcaliburResolveBeamClash(GameTestHelper helper) {
      var level = helper.getLevel();
      var eaOwner = helper.makeMockServerPlayerInLevel();
      var excaliburOwner = helper.makeMockServerPlayerInLevel();
      eaOwner.teleportTo(helper.absolutePos(new BlockPos(2, 100, 5)).getCenter().x,
         helper.absolutePos(new BlockPos(2, 100, 5)).getY(), helper.absolutePos(new BlockPos(2, 100, 5)).getCenter().z);
      excaliburOwner.teleportTo(helper.absolutePos(new BlockPos(26, 100, 5)).getCenter().x,
         helper.absolutePos(new BlockPos(26, 100, 5)).getY(), helper.absolutePos(new BlockPos(26, 100, 5)).getCenter().z);
      eaOwner.setYRot(-90.0F);
      eaOwner.setYHeadRot(-90.0F);
      excaliburOwner.setYRot(90.0F);
      excaliburOwner.setYHeadRot(90.0F);
      var eaMana = eaOwner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      eaMana.player_mana = 10000.0;
      eaMana.servant_card_mana = 1000.0;
      eaMana.servant_card_max_mana = 1000.0;
      var excaliburMana = excaliburOwner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      excaliburMana.servant_card_mana = 1000.0;
      excaliburMana.servant_card_max_mana = 1000.0;

      var ea = new net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity(level, eaOwner, new net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0));
      ea.requestRelease(net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity.WIND_TICKS);
      level.addFreshEntity(ea);
      var excalibur = new java.util.concurrent.atomic.AtomicReference<net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity>();
      helper.runAfterDelay(80, () -> {
         var beam = new net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity(
            level, excaliburOwner, excaliburOwner.getEyePosition(), 220, 0, 1.0F);
         excalibur.set(beam);
         level.addFreshEntity(beam);
      });
      helper.runAfterDelay(110, () -> {
         helper.assertTrue(excalibur.get() != null && ea.isClashing() && excalibur.get().isClashing(),
            "EA and Excalibur did not enter the shared beam clash state");
      });
      helper.runAfterDelay(175, () -> {
         helper.assertTrue(!ea.isAlive(), "low-charge EA survived a full-power Excalibur clash");
         helper.assertTrue(excalibur.get() != null && excalibur.get().isAlive() && !excalibur.get().isClashing(),
            "Excalibur did not continue with residual output after winning the clash");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void servantSingleAndDoubleBackJumpRespectAirLimit(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos center = helper.absolutePos(new BlockPos(5, 2, 5));
      for (int x = -5; x <= 5; x++) {
         for (int z = -5; z <= 5; z++) {
            BlockPos floor = center.offset(x, -1, z);
            level.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(floor.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(floor.above(2), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(floor.above(3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
         }
      }
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon"),
         center);
      helper.assertTrue(summoned instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity,
         "could not create servant for evasion test");
      var servant = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)summoned;
      servant.setNoAi(true);
      helper.runAfterDelay(2, () -> {
         servant.moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.0F, 0.0F);
         servant.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
         servant.setOnGround(true);
         servant.getPersistentData().putLong("TypeMoonAiLastEvasionTick", -100L);
         var threat = servant.position().add(0.0, 0.0, -3.0);
         boolean first = net.xxxjk.TYPE_MOON_WORLD.combat.ai.EvasionMovementService.tryEvade(servant, threat, 3, false);
         helper.assertTrue(first && servant.getDeltaMovement().y > 0.0, "C-rank servant did not perform its ground back jump");
         servant.setPos(servant.getX(), center.getY() + 3.0, servant.getZ());
         servant.setOnGround(false);
         servant.getPersistentData().putLong("TypeMoonAiLastEvasionTick", -100L);
         boolean second = net.xxxjk.TYPE_MOON_WORLD.combat.ai.EvasionMovementService.tryEvade(servant, threat, 5, false);
         helper.assertTrue(second && servant.getPersistentData().getBoolean("TypeMoonAiDoubleJumpUsed"),
            "A-rank airborne servant did not perform its second jump");
         servant.getPersistentData().putLong("TypeMoonAiLastEvasionTick", -100L);
         helper.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.combat.ai.EvasionMovementService.tryEvade(servant, threat, 5, false),
            "servant performed more than one second jump in the same airtime");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 100)
   public static void fiftyCombatNpcArbitrationStress(GameTestHelper helper) {
      var level = helper.getLevel();
      var target = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(6, 2, 6));
      target.setNoAi(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000.0);
      target.setHealth(100000.0F);
      int spawned = 0;
      for (int i = 0; i < 50; i++) {
         BlockPos relative = new BlockPos(1 + i % 10, 2, 1 + i / 10);
         var entity = TypeMoonWorldApi.addon("typemoonworld").servants().summon(level,
            ResourceLocation.fromNamespaceAndPath("typemoonworld", i % 2 == 0 ? "artoria_pendragon" : "emiya_archer"),
            helper.absolutePos(relative));
         if (entity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant) {
            servant.setTarget(target);
            spawned++;
         }
      }
      helper.assertTrue(spawned == 50, "could not spawn the 50-NPC stress group");
      helper.runAfterDelay(30, () -> {
         var servants = level.getEntitiesOfClass(net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity.class,
            new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48.0));
         helper.assertTrue(servants.size() >= 50, "combat arbitration lost NPCs during stress run: " + servants.size());
         var metrics = net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction.lastMetrics(level);
         helper.assertTrue(metrics.checkedBlocks() <= net.xxxjk.TYPE_MOON_WORLD.Config.terrainChecksPerTick,
            "terrain queue exceeded its voxel check cap: " + metrics.checkedBlocks());
         long physicalDebris = level.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,
            new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48.0),
            net.xxxjk.TYPE_MOON_WORLD.world.terrain.PhysicalTerrainDebrisService::isPhysicalDebris).size();
         helper.assertTrue(physicalDebris <= net.xxxjk.TYPE_MOON_WORLD.world.terrain.PhysicalTerrainDebrisService.maxActivePerDimension(),
            "physical terrain debris exceeded its dimension cap: " + physicalDebris);
         helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void heraclesAndGawainWalkingDoNotBreakGround(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos heraclesFloor = helper.absolutePos(new BlockPos(3, 1, 3));
      BlockPos gawainFloor = helper.absolutePos(new BlockPos(9, 1, 3));
      for (int x = -2; x <= 2; x++) {
         for (int z = -2; z <= 2; z++) {
            level.setBlock(heraclesFloor.offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(gawainFloor.offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
         }
      }
      var heracles = ModEntities.HERACLES.get().create(level);
      var gawain = ModEntities.GAWAIN.get().create(level);
      helper.assertTrue(heracles != null && gawain != null, "could not create collision-test servants");
      heracles.setNoAi(true);
      gawain.setNoAi(true);
      heracles.moveTo(heraclesFloor.getX() + 0.5, heraclesFloor.getY() + 1.0, heraclesFloor.getZ() + 0.5, -90.0F, 0.0F);
      gawain.moveTo(gawainFloor.getX() + 0.5, gawainFloor.getY() + 1.0, gawainFloor.getZ() + 0.5, -90.0F, 0.0F);
      level.addFreshEntity(heracles);
      level.addFreshEntity(gawain);
      heracles.setOnGround(true);
      gawain.setOnGround(true);
      heracles.horizontalCollision = false;
      gawain.horizontalCollision = false;
      heracles.setDeltaMovement(0.32, 0.0, 0.0);
      gawain.setDeltaMovement(0.32, 0.0, 0.0);
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper.tickNpcSprintCollision(heracles);
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantSprintCollisionHelper.tickNpcSprintCollision(gawain);
      heracles.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
      gawain.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

      helper.runAfterDelay(12, () -> {
         for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
               helper.assertTrue(level.getBlockState(heraclesFloor.offset(x, 0, z)).is(net.minecraft.world.level.block.Blocks.STONE),
                  "Heracles walking damaged the ground");
               helper.assertTrue(level.getBlockState(gawainFloor.offset(x, 0, z)).is(net.minecraft.world.level.block.Blocks.STONE),
                  "Gawain walking damaged the ground");
            }
         }
         helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void magicProgressAndPresetValidation(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "magic_bullet");
      var magic = TypeMoonWorldApi.addon("typemoonworld").magics();
      helper.assertTrue(magic.knowledge(player).learn(id), "magic learning failed");
      magic.knowledge(player).setProficiency(id, 250);
      helper.assertTrue(magic.knowledge(player).proficiency(id) == 100.0, "proficiency was not clamped");
      CompoundTag hostile = new CompoundTag();
      for (int i = 0; i < 80; i++) hostile.putString("field_" + i, "x".repeat(200));
      helper.assertTrue(MagicPresetRegistry.normalize(id.toString(), hostile).payload().isEmpty(), "oversized preset was accepted");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void imaginaryAttributeFacadeReadsExistingState(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.player_magic_attributes_imaginary_number = true;
      var attributes = TypeMoonWorldApi.magicAttributes(player);
      helper.assertTrue(attributes.has(MagicAttributes.IMAGINARY_NUMBER), "imaginary-number attribute was not exposed");
      helper.assertTrue(attributes.attributes().contains(MagicAttributes.IMAGINARY_NUMBER), "attribute snapshot was incomplete");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void heraclesCardPrimaryAttackDealsDamage(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(TypeMoonWorldApi.servantForm(player).transform(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "heracles")), "Heracles transform failed");
      helper.assertTrue(player.getMainHandItem().is(ModItems.TEMPLE_STONE_SWORD_AXE.get()), "Heracles weapon was not equipped");
      BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 2));
      player.teleportTo(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
      player.setYRot(-90.0F);
      var target = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 2));
      target.setHealth(target.getMaxHealth());
      float before = target.getHealth();
      helper.runAfterDelay(1, () -> {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHeraclesSkills.performBasicSweep(player);
         float afterFirst = target.getHealth();
         helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) > 0.0,
            "Heracles attack attribute was not positive");
         helper.assertTrue(afterFirst < before,
            "Heracles primary sweep dealt no damage; attack=" + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHeraclesSkills.performBasicSweep(player);
         helper.assertTrue(target.getHealth() == afterFirst,
            "Heracles primary sweep ignored its 8-tick cooldown");
         helper.succeed();
      });
   }
}

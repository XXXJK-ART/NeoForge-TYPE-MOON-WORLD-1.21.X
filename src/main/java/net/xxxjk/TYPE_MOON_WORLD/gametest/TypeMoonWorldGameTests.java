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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicPresetRegistry;
import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.CursedArmorService;
import com.example.typemoonaddon.magic.MatouSakuraMasterProfile;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.xxxjk.TYPE_MOON_WORLD.entity.ContenderBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicDisplayMetadata;

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
      firstTarget.setInvulnerable(true);
      secondTarget.setInvulnerable(true);
      firstTarget.setPersistenceRequired();
      secondTarget.setPersistenceRequired();
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
               + ", last=" + rider.getPersistentData().getLong("UshiwakamaruLastEightBoat")
               + ", tactical=" + rider.wasTacticalAiHandledThisTick()
               + ", noAi=" + rider.isNoAi()
               + ", selected=" + rider.getPersistentData().getString("TypeMoonAiSelectedAction")
               + ", aiPhase=" + rider.getPersistentData().getString("TypeMoonAiCombatPhase")
               + ", memory=" + rider.getPersistentData().hasUUID("ServantCombatTargetMemory")
               + ", riderPos=" + rider.position() + ", targetPos=" + firstTarget.position()
               + ", targetAlive=" + firstTarget.isAlive());
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
      masterPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_active = true;
      helper.assertTrue(master.bind(servantPlayer), "master contract failed");
      helper.assertTrue(master.boundServant() != null && servant.transformed(), "contract state was not persisted");
      helper.assertTrue(servant.release(), "servant release failed");
      helper.assertTrue(master.release(), "master release failed");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 60)
   public static void zhaoYunCardHakuryuAcceptsLinkedMasterSecondSeat(GameTestHelper helper) {
      var master = helper.makeMockServerPlayerInLevel();
      var servant = helper.makeMockServerPlayerInLevel();
      var outsider = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "zhao_yun_rider"),
         "Zhao Yun servant-card transform failed");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master),
         "master activation failed");
      master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_active = true;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bind(master, servant),
         "master contract failed");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardZhaoYunSkills.summonSkillHakuryu(servant),
         "Zhao Yun skill Hakuryu was not summoned");
      helper.assertTrue(servant.getVehicle() instanceof net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity,
         "Zhao Yun card owner did not mount Hakuryu");
      var mount = (net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity)servant.getVehicle();

      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun.ZhaoYunHakuryuRideService.tryToggle(master, mount),
         "linked master could not board Hakuryu's second seat");
      helper.assertTrue(mount.getPassengers().size() == 2
            && mount.getPassengers().get(0) == servant
            && mount.getPassengers().get(1) == master,
         "Hakuryu passenger order was not card owner then linked master");
      helper.assertTrue(!mount.canPlayerMount(outsider)
            && !net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun.ZhaoYunHakuryuRideService.tryToggle(outsider, mount),
         "outsider was allowed onto Hakuryu");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.zhaoyun.ZhaoYunHakuryuRideService.tryToggle(master, mount),
         "linked master could not dismount Hakuryu");
      helper.assertTrue(master.getVehicle() == null && servant.getVehicle() == mount,
         "linked master dismount changed the owner seat");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void terminatedPlayerContractDoesNotStartMasterLoss(GameTestHelper helper) {
      var master = helper.makeMockServerPlayerInLevel();
      var servant = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "artoria_pendragon"),
         "servant transform failed");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master), "master activation failed");
      master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_active = true;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bind(master, servant), "contract failed");
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.terminateContract(master, servant);
      var masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      helper.assertTrue(masterVars.master_servant_uuid.isEmpty() && servantVars.servant_card_master_uuid.isEmpty(),
         "contract UUIDs were not cleared symmetrically");
      helper.assertTrue("none".equals(servantVars.master_servant_survival_state)
         && masterVars.master_servant_backlash_ticks == 0, "normal termination incorrectly caused death state");
      helper.assertTrue("masterless".equals(servantVars.servant_card_contract_state),
         "terminated contract did not leave a masterless servant");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void servantManaRegenUsesThreeContractStates(GameTestHelper helper) {
      var master = helper.makeMockServerPlayerInLevel();
      var servant = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "artoria_pendragon"),
         "servant transform failed");
      var servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      helper.assertTrue("native".equals(servantVars.servant_card_contract_state),
         "fresh servant card was not native");
      servantVars.servant_card_mana = servantVars.servant_card_max_mana - 20.0;
      double nativeMana = servantVars.servant_card_mana;
      net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService.tick(servant, servantVars);
      helper.assertTrue(servantVars.servant_card_mana > nativeMana,
         "native servant did not regenerate its own mana");

      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master),
         "master activation failed");
      var masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      masterVars.master_card_active = true;
      masterVars.player_mana_egenerated_every_moment = 10.0;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bind(master, servant),
         "contract failed");
      servantVars.servant_card_mana = servantVars.servant_card_max_mana - 20.0;
      double contractedMana = servantVars.servant_card_mana;
      net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService.tick(servant, servantVars);
      helper.assertTrue("contracted".equals(servantVars.servant_card_contract_state)
         && servantVars.servant_card_mana > contractedMana,
         "contracted servant did not use in-range master mana regeneration");

      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.terminateContract(master, servant);
      servantVars.servant_card_mana = servantVars.servant_card_max_mana - 20.0;
      double masterlessMana = servantVars.servant_card_mana;
      for (int i = 0; i < 20; i++) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService.tick(servant, servantVars);
      }
      helper.assertTrue("masterless".equals(servantVars.servant_card_contract_state)
         && Math.abs(servantVars.servant_card_mana - masterlessMana) < 1.0E-9,
         "masterless servant regenerated passive mana");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void servantCardCanRestoreMpFromManaMedia(GameTestHelper helper) {
      var servant = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "artoria_pendragon"),
         "servant transform failed");
      var vars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_mana = 0.0;
      servant.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.MAGIC_FRAGMENTS.get()));
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService.restoreFromInventory(servant, vars),
         "servant card did not consume mana media");
      helper.assertTrue(Math.abs(vars.servant_card_mana - 10.0) < 1.0E-9
         && servant.getInventory().countItem(ModItems.MAGIC_FRAGMENTS.get()) == 0,
         "mana media did not restore the servant card MP pool");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 80)
   public static void masterCanHealFromVanillaFood(GameTestHelper helper) {
      var master = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master),
         "master activation failed");
      master.getFoodData().setFoodLevel(20);
      master.getFoodData().setSaturation(5.0F);
      master.setHealth(master.getMaxHealth() - 10.0F);
      float damagedHealth = master.getHealth();
      var naturalRegen = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION);
      boolean previousNaturalRegen = naturalRegen.get();
      try {
         naturalRegen.set(true, helper.getLevel().getServer());
         for (int i = 0; i < 40; i++) master.getFoodData().tick(master);
         helper.assertTrue(master.getFoodData().getFoodLevel() >= 18 && master.getHealth() > damagedHealth,
            "master hunger was locked or vanilla food regeneration did not heal");
      } finally {
         naturalRegen.set(previousNaturalRegen, helper.getLevel().getServer());
      }
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void masterLossForcesHeraclesDeathOnTick200(GameTestHelper helper) {
      var master = helper.makeMockServerPlayerInLevel();
      var servant = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "heracles"),
         "Heracles transform failed");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master), "master activation failed");
      master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_active = true;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bind(master, servant), "contract failed");
      var masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.onMasterLost(master, masterVars);
      helper.assertTrue("forced_death".equals(servantVars.master_servant_survival_state),
         "servant without Independent Action did not enter forced death");
      for (int i = 0; i < 199; i++) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.tick(servant, servantVars);
      }
      helper.assertTrue(servant.isAlive() && servant.getPersistentData().getInt("GodHandLives") > 0,
         "Heracles died or spent God Hand before tick 200");
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.tick(servant, servantVars);
      helper.assertTrue(!servant.isAlive() && servant.getPersistentData().getInt("GodHandLives") == 0,
         "forced death was prevented by God Hand");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void independentActionAndDistanceKeepContractRules(GameTestHelper helper) {
      var firstMaster = helper.makeMockServerPlayerInLevel();
      var servant = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "gilgamesh"),
         "Gilgamesh transform failed");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(firstMaster), "master activation failed");
      firstMaster.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_active = true;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bind(firstMaster, servant), "contract failed");
      var firstMasterVars = firstMaster.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      float health = servant.getHealth();
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.onMasterLost(firstMaster, firstMasterVars);
      for (int i = 0; i < 100; i++) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.tick(servant, servantVars);
      }
      helper.assertTrue("independent_action".equals(servantVars.master_servant_survival_state)
         && servant.getHealth() == health, "Independent Action grace damaged the servant");

      var secondMaster = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(secondMaster), "second master activation failed");
      secondMaster.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_active = true;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bind(secondMaster, servant), "recontract failed");
      helper.assertTrue("none".equals(servantVars.master_servant_survival_state), "recontract did not cancel survival timer");
      var secondMasterVars = secondMaster.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      servant.setPos(secondMaster.getX() + 200.0, secondMaster.getY(), secondMaster.getZ());
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.tick(servant, servantVars);
      helper.assertTrue(servantVars.master_servant_master_position_valid
         && servantVars.master_servant_master_position_online
         && servantVars.master_servant_master_dimension.equals(secondMaster.level().dimension().location().toString())
         && Math.abs(servantVars.master_servant_master_x - secondMaster.getX()) < 0.001,
         "servant did not retain a live master position sense at long range");
      helper.assertTrue(servant.getUUID().toString().equals(secondMasterVars.master_servant_uuid)
         && secondMaster.getUUID().toString().equals(servantVars.servant_card_master_uuid),
         "long distance broke the contract UUIDs");
      helper.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.canUseMasterMana(
         servant, servantVars, secondMaster) && servant.getHealth() == health,
         "long distance still supplied mana or changed health");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void independentActionExpiryStartsUnavoidableTenSecondDeath(GameTestHelper helper) {
      var servant = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "gilgamesh"),
         "Independent Action servant transform failed");
      var vars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.master_servant_survival_state = "independent_action";
      vars.master_servant_survival_ticks = 1;
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.tick(servant, vars);
      helper.assertTrue("forced_death".equals(vars.master_servant_survival_state)
         && vars.master_servant_survival_ticks == 200 && servant.isAlive(),
         "Independent Action expiry did not begin a fresh 200-tick death countdown");
      for (int i = 0; i < 199; i++) {
         net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.tick(servant, vars);
      }
      helper.assertTrue(servant.isAlive(), "Independent Action servant died before countdown tick 200");
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.tick(servant, vars);
      helper.assertTrue(!servant.isAlive(), "Independent Action servant survived countdown tick 200");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void playerCardContractsRejectSameRolesAndAllowEitherDirection(GameTestHelper helper) {
      var master = helper.makeMockServerPlayerInLevel();
      var otherMaster = helper.makeMockServerPlayerInLevel();
      var servant = helper.makeMockServerPlayerInLevel();
      var otherServant = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master), "master activation failed");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(otherMaster), "second master activation failed");
      master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_active = true;
      otherMaster.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_card_active = true;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "artoria_pendragon"),
         "servant transform failed");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(otherServant, "medusa"),
         "second servant transform failed");
      helper.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bindByContract(master, otherMaster),
         "Master Card to Master Card contract was accepted");
      helper.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bindByContract(servant, otherServant),
         "Servant Card to Servant Card contract was accepted");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bindByContract(servant, master),
         "Servant Card to Master Card interaction direction was rejected");
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.onServantLost(
         servant, servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      helper.assertTrue(master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_servant_backlash_ticks == 1200,
         "master did not receive the servant-death sensation backlash");
      helper.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(master, "medea"),
         "Servant Card activated while Master Card was active");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void servantOutOfCombatHealingUsesOwnMana(GameTestHelper helper) {
      var servant = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "artoria_pendragon"),
         "servant transform failed");
      var vars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      servant.setHealth(servant.getMaxHealth() - 20.0F);
      vars.servant_card_mana = 50.0;
      vars.servant_card_last_combat_tick = servant.level().getGameTime() - 200L;
      servant.tickCount = 20;
      float beforeHealth = servant.getHealth();
      net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHealthService.tick(servant, vars);
      helper.assertTrue(Math.abs(vars.servant_card_mana - 49.0) < 0.001,
         "out-of-combat heal did not consume exactly one servant mana");
      helper.assertTrue(Math.abs(servant.getHealth() - (beforeHealth + servant.getMaxHealth() * 0.005F)) < 0.01F,
         "out-of-combat heal amount was not 0.5% max health");

      servant.invulnerableTime = 0;
      boolean damageApplied = servant.hurt(servant.damageSources().genericKill(), 1.0F);
      helper.assertTrue(damageApplied && vars.servant_card_last_combat_tick == servant.level().getGameTime(),
         "final applied damage was not recorded as combat");
      double manaAfterDamage = vars.servant_card_mana;
      float healthAfterDamage = servant.getHealth();
      net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHealthService.tick(servant, vars);
      helper.assertTrue(Math.abs(vars.servant_card_mana - manaAfterDamage) < 0.001
         && Math.abs(servant.getHealth() - healthAfterDamage) < 0.001F,
         "new effective damage did not reset the combat timer");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void servantFoodSnapshotRestoresAndMasterFoodIsUntouched(GameTestHelper helper) {
      var servant = helper.makeMockServerPlayerInLevel();
      servant.getFoodData().setFoodLevel(7);
      servant.getFoodData().setSaturation(2.0F);
      servant.getFoodData().setExhaustion(3.0F);
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.transform(servant, "artoria_pendragon"),
         "servant transform failed");
      var servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.tick(servant, servantVars);
      helper.assertTrue(servant.getFoodData().getFoodLevel() == 17 && servant.getFoodData().getSaturationLevel() == 0.0F,
         "servant form did not suppress vanilla hunger healing");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.release(servant, false),
         "servant release failed");
      helper.assertTrue(servant.getFoodData().getFoodLevel() == 7
         && servant.getFoodData().getSaturationLevel() == 2.0F
         && servant.getFoodData().getExhaustionLevel() == 3.0F,
         "servant release did not restore the food snapshot");

      var master = helper.makeMockServerPlayerInLevel();
      master.getFoodData().setFoodLevel(6);
      master.getFoodData().setSaturation(1.0F);
      master.getFoodData().setExhaustion(2.0F);
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master), "master activation failed");
      var masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.tick(master, masterVars);
      helper.assertTrue(master.getFoodData().getFoodLevel() == 6
         && master.getFoodData().getSaturationLevel() == 1.0F
         && master.getFoodData().getExhaustionLevel() == 2.0F,
         "master tick still normalizes hunger");
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
      helper.assertTrue(servant.getCommandMode() == net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode.FOLLOW,
         "entity servant did not start in FOLLOW mode");
      helper.assertTrue(servant.cycleCommandMode() == net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode.GUARD,
         "entity servant could not switch to GUARD mode");
      helper.assertTrue(servant.getCommandMode() == net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode.GUARD,
         "GUARD mode was not retained");
      var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 2));
      master.setLastHurtByMob(attacker);
      net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalController.tick(servant);
      helper.assertTrue(servant.getTarget() == attacker, "GUARD did not prioritize the master's attacker");
      helper.assertTrue(servant.cycleCommandMode() == net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode.STAY,
         "entity servant could not switch to STAY mode");
      helper.assertTrue(servant.getCommandMode() == net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantCommandMode.STAY,
         "STAY mode was not retained");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.unbindEntityServant(master, servant),
         "entity servant unbind failed");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void entityServantDeathIsFeltByMaster(GameTestHelper helper) {
      var master = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.activate(master), "master activation failed");
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(helper.getLevel(),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon"), helper.absolutePos(new BlockPos(2, 2, 2)));
      helper.assertTrue(summoned instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity,
         "entity servant summon failed");
      var servant = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)summoned;
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager.bindEntityServant(master, servant),
         "entity servant contract failed");
      servant.die(servant.damageSources().genericKill());
      var vars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      helper.assertTrue(vars.master_servant_uuid.isEmpty() && vars.master_servant_backlash_ticks == 1200,
         "entity servant death did not break the link and notify the master");
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

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void combatMotionTracksLaunchPursuitAndControlDecay(GameTestHelper helper) {
      var level = helper.getLevel();
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(level,
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon"),
         helper.absolutePos(new BlockPos(2, 20, 2)));
      helper.assertTrue(summoned instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity,
         "could not create servant for combat-motion test");
      var servant = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)summoned;
      servant.setNoAi(true);
      var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 20, 2));
      target.setNoAi(true);

      var first = net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.launch(
         servant, target, new net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0), 1.2, 0.5,
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL, 24);
      helper.assertTrue(first.applied() && first.controlDepth() == 1,
         "first launch was not registered as a fresh control action");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.isLaunched(target),
         "launched state was not stored on the target");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.canPursue(servant, target),
         "launch did not open an attacker-bound pursuit window");

      var second = net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.launch(
         servant, target, new net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0), 1.2, 0.5,
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL, 24);
      helper.assertTrue(second.applied() && second.controlDepth() == 2,
         "consecutive launch did not increase control depth");
      helper.assertTrue(Math.abs(second.controlScale() - 0.72) < 0.0001,
         "second launch did not apply the expected control decay: " + second.controlScale());
      helper.assertTrue(second.horizontalPower() < first.horizontalPower()
            && second.verticalPower() < first.verticalPower(),
         "control decay did not reduce the repeated launch force");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 80)
   public static void combatMotionResolvesARealWallImpactAfterFlight(GameTestHelper helper) {
      var level = helper.getLevel();
      BlockPos attackerPos = helper.absolutePos(new BlockPos(2, 20, 2));
      BlockPos wall = helper.absolutePos(new BlockPos(8, 20, 2));
      for (int x = 4; x <= 8; x++) {
         for (int z = 1; z <= 3; z++) {
            level.setBlock(helper.absolutePos(new BlockPos(x, 19, z)),
               net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
         }
      }
      for (int y = 0; y < 3; y++) {
         level.setBlock(wall.above(y), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
      }
      var summoned = TypeMoonWorldApi.addon("typemoonworld").servants().summon(level,
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "artoria_pendragon"), attackerPos);
      helper.assertTrue(summoned instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity,
         "could not create wall-impact attacker");
      var attacker = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)summoned;
      attacker.setNoAi(true);
      var targetEntity = TypeMoonWorldApi.addon("typemoonworld").servants().summon(level,
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "emiya_archer"),
         helper.absolutePos(new BlockPos(5, 20, 2)));
      helper.assertTrue(targetEntity instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity,
         "could not create wall-impact servant target");
      var target = (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity)targetEntity;
      var launch = net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.launch(
         attacker, target, new net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0), 2.6, 0.35,
         net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.MEDIUM, 24);
      helper.assertTrue(launch.applied(), "wall-impact launch was rejected");
      helper.assertTrue(level.getBlockState(wall).is(net.minecraft.world.level.block.Blocks.STONE),
         "launch call damaged a wall before flight");
      helper.runAfterDelay(12, () -> {
         helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.state(target)
               == net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.MotionState.WALL_STAGGER,
            "target did not enter wall stagger after a real swept collision: state="
               + net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.state(target)
               + ", position=" + target.position() + ", motion=" + target.getDeltaMovement());
         helper.assertTrue(!level.getBlockState(wall).is(net.minecraft.world.level.block.Blocks.STONE),
            "real wall impact did not damage the collision surface");
         helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService.canPursue(attacker, target),
            "wall impact removed the attacker's pursuit window");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void allServantsLoadDedicatedBoundedTacticalProfiles(GameTestHelper helper) {
      var profileIds = new java.util.HashSet<String>();
      var definitions = ServantDataRegistry.getAll();
      helper.assertTrue(definitions.size() == 20,
         "expected 20 servant definitions, found " + definitions.size());
      for (var definition : definitions.values()) {
         String profileId = definition.aiConfigId();
         var profile = ServantAiDefinitionRegistry.get(profileId);
         helper.assertTrue(profile != null,
            "missing tactical profile for " + definition.id() + ": " + profileId);
         helper.assertTrue(profileIds.add(profileId),
            "servants share a tactical profile instead of using dedicated behavior: " + profileId);
         var tactical = profile.tactical();
         helper.assertTrue(tactical.minimumRange() <= tactical.preferredRange()
               && tactical.preferredRange() <= tactical.maximumRange(),
            "invalid tactical range order for " + profileId);
         helper.assertTrue(tactical.maximumRange() <= 48.0,
            "tactical range exceeds the 48-block normal-combat limit for " + profileId);
      }
      helper.assertTrue(profileIds.size() == 20,
         "not all servants loaded an independent tactical profile");
      helper.succeed();
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
      eaMana.player_max_mana = 10000.0;
      eaMana.servant_card_mana = 1000.0;
      eaMana.servant_card_max_mana = 1000.0;
      var excaliburMana = excaliburOwner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      excaliburMana.player_mana = 1000.0;
      excaliburMana.player_max_mana = 1000.0;
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

   @GameTest(template = "ancient_temple", timeoutTicks = 210)
   public static void fullEaOpeningDamageGapBeatsEqualManaExcalibur(GameTestHelper helper) {
      var level = helper.getLevel();
      var eaOwner = helper.makeMockServerPlayerInLevel();
      var excaliburOwner = helper.makeMockServerPlayerInLevel();
      placeBeamClashOwners(helper, eaOwner, excaliburOwner);
      setPlayerMana(eaOwner, 2000.0, 2000.0);
      setPlayerMana(excaliburOwner, 1000.0, 1000.0);

      var ea = new net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity(level, eaOwner, new net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0));
      level.addFreshEntity(ea);
      var excalibur = new java.util.concurrent.atomic.AtomicReference<net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity>();
      helper.runAfterDelay(145, () -> ea.requestRelease(
         net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity.WIND_TICKS
            + net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity.ORB_CHARGE_TICKS));
      helper.runAfterDelay(160, () -> {
         setPlayerMana(eaOwner, 1000.0, 1000.0);
         setPlayerMana(excaliburOwner, 1000.0, 1000.0);
         var beam = new net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity(
            level, excaliburOwner, excaliburOwner.getEyePosition(), 220, 0, 1.0F);
         excalibur.set(beam);
         level.addFreshEntity(beam);
      });
      helper.runAfterDelay(178, () -> {
         helper.assertTrue(ea.isAlive() && !ea.isClashing(), "full EA did not survive the opening damage gap");
         helper.assertTrue(excalibur.get() != null && !excalibur.get().isAlive(),
            "equal-mana Excalibur survived after paying EA's 1000 damage gap");
         helper.assertTrue(excaliburOwner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana <= 0.001,
            "Excalibur owner did not spend all mana on the opening damage gap");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 210)
   public static void excaliburWithExtraManaContinuesAfterOpeningDamageGap(GameTestHelper helper) {
      var level = helper.getLevel();
      var eaOwner = helper.makeMockServerPlayerInLevel();
      var excaliburOwner = helper.makeMockServerPlayerInLevel();
      placeBeamClashOwners(helper, eaOwner, excaliburOwner);
      setPlayerMana(eaOwner, 2000.0, 2000.0);
      setPlayerMana(excaliburOwner, 1600.0, 1600.0);

      var ea = new net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity(level, eaOwner, new net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0));
      level.addFreshEntity(ea);
      var excalibur = new java.util.concurrent.atomic.AtomicReference<net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity>();
      helper.runAfterDelay(145, () -> ea.requestRelease(
         net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity.WIND_TICKS
            + net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity.ORB_CHARGE_TICKS));
      helper.runAfterDelay(160, () -> {
         setPlayerMana(eaOwner, 1000.0, 1000.0);
         setPlayerMana(excaliburOwner, 1600.0, 1600.0);
         var beam = new net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity(
            level, excaliburOwner, excaliburOwner.getEyePosition(), 220, 0, 1.0F);
         excalibur.set(beam);
         level.addFreshEntity(beam);
      });
      helper.runAfterDelay(172, () -> {
         double excaliburMana = excaliburOwner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana;
         helper.assertTrue(ea.isClashing() && excalibur.get() != null && excalibur.get().isClashing(),
            "extra-mana Excalibur did not enter the sustained clash after paying the opening gap");
         helper.assertTrue(excaliburMana > 0.001 && excaliburMana < 700.0,
            "Excalibur owner did not pay the opening damage gap before the sustained contest");
         helper.succeed();
      });
   }

   private static void placeBeamClashOwners(GameTestHelper helper, ServerPlayer eaOwner, ServerPlayer excaliburOwner) {
      eaOwner.teleportTo(helper.absolutePos(new BlockPos(2, 100, 5)).getCenter().x,
         helper.absolutePos(new BlockPos(2, 100, 5)).getY(), helper.absolutePos(new BlockPos(2, 100, 5)).getCenter().z);
      excaliburOwner.teleportTo(helper.absolutePos(new BlockPos(26, 100, 5)).getCenter().x,
         helper.absolutePos(new BlockPos(26, 100, 5)).getY(), helper.absolutePos(new BlockPos(26, 100, 5)).getCenter().z);
      eaOwner.setYRot(-90.0F);
      eaOwner.setYHeadRot(-90.0F);
      excaliburOwner.setYRot(90.0F);
      excaliburOwner.setYHeadRot(90.0F);
   }

   private static void setPlayerMana(ServerPlayer player, double current, double maximum) {
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.player_mana = current;
      vars.player_max_mana = maximum;
      vars.servant_card_mana = current;
      vars.servant_card_max_mana = maximum;
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

   @GameTest(template = "ancient_temple", timeoutTicks = 480)
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
      helper.runAfterDelay(420, () -> {
         var servants = level.getEntitiesOfClass(net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity.class,
            new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48.0));
         helper.assertTrue(servants.size() >= 50, "combat arbitration lost NPCs during stress run: " + servants.size());
         long maxDisconnected = servants.stream().mapToLong(servant ->
            net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService.disconnectedTicks(
               servant, level.getGameTime())).max().orElse(0L);
         helper.assertTrue(maxDisconnected <= net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService.MAX_DISCONNECTED_TICKS,
            "combat tempo exceeded the no-contact deadline: " + maxDisconnected);
         helper.assertTrue(servants.stream().noneMatch(servant -> servant.getY() > target.getY() + 16.0),
            "combat stress group accumulated an invalid flight height");
         var metrics = net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction.lastMetrics(level);
         helper.assertTrue(metrics.checkedBlocks() <= net.xxxjk.TYPE_MOON_WORLD.Config.terrainChecksPerTick,
            "terrain queue exceeded its voxel check cap: " + metrics.checkedBlocks());
         helper.assertTrue(metrics.queuedJobs()
               <= net.xxxjk.TYPE_MOON_WORLD.world.terrain.DeferredTerrainDestruction.maximumQueuedJobsPerDimension(),
            "terrain queue exceeded its per-dimension job cap: " + metrics.queuedJobs());
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

    @GameTest(template = "ancient_temple", timeoutTicks = 20)
    public static void sakuraMagicCannotEnterCrest(GameTestHelper helper) {
       helper.assertTrue(!MagicDisplayMetadata.canEnterMagicCrest(SakuraTypeMoonIntegration.IMAGINARY_STORAGE.toString()),
          "Sakura base magic still enters crest");
       helper.assertTrue(!MagicDisplayMetadata.canEnterMagicCrest(SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION.toString()),
          "Sakura evolved magic still enters crest");
       helper.assertTrue(!MagicDisplayMetadata.canEnterMagicCrest(SakuraTypeMoonIntegration.SHADOW_MATERIALIZATION.toString()),
          "Sakura shadow magic still enters crest");
       helper.assertTrue(!MagicDisplayMetadata.canEnterMagicCrest(SakuraTypeMoonIntegration.SHADOW_ART.toString()),
          "Sakura black-art magic still enters crest");
       helper.succeed();
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 20)
    public static void alterBlackSakuraMigratesBaseMagic(GameTestHelper helper) {
       var player = helper.makeMockServerPlayerInLevel();
       var data = prepareBlackSakuraState(player, MatouSakuraMasterProfile.Variant.ALTER);
       var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
       vars.learned_magics.add(SakuraTypeMoonIntegration.IMAGINARY_STORAGE.toString());
       TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(0, 0);
       slot.sourceType = "self";
       slot.magicId = SakuraTypeMoonIntegration.IMAGINARY_STORAGE.toString();
       vars.setWheelSlotEntry(0, 0, slot);

       helper.assertTrue(SakuraTypeMoonIntegration.normalizeBlackSakuraLoadout(player, data),
          "ALTER Sakura loadout did not normalize");
       helper.assertTrue(vars.learned_magics.contains(SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION.toString()),
          "ALTER Sakura did not unlock evolved magic");
       helper.assertTrue(!vars.learned_magics.contains(SakuraTypeMoonIntegration.IMAGINARY_STORAGE.toString()),
          "ALTER Sakura kept the base magic alongside the evolved one");
       helper.assertTrue(SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION.toString().equals(vars.getWheelSlotEntry(0, 0).magicId),
          "ALTER Sakura wheel slot was not migrated to the evolved magic");
       helper.succeed();
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 20)
    public static void blackSakuraOverlayFormsAndReleasesWithoutTouchingArmor(GameTestHelper helper) {
       var player = helper.makeMockServerPlayerInLevel();
       var data = prepareBlackSakuraState(player, MatouSakuraMasterProfile.Variant.ALTER);
       player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
       player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
       player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
       player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));

       helper.assertTrue(CursedArmorService.beginFormation(player), "Black Sakura armor did not begin forming");
       helper.assertTrue(data.cursedArmorState() == ImaginarySpaceData.CursedArmorState.FORMING, "Black Sakura armor was not forming");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "Helmet changed during formation");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "Chestplate changed during formation");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(Items.DIAMOND_LEGGINGS), "Leggings changed during formation");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS), "Boots changed during formation");

       helper.assertTrue(data.activateCursedArmor(), "Black Sakura armor did not become active");
       helper.assertTrue(data.cursedArmorState() == ImaginarySpaceData.CursedArmorState.ACTIVE, "Black Sakura armor was not active");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "Helmet changed after activation");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "Chestplate changed after activation");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(Items.DIAMOND_LEGGINGS), "Leggings changed after activation");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS), "Boots changed after activation");
       helper.assertTrue(CursedArmorService.beginDissolution(player), "Black Sakura armor did not begin dissolving");
       helper.assertTrue(data.cursedArmorState() == ImaginarySpaceData.CursedArmorState.DISSOLVING, "Black Sakura armor was not dissolving");
       helper.assertTrue(data.finishCursedArmorDissolution(), "Black Sakura armor did not fully clear");
       helper.assertTrue(data.cursedArmorState() == ImaginarySpaceData.CursedArmorState.REMOVED,
          "Black Sakura armor did not end in the removed state");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "Helmet changed after clear");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "Chestplate changed after clear");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.LEGS).is(Items.DIAMOND_LEGGINGS), "Leggings changed after clear");
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS), "Boots changed after clear");
       helper.succeed();
    }

    @GameTest(template = "ancient_temple", timeoutTicks = 20)
    public static void fhaInitializesWithVoidRingAndNoBlackOverlay(GameTestHelper helper) {
       var player = helper.makeMockServerPlayerInLevel();
       MatouSakuraMasterProfile.initialize(player, MatouSakuraMasterProfile.Variant.FHA);
       helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(AddonItems.VOID_RING_REGALIA.get()),
          "FHA did not equip Void Ring regalia");
       helper.assertTrue(!player.getData(AddonAttachments.IMAGINARY_SPACE.get()).cursedArmorPresent(),
          "FHA unexpectedly spawned black Sakura armor");
       helper.assertTrue(player.getData(AddonAttachments.IMAGINARY_SPACE.get()).cursedArmorState() == ImaginarySpaceData.CursedArmorState.REMOVED,
          "FHA did not finish in the removed state");
       helper.succeed();
    }

    private static ImaginarySpaceData prepareBlackSakuraState(ServerPlayer player, MatouSakuraMasterProfile.Variant variant) {
       var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
       vars.master_card_id = variant == MatouSakuraMasterProfile.Variant.ALTER
          ? MatouSakuraMasterProfile.ALTER_ID.toString()
          : MatouSakuraMasterProfile.ID.toString();
       vars.master_card_active = true;
       vars.master_active = true;
       SakuraTypeMoonIntegration.grantMasterCardAttributes(player);

       var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
       data.unlock();
       data.assimilateCrestWorm();
       data.ascendGrailWorm();
       data.increaseGrailErosion(1L);
       data.increaseGrailErosion(1L);
       data.increaseGrailErosion(1L);
       data.completePendingGrailErosion(Long.MAX_VALUE);
       return data;
    }
}

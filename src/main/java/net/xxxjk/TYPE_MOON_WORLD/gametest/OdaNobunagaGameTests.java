package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.*;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import static net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource.Ability.*;

@GameTestHolder("typemoonworld_oda")
@PrefixGameTestTemplate(false)
public final class OdaNobunagaGameTests {
   private OdaNobunagaGameTests() {}
   private static OdaNobunagaEntity owner(GameTestHelper helper) {
      var level = helper.getLevel();
      var previous = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
      for (var entity : level.getEntities().getAll()) if (entity.getTags().contains("oda_test_fixture")) previous.add(entity);
      previous.forEach(net.minecraft.world.entity.Entity::discard);
      var owner = ModEntities.ODA_NOBUNAGA.get().create(level);
      owner.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 6, 6))));
      // Formation radii extend past the template's chunk tickets. Keep the fixture's full combat area ticking.
      var chunk = owner.chunkPosition();
      for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.setChunkForced(chunk.x + x, chunk.z + z, true);
      owner.addTag("oda_test_fixture");
      owner.setNoAi(true); owner.setNoGravity(true);
      owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); owner.setHealth(1000);
      owner.setCurrentMp(1000);
      level.addFreshEntity(owner);
      return owner;
   }
   private static net.minecraft.world.entity.monster.Zombie target(GameTestHelper helper, Vec3 position) {
      var target = EntityType.HUSK.create(helper.getLevel());
      target.addTag("oda_test_fixture");
      target.setPersistenceRequired();
      target.setPos(position); target.setNoAi(true); target.setNoGravity(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
      target.getAttribute(Attributes.ARMOR).setBaseValue(0);
      helper.getLevel().addFreshEntity(target);
      return target;
   }
   private static void close(GameTestHelper helper, double actual, double expected, String message) {
      helper.assertTrue(Math.abs(actual - expected) < .01, message + ": " + actual + " expected " + expected);
   }
   private static void tickMockFlight(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player, int ticks) {
      // GameTest's mock does not run the real player's CommonEvents tick pipeline.
      for (int tick = 1; tick <= ticks; tick++) helper.runAfterDelay(tick, () -> {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         ServantCardFlightController.tick(player, vars);
         ServantCardOdaNobunagaSkills.tick(player, vars);
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_profileHasTenTraitsAndBothNoblePhantasms")
   public static void profileHasTenTraitsAndBothNoblePhantasms(GameTestHelper helper) {
      var definition = ServantDataRegistry.get("oda_nobunaga");
      helper.assertTrue(definition.traits().size() == 10 && !definition.traits().contains(ServantTraitTag.NEUTRAL), "Incorrect trait count/alignment");
      helper.assertTrue(definition.traits().contains(ServantTraitTag.KING) && definition.traits().contains(ServantTraitTag.NOBUNAGA), "Missing new traits");
      helper.assertTrue(definition.parameters().luck() == StatRank.A_MINUS && definition.parameters().luck().coefficient() == 25, "A- was not parsed");
      helper.assertTrue(definition.noblePhantasmIds().size() == 2 && !definition.noblePhantasmId().isBlank(), "Multiple/single NP interfaces are inconsistent");
      helper.assertTrue(ServantNoblePhantasmDataRegistry.get("oda_three_thousand").type() == ServantNoblePhantasmDefinition.NpType.ARMY, "Three Thousand NP type");
      helper.assertTrue(ServantNoblePhantasmDataRegistry.get("oda_hajun").type() == ServantNoblePhantasmDefinition.NpType.ANTI_DIVINE, "Hajun NP type");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_allCombatFormationsAbsorbInDistanceOrderAndExcludeMount")
   public static void allCombatFormationsAbsorbInDistanceOrderAndExcludeMount(GameTestHelper helper) {
      var owner = owner(helper);
      var far = OdaMatchlockGunEntity.oneShotTracking(owner.level(), owner, null, owner.position().add(3,0,0), 50).configure(PURSUIT,1,4,80);
      var near = new OdaMatchlockGunEntity(owner.level(), owner, owner.position().add(1,0,0), owner.getLookAngle(), 50).configure(ROTATION,1,4,80);
      var middle = OdaMatchlockGunEntity.threeThousandWorlds(owner.level(), owner, null, owner.position().add(2,0,0), owner.getLookAngle(), 50);
      var mount = OdaMatchlockGunEntity.flightMount(owner.level(), owner);
      for (var gun : java.util.List.of(far, near, middle, mount)) helper.getLevel().addFreshEntity(gun);
      close(helper, OdaAbilityService.absorb(owner, 35), 0, "35 damage should fit into two guns");
      helper.assertTrue(near.isRemoved(), "Nearest gun did not die first");
      close(helper, middle.getGunHealth(), 25, "Second gun health");
      close(helper, far.getGunHealth(), 30, "Far gun health");
      close(helper, OdaAbilityService.absorb(owner, 65), 10, "Three guns must leave ten from total 100");
      close(helper, mount.getGunHealth(), 120, "Mount was included in substitution");
      owner.getAttribute(Attributes.ARMOR).setBaseValue(0);
      owner.getPersistentData().putLong("TypeMoonCombatSuppressedUntil", owner.level().getGameTime() + 100);
      for (int i = 0; i < 3; i++) helper.getLevel().addFreshEntity(new OdaMatchlockGunEntity(owner.level(), owner, owner.position().add(i + 1,0,0), owner.getLookAngle(), 100));
      float before = owner.getHealth(); owner.hurt(owner.damageSources().generic(), 100);
      close(helper, before - owner.getHealth(), 10, "Remaining damage did not reach the body exactly once");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_immunityAndDodgeDoNotSpendGunHealth")
   public static void immunityAndDodgeDoNotSpendGunHealth(GameTestHelper helper) {
      var owner = owner(helper);
      var gun = OdaMatchlockGunEntity.floating(owner.level(), owner, 0, 1, 600, 100);
      helper.getLevel().addFreshEntity(gun);
      helper.assertTrue(!owner.hurt(owner.damageSources().lava(), 100), "Fire immunity failed");
      close(helper, gun.getGunHealth(), 30, "Immune fire consumed gun health");
      owner.getPersistentData().putLong("TypeMoonCombatInvulnUntil", owner.level().getGameTime() + 40);
      owner.hurt(owner.damageSources().generic(), 10);
      close(helper, gun.getGunHealth(), 30, "Dodge invulnerability consumed gun health");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_ordinaryGunDoesNotGainRidingBonusAndHajunHasOneBase")
   public static void ordinaryGunDoesNotGainRidingBonusAndHajunHasOneBase(GameTestHelper helper) {
      var owner = owner(helper); var target = target(helper, owner.position().add(0,0,3));
      target.getPersistentData().putBoolean("TypeMoonTrait_riding", true);
      float before = target.getHealth();
      OdaAbilityService.hurt(owner, target, 10, ORBIT, false);
      close(helper, before - target.getHealth(), 10, "Ordinary gun gained riding/era modifiers");
      before = target.getHealth(); OdaAbilityService.hurt(owner, target, 10, THREE_THOUSAND, false);
      close(helper, before - target.getHealth(), 23.2, "NP riding + strategy");
      target.getPersistentData().putBoolean("TypeMoonTrait_divine", true);
      before = target.getHealth(); OdaAbilityService.hurt(owner, target, 52, HAJUN, false);
      close(helper, before - target.getHealth(), 104, "Hajun base/divinity applied more than once");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 90, batch = "oda_pursuitReportsInLaunchOrderAndResetsAfterEveryThree")
   public static void pursuitReportsInLaunchOrderAndResetsAfterEveryThree(GameTestHelper helper) {
      var owner = owner(helper); var target = target(helper, owner.position().add(0,0,4));
      owner.setYRot(0); owner.setXRot(0);
      // Aim at the target's middle, and keep the whole volley outside terrain.
      for (int x = 1; x < 13; x++) for (int y = 5; y < 10; y++) for (int z = 1; z < 13; z++) helper.setBlock(new BlockPos(x,y,z), Blocks.AIR);
      helper.assertTrue(OdaAbilityService.cast(owner, 2), "Pursuit failed to lock");
      var guns = helper.getLevel().getEntitiesOfClass(OdaMatchlockGunEntity.class, owner.getBoundingBox().inflate(10), g -> g.isCombatFor(owner.getUUID()));
      helper.assertTrue(guns.size() == 10, "Pursuit did not summon ten guns");
      int[] explosions = {0};
      java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> listener = event -> {
         if (event.getEntity() == target && event.getSource() instanceof OdaDamageSource source && source.ability() == PURSUIT_EXPLOSION) explosions[0]++;
      };
      NeoForge.EVENT_BUS.addListener(listener);
      helper.runAfterDelay(65, () -> {
         NeoForge.EVENT_BUS.unregister(listener);
         close(helper, explosions[0], 3, "Ten hits must produce exactly three explosions; target=" + target.getHealth() + " removed=" + target.isRemoved() + " owner=" + owner.getHealth() + " ownerRemoved=" + owner.isRemoved());
         close(helper, 1000 - target.getHealth(), 160, "Pursuit hit + explosion total");
         OdaAbilityService.clear(owner); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 330, batch = "oda_threeWavesFireWhileGunsAppear")
   public static void threeWavesFireWhileTheirGunsAreStillAppearing(GameTestHelper helper) {
      verifyThreeThousandWaves(helper, true);
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 330, batch = "oda_fixedThreeWavesFireWhileGunsAppear")
   public static void fixedThreeWavesKeepTheirTallSpiralAndShotCadence(GameTestHelper helper) {
      verifyThreeThousandWaves(helper, false);
   }
   private static void verifyThreeThousandWaves(GameTestHelper helper, boolean follow) {
      var owner = owner(helper);
      int[] gunCount = {0};
      int[] shotCount = {0};
      double[] highestGun = {0};
      var guns = new java.util.ArrayList<OdaMatchlockGunEntity>();
      var formation = new OdaMatchlockGunEntity[OdaRules.THREE_THOUSAND_TOTAL_GUNS];
      var shotTimes = new java.util.HashMap<java.util.UUID, java.util.List<Long>>();
      java.util.function.Consumer<EntityJoinLevelEvent> listener = event -> {
         if (event.getEntity() instanceof OdaMatchlockGunEntity gun && gun.isOwnedBy(owner.getUUID()) && gun.ability() == THREE_THOUSAND) {
            int index = gun.getFormationIndex();
            if (index >= OdaRules.THREE_THOUSAND_GUNS_PER_WAVE) {
               var previous = formation[index - OdaRules.THREE_THOUSAND_GUNS_PER_WAVE];
               helper.assertTrue(previous != null && previous.isRemoved() && previous.getShotsLeft() == 0,
                  "A position was replaced before its preceding gun finished its shot");
               close(helper, helper.getLevel().getGameTime() - shotTimes.get(previous.getUUID()).getFirst(),
                  OdaRules.THREE_THOUSAND_WAVE_INTERVAL, "Each position must wait one second before its next wave");
               if (follow) {
                  int gunIndex = OdaRules.threeThousandGunIndexInWave(index);
                  int tier = OdaRules.threeThousandTier(gunIndex);
                  int slot = gunIndex % OdaRules.THREE_THOUSAND_GUNS_PER_TIER;
                  close(helper, gun.position().subtract(owner.position()).horizontalDistance(),
                     OdaRules.threeThousandRadius(tier, slot, OdaRules.threeThousandWave(index)), "Follow relay radius");
                  close(helper, gun.getY() - owner.getY() - owner.getBbHeight() * .55,
                     OdaRules.threeThousandHeight(tier, slot, OdaRules.threeThousandWave(index)), "Follow relay height");
               } else helper.assertTrue(previous.position().distanceTo(gun.position()) < 1e-6,
                  "The replacement did not appear at the corresponding gun position");
            }
            formation[index] = gun;
            gunCount[0]++;
            guns.add(gun);
            highestGun[0] = Math.max(highestGun[0], gun.getY() - owner.getY());
            gun.fixedDirection(new Vec3(0,1,0));
         }
         if (event.getEntity() instanceof OdaMatchlockBulletEntity bullet && bullet.getOwner() == owner && bullet.ability() == THREE_THOUSAND) {
            close(helper, bullet.getDirectDamage(), OdaRules.THREE_THOUSAND_SHOT_DAMAGE, "Three Thousand shot damage");
            shotCount[0]++;
            var source = guns.stream().filter(gun -> !gun.isRemoved() && gun.position().distanceToSqr(bullet.position()) < 1e-8).findFirst().orElseThrow();
            shotTimes.computeIfAbsent(source.getUUID(), id -> new java.util.ArrayList<>()).add(helper.getLevel().getGameTime());
         }
      };
      NeoForge.EVENT_BUS.addListener(listener);
      OdaAbilityService.threeThousand(owner, null, follow, owner.position());
      helper.runAfterDelay(4, () -> {
         long firstWaveGuns = java.util.Arrays.stream(formation, 0, OdaRules.THREE_THOUSAND_GUNS_PER_WAVE)
            .filter(java.util.Objects::nonNull).count();
         helper.assertTrue(firstWaveGuns > 0 && firstWaveGuns < OdaRules.THREE_THOUSAND_GUNS_PER_WAVE,
            "The first gun wave should still be appearing during the initial shots");
         helper.assertTrue(shotCount[0] > 0, "Guns did not begin firing during their gradual appearance");
         var firstGun = guns.stream().filter(gun -> !gun.isRemoved()).findFirst().orElseThrow();
         Vec3 before = firstGun.position();
         owner.setPos(owner.position().add(2, 1, 0));
         helper.runAfterDelay(2, () -> {
            close(helper, firstGun.getY() - before.y, follow ? 1 : 0, "Follow/fixed spiral movement");
         });
         helper.runAfterDelay(278, () -> {
            NeoForge.EVENT_BUS.unregister(listener);
            close(helper, gunCount[0], OdaRules.THREE_THOUSAND_TOTAL_GUNS, "NP spawned gun total");
            close(helper, shotCount[0], OdaRules.THREE_THOUSAND_TOTAL_SHOTS, "NP shot total");
            helper.assertTrue(highestGun[0] > 1 && highestGun[0] < 10, "The compact spiral exceeded ten blocks");
            close(helper, shotTimes.size(), OdaRules.THREE_THOUSAND_TOTAL_GUNS, "Every gun must fire");
            for (var times : shotTimes.values()) {
               close(helper, times.size(), 1, "Each gun must fire exactly once");
            }
            helper.assertTrue(guns.stream().allMatch(OdaMatchlockGunEntity::isRemoved), "Finished guns did not disappear");
            OdaAbilityService.clear(owner);
            helper.succeed();
         });
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 290, batch = "oda_eachPositionRelaysIndependently")
   public static void eachGunPositionRelaysWhileOtherPositionsAreStillWaiting(GameTestHelper helper) {
      var owner = owner(helper); owner.setPos(owner.position().add(0, 100, 0));
      var guns = new java.util.ArrayList<OdaMatchlockGunEntity>();
      java.util.function.Consumer<EntityJoinLevelEvent> listener = event -> {
         if (event.getEntity() instanceof OdaMatchlockGunEntity gun && gun.isOwnedBy(owner.getUUID()) && gun.ability() == THREE_THOUSAND) guns.add(gun);
      };
      NeoForge.EVENT_BUS.addListener(listener);
      OdaAbilityService.threeThousand(owner, null, true, owner.position());
      helper.runAfterDelay(100, () -> {
         close(helper, guns.size(), OdaRules.THREE_THOUSAND_GUNS_PER_WAVE, "Waiting first wave started another wave on a timer");
         helper.assertTrue(guns.stream().allMatch(gun -> !gun.isRemoved() && gun.getShotsLeft() == 1), "Waiting guns wasted ammunition");
         guns.getFirst().fixedDirection(new Vec3(0,1,0));
      });
      helper.runAfterDelay(110, () -> {
         helper.assertTrue(guns.getFirst().isRemoved(), "The first gun did not finish its shot");
         close(helper, guns.size(), OdaRules.THREE_THOUSAND_GUNS_PER_WAVE, "The next wave skipped its one-second wait");
         helper.assertTrue(OdaAbilityService.hasThreeThousandArray(owner), "The array ended during its wave interval");
      });
      helper.runAfterDelay(175, () -> {
         close(helper, guns.size(), OdaRules.THREE_THOUSAND_GUNS_PER_WAVE + 1,
            "One completed position must receive its second gun after one second");
         helper.assertTrue(guns.getFirst().isRemoved()
            && guns.get(OdaRules.THREE_THOUSAND_GUNS_PER_WAVE).getFormationIndex() == OdaRules.THREE_THOUSAND_GUNS_PER_WAVE,
            "The completed position did not relay to its own second-wave gun");
         helper.assertTrue(guns.subList(1, OdaRules.THREE_THOUSAND_GUNS_PER_WAVE)
            .stream().allMatch(g -> !g.isRemoved() && g.getShotsLeft() == 1),
            "Waiting positions were advanced without finishing");
         guns.get(OdaRules.THREE_THOUSAND_GUNS_PER_WAVE).fixedDirection(new Vec3(0,1,0));
      });
      helper.runAfterDelay(185, () -> {
         helper.assertTrue(guns.get(OdaRules.THREE_THOUSAND_GUNS_PER_WAVE).isRemoved(), "The second gun did not finish its shot");
         close(helper, guns.size(), OdaRules.THREE_THOUSAND_GUNS_PER_WAVE + 1, "The third wave skipped its one-second wait");
      });
      helper.runAfterDelay(250, () -> {
         close(helper, guns.size(), OdaRules.THREE_THOUSAND_GUNS_PER_WAVE + 2,
            "The completed position did not reach its third gun");
         helper.assertTrue(guns.get(OdaRules.THREE_THOUSAND_GUNS_PER_WAVE + 1).getFormationIndex()
            == 2 * OdaRules.THREE_THOUSAND_GUNS_PER_WAVE, "The third gun did not reuse its position index");
         OdaAbilityService.clear(owner);
      });
      helper.runAfterDelay(275, () -> {
         NeoForge.EVENT_BUS.unregister(listener);
         close(helper, guns.size(), OdaRules.THREE_THOUSAND_GUNS_PER_WAVE + 2,
            "Clearing the owner did not cancel pending gun relays");
         helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 95, batch = "oda_chantFlightAllowsMovementAndKeepsFlying")
   public static void chantFlightAllowsMovementAndKeepsFlyingAfterInterruption(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 12, 6)));
      player.setPos(origin);
      for (int x = 4; x <= 8; x++) for (int z = 4; z <= 8; z++) {
         helper.setBlock(new BlockPos(x, 11, z), Blocks.STONE);
         for (int y = 12; y <= 20; y++) helper.setBlock(new BlockPos(x,y,z), Blocks.AIR);
      }
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true; vars.servant_card_id = "oda_nobunaga";
      vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(1000);
      double[] beforeDescent = {0}, afterDescent = {0};
      ServantCardOdaNobunagaSkills.startOdaThreeThousand(player);
      helper.assertTrue(player.getVehicle() instanceof OdaMatchlockGunEntity, "Chant did not seat the player on a matchlock");
      var gun = (OdaMatchlockGunEntity)player.getVehicle();
      tickMockFlight(helper, player, 74);
      helper.runAfterDelay(15, () -> {
         helper.assertTrue(gun.isChantLift() && player.getY() > origin.y && player.getY() < origin.y + 3,
            "Chant lift teleported instead of rising smoothly");
         helper.assertTrue(vars.servantCardState().flying(), "The chant did not enable normal player flight");
         ServantCardFlightController.setInput(player, false, 1, .5, 0);
      });
      helper.runAfterDelay(30, () -> {
         helper.assertTrue(player.position().subtract(origin).horizontalDistance() > 1, "Chant flight ignored horizontal input");
         beforeDescent[0] = player.getY();
         ServantCardFlightController.setInput(player, false, 1, 0, -1);
      });
      helper.runAfterDelay(34, () -> {
         afterDescent[0] = player.getY();
         helper.assertTrue(afterDescent[0] < beforeDescent[0], "Manual descent did not override the automatic lift");
         ServantCardFlightController.setInput(player, false, 1, 0, 1);
      });
      helper.runAfterDelay(42, () -> {
         helper.assertTrue(player.getY() > afterDescent[0], "Manual ascent did not work during chanting");
         ServantCardFlightController.setInput(player, false, 0, 0, 0);
         vars.servantCardState().setJumpCharges(4);
         helper.assertTrue(ServantCardFlightController.tryDash(player, 1, 0), "The chant blocked the normal flight dash");
         net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.cancel(player, false);
      });
      helper.runAfterDelay(75, () -> {
         helper.assertTrue(!gun.isRemoved() && player.getVehicle() == gun && !gun.isChantLift(), "Ending the chant removed the player's flight mount");
         helper.assertTrue(vars.servantCardState().flying(), "Ending the chant forced the player out of flight");
         helper.assertTrue(vars.servantCardState().mana() < 1000, "Normal flight did not pay its usual mana cost");
         ServantCardFlightController.stop(player, vars, true);
         ServantCardOdaNobunagaSkills.clear(player); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 430, batch = "oda_npcChantRideReleasesAndLands")
   public static void npcChantRideReleasesTheArrayAndReturnsWithoutAiMovement(GameTestHelper helper) {
      var npc = owner(helper); npc.setPos(npc.position().add(0,100,0));
      Vec3 origin = npc.position();
      var target = target(helper, origin.add(0,0,12));
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000); target.setHealth(100000);
      target.setInvulnerable(true);
      npc.setTarget(target);
      long now = npc.level().getGameTime();
      npc.getPersistentData().putLong("TypeMoonAiCombatStartedAt", now - 600);
      npc.getPersistentData().putLong("TypeMoonAiLastCombatAt", now);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.tickBeforeAi(npc);
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.tick(npc);
      helper.assertTrue(npc.getVehicle() instanceof OdaMatchlockGunEntity, "NPC did not mount its chant gun");
      var mount = (OdaMatchlockGunEntity)npc.getVehicle();
      helper.runAfterDelay(45, () -> close(helper, npc.getY() - origin.y, 3, "NPC chant hover height"));
      helper.runAfterDelay(net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.releaseAt(npc) - now + 2, () -> {
         helper.assertTrue(!mount.isRemoved() && npc.getVehicle() == mount, "NPC did not remain in the center of its active array");
         helper.assertTrue(!helper.getLevel().getEntitiesOfClass(OdaMatchlockGunEntity.class, npc.getBoundingBox().inflate(40),
            gun -> gun.isOwnedBy(npc.getUUID()) && gun.ability() == THREE_THOUSAND).isEmpty(), "Mounted NPC never released its array");
      });
      helper.runAfterDelay(390, () -> {
         helper.assertTrue(mount.isRemoved() && !npc.isPassenger(), "NPC did not return after its array finished");
         OdaAbilityService.clear(npc); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 85, batch = "oda_chantBorrowsExistingFlightMount")
   public static void chantPreservesAnExistingFlightMountAndItsControls(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(6,100,6))));
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true; vars.servant_card_id = "oda_nobunaga";
      vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(1000);
      ServantCardFlightController.setInput(player, true, 0, 0, 0);
      var mount = (OdaMatchlockGunEntity)player.getVehicle();
      ServantCardOdaNobunagaSkills.startOdaThreeThousand(player);
      helper.assertTrue(player.getVehicle() == mount && mount.isChantLift(), "Chant replaced the existing flight mount");
      Vec3 origin = mount.position();
      tickMockFlight(helper, player, 59);
      ServantCardFlightController.setInput(player, false, 1, 1, 0);
      helper.runAfterDelay(20, () -> helper.assertTrue(mount.position().subtract(origin).horizontalDistance() > 2,
         "The chant locked an already flying player in place"));
      helper.runAfterDelay(45, () -> net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.cancel(player, false));
      helper.runAfterDelay(60, () -> {
         helper.assertTrue(player.getVehicle() == mount && !mount.isRemoved() && !mount.isChantLift(), "Chant did not return control of the existing mount");
         helper.assertTrue(vars.servantCardState().flying(), "Chant disabled pre-existing flight");
         ServantCardFlightController.stop(player, vars, true);
         ServantCardOdaNobunagaSkills.clear(player); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 200, batch = "oda_threeThousandReleaseKeepsPlayerFlight")
   public static void releasingThreeThousandDoesNotLandOrLockThePlayer(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 12, 6)));
      player.setPos(origin);
      for (int x = 3; x <= 9; x++) for (int z = 3; z <= 9; z++) {
         helper.setBlock(new BlockPos(x, 11, z), Blocks.STONE);
         for (int y = 12; y <= 20; y++) helper.setBlock(new BlockPos(x,y,z), Blocks.AIR);
      }
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true; vars.servant_card_id = "oda_nobunaga";
      vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(1000);
      ServantCardOdaNobunagaSkills.startOdaThreeThousand(player);
      var mount = (OdaMatchlockGunEntity)player.getVehicle();
      tickMockFlight(helper, player, 179);
      helper.runAfterDelay(160, () -> {
         helper.assertTrue(vars.servantCardState().flying() && player.getVehicle() == mount && !mount.isChantLift(),
            "Noble Phantasm release did not leave the player in normal flight");
         helper.assertTrue(OdaAbilityService.hasThreeThousandArray(player), "The gun array was not released");
         ServantCardFlightController.setInput(player, false, 1, 0, 0);
      });
      helper.runAfterDelay(180, () -> {
         helper.assertTrue(player.position().subtract(origin).horizontalDistance() > 2, "The active gun array locked player movement");
         ServantCardFlightController.stop(player, vars, true);
         ServantCardOdaNobunagaSkills.clear(player); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 630, batch = "oda_unfinishedThreeThousandGunsExpireAfterThirtySeconds")
   public static void unfinishedThreeThousandGunsWaitAndExpireAfterThirtySeconds(GameTestHelper helper) {
      var owner = owner(helper);
      owner.setPos(owner.position().add(0, 100, 0));
      var fixed = OdaMatchlockGunEntity.threeThousandWorlds(owner.level(), owner, null,
         owner.position().add(4, 0, 0), owner.getLookAngle(), 2);
      var follow = OdaMatchlockGunEntity.threeThousandWorldsFollow(owner.level(), owner, null,
         OdaRules.THREE_THOUSAND_TOTAL_GUNS - 1, OdaRules.THREE_THOUSAND_TOTAL_GUNS, 2);
      helper.getLevel().addFreshEntity(fixed);
      helper.getLevel().addFreshEntity(follow);
      helper.runAfterDelay(590, () -> {
         helper.assertTrue(!fixed.isRemoved() && !follow.isRemoved(), "Unfinished guns expired before thirty seconds");
         close(helper, fixed.getShotsLeft(), 1, "Fixed gun wasted ammunition without a target");
         close(helper, follow.getShotsLeft(), 1, "Follow gun wasted ammunition without a target");
      });
      helper.runAfterDelay(602, () -> {
         helper.assertTrue(fixed.isRemoved() && follow.isRemoved(), "Unfinished guns exceeded thirty seconds");
         OdaAbilityService.clear(owner); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 120, batch = "oda_threeThousandGunsResumeRemainingShots")
   public static void threeThousandGunsResumeRemainingShotsWhenANewTargetAppears(GameTestHelper helper) {
      var owner = owner(helper);
      owner.setPos(owner.position().add(0, 100, 0));
      var target = target(helper, owner.position().add(0, 0, 5));
      var gun = OdaMatchlockGunEntity.threeThousandWorldsFollow(owner.level(), owner, target,
         OdaRules.THREE_THOUSAND_TOTAL_GUNS - 1, OdaRules.THREE_THOUSAND_TOTAL_GUNS, 2);
      helper.getLevel().addFreshEntity(gun);
      target.discard();
      helper.runAfterDelay(10, () -> {
         close(helper, gun.getShotsLeft(), 1, "High spiral gun wasted its shot after losing its target");
      });
      helper.runAfterDelay(55, () -> {
         helper.assertTrue(!gun.isRemoved(), "Gun disappeared when its target was lost");
         close(helper, gun.getShotsLeft(), 1, "Gun wasted its remaining shot");
         target(helper, owner.position().add(0, 0, 6));
      });
      helper.runAfterDelay(90, () -> {
         close(helper, gun.getShotsLeft(), 0, "Gun failed to resume its remaining shots");
         helper.assertTrue(gun.isRemoved(), "Gun did not disappear after completing its shot");
         OdaAbilityService.clear(owner); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_cardRefillFullAndMissingLockDoNotPay")
   public static void cardRefillFullAndMissingLockDoNotPay(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 6, 6))));
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true; vars.servant_card_id = "oda_nobunaga";
      vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(500);
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 0), "Card refill failed");
      close(helper, vars.servantCardState().mana(), 480, "Card refill cost");
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 0), "Full refill should not cast");
      close(helper, vars.servantCardState().mana(), 480, "Full refill consumed MP");
      player.setXRot(-90);
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 2), "No-target pursuit should not cast");
      close(helper, vars.servantCardState().mana(), 480, "No-target pursuit consumed MP");
      OdaAbilityService.clear(player); helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_offhandWeaponUsesRightClickAndOrdinaryLeftClickIsFree")
   public static void offhandWeaponUsesRightClickAndOrdinaryLeftClickIsFree(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true; vars.servant_card_id = "oda_nobunaga";
      vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(500);
      player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ModItems.HESHIKIRI_HASEBE.get().getDefaultInstance());
      helper.assertTrue(ServantCardOdaNobunagaSkills.handleHeshikiriRightClick(player), "Offhand weapon not recognized");
      close(helper, vars.servantCardState().mana(), 497, "Right-click MP");
      ServantCardOdaNobunagaSkills.handleBasicAttackPacket(player, false);
      close(helper, vars.servantCardState().mana(), 497, "Ordinary left-click consumed MP");
      OdaAbilityService.clear(player); helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_divineEMythologyAndRidingUseOneAdditiveZone")
   public static void divineEMythologyAndRidingUseOneAdditiveZone(GameTestHelper helper) {
      var owner = owner(helper);
      var medusa = ModEntities.MEDUSA.get().create(helper.getLevel());
      medusa.setPos(owner.position().add(0,0,4)); medusa.setNoAi(true); helper.getLevel().addFreshEntity(medusa);
      var normal = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(medusa,
         new net.neoforged.neoforge.common.damagesource.DamageContainer(new OdaDamageSource(owner.damageSources().mobAttack(owner), owner, owner, ORBIT), 10));
      OdaCombatEvents.outgoing(normal);
      close(helper, normal.getAmount(), 50, "E + mythology = x5");
      var np = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(medusa,
         new net.neoforged.neoforge.common.damagesource.DamageContainer(new OdaDamageSource(owner.damageSources().mobAttack(owner), owner, owner, THREE_THOUSAND), 10));
      OdaCombatEvents.outgoing(np);
      close(helper, np.getAmount(), 69.6, "E + mythology + riding + strategy");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_strategyDefenseOnlyRecognizesNoblePhantasmType")
   public static void strategyDefenseOnlyRecognizesNoblePhantasmType(GameTestHelper helper) {
      var owner = owner(helper); var enemy = target(helper, owner.position().add(0,0,4));
      for (var ability : java.util.List.of(THREE_THOUSAND, HAJUN, ORBIT, SCORCHED)) {
         var event = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(owner,
            new net.neoforged.neoforge.common.damagesource.DamageContainer(new OdaDamageSource(enemy.damageSources().mobAttack(enemy), enemy, enemy, ability), 100));
         OdaCombatEvents.outgoing(event);
         close(helper, event.getAmount(), ability == THREE_THOUSAND ? 84 : 100, "Strategy defense " + ability);
      }
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 100, batch = "oda_destroyedPursuitGunBreaksTheConsecutiveHitChain")
   public static void destroyedPursuitGunBreaksTheConsecutiveHitChain(GameTestHelper helper) {
      var owner = owner(helper); var target = target(helper, owner.position().add(0,0,4)); owner.setYRot(0); owner.setXRot(0);
      for (int x = 1; x < 13; x++) for (int y = 5; y < 10; y++) for (int z = 1; z < 13; z++) helper.setBlock(new BlockPos(x,y,z), Blocks.AIR);
      helper.assertTrue(OdaAbilityService.cast(owner, 2), "Pursuit lock failed");
      var guns = helper.getLevel().getEntitiesOfClass(OdaMatchlockGunEntity.class, owner.getBoundingBox().inflate(10), g -> g.isCombatFor(owner.getUUID()));
      guns.stream().filter(g -> g.getPursuitIndex() == 5).findFirst().orElseThrow().hurt(target.damageSources().mobAttack(target), 30);
      int[] explosions = {0};
      java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> listener = event -> {
         if (event.getEntity() == target && event.getSource() instanceof OdaDamageSource source && source.ability() == PURSUIT_EXPLOSION) explosions[0]++;
      };
      NeoForge.EVENT_BUS.addListener(listener);
      helper.runAfterDelay(65, () -> {
         NeoForge.EVENT_BUS.unregister(listener);
         close(helper, explosions[0], 2, "A destroyed sixth gun must split streaks 5 + 4");
         close(helper, 1000 - target.getHealth(), 130, "Nine bullets + two explosions");
         OdaAbilityService.clear(owner); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 190, batch = "oda_scorchedEarthHasThreePulsesAndFiveBurnsWithoutVanillaFire")
   public static void scorchedEarthHasThreePulsesAndFiveBurnsWithoutVanillaFire(GameTestHelper helper) {
      var owner = owner(helper); owner.setYRot(0); owner.setXRot(0);
      var target = target(helper, owner.position().add(0,0,8));
      for (int x = 1; x < 13; x++) for (int y = 5; y < 10; y++) for (int z = 1; z < 17; z++) helper.setBlock(new BlockPos(x,y,z), Blocks.AIR);
      helper.assertTrue(OdaAbilityService.cast(owner, 5), "Scorched earth cast");
      helper.runAfterDelay(170, () -> {
         close(helper, 1000 - target.getHealth(), 49, "3x8 pulses + 5x5 burns");
         helper.assertTrue(!target.isOnFire(), "Scorched earth added vanilla fire damage");
         OdaAbilityService.clear(owner); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 50, batch = "oda_crossfireSlowIsThirtyPercentAndEndsOnLeaving")
   public static void crossfireSlowIsThirtyPercentAndEndsOnLeaving(GameTestHelper helper) {
      var owner = owner(helper); owner.setYRot(0); owner.setXRot(0);
      var target = target(helper, owner.position().add(0,0,14));
      double speed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
      helper.assertTrue(OdaAbilityService.cast(owner, 4), "Crossfire cast");
      helper.runAfterDelay(3, () -> {
         close(helper, target.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * .7, "Crossfire slow");
         target.setPos(owner.position().add(20,0,14));
      });
      helper.runAfterDelay(6, () -> {
         close(helper, target.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "Crossfire slow lingered outside area");
         OdaAbilityService.clear(owner); helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_legacyGunWithoutProvenanceIsReclaimed")
   public static void legacyGunWithoutProvenanceIsReclaimed(GameTestHelper helper) {
      var owner = owner(helper);
      var original = OdaMatchlockGunEntity.floating(owner.level(), owner, 0, 1, 600, 0);
      var tag = original.saveWithoutId(new net.minecraft.nbt.CompoundTag()); tag.remove("Ability");
      var restored = ModEntities.ODA_MATCHLOCK_GUN.get().create(helper.getLevel()); restored.load(tag); helper.getLevel().addFreshEntity(restored);
      helper.runAfterDelay(3, () -> { helper.assertTrue(restored.isRemoved(), "Old gun without source survived loading"); helper.succeed(); });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 50, batch = "oda_clearingOwnerCancelsDelayedWavesAndAllOwnedGuns")
   public static void clearingOwnerCancelsDelayedWavesAndAllOwnedGuns(GameTestHelper helper) {
      var owner = owner(helper); OdaAbilityService.cast(owner, 1); OdaAbilityService.cast(owner, 0);
      OdaAbilityService.clear(owner);
      helper.runAfterDelay(30, () -> {
         helper.assertTrue(helper.getLevel().getEntitiesOfClass(OdaMatchlockGunEntity.class, owner.getBoundingBox().inflate(48), g -> g.isOwnedBy(owner.getUUID())).isEmpty(), "Canceled owner spawned delayed guns");
         helper.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_actorParity")
   public static void playerAndNpcHaveIdenticalDamageManifestationAndRefillCosts(GameTestHelper helper) {
      var npc = owner(helper); var target = target(helper, npc.position().add(0,0,12)); npc.setTarget(target);
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.tick(npc);
      close(helper, npc.getCurrentMp(), 980, "NPC refill MP cost");
      close(helper, npc.getPersistentData().getLong("OdaResetCooldown0") - npc.level().getGameTime(), 200, "NPC refill cooldown");
      var player = helper.makeMockServerPlayerInLevel(); player.setPos(npc.position().add(4,0,0));
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true; vars.servant_card_id = "oda_nobunaga";
      vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(1000);
      helper.assertTrue(ServantCardTransformManager.triggerAction(player,0), "Player refill");
      close(helper, vars.servantCardState().mana(), 980, "Player refill MP cost");
      close(helper, ServantCardTransformManager.getSkillCooldown(vars,0), 200, "Player refill cooldown");
      target.getPersistentData().putBoolean("TypeMoonTrait_divine",true);
      OdaAbilityService.cast(player,7); OdaAbilityService.cast(npc,7);
      player.getPersistentData().putBoolean("IndependentActionActive",true);
      player.getPersistentData().putFloat("IndependentActionCritDamageBonus",10);
      for (var ability : java.util.List.of(ORBIT, DASH, SCORCHED, PURSUIT_EXPLOSION, HAJUN, THREE_THOUSAND)) {
         float before = target.getHealth(); OdaAbilityService.hurt(player,target,10,ability,ability == SCORCHED);
         float playerDamage = before - target.getHealth();
         before = target.getHealth(); OdaAbilityService.hurt(npc,target,10,ability,ability == SCORCHED);
         close(helper, before - target.getHealth(), playerDamage, "Actor parity for " + ability);
         close(helper, playerDamage, ability == THREE_THOUSAND ? 30.16 : 26, "Manifestation/public system applied more than once for " + ability);
      }
      OdaAbilityService.clear(player); OdaAbilityService.clear(npc); helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_refillPreemptsCloseRangeSkill")
   public static void refillPreemptsCloseRangeSkillWhenFormationIsIncomplete(GameTestHelper helper) {
      var npc = owner(helper);
      var target = target(helper, npc.position().add(0, 0, 4));
      npc.setTarget(target);
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.tick(npc);
      close(helper, npc.getCurrentMp(), 980, "Refill did not get the first close-range skill decision");
      close(helper, npc.getPersistentData().getLong("OdaResetCooldown0") - npc.level().getGameTime(), 200, "Refill cooldown");
      helper.assertTrue(!npc.getPersistentData().contains("OdaResetCooldown-1"), "Close-range thrust ran before refill");
      OdaAbilityService.clear(npc);
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "odaMatchlockFlightStartsAtPhaseTwo")
   public static void matchlockFlightStartsAtSecondPhaseHealthThreshold(GameTestHelper helper) {
      var npc = owner(helper);
      var target = target(helper, npc.position().add(0, 0, 12));
      npc.setTarget(target);
      npc.setHealth(800);
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.tick(npc);
      helper.assertTrue(npc.isNoGravity(), "Matchlock mount flight did not start at 80% health");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.isCombatFlying(npc, npc.level().getGameTime()), "Phase-two flight was not reported to the flight controller");
      helper.assertTrue(!helper.getLevel().getEntitiesOfClass(OdaMatchlockGunEntity.class, npc.getBoundingBox().inflate(4), gun -> gun.isFootSupportFor(npc.getUUID())).isEmpty(), "Flight mount was not spawned");
      OdaAbilityService.clear(npc);
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "odaGenericThirdPhaseStartsNoblePhantasm")
   public static void genericThirdPhaseStartsNoblePhantasmAfterThirtySeconds(GameTestHelper helper) {
      var npc = owner(helper);
      var target = target(helper, npc.position().add(0, 0, 12));
      npc.setTarget(target);
      long now = npc.level().getGameTime();
      npc.getPersistentData().putLong("TypeMoonAiCombatStartedAt", now - 600L);
      npc.getPersistentData().putLong("TypeMoonAiLastCombatAt", now);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.tickBeforeAi(npc);
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.canUseNoblePhantasm(npc), "30-second generic phase did not unlock Nobunaga's NP");
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.tick(npc);
      helper.assertTrue(npc.getPersistentData().getLong(net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.TAG_THREE_THOUSAND_CHANT_END) > now, "Nobunaga did not start her NP after generic decisive phase");
      OdaAbilityService.clear(npc);
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_hajunPayment")
   public static void hajunStartupAndInterruptPaymentMatchForBothActors(GameTestHelper helper) {
      var npc = owner(helper);
      var medusa = ModEntities.MEDUSA.get().create(helper.getLevel()); medusa.setPos(npc.position().add(0,0,12)); medusa.setNoAi(true); helper.getLevel().addFreshEntity(medusa);
      npc.setTarget(medusa); npc.setHealth(500); npc.setCurrentMp(500);
      npc.getPersistentData().putLong(net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.TAG_LAND_FOR_NP_UNTIL, npc.level().getGameTime() + 500);
      net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.tick(npc);
      close(helper,npc.getCurrentMp(),465,"NPC Hajun startup");
      var player = helper.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true; vars.servant_card_id = "oda_nobunaga";
      vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(500);
      helper.assertTrue(ServantCardOdaNobunagaSkills.performOdaHajunAction(player,vars,ServantCardSkillLayout.actionFor("oda_nobunaga",9,false)),"Player Hajun startup");
      close(helper,vars.servantCardState().mana(),465,"Player Hajun startup cost");
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.cancel(player,true);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.cancel(npc,true);
      close(helper,vars.servantCardState().mana(),482.5,"Player interrupted startup refund");
      close(helper,npc.getCurrentMp(),482.5,"NPC interrupted startup refund");
      close(helper,vars.servantCardState().npCooldown(),1800,"Player interruption cooldown");
      close(helper,npc.getPersistentData().getLong("OdaSharedNpUntil") - npc.level().getGameTime(),1800,"NPC interruption cooldown");
      ServantCardOdaNobunagaSkills.clear(player); OdaAbilityService.clear(npc); helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_hajunExitKey")
   public static void hajunFieldExitUsesOnlyItsSkillSlot(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true;
      vars.servant_card_id = "oda_nobunaga";
      String dimension = player.level().dimension().location().toString();
      var data = player.getPersistentData();
      data.putLong("ServantCardOdaHajunFieldActiveUntil", player.level().getGameTime() + 100L);
      data.putString("ServantCardOdaHajunFieldReturnDim", dimension);
      data.putDouble("ServantCardOdaHajunFieldReturnX", player.getX());
      data.putDouble("ServantCardOdaHajunFieldReturnY", player.getY());
      data.putDouble("ServantCardOdaHajunFieldReturnZ", player.getZ());
      data.putString("OdaCardFieldDimension", dimension);
      ServantCardTransformManager.setSkillCooldownUntil(player, vars, 0, player.level().getGameTime() + 40L);
      ServantCardTransformManager.triggerAction(player, 0);
      helper.assertTrue(data.getLong("ServantCardOdaHajunFieldActiveUntil") > 0L,
         "A non-Hajun skill key exited the domain");
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Hajun skill key did not return the player");
      helper.assertTrue(data.getLong("ServantCardOdaHajunFieldActiveUntil") == 0L, "Hajun field state was not cleared on return");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_mountFlightDash")
   public static void otherFlyingServantDashesTheMatchlockMount(GameTestHelper helper) {
      var owner = helper.makeMockServerPlayerInLevel();
      var ownerVars = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ownerVars.servant_card_transformed = true;
      ownerVars.servant_card_id = "oda_nobunaga";
      var rider = helper.makeMockServerPlayerInLevel();
      var riderVars = rider.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      riderVars.servant_card_transformed = true;
      riderVars.servant_card_id = "enkidu";
      riderVars.servantCardState().setFlying(true);
      riderVars.servantCardState().setJumpCharges(4);
      var mount = OdaMatchlockGunEntity.flightMount(helper.getLevel(), owner);
      helper.getLevel().addFreshEntity(mount);
      helper.assertTrue(rider.startRiding(mount, true), "The other flying servant could not mount the matchlock");
      Vec3 start = mount.position();
      helper.assertTrue(ServantCardFlightController.tryDash(rider, 1.0F, 0.0F), "The mounted servant dash was rejected");
      helper.runAfterDelay(6, () -> {
         helper.assertTrue(mount.isAlive(), "The mount was discarded while another servant rode it");
         helper.assertTrue(mount.position().distanceToSqr(start) > 1.0, "The rider dash impulse did not move the matchlock");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_mountFlightSprint")
   public static void matchlockMountAccelerationUsesRiderSprintInput(GameTestHelper helper) {
      var owner = helper.makeMockServerPlayerInLevel();
      var ownerVars = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ownerVars.servant_card_transformed = true;
      ownerVars.servant_card_id = "oda_nobunaga";
      var rider = helper.makeMockServerPlayerInLevel();
      var riderVars = rider.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      riderVars.servant_card_transformed = true;
      riderVars.servant_card_id = "enkidu";
      var mount = OdaMatchlockGunEntity.flightMount(helper.getLevel(), owner);
      helper.getLevel().addFreshEntity(mount);
      helper.assertTrue(rider.startRiding(mount, true), "The other flying servant could not mount the matchlock");
      mount.setMountInput(1.0, 0.0, 0.0);
      helper.runAfterDelay(2, () -> {
         double baseSpeed = mount.getDeltaMovement().horizontalDistance();
         rider.getPersistentData().putBoolean("ServantCardFlightSprint", true);
         helper.runAfterDelay(3, () -> {
            helper.assertTrue(mount.getDeltaMovement().horizontalDistance() > baseSpeed * 1.5,
               "Rider sprint input did not increase the mount's acceleration");
            helper.succeed();
         });
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_ownedBurn")
   public static void hajunBurnRetainsCasterAndUsesTheSameSpecialZone(GameTestHelper helper) {
      var owner = owner(helper); var target = target(helper, owner.position().add(0,0,4));
      target.getPersistentData().putBoolean("TypeMoonTrait_divine",true);
      OdaAbilityService.hajunPulse(owner,helper.getLevel(),false);
      float before = target.getHealth(); target.invulnerableTime = 0;
      target.hurt(target.damageSources().onFire(),1);
      close(helper,before - target.getHealth(),2,"Owned burning did not receive one divinity modifier");
      OdaAbilityService.clear(owner);
      helper.assertTrue(!target.isOnFire() && !target.getPersistentData().hasUUID("OdaBurnOwner"),"Owned burning survived caster cleanup");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_finalEra")
   public static void futureEraScalesTheFinalDamageAndOrdinaryMobsRemainUnchanged(GameTestHelper helper) {
      var owner = owner(helper); var future = helper.makeMockServerPlayerInLevel();
      var vars = future.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES); vars.servant_card_transformed = true; vars.servant_card_id = "emiya_archer";
      var source = new OdaDamageSource(owner.damageSources().mobAttack(owner),owner,owner,ORBIT);
      var container = new net.neoforged.neoforge.common.damagesource.DamageContainer(source,100); container.setNewDamage(40);
      var event = new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(future,container); OdaCombatEvents.finalEra(event);
      close(helper,event.getNewDamage(),10,"Future reduction was not applied at the final damage stage");
      var ordinary = target(helper,owner.position().add(0,0,4));
      container = new net.neoforged.neoforge.common.damagesource.DamageContainer(source,100); container.setNewDamage(40);
      event = new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(ordinary,container); OdaCombatEvents.finalEra(event);
      close(helper,event.getNewDamage(),40,"Mob with no servant era was penalized");
      helper.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20, batch = "oda_foreignNp")
   public static void existingGallatinCannotOverwriteStrategyOrGunSubstitution(GameTestHelper helper) {
      var gawain = ModEntities.GAWAIN.get().create(helper.getLevel());
      var owner = owner(helper); owner.getAttribute(Attributes.ARMOR).setBaseValue(0);
      var gallatin = net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier.withNoblePhantasm(
         gawain.damageSources().mobAttack(gawain), "excalibur_gallatin");
      helper.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier.typeOf(gallatin)
         == ServantNoblePhantasmDefinition.NpType.ARMY, "Gallatin lost its configured anti-army category");
      var incoming = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(owner,
         new net.neoforged.neoforge.common.damagesource.DamageContainer(gallatin,100));
      OdaCombatEvents.outgoing(incoming);
      close(helper,incoming.getAmount(),84,"Existing anti-army NP ignored Strategy B");
      for (int i=0;i<3;i++) helper.getLevel().addFreshEntity(new OdaMatchlockGunEntity(owner.level(),owner,owner.position().add(i + 1,0,0),owner.getLookAngle(),100));
      incoming = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(owner,
         new net.neoforged.neoforge.common.damagesource.DamageContainer(gallatin,100));
      close(helper,OdaAbilityService.absorb(owner,100),10,"Remaining NP damage should reach the body after three guns");
      OdaAbilityService.clear(owner); helper.succeed();
   }
}

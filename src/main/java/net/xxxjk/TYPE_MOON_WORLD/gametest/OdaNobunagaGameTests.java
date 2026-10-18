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
   @GameTest(template = "ancient_temple", timeoutTicks = 100, batch = "oda_ninetyNineGunsFireExactlyTwoHundredNinetySevenShots")
   public static void ninetyNineGunsFireExactlyTwoHundredNinetySevenShots(GameTestHelper helper) {
      var owner = owner(helper);
      int[] count = {0};
      java.util.function.Consumer<EntityJoinLevelEvent> listener = event -> {
         if (event.getEntity() instanceof OdaMatchlockBulletEntity bullet && bullet.getOwner() == owner && bullet.ability() == THREE_THOUSAND) count[0]++;
      };
      NeoForge.EVENT_BUS.addListener(listener);
      OdaAbilityService.threeThousand(owner, null, true, owner.position());
      var guns = helper.getLevel().getEntitiesOfClass(OdaMatchlockGunEntity.class, owner.getBoundingBox().inflate(20), g -> g.isCombatFor(owner.getUUID()));
      helper.assertTrue(guns.size() == 99 && guns.stream().allMatch(g -> g.getShotsLeft() == 3), "NP gun/shot count");
      // Do not let this stress volley target entities belonging to neighboring concurrent tests.
      guns.forEach(gun -> gun.fixedDirection(new Vec3(0,1,0)));
      helper.runAfterDelay(80, () -> {
         NeoForge.EVENT_BUS.unregister(listener);
         close(helper, count[0], 297, "NP shot total");
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

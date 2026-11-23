package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;

@GameTestHolder("typemoonworld_carry")
@PrefixGameTestTemplate(false)
public final class ServantMasterCarryGameTests {
   private ServantMasterCarryGameTests() {}
   private record Pair(ServerPlayer servant, ServerPlayer master, LivingEntity attacker) implements AutoCloseable {
      @Override public void close() { ServantMasterCarryService.dismount(servant); servant.discard(); master.discard(); attacker.discard(); }
   }
   private static ServerPlayer player(GameTestHelper h, String name) {
      var cookie = CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), name), false);
      var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
      var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
      new io.netty.channel.embedded.EmbeddedChannel(connection);
      h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
      p.setGameMode(GameType.SURVIVAL);
      p.setPos(h.absolutePos(new BlockPos(4, 3, 4)).getCenter());
      for (int i = 0; i < 60; i++) p.tick();
      p.setNoGravity(true);
      return p;
   }
   private static Pair pair(GameTestHelper h) {
      var servant = player(h, "carry-servant");
      var master = player(h, "carry-master");
      h.assertTrue(ServantCardTransformManager.transform(servant, "oda_nobunaga"), "transform failed");
      var sv = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var mv = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      mv.master_active = true; mv.master_servant_uuid = servant.getUUID().toString();
      sv.servant_card_master_uuid = master.getUUID().toString();
      MasterServantLinkService.establishContract(master, mv, servant, sv);
      master.setPos(servant.position().add(1.0, 0, 0));
      ServantCombatDefenseService.initialize(servant);
      servant.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 0);
      servant.getPersistentData().putLong(ServantCombatDefenseService.AUTO_RESTORE, Long.MAX_VALUE);
      var attacker = h.spawn(EntityType.HUSK, new BlockPos(4, 3, 6));
      attacker.setNoAi(true); attacker.setNoGravity(true);
      return new Pair(servant, master, attacker);
   }
   private static void carry(GameTestHelper h, Pair p) { h.assertTrue(ServantMasterCarryService.tryCarry(p.servant, p.master), "pickup failed"); }
   private static void equal(GameTestHelper h, double expected, double actual, String message) {
      h.assertTrue(Math.abs(expected - actual) < 0.002, message + ": " + expected + " != " + actual);
   }
   private static void guard(GameTestHelper h, Pair p) {
      h.assertTrue(ServantCombatDefenseService.setGuarding(p.servant, true), "guard failed");
      p.servant.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_START, h.getLevel().getGameTime() - 20);
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void bothPlayersCanRequestPickupAndOffhandDoesNotToggle(GameTestHelper h) {
      try (var p = pair(h)) {
         p.master.setShiftKeyDown(true);
         var request = new PlayerInteractEvent.EntityInteract(p.master, InteractionHand.MAIN_HAND, p.servant);
         NeoForge.EVENT_BUS.post(request);
         h.assertTrue(request.isCanceled() && ServantMasterCarryService.isCarriedMaster(p.master), "master request failed");
         ServantMasterCarryService.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p.master));
         h.assertTrue(p.master.getVehicle() == p.servant, "held pickup Shift immediately dismounted");
         NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(p.master, InteractionHand.OFF_HAND, p.servant));
         h.assertTrue(p.master.getVehicle() == p.servant, "offhand toggled carry");
         ServantMasterCarryService.dismount(p.master);
         p.master.setShiftKeyDown(false); p.servant.setShiftKeyDown(true);
         NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(p.servant, InteractionHand.MAIN_HAND, p.master));
         h.assertTrue(p.master.getVehicle() == p.servant, "servant request failed");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void invalidPickupConditionsAreRejected(GameTestHelper h) {
      try (var p = pair(h)) {
         var sv = p.servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         String id = sv.servant_card_id; sv.servant_card_id = "unknown-carry-definition";
         h.assertTrue(!ServantMasterCarryService.tryCarry(p.servant, p.master), "missing strength data allowed pickup");
         sv.servant_card_id = id;
         p.master.setPos(p.servant.position().add(8, 0, 0));
         h.assertTrue(!ServantMasterCarryService.tryCarry(p.servant, p.master), "distance ignored");
         p.master.setPos(p.servant.position().add(1, 0, 0));
         var passenger = h.spawn(EntityType.PIG, new BlockPos(4, 3, 4)); passenger.startRiding(p.servant, true);
         h.assertTrue(!ServantMasterCarryService.tryCarry(p.servant, p.master), "existing passenger ignored");
         passenger.stopRiding(); passenger.discard();
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void transferSkipsMasterArmorAndPreservesSource(GameTestHelper h) {
      try (var p = pair(h)) {
         carry(h, p);
         p.master.getAttribute(Attributes.ARMOR).setBaseValue(1000);
         var source = p.attacker.damageSources().mobAttack(p.attacker);
         int[] masterEvents = {0}, servantEvents = {0}; float[] incoming = {0};
         Consumer<LivingIncomingDamageEvent> listener = event -> {
            if (event.getEntity() == p.master) masterEvents[0]++;
            if (event.getEntity() == p.servant) { servantEvents[0]++; incoming[0] = event.getOriginalAmount(); h.assertTrue(event.getSource() == source, "source replaced"); }
         };
         NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, listener);
         try { float hp = p.master.getHealth(); p.master.hurt(source, 40); equal(h, hp, p.master.getHealth(), "master damaged"); }
         finally { NeoForge.EVENT_BUS.unregister(listener); }
         h.assertTrue(masterEvents[0] == 0 && servantEvents[0] == 1, "wrong damage recipients/count");
         equal(h, 40, incoming[0], "master reduced incoming damage");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void environmentalDamageTransfersButPassengerFallAndWallDoNot(GameTestHelper h) {
      try (var p = pair(h)) {
         carry(h, p); float masterHp = p.master.getHealth(), servantHp = p.servant.getHealth();
         p.master.hurt(p.master.damageSources().fall(), 100);
         p.master.hurt(p.master.damageSources().inWall(), 100);
         equal(h, servantHp, p.servant.getHealth(), "passenger placement damaged servant");
         for (var source : new net.minecraft.world.damagesource.DamageSource[]{p.master.damageSources().onFire(), p.master.damageSources().wither(), p.master.damageSources().source(net.neoforged.neoforge.common.NeoForgeMod.POISON_DAMAGE)}) {
            p.servant.invulnerableTime = 0; p.master.invulnerableTime = 0;
            p.master.hurt(source, 6);
         }
         equal(h, masterHp, p.master.getHealth(), "environment damaged master");
         h.assertTrue(p.servant.getHealth() < servantHp, "environment never reached servant");
      }
      h.succeed();
   }
   private static void area(GameTestHelper h, boolean masterFirst) {
      try (var p = pair(h)) {
         carry(h, p); var source = p.attacker.damageSources().mobAttack(p.attacker);
         int[] count = {0}; float[] amount = {0};
         Consumer<LivingIncomingDamageEvent> listener = event -> { if (event.getEntity() == p.servant) { count[0]++; amount[0] = event.getOriginalAmount(); } };
         NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, listener);
         float hp = p.master.getHealth();
         try (var segment = ServantMasterDamageRouter.attackSegment()) {
            if (masterFirst) p.master.hurt(source, 30);
            p.servant.hurt(source, 40);
            if (!masterFirst) p.master.hurt(source, 30);
         } finally { NeoForge.EVENT_BUS.unregister(listener); }
         h.assertTrue(count[0] == 1, "area attack settled more than once"); equal(h, 40, amount[0], "area did not take max");
         equal(h, hp, p.master.getHealth(), "area damaged master");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30) public static void areaMasterFirst(GameTestHelper h) { area(h, true); }
   @GameTest(template = "ancient_temple", timeoutTicks = 30) public static void areaServantFirst(GameTestHelper h) { area(h, false); }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void sameTickSeparateSegmentsAndProjectilesStayIndependent(GameTestHelper h) {
      try (var p = pair(h)) {
         carry(h, p); int[] count = {0};
         Consumer<LivingIncomingDamageEvent> listener = event -> { if (event.getEntity() == p.servant) count[0]++; };
         NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, listener);
         try {
            for (int i = 0; i < 3; i++) {
               p.master.invulnerableTime = 0;
               try (var segment = ServantMasterDamageRouter.attackSegment()) { p.master.hurt(p.attacker.damageSources().mobAttack(p.attacker), 5); }
            }
         } finally { NeoForge.EVENT_BUS.unregister(listener); }
         h.assertTrue(count[0] == 3, "same-tick attacks merged");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void lethalCausalAreaNeverSpillsBackToMaster(GameTestHelper h) {
      try (var p = pair(h)) {
         // Existing servant-loss backlash lowers maximum HP by 20%; keep current HP below that cap.
         p.master.setHealth(10);
         carry(h, p); float hp = p.master.getHealth();
         var source = h.getLevel().damageSources().source(net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaDamageTypes.TSUMUKARI_MURAMASA, p.attacker, p.attacker);
         try (var segment = ServantMasterDamageRouter.attackSegment()) {
            p.servant.hurt(source, 40); p.master.hurt(source, 30);
         }
         h.assertTrue(!p.servant.isAlive(), "causal damage lost lethality");
         equal(h, hp, p.master.getHealth(), "lethal damage spilled to master");
         h.assertTrue(!p.master.isPassenger(), "death left passenger attached");
         p.master.invulnerableTime = 0; p.master.hurt(p.master.damageSources().generic(), 3);
         h.assertTrue(p.master.getHealth() < hp, "master retained protection after death");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void activeGuardProtectsAtTwoBlocksWithoutCarrying(GameTestHelper h) {
      try (var p = pair(h)) {
         p.master.setPos(p.servant.position().add(2, 0, 0)); guard(h, p);
         h.assertTrue(ServantMasterCarryService.protector(p.master) == p.servant, "two-block guard absent");
         float hp = p.master.getHealth(); p.master.hurt(p.attacker.damageSources().mobAttack(p.attacker), 30);
         equal(h, hp, p.master.getHealth(), "guard did not protect master");
         h.assertTrue(p.servant.getPersistentData().getInt("TypeMoonDefenseGuardCount") == 1, "guard not settled once");
         p.master.setPos(p.servant.position().add(2.01, 0, 0));
         h.assertTrue(ServantMasterCarryService.protector(p.master) == null, "guard exceeded two blocks");
         p.master.setPos(p.servant.position().add(1, 0, 0));
         p.servant.getPersistentData().putLong(ServantCombatSystem.TAG_STUN_UNTIL, h.getLevel().getGameTime() + 40);
         h.assertTrue(ServantMasterCarryService.protector(p.master) == null, "stunned guard protected master");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void proximityAloneDoesNotProtectAndContractLossCleansCarry(GameTestHelper h) {
      try (var p = pair(h)) {
         h.assertTrue(ServantMasterCarryService.protector(p.master) == null, "proximity auto-protected");
         carry(h, p);
         MasterServantLinkService.clearPlayerContract(p.master, p.master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
         h.assertTrue(!p.master.isPassenger() && ServantMasterCarryService.protector(p.master) == null, "contract cleanup failed");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void nativeExplosionSettlesOnlyMaximumCandidate(GameTestHelper h) {
      try (var p = pair(h)) {
         carry(h, p);
         var origin = p.servant.position().add(0, 0, 2);
         var explosion = new net.minecraft.world.level.Explosion(h.getLevel(), p.attacker,
            origin.x, origin.y, origin.z, 3.0F, false, net.minecraft.world.level.Explosion.BlockInteraction.KEEP);
         var calculator = new net.minecraft.world.level.ExplosionDamageCalculator();
         float expected = Math.max(calculator.getEntityDamageAmount(explosion, p.servant), calculator.getEntityDamageAmount(explosion, p.master));
         int[] count = {0}; float[] damage = {0};
         Consumer<LivingIncomingDamageEvent> listener = event -> {
            if (event.getEntity() == p.servant && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
               count[0]++; damage[0] = event.getOriginalAmount();
            }
         };
         NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, listener);
         float hp = p.master.getHealth();
         try { explosion.explode(); } finally { NeoForge.EVENT_BUS.unregister(listener); }
         h.assertTrue(count[0] == 1, "native explosion settled multiple hits");
         equal(h, expected, damage[0], "native explosion did not use maximum candidate");
         equal(h, hp, p.master.getHealth(), "native explosion damaged master");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void forcedHealthSettlementAlsoTargetsProtector(GameTestHelper h) {
      try (var p = pair(h)) {
         p.master.setHealth(10); carry(h, p); float hp = p.master.getHealth();
         int[] settlements = {0};
         try (var segment = ServantMasterDamageRouter.attackSegment()) {
            for (LivingEntity target : new LivingEntity[]{p.master, p.servant}) {
               h.assertTrue(ServantMasterDamageRouter.special(target, 100, defender -> {
                  settlements[0]++; defender.setHealth(0); defender.die(p.attacker.damageSources().genericKill());
               }), "special route was not accepted");
            }
         }
         h.assertTrue(settlements[0] == 1 && !p.servant.isAlive(), "special settled twice or missed servant");
         equal(h, hp, p.master.getHealth(), "forced fallback killed master");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void deferredAreaPulsesRetainTheirAttackSegment(GameTestHelper h) {
      var p = pair(h); carry(h, p);
      float hp = p.master.getHealth();
      int[] count = {0};
      Consumer<LivingIncomingDamageEvent> listener = event -> { if (event.getEntity() == p.servant) count[0]++; };
      NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, listener);
      try (var origin = ServantMasterDamageRouter.attackSegment()) {
         for (var target : new ServerPlayer[]{p.master, p.servant}) {
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(1, () -> {
               target.invulnerableTime = 0; target.hurt(p.attacker.damageSources().mobAttack(p.attacker), 5);
            });
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(2, () -> {
               target.invulnerableTime = 0; target.hurt(p.attacker.damageSources().mobAttack(p.attacker), 5);
            });
         }
      }
      h.runAfterDelay(4, () -> {
         try { h.assertTrue(count[0] == 2, "deferred pulses merged with each other or hit twice"); equal(h, hp, p.master.getHealth(), "deferred pulse hurt master"); }
         finally { NeoForge.EVENT_BUS.unregister(listener); p.close(); }
         h.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void shiftReleaseAndAirDismountDoNotGrantImmunity(GameTestHelper h) {
      try (var p = pair(h)) {
         p.master.setShiftKeyDown(true); carry(h, p);
         p.servant.setOnGround(false);
         p.master.setShiftKeyDown(false);
         ServantMasterCarryService.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p.master));
         p.master.setShiftKeyDown(true);
         ServantMasterCarryService.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p.master));
         h.assertTrue(!p.master.isPassenger(), "new Shift press failed to dismount in air");
         float hp = p.master.getHealth(); p.master.invulnerableTime = 0;
         p.master.hurt(p.master.damageSources().fall(), 3);
         h.assertTrue(p.master.getHealth() < hp, "air dismount granted fall immunity");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void carryBlocksAttacksAndSkillsButPreservesMasterItemsAndHealthCosts(GameTestHelper h) {
      try (var p = pair(h)) {
         carry(h, p);
         var sv = p.servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         double mana = sv.servant_card_mana;
         h.assertTrue(!ServantCardTransformManager.triggerAction(p.servant, 0), "active skill escaped restriction");
         h.assertTrue(!net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantTrueSweepService.triggerPlayerAttack(p.servant, sv), "sweep escaped restriction");
         for (var player : new ServerPlayer[]{p.master, p.servant}) {
            var attack = new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(player, p.attacker);
            NeoForge.EVENT_BUS.post(attack); h.assertTrue(attack.isCanceled(), "normal attack escaped restriction");
         }
         equal(h, mana, sv.servant_card_mana, "blocked skill spent mana");
         var use = new PlayerInteractEvent.RightClickItem(p.master, InteractionHand.MAIN_HAND);
         ServantMasterCarryService.onRightClickItem(use);
         h.assertTrue(!use.isCanceled(), "master item use was blocked");
         var mv = p.master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         mv.player_max_mana = 100; mv.player_mana = 0;
         float masterHp = p.master.getHealth(), servantHp = p.servant.getHealth();
         net.xxxjk.TYPE_MOON_WORLD.procedures.Manually_deduct_health_to_restore_mana.execute(p.master);
         equal(h, masterHp - 1, p.master.getHealth(), "life payment transferred or was canceled");
         equal(h, servantHp, p.servant.getHealth(), "servant paid master's life cost");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void weakServantCanStillOfferActiveGuardAndBrokenGuardCannot(GameTestHelper h) {
      try (var p = pair(h)) {
         p.servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_id = "medea";
         h.assertTrue(!ServantMasterCarryService.canCarry(p.servant), "E strength passed carry threshold");
         h.assertTrue(!ServantMasterCarryService.tryCarry(p.servant, p.master), "weak servant carried master");
         guard(h, p);
         h.assertTrue(ServantMasterCarryService.protector(p.master) == p.servant, "carry threshold leaked into guard protection");
         p.servant.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_BROKEN, Long.MAX_VALUE);
         h.assertTrue(ServantMasterCarryService.protector(p.master) == null, "broken guard protected master");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void blockedSightAndOccupiedVehiclesRejectPickup(GameTestHelper h) {
      try (var p = pair(h)) {
         p.master.setPos(p.servant.position().add(2.4, 0, 0));
         BlockPos wall = p.servant.blockPosition().offset(1, 1, 0);
         h.getLevel().setBlock(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
         h.assertTrue(!ServantMasterCarryService.tryCarry(p.servant, p.master), "pickup passed through wall");
         h.getLevel().setBlock(wall, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
         var mount = h.spawn(EntityType.PIG, new BlockPos(4, 3, 4));
         p.master.startRiding(mount, true);
         h.assertTrue(!ServantMasterCarryService.tryCarry(p.servant, p.master), "riding master was picked up");
         p.master.stopRiding(); mount.discard();
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void pickupInterruptsChantAndKeepsReleasedProjectiles(GameTestHelper h) {
      try (var p = pair(h)) {
         var vars = p.servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.is_chanting_ubw = true; vars.ubw_chant_progress = 2;
         var arrow = EntityType.ARROW.create(h.getLevel());
         arrow.setOwner(p.servant); arrow.setPos(p.servant.position()); h.getLevel().addFreshEntity(arrow);
         carry(h, p);
         h.assertTrue(!vars.is_chanting_ubw && vars.ubw_chant_progress == 0, "chant survived pickup");
         h.assertTrue(arrow.isAlive() && !arrow.isRemoved(), "released projectile was discarded");
         arrow.discard();
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void guardAreaSettlesOneResourceTransaction(GameTestHelper h) {
      try (var p = pair(h)) {
         guard(h, p); float hp = p.master.getHealth();
         try (var segment = ServantMasterDamageRouter.attackSegment()) {
            p.master.hurt(p.attacker.damageSources().mobAttack(p.attacker), 30);
            p.servant.hurt(p.attacker.damageSources().mobAttack(p.attacker), 40);
         }
         equal(h, hp, p.master.getHealth(), "guarded area damaged master");
         h.assertTrue(p.servant.getPersistentData().getInt("TypeMoonDefenseGuardCount") == 1, "area consumed guard twice");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void logoutCleansPassengerRelationship(GameTestHelper h) {
      try (var p = pair(h)) {
         carry(h, p);
         ServantMasterCarryService.onLogout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p.servant));
         h.assertTrue(!p.master.isPassenger() && !ServantMasterCarryService.isCarryingMaster(p.servant), "logout left carry attached");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void modForwardArcUsesTheSameSingleSettlement(GameTestHelper h) {
      try (var p = pair(h)) {
         carry(h, p);
         var caster = player(h, "carry-arc-caster");
         h.assertTrue(ServantCardTransformManager.transform(caster, "oda_nobunaga"), "arc caster transform failed");
         caster.setPos(p.servant.position().add(0, 0, 2));
         int[] count = {0};
         Consumer<LivingIncomingDamageEvent> listener = event -> { if (event.getEntity() == p.servant) count[0]++; };
         NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, listener);
         float hp = p.master.getHealth();
         try { ServantCardSkillUtils.hitForwardArc(caster, new net.minecraft.world.phys.Vec3(0, 0, -1), 4.0, 10); }
         finally { NeoForge.EVENT_BUS.unregister(listener); caster.discard(); }
         h.assertTrue(count[0] == 1, "MOD arc did not merge the carry pair: settlements=" + count[0]);
         equal(h, hp, p.master.getHealth(), "MOD arc damaged master");
      }
      h.succeed();
   }
}

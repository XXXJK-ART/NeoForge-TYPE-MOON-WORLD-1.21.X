package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaWindDefenseSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillLayout;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;

@GameTestHolder("typemoonworld_artoria")
@PrefixGameTestTemplate(false)
public final class ArtoriaResetGameTests {
   private static void arena(GameTestHelper h) {
      for (int x = 1; x <= 9; x++) for (int z = 1; z <= 12; z++) for (int y = 2; y <= 8; y++)
         h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
   }
   private static ArtoriaPendragonEntity npc(GameTestHelper h) {
      arena(h);
      var owner = ModEntities.ARTORIA_PENDRAGON.get().create(h.getLevel());
      owner.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 3, 4))));
      owner.setNoAi(true); owner.setNoGravity(true);
      h.getLevel().addFreshEntity(owner);
      owner.ensureDefaultNpcLoadout(false); owner.setCurrentMp(500);
      owner.setYRot(0); owner.setYHeadRot(0); owner.setXRot(0);
      return owner;
   }
   private static ServerPlayer card(GameTestHelper h) {
      arena(h);
      var owner = h.makeMockServerPlayerInLevel();
      owner.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 3, 4))));
      h.assertTrue(ServantCardTransformManager.transform(owner, ArtoriaRules.ID), "Saber transform rejected");
      owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(500);
      return owner;
   }
   private static Husk target(GameTestHelper h, Vec3 position) {
      var target = EntityType.HUSK.create(h.getLevel());
      target.setPos(position); target.setNoAi(true); target.setNoGravity(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
      target.getAttribute(Attributes.ARMOR).setBaseValue(0);
      target.setHealth(1000); h.getLevel().addFreshEntity(target); return target;
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void npcOnlyReleasesInvisibleAirInDecisivePhase(GameTestHelper h) {
      var owner = npc(h);
      double mana = owner.getCurrentMp();
      h.assertTrue(ArtoriaStateService.isWindBarrierEnabled(owner), "NPC did not start with Invisible Air active");
      h.assertTrue(!ArtoriaAbilityService.tryNpcLightCannon(owner), "NPC used the short Excalibur before phase three");
      h.assertTrue(Math.abs(owner.getCurrentMp() - mana) < 0.001, "Early short Excalibur consumed mana");
      ArtoriaStateService.autoReleaseWindBarrier(owner);
      h.assertTrue(ArtoriaStateService.isWindBarrierEnabled(owner), "NPC released Invisible Air before phase three");
      ServantCombatSystem.forcePhaseAtLeast(owner, ServantCombatPhase.DECISIVE);
      ArtoriaStateService.autoReleaseWindBarrier(owner);
      h.assertTrue(!ArtoriaStateService.isWindBarrierEnabled(owner), "NPC did not release Invisible Air in phase three");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void playerAndNpcHaveTheSameIsolatedSwordComponents(GameTestHelper h) {
      var owner = npc(h);
      ArtoriaAbilityService.reveal(owner, 100);
      ArtoriaStateService.startBurst(owner);
      var source = new CombatDamageSource(owner.damageSources().mobAttack(owner), AttackOrigin.skill(ArtoriaRules.ID, "basic"));
      float normal = ArtoriaPendragonCombatHelper.applyManaBurstOutgoing(owner, source, 33);
      h.assertTrue(Math.abs(ArtoriaPendragonCombatHelper.applyRevealedSwordBonus(owner, source, normal) - 330) < .001, "NPC Mana Burst multiplier mismatch");
      var player = card(h);
      ArtoriaAbilityService.reveal(player, 100); ArtoriaStateService.startBurst(player);
      var hit = new CombatDamageSource(player.damageSources().playerAttack(player), AttackOrigin.skill(ArtoriaRules.ID, "basic"), .5F);
      normal = ArtoriaPendragonCombatHelper.applyManaBurstOutgoing(player, hit, 16.5F);
      h.assertTrue(Math.abs(ArtoriaPendragonCombatHelper.applyRevealedSwordBonus(player, hit, normal) - 165) < .001, "Player Mana Burst multiplier mismatch");
      var wind = new CombatDamageSource(player.damageSources().playerAttack(player), AttackOrigin.skill(ArtoriaRules.ID, "invisible_air_hammer"));
      h.assertTrue(ArtoriaPendragonCombatHelper.applyRevealedSwordBonus(player, wind, 90) == 90
         && ArtoriaPendragonCombatHelper.applyManaBurstOutgoing(player, wind, 90) == 90, "Wind inherited sword bonuses");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void actualSwordHitAddsOnePhysicalComponent(GameTestHelper h) {
      var owner = npc(h); var target = target(h, owner.position().add(0, 0, 3));
      ArtoriaAbilityService.reveal(owner, 100); ArtoriaStateService.startBurst(owner);
      target.hurt(new CombatDamageSource(owner.damageSources().mobAttack(owner), AttackOrigin.skill(ArtoriaRules.ID, "basic")), 33);
      h.assertTrue(Math.abs(target.getHealth() - 670) < .02, "Actual physical hit was not 330: " + target.getHealth());
      h.assertTrue(Math.abs(owner.getCurrentMp() - (500 - ArtoriaRules.BURST_SWORD_HIT_COST)) < .02,
         "Mana Burst did not debit exactly 20 MP after the landed hit: " + owner.getCurrentMp());
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 18)
   public static void knightTripleSlashDealsThreeSeparateSixteenSixteenTwentyHits(GameTestHelper h) {
      var owner = npc(h); var target = target(h, owner.position().add(0, 0, 3));
      h.assertTrue(ArtoriaAbilityService.tryNpcSkill(owner, ArtoriaRules.KNIGHT_TRIPLE_SLASH), "NPC could not start the triple slash");
      h.assertTrue(Math.abs(owner.getCurrentMp() - (500 - ArtoriaRules.TRIPLE_SLASH_MP)) < 0.001, "Triple slash MP cost mismatch");
      h.runAtTickTime(14, () -> {
         h.assertTrue(Math.abs(target.getHealth() - 948) < 0.01, "Triple slash total was not 52: " + target.getHealth());
         var swordHit = new CombatDamageSource(owner.damageSources().mobAttack(owner),
            AttackOrigin.skill(ArtoriaRules.ID, ArtoriaRules.KNIGHT_TRIPLE_SLASH));
         h.assertTrue(Math.abs(ArtoriaPendragonCombatHelper.applyManaBurstOutgoing(owner, swordHit, 52) - 52) < 0.001,
            "Inactive Mana Burst modified sword damage");
         ArtoriaAbilityService.reveal(owner, 100);
         ArtoriaStateService.startBurst(owner);
          h.assertTrue(Math.abs(ArtoriaPendragonCombatHelper.applyManaBurstOutgoing(owner, swordHit, 52) - 520) < 0.001,
             "Active Mana Burst did not apply the tenfold multiplier once");
         ArtoriaStateService.stopBurst(owner);
         h.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 16)
   public static void swordTechniquesCanStartWithoutASelectedTarget(GameTestHelper h) {
      var owner = npc(h);
      for (String skill : new String[] { ArtoriaRules.KNIGHT_TRIPLE_SLASH, ArtoriaRules.INTERCEPT_THRUST,
         ArtoriaRules.SPINNING_SLASH, ArtoriaRules.UPWARD_SLASH, ArtoriaRules.KNIGHT_HEAVY_SLASH }) {
         owner.getPersistentData().putLong(ArtoriaAbilityService.ACTION_LOCK_UNTIL, owner.level().getGameTime());
         h.assertTrue(ArtoriaAbilityService.canPerformSwordSkill(owner, skill), skill + " still requires a target to activate");
         h.assertTrue(ArtoriaAbilityService.performSwordSkill(owner, skill), skill + " did not start without a target");
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void manaBurstChargesSixtyToActivateAndThirtyPerSecond(GameTestHelper h) {
      var owner = card(h);
      var state = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      h.assertTrue(ServantCardTransformManager.triggerAction(owner, 0), "Mana Burst did not activate");
      h.assertTrue(Math.abs(state.mana() - 440.0) < 0.001, "Mana Burst did not charge 60 MP at activation: " + state.mana());
      owner.getPersistentData().putLong(ArtoriaStateService.NEXT_DRAIN, owner.level().getGameTime());
      ArtoriaStateService.tick(owner);
      h.assertTrue(Math.abs(state.mana() - 410.0) < 0.001, "Mana Burst did not drain 30 MP per second: " + state.mana());
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 12)
   public static void guardCounterBlocksOnlyTheFirstFrontFacingMeleeHit(GameTestHelper h) {
      var owner = npc(h); var attacker = target(h, owner.position().add(0, 0, 2));
      float health = owner.getHealth();
      h.assertTrue(ArtoriaAbilityService.performSwordSkill(owner, ArtoriaRules.GUARD_COUNTER), "Counter stance did not start");
      boolean damaged = owner.hurt(attacker.damageSources().mobAttack(attacker), 12);
      h.assertTrue(!damaged && Math.abs(owner.getHealth() - health) < 0.001, "Counter did not block the incoming hit");
      h.assertTrue(Math.abs(attacker.getHealth() - 962) < 0.01, "Counter response was not 38: " + attacker.getHealth());
      h.assertTrue(!owner.getPersistentData().contains(ArtoriaAbilityService.COUNTER_UNTIL), "Counter did not consume its window");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 12)
   public static void windBarrierToggleUsesSlotNineAndShortExcaliburIsNotASkillSlot(GameTestHelper h) {
      var owner = card(h);
      var state = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      h.assertTrue(ServantCardTransformManager.triggerAction(owner, 9), "Slot nine wind toggle was rejected");
      h.assertTrue(!ArtoriaStateService.isWindBarrierEnabled(owner), "Slot nine did not reveal Excalibur");
      h.assertTrue(Math.abs(state.mana() - 500) < 0.001 && state.npCooldown() == 0,
         "Wind toggle charged mana or the NP cooldown");
      h.assertTrue(ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 6, false).effectId().equals(ArtoriaRules.WIND_DEFENSE),
         "Slot six is not Wind Defense");
      h.assertTrue(ServantCardSkillLayout.actionFor(ArtoriaRules.ID, 9, false).effectId().equals(ArtoriaRules.WIND_BARRIER_TOGGLE),
         "Slot nine is not the Wind King barrier toggle");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void windDefenseReflectsProjectilesPushesEnemiesAndClearsOnRelease(GameTestHelper h) {
      var owner = npc(h);
      var enemy = target(h, owner.position().add(0, 0, 3));
      var shooter = target(h, owner.position().add(0, 0, 8));
      double mpBefore = owner.getCurrentMp();
      h.assertTrue(ArtoriaStateService.startWindDefense(owner, false), "NPC wind defense did not start");
      h.assertTrue(Math.abs(owner.getCurrentMp() - (mpBefore - ArtoriaRules.WIND_DEFENSE_MP)) < 0.001,
         "Wind defense payment mismatch");
       h.assertTrue(ArtoriaStateService.isWindDefenseActive(owner)
          && !owner.getPersistentData().hasUUID(ArtoriaStateService.WIND_DEFENSE_SWORD),
          "Defense field did not start as an animation-only action");

      var projectile = new net.minecraft.world.entity.projectile.Snowball(h.getLevel(), shooter);
      projectile.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ() + 3.0);
      projectile.setDeltaMovement(0, 0, -1);
      h.getLevel().addFreshEntity(projectile);
      var impact = new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(projectile,
         new net.minecraft.world.phys.EntityHitResult(owner));
      ArtoriaStateService.blockProjectileImpact(impact);
      h.assertTrue(impact.isCanceled(), "Wind defense did not cancel the projectile impact");
      h.assertTrue(projectile.getDeltaMovement().z > 0, "Projectile did not turn outward");

      ArtoriaStateService.tick(owner);
      Vec3 away = enemy.position().subtract(owner.position()).multiply(1, 0, 1).normalize();
      h.assertTrue(enemy.getDeltaMovement().multiply(1, 0, 1).dot(away) > 0.1, "Wind field did not push the nearby enemy");
      ArtoriaStateService.stopWindDefense(owner);
       h.assertTrue(!ArtoriaStateService.isWindDefenseActive(owner)
          && !owner.getPersistentData().hasUUID(ArtoriaStateService.WIND_DEFENSE_SWORD), "Release did not clear the field");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 12)
   public static void cardWindDefensePaysOnceAndReleaseStopsTheField(GameTestHelper h) {
      var owner = card(h);
      var mana = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      ServantCardTransformManager.handleHoldAction(owner, 6, true);
      h.assertTrue(ArtoriaStateService.isWindDefenseActive(owner), "Slot six did not start the held defense");
      h.assertTrue(Math.abs(mana.mana() - (500 - ArtoriaRules.WIND_DEFENSE_MP)) < 0.001,
         "Player wind defense was charged incorrectly");
      ServantCardTransformManager.handleHoldAction(owner, 6, false);
      h.assertTrue(!ArtoriaStateService.isWindDefenseActive(owner)
         && !owner.getPersistentData().hasUUID(ArtoriaStateService.WIND_DEFENSE_SWORD), "Key release did not end the field");
      h.assertTrue(owner.getPersistentData().getLong(ArtoriaStateService.WIND_READY)
         >= owner.level().getGameTime() + ArtoriaRules.WIND_REGATHER_TICKS,
         "Key release did not start shared wind recovery");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 112)
   public static void windDefenseAutomaticallyEndsAtFiveSeconds(GameTestHelper h) {
      var owner = npc(h);
      h.assertTrue(ArtoriaStateService.startWindDefense(owner, false), "Wind defense did not start for timeout test");
      h.runAtTickTime(ArtoriaRules.WIND_DEFENSE_MAX_TICKS + 2, () -> {
         h.assertTrue(!ArtoriaStateService.isWindDefenseActive(owner), "Wind defense exceeded its five-second cap");
          h.assertTrue(!owner.getPersistentData().hasUUID(ArtoriaStateService.WIND_DEFENSE_SWORD), "Timed defense left sword state behind");
         h.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 8)
   public static void lightCannonDealsFixedHundredMagicDamage(GameTestHelper h) {
      var owner = npc(h); var target = target(h, owner.position().add(0, 0, 8));
      ArtoriaAbilityService.lightCannon(owner);
      h.assertTrue(Math.abs(target.getHealth() - 900) < 0.01, "Light cannon damage was not fixed at 100: " + target.getHealth());
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 8)
   public static void lightCannonRemovesAtMostSixSoftBlocks(GameTestHelper h) {
      var owner = npc(h);
      // Keep the temporary arena open above the beam so surface-depth protection
      // measures the intended floor instead of the temple roof.
      for (int x = 1; x <= 9; x++) for (int z = 1; z <= 12; z++) for (int y = 9; y <= 64; y++)
         h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
      for (int z = 6; z <= 12; z++) h.setBlock(new BlockPos(4, 4, z), Blocks.GRASS_BLOCK);
      boolean oldTerrain = net.xxxjk.TYPE_MOON_WORLD.Config.terrainDestructionEnabled;
      boolean oldNpcTerrain = net.xxxjk.TYPE_MOON_WORLD.Config.npcTerrainDestructionEnabled;
      boolean oldMobGriefing = h.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
      net.xxxjk.TYPE_MOON_WORLD.Config.terrainDestructionEnabled = true;
      net.xxxjk.TYPE_MOON_WORLD.Config.npcTerrainDestructionEnabled = true;
      try {
         h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(true, h.getLevel().getServer());
         BlockPos first = h.absolutePos(new BlockPos(4, 4, 6));
         var firstState = h.getLevel().getBlockState(first);
         h.assertTrue(firstState.is(Blocks.GRASS_BLOCK), "Expected first beam block is missing: " + first + " / " + firstState);
         Vec3 start = owner.getEyePosition();
         Vec3 finish = start.add(owner.getLookAngle().normalize().scale(32));
         var clipped = h.getLevel().clip(new net.minecraft.world.level.ClipContext(start, finish,
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, owner));
         h.assertTrue(clipped instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getBlockPos().equals(first),
            "Beam did not clip the first block: " + clipped + " expected=" + first + " eye=" + start + " look=" + owner.getLookAngle());
         h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.world.terrain.CombatTerrainProtection.canRemoveWithinSurfaceDepth(h.getLevel(), first, 1),
            "Terrain protection rejected the allowed shallow block");
         ArtoriaAbilityService.lightCannon(owner);
         for (int z = 6; z < 12; z++) h.assertTrue(h.getBlockState(new BlockPos(4, 4, z)).isAir(), "Beam left a soft block before the six-block cap: " + z);
         h.assertTrue(h.getBlockState(new BlockPos(4, 4, 12)).is(Blocks.GRASS_BLOCK), "Beam broke more than six blocks");
      } finally {
         net.xxxjk.TYPE_MOON_WORLD.Config.terrainDestructionEnabled = oldTerrain;
         net.xxxjk.TYPE_MOON_WORLD.Config.npcTerrainDestructionEnabled = oldNpcTerrain;
         h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(oldMobGriefing, h.getLevel().getServer());
      }
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void redSpearBypassesOnlyTheMagicDefenseOnFirstContact(GameTestHelper h) {
      var owner = npc(h); var attacker = target(h, owner.position().add(0, 0, 3));
      ArtoriaStateService.startBurst(owner);
      var normal = attacker.damageSources().mobAttack(attacker);
      var red = new CombatDamageSource(normal, AttackOrigin.skill("diarmuid_ua_duibhne", "red_contact"), 1, true);
      h.assertTrue(Math.abs(ArtoriaPendragonCombatHelper.applyManaBurstDefense(owner, normal, 100) - 30) < .001, "Burst defense mismatch");
      h.assertTrue(ArtoriaPendragonCombatHelper.applyManaBurstDefense(owner, red, 100) == 100, "First red contact was reduced");
      owner.getPersistentData().putLong(DiarmuidCombatHelper.MAGIC_DEFENSE_BROKEN_TAG, owner.level().getGameTime() + 100);
      h.assertTrue(ArtoriaPendragonCombatHelper.applyManaBurstDefense(owner, normal, 100) == 100, "Broken window not applied");
      h.assertTrue(ArtoriaPendragonCombatHelper.isManaBurstActive(owner), "Red contact disabled offensive burst");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void emptyManaStopsBurstAndKeepsReopenCooldown(GameTestHelper h) {
      var owner = npc(h); double speed = owner.getAttributeValue(Attributes.MOVEMENT_SPEED);
      ArtoriaStateService.startBurst(owner);
      h.assertTrue(Math.abs(owner.getAttributeValue(Attributes.MOVEMENT_SPEED) / speed - 1.15) < .001, "Speed is not exactly +15%");
      owner.setCurrentMp(0); owner.getPersistentData().putLong(ArtoriaStateService.NEXT_DRAIN, owner.level().getGameTime());
      ArtoriaStateService.tick(owner);
      h.assertTrue(!ArtoriaPendragonCombatHelper.isManaBurstActive(owner), "Unpaid burst did not stop");
      h.assertTrue(Math.abs(owner.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < .001, "Speed modifier survived shutdown");
      long deadline = owner.getPersistentData().getLong(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_REOPEN_AT);
      ArtoriaStateService.clearTemporary(owner);
      h.assertTrue(deadline == owner.level().getGameTime() + 120 && owner.getPersistentData().getLong(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_REOPEN_AT) == deadline, "Cleanup reset cooldown");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void jetBudgetSurvivesAirborneTogglesAndOnlyGroundRefills(GameTestHelper h) {
      var owner = npc(h); ArtoriaStateService.startBurst(owner); owner.setOnGround(false);
      for (int i = 0; i < 40; i++) h.assertTrue(ArtoriaStateService.jet(owner, new Vec3(1, 0, 0), true, false), "Jet stopped early at " + i);
      ArtoriaStateService.stopBurst(owner); ArtoriaStateService.startBurst(owner);
      h.assertTrue(!ArtoriaStateService.jet(owner, new Vec3(1, 0, 0), true, false), "Airborne toggle refilled jet");
      h.setBlock(new BlockPos(4, 2, 4), Blocks.STONE); owner.setDeltaMovement(Vec3.ZERO); owner.setOnGround(true);
      ArtoriaStateService.tick(owner);
      h.assertTrue(owner.getPersistentData().getInt(ArtoriaStateService.JET_USED) == 0, "Real landing did not refill jet");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void instinctUsesThreeSharedChargesAndFifteenSecondRecovery(GameTestHelper h) {
      var owner = npc(h); ArtoriaStateService.syncInstinct(owner);
      for (int i = 0; i < 3; i++) h.assertTrue(ServantCombatDefenseService.trySkillAutoDodge(owner), "Missing instinct charge");
      h.assertTrue(!ServantCombatDefenseService.trySkillAutoDodge(owner), "Fourth dodge was granted");
      long deadline = owner.getPersistentData().getLong(ArtoriaStateService.RESTORE);
      h.assertTrue(deadline == owner.level().getGameTime() + 300, "Instinct uses another character's recovery");
      ArtoriaStateService.clearTemporary(owner); ArtoriaStateService.syncInstinct(owner);
      h.assertTrue(owner.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES) == 0, "Cleanup refilled instinct");
      owner.getPersistentData().putLong(ArtoriaStateService.RESTORE, owner.level().getGameTime()); ArtoriaStateService.syncInstinct(owner);
      h.assertTrue(owner.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES) == 3, "Group did not recover");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void specialJudgementsReuseOneCoinFlipAndAvalonDoesNotSpendCharges(GameTestHelper h) {
      var owner = npc(h); Object token = new Object();
      boolean first = ArtoriaStateService.negateSpecial(owner, token);
      for (int i = 0; i < 100; i++) h.assertTrue(ArtoriaStateService.negateSpecial(owner, token) == first, "Repeated special judgement re-rolled");
      ArtoriaStateService.syncInstinct(owner);
      owner.getPersistentData().putBoolean(ArtoriaPendragonCombatHelper.TAG_HAS_AVALON, true);
      h.assertTrue(ArtoriaStateService.negateSpecial(owner, new Object()), "Avalon did not intercept");
      h.assertTrue(owner.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES) == 3, "Avalon spent instinct");
      owner.getPersistentData().remove(ArtoriaPendragonCombatHelper.TAG_HAS_AVALON);
      h.assertTrue(!ArtoriaPendragonCombatHelper.tryProtectWithAvalon(owner), "Removed Avalon still protects");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void windSharedLockKeepsTheActualWeaponAndNativeRegenIsDoubledOnce(GameTestHelper h) {
      var owner = npc(h);
      h.assertTrue(owner.getMainHandItem().is(ModItems.EXCALIBUR.get()), "Veiling removed the real sword");
      h.assertTrue(ArtoriaAbilityService.canWind(owner), "Initial wind is locked");
      ArtoriaAbilityService.hammer(owner);
      h.assertTrue(!ArtoriaAbilityService.canWind(owner) && owner.isExcaliburVisible(), "Hammer did not reveal and lock wind");
      ArtoriaStateService.clearTemporary(owner);
      h.assertTrue(!ArtoriaAbilityService.canWind(owner), "Form cleanup bypassed wind cooldown");
      h.assertTrue(Math.abs(ServantCardManaService.regenPerSecondFor(ArtoriaRules.ID) - 1000.0 / 30) < .001, "Dragon Core regen duplicated or missing");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void windHammerDelayedHitUsesRestoredDamageAndStartsRecovery(GameTestHelper h) {
      var owner = npc(h);
      var target = target(h, owner.position().add(0, 0, 3));
      h.assertTrue(ArtoriaAbilityService.hammer(owner), "NPC hammer was rejected while wind was ready");
      h.runAtTickTime(12, () -> {
         h.assertTrue(Math.abs(target.getHealth() - 700.0F) < 0.02F,
            "Delayed Wind Hammer hit did not deal the restored 300 damage: " + target.getHealth());
         h.assertTrue(owner.getPersistentData().getLong(ArtoriaStateService.WIND_READY) > owner.level().getGameTime(),
            "Wind recovery was not active after the hammer hit");
         h.succeed();
      });
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void windDefenseAnchorsThePlayerWithoutSpawningAWorldSword(GameTestHelper h) {
      var owner = card(h);
      Vec3 anchor = owner.position();
      h.assertTrue(ArtoriaStateService.startWindDefense(owner, true), "Player wind defense did not start from active wind");
      h.assertTrue(ArtoriaStateService.isWindDefenseActive(owner), "Player wind defense state was not activated");
      owner.setPos(anchor.x + 2.0, anchor.y + 1.0, anchor.z + 2.0);
      owner.setDeltaMovement(1.0, 1.0, 1.0);
      ArtoriaStateService.tick(owner);
      h.assertTrue(owner.position().distanceToSqr(anchor) < 1.0E-4,
         "Player escaped the planted-sword defense anchor");
      h.assertTrue(owner.getDeltaMovement().lengthSqr() < 1.0E-6,
         "Player kept movement during planted-sword defense");
      h.assertTrue(h.getLevel().getEntitiesOfClass(ArtoriaWindDefenseSwordEntity.class,
         owner.getBoundingBox().inflate(8.0)).isEmpty(), "Wind defense spawned a separate sword entity");
      h.succeed();
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 180)
   public static void excaliburPaysOnceAndEarlyReleaseNeverCreatesAShortBeam(GameTestHelper h) {
      var owner = card(h); var card = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      h.assertTrue(PlayerNoblePhantasmHelper.startExcaliburCharge(owner), "Excalibur rejected");
      h.assertTrue(Math.abs(card.mana() - 350) < .001, "Acceptance did not cost exactly 150 MP");
      h.assertTrue(!PlayerNoblePhantasmHelper.startExcaliburCharge(owner) && Math.abs(card.mana() - 350) < .001, "Duplicate input paid twice");
      PlayerNoblePhantasmHelper.releaseExcalibur(owner);
      h.assertTrue(Math.abs(card.mana() - 425) < .001, "Cancelled chant did not retain half cost");
      h.assertTrue(owner.getPersistentData().getLong(ArtoriaStateService.NP_READY) == owner.level().getGameTime() + 1800, "Cancelled chant did not use half cooldown");
      h.runAtTickTime(165, () -> { h.assertTrue(!owner.getPersistentData().hasUUID(ArtoriaStateService.NP_BEAM), "Early release queued a short beam"); h.succeed(); });
   }
   private static final class Probe extends net.minecraft.world.entity.animal.Cow {
      int pulses, impacts; float total;
      Probe(ServerLevel level) {
         super(EntityType.COW, level); setNoAi(true); setNoGravity(true);
         getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); setHealth(1000);
      }
      @Override public boolean hurt(DamageSource source, float amount) {
         if (source.getDirectEntity() instanceof ArtoriaExcaliburBeamEntity) {
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) impacts++;
            else { pulses++; total += amount; }
            return true;
         }
         return super.hurt(source, amount);
      }
   }
   @GameTest(template = "ancient_temple", timeoutTicks = 175)
   public static void beamHitsFifteenTimesAndAbnormalCleanupDoesNotExplode(GameTestHelper h) {
      var owner = npc(h); var level = h.getLevel();
      Probe probe = new Probe(level); probe.setPos(owner.position().add(0, 0, 6)); level.addFreshEntity(probe);
      var beam = new ArtoriaExcaliburBeamEntity(level, owner, owner.getEyePosition(), 150, 0, 1);
      owner.getPersistentData().putUUID(ArtoriaStateService.NP_BEAM, beam.getUUID()); level.addFreshEntity(beam);
      h.runAtTickTime(80, () -> h.assertTrue(probe.pulses >= 7 && probe.pulses <= 8, "Middle pulse count differs: " + probe.pulses
         + " emitted=" + beam.emittedPulses() + " removed=" + beam.isRemoved() + " owner=" + owner.isAlive()
         + " look=" + owner.getLookAngle() + " relative=" + probe.position().subtract(owner.position()) + " targetAlive=" + probe.isAlive()));
      h.runAtTickTime(160, () -> {
         h.assertTrue(probe.pulses == 15 && probe.total == 7500, "Beam total is not fifteen fixed 500 pulses: " + probe.pulses + "/" + probe.total);
         h.assertTrue(beam.isRemoved(), "Normal beam survived");
         var cancelled = new ArtoriaExcaliburBeamEntity(level, owner, owner.getEyePosition(), 150, 0, 1);
         level.addFreshEntity(cancelled); cancelled.discard();
         h.assertTrue(probe.impacts == 0, "Abnormal removal created an endpoint explosion"); h.succeed();
      });
   }
}

package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService;

@GameTestHolder("typemoonworld_sasaki")
@PrefixGameTestTemplate(false)
public final class SasakiKojiroGameTests {
   private SasakiKojiroGameTests() { }

   private static ServerPlayer card(GameTestHelper helper) {
      clearArena(helper);
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
      player.teleportTo(position.x, position.y, position.z);
      player.setYRot(0.0F);
      player.setXRot(0.0F);
      helper.assertTrue(ServantCardTransformManager.transform(player, "sasaki_kojiro"), "Sasaki card transform failed");
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(200.0);
      return player;
   }

   private static void clearArena(GameTestHelper helper) {
      for (int x = 2; x <= 8; x++) for (int z = 2; z <= 8; z++) for (int y = 2; y <= 5; y++) {
         helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
      }
   }

   private static SasakiKojiroEntity sasaki(GameTestHelper helper, BlockPos position) {
      clearArena(helper);
      SasakiKojiroEntity entity = ModEntities.SASAKI_KOJIRO.get().create(helper.getLevel());
      entity.setPos(Vec3.atBottomCenterOf(helper.absolutePos(position)));
      entity.setNoAi(true);
      entity.setNoGravity(true);
      entity.setCurrentMp(200.0);
      helper.getLevel().addFreshEntity(entity);
      return entity;
   }

   private static LivingEntity target(GameTestHelper helper, BlockPos position) {
      var entity = EntityType.HUSK.create(helper.getLevel());
      entity.setPos(Vec3.atBottomCenterOf(helper.absolutePos(position)));
      entity.setNoAi(true);
      entity.setNoGravity(true);
      entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
      entity.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
      entity.setHealth(1000.0F);
      helper.getLevel().addFreshEntity(entity);
      return entity;
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void tsubameGaeshiAppliesOne450PointPhysicalHit(GameTestHelper helper) {
      SasakiKojiroEntity attacker = sasaki(helper, new BlockPos(4, 2, 4));
      LivingEntity defender = target(helper, new BlockPos(6, 2, 4));
      helper.assertTrue(SasakiKojiroCombatService.applyTsubameDamage(attacker, defender), "Tsubame hit was rejected");
      helper.assertTrue(Math.abs(defender.getHealth() - 550.0F) < 0.02F,
         "Tsubame must apply one 450 physical damage event, remaining health=" + defender.getHealth());
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void tsubameGaeshiCannotBeGuarded(GameTestHelper helper) {
      SasakiKojiroEntity attacker = sasaki(helper, new BlockPos(4, 2, 4));
      SasakiKojiroEntity defender = sasaki(helper, new BlockPos(6, 2, 4));
      defender.getPersistentData().putBoolean(ServantCombatDefenseService.TAG_GUARDING, true);
      defender.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_START, helper.getLevel().getGameTime());
      var source = new CombatDamageSource(attacker.damageSources().mobAttack(attacker),
         AttackOrigin.skill("sasaki_kojiro", "tsubame_gaeshi"));
      Float reduced = ServantCombatDefenseService.applyGuardHit(defender, source, 450.0F);
      helper.assertTrue(reduced == null, "Tsubame was handled by block/parry stamina logic");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void tsubameKeepsSkillDamageThroughNpcDuelScaling(GameTestHelper helper) {
      SasakiKojiroEntity attacker = sasaki(helper, new BlockPos(4, 2, 4));
      SasakiKojiroEntity defender = sasaki(helper, new BlockPos(6, 2, 4));
      defender.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 0);
      defender.getPersistentData().putLong(ServantCombatDefenseService.TAG_DODGE_COOLDOWN, Long.MAX_VALUE);
      var source = new CombatDamageSource(attacker.damageSources().mobAttack(attacker),
         AttackOrigin.skill("sasaki_kojiro", "tsubame_gaeshi"));
      var incoming = new LivingIncomingDamageEvent(defender, new DamageContainer(source, 450.0F));
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.handleIncomingDamage(defender, incoming);
      helper.assertTrue(Math.abs(incoming.getAmount() - 450.0F) < 0.02F,
         "Tsubame was softened as an ordinary duel strike: " + incoming.getAmount());
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void presenceToggleChargesOnlyOnActivationAndNeedsNoManaToClose(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      var state = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      helper.assertTrue(!SasakiKojiroCombatService.isConcealed(player), "Card initialized with free concealment");
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 0), "Concealment activation failed");
      helper.assertTrue(Math.abs(state.mana() - 182.0) < 0.001, "Concealment did not cost exactly 18 MP");
      helper.assertTrue(player.getPersistentData().getLong(SasakiKojiroCombatService.CONCEALMENT_UNTIL) == 0L,
         "Presence concealment has an expiry");
      state.setMana(0.0);
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 0), "Free closure failed at zero MP");
      helper.assertTrue(!SasakiKojiroCombatService.isConcealed(player) && !player.hasEffect(MobEffects.INVISIBILITY),
         "Closing concealment left invisibility behind");
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 0), "Concealment activated without MP");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardTsubameUsesExactSkillResourcesAndRejectsCooldown(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      LivingEntity defender = target(helper, new BlockPos(4, 2, 6));
      var state = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Card Tsubame failed");
      helper.assertTrue(Math.abs(defender.getHealth() - 550.0F) < 0.02F, "Card Tsubame damage differs from NPC");
      helper.assertTrue(Math.abs(state.mana() - 180.0) < 0.001, "Card Tsubame did not cost 20 MP");
      helper.assertTrue(state.npCooldown() == 0, "Tsubame wrote noble phantasm cooldown");
      helper.assertTrue(ServantCardTransformManager.getSkillCooldown(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), 9) == 600,
         "Tsubame did not populate the cooldown slot used by its HUD");
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 9), "Tsubame ignored skill cooldown");
      helper.assertTrue(Math.abs(state.mana() - 180.0) < 0.001, "Cooldown rejection spent mana");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void concealmentSurvivesDodgedHitsAndBreaksOnActualDamage(GameTestHelper helper) {
      SasakiKojiroEntity defender = sasaki(helper, new BlockPos(4, 2, 4));
      LivingEntity attacker = target(helper, new BlockPos(6, 2, 4));
      SasakiKojiroCombatService.setConcealed(defender, true);
      defender.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 1);
      float health = defender.getHealth();
      defender.hurt(attacker.damageSources().mobAttack(attacker), 10.0F);
      helper.assertTrue(defender.getHealth() == health && SasakiKojiroCombatService.isConcealed(defender),
         "Dodged damage removed concealment");
      defender.hurt(defender.damageSources().genericKill(), 1.0F);
      helper.assertTrue(defender.getHealth() < health && !SasakiKojiroCombatService.isConcealed(defender),
         "Actual damage failed to remove concealment");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void attackingBreaksConcealmentAndTsubameCanBeDodged(GameTestHelper helper) {
      SasakiKojiroEntity attacker = sasaki(helper, new BlockPos(4, 2, 4));
      SasakiKojiroEntity defender = sasaki(helper, new BlockPos(6, 2, 4));
      SasakiKojiroCombatService.setConcealed(attacker, true);
      attacker.getRandom().setSeed(123L);
      defender.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 1);
      float health = defender.getHealth();
      SasakiKojiroCombatService.performTsubameGaeshi(attacker, defender);
      helper.assertTrue(!SasakiKojiroCombatService.isConcealed(attacker), "Attack retained concealment");
      helper.assertTrue(defender.getHealth() == health, "Tsubame bypassed automatic dodge");
      helper.assertTrue(defender.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES) == 0,
         "Tsubame did not use the shared dodge charge");
      helper.succeed();
   }

   private static void transparencyFloor(GameTestHelper helper) {
      for (int x = 2; x <= 8; x++) for (int z = 2; z <= 8; z++) {
         helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
      }
   }

   private static void assertTransparencyResult(GameTestHelper helper, LivingEntity entity, LivingEntity enemy) {
      Vec3 offset = entity.position().subtract(enemy.position());
      helper.assertTrue(offset.dot(enemy.getLookAngle()) < -0.5 && entity.distanceTo(enemy) < 2.5,
         "Transparency did not teleport behind the enemy");
      helper.assertTrue(entity.getLookAngle().dot(enemy.position().subtract(entity.position()).normalize()) > 0.95,
         "Transparency did not face the enemy");
      helper.assertTrue(!entity.hasEffect(MobEffects.WEAKNESS) && entity.hasEffect(MobEffects.MOVEMENT_SPEED),
         "Transparency failed to remove only harmful effects");
      helper.assertTrue(!SasakiKojiroCombatService.isConcealed(entity) && !entity.hasEffect(MobEffects.INVISIBILITY)
         && !entity.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Transparency granted concealment or resistance");
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void transparencyCleansesAndTeleportsNpcBehindEnemy(GameTestHelper helper) {
      SasakiKojiroEntity entity = sasaki(helper, new BlockPos(4, 2, 4));
      LivingEntity enemy = target(helper, new BlockPos(6, 2, 4));
      enemy.setYRot(180.0F);
      entity.setTarget(enemy);
      transparencyFloor(helper);
      entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
      helper.assertTrue(SasakiKojiroCombatService.tickNpc(entity, enemy), "NPC did not use transparency");
      assertTransparencyResult(helper, entity, enemy);
      helper.assertTrue(Math.abs(entity.getCurrentMp() - 180.0) < 0.001, "NPC transparency did not cost 20 MP");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardTransparencyTeleportsWithoutNeedingALookTarget(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      LivingEntity enemy = target(helper, new BlockPos(6, 2, 4));
      enemy.setYRot(180.0F);
      transparencyFloor(helper);
      player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 5), "Card transparency failed");
      assertTransparencyResult(helper, player, enemy);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      helper.assertTrue(Math.abs(vars.servantCardState().mana() - 180.0) < 0.001
         && ServantCardTransformManager.getSkillCooldown(vars, 5) == 600, "Transparency resources differ from NPC");
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 5), "Transparency ignored cooldown");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardTransparencyRequiresEnemyAndSafeLanding(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      transparencyFloor(helper);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
      Vec3 start = player.position();
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 5), "Transparency succeeded without an enemy");
      LivingEntity enemy = target(helper, new BlockPos(4, 2, 6));
      enemy.setYRot(180.0F);
      for (int x = 2; x <= 8; x++) for (int z = 7; z <= 9; z++) for (int y = 2; y <= 4; y++) {
         helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE);
      }
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 5), "Transparency accepted a blocked landing");
      helper.assertTrue(player.position().distanceToSqr(start) < 0.001 && player.hasEffect(MobEffects.WEAKNESS),
         "Failed transparency changed position or effects");
      helper.assertTrue(Math.abs(vars.servantCardState().mana() - 200.0) < 0.001
         && ServantCardTransformManager.getSkillCooldown(vars, 5) == 0, "Failed transparency consumed resources");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void souwaRankBBypassesDefenseOnExactlyTheSelectedRolls(GameTestHelper helper) {
      SasakiKojiroEntity attacker = sasaki(helper, new BlockPos(4, 2, 4));
      var source = new CombatDamageSource(attacker.damageSources().mobAttack(attacker),
         AttackOrigin.skill("sasaki_kojiro", "sasaki_vertical_slash"));
      int bypasses = 0;
      for (int i = 0; i < 32; i++) {
         long seed = 123L + i * 104729L;
         attacker.getRandom().setSeed(seed);
         boolean bypass = net.minecraft.util.RandomSource.create(seed).nextFloat() < 0.15F;
         if (bypass) bypasses++;
         SasakiKojiroEntity defender = sasaki(helper, new BlockPos(6, 2, 4));
         defender.getPersistentData().putInt(ServantCombatDefenseService.AUTO_CHARGES, 0);
         defender.getPersistentData().putLong(ServantCombatDefenseService.TAG_DODGE_COOLDOWN, Long.MAX_VALUE);
         defender.getPersistentData().putBoolean(ServantCombatDefenseService.TAG_GUARDING, true);
         defender.getPersistentData().putLong(ServantCombatDefenseService.TAG_GUARD_START, helper.getLevel().getGameTime() - 100L);
         var incoming = new LivingIncomingDamageEvent(defender, new DamageContainer(source, 20.0F));
         ServantCombatSystem.handleIncomingDamage(defender, incoming);
         helper.assertTrue(Math.abs(incoming.getAmount() - (bypass ? 20.0F : 4.0F)) < 0.02F,
            "Souwa rank B defense bypass disagreed with roll " + i + ": " + incoming.getAmount());
         defender.discard();
      }
      helper.assertTrue(bypasses > 0 && bypasses < 32, "Test seeds must cover bypass and normal defense");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void souwaReadsTheSameRankForPlayerAndNpcAttackers(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      SasakiKojiroEntity npc = sasaki(helper, new BlockPos(6, 2, 4));
      helper.assertTrue(Math.abs(SasakiKojiroCombatService.souwaBonus(player) - 0.20) < 0.001
         && Math.abs(SasakiKojiroCombatService.souwaBonus(npc) - 0.20) < 0.001,
         "Player and NPC did not resolve Souwa rank B");
      for (int i = 0; i < 32; i++) {
         long seed = 123L + i * 104729L;
         player.getRandom().setSeed(seed);
         npc.getRandom().setSeed(seed);
         boolean expected = net.minecraft.util.RandomSource.create(seed).nextFloat() < 0.15F;
         helper.assertTrue(SasakiKojiroCombatService.rollDefenseBypass(player.damageSources().playerAttack(player)) == expected
            && SasakiKojiroCombatService.rollDefenseBypass(npc.damageSources().mobAttack(npc)) == expected,
            "Player/NPC rank B bypass probabilities differ");
      }
      helper.succeed();
   }
}

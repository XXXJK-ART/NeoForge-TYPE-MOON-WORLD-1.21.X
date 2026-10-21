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
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroRules.Technique;

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
      entity.ensureDefaultNpcLoadout(false);
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

   private static void resetCardResources(ServerPlayer player) {
      var state = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      state.setMana(200.0);
      state.setSkillCooldowns("");
      state.setSkillCooldownEnds("");
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardSwordAttacksHitAtFourBlocksWithoutAiming(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      player.setYRot(-45.0F);
      LivingEntity defender = target(helper, new BlockPos(4, 2, 8));
      Vec3 position = defender.position();
      helper.assertTrue(ServantCardSkillUtils.findLookTarget(player, 4.0, 1.0) == null,
         "Test target must be outside the aimed target search");
      for (Technique technique : Technique.values()) {
         if (technique == Technique.TSUBAME_GAESHI || technique == Technique.COUNTER_STANCE) continue;
         resetCardResources(player);
         defender.setPos(position);
         defender.setHealth(1000.0F);
         defender.invulnerableTime = 0;
         helper.assertTrue(ServantCardTransformManager.triggerAction(player, technique.slot()),
            "Off-aim card attack failed: " + technique);
         float damage = technique.damage() * (technique == Technique.CROUCH_CUTBACK ? 2.0F : 1.0F);
         helper.assertTrue(Math.abs(defender.getHealth() - (1000.0F - damage)) < 0.02F,
            "Off-aim four-block damage differs: " + technique + ", health=" + defender.getHealth());
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         helper.assertTrue(Math.abs(vars.servantCardState().mana() - (200.0 - technique.mpCost())) < 0.001
            && ServantCardTransformManager.getSkillCooldown(vars, technique.slot() < 0 ? 6 : technique.slot()) == technique.cooldownTicks(),
            "Off-aim attack resources differ: " + technique);
      }
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardSkillsRequireTheDedicatedSwordWithoutSpendingResources(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var sword = player.getMainHandItem().copy();
      helper.assertTrue(sword.is(net.xxxjk.TYPE_MOON_WORLD.item.ModItems.BIZEN_NAGAMITSU.get()),
         "Sasaki card did not equip its dedicated sword");
      player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 1), "Skill activated without a sword");
      helper.assertTrue(Math.abs(vars.servantCardState().mana() - 200.0) < 0.001
         && ServantCardTransformManager.getSkillCooldown(vars, 1) == 0,
         "Rejected unarmed skill consumed mana or cooldown");
      player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 1), "Dedicated sword did not satisfy the skill requirement");
      helper.assertTrue(Math.abs(vars.servantCardState().mana() - 188.0) < 0.001,
         "Armed skill did not consume its normal mana cost");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardSwordAttacksCanMissWithoutBypassingResourceChecks(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      for (Technique technique : Technique.values()) {
         if (technique == Technique.TSUBAME_GAESHI) continue;
         resetCardResources(player);
         helper.assertTrue(ServantCardTransformManager.triggerAction(player, technique.slot()),
            "Card could not use a technique without a target: " + technique);
         helper.assertTrue(Math.abs(vars.servantCardState().mana() - (200.0 - technique.mpCost())) < 0.001,
            "Missed attack did not consume its normal MP: " + technique);
         helper.assertTrue(!ServantCardTransformManager.triggerAction(player, technique.slot()),
            "Missed attack ignored cooldown: " + technique);
      }
      resetCardResources(player);
      vars.servantCardState().setMana(0.0);
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 1), "Empty attack ignored insufficient MP");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardSwordAttacksRespectFourBlockRangeAndWalls(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      LivingEntity defender = target(helper, new BlockPos(4, 2, 8));
      defender.setPos(player.position().add(0.0, 0.0, 4.05));
      for (Technique technique : Technique.values()) {
         if (technique == Technique.TSUBAME_GAESHI || technique == Technique.COUNTER_STANCE) continue;
         resetCardResources(player);
         helper.assertTrue(ServantCardTransformManager.triggerAction(player, technique.slot()),
            "Attack could not miss an out-of-range target: " + technique);
         helper.assertTrue(defender.getHealth() == 1000.0F, "Attack reached beyond four blocks: " + technique);
      }
      defender.setPos(player.position().add(0.0, 0.0, 3.0));
      for (int y = 2; y <= 5; y++) helper.setBlock(new BlockPos(4, y, 6), net.minecraft.world.level.block.Blocks.STONE);
      for (Technique technique : Technique.values()) {
         if (technique == Technique.TSUBAME_GAESHI || technique == Technique.COUNTER_STANCE) continue;
         resetCardResources(player);
         helper.assertTrue(ServantCardTransformManager.triggerAction(player, technique.slot()),
            "Attack could not miss a blocked target: " + technique);
         helper.assertTrue(defender.getHealth() == 1000.0F, "Attack damaged a target through a wall: " + technique);
      }
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardSwordSkillsAndTsubameBroadcastTheirVoices(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      target(helper, new BlockPos(4, 2, 6));
      java.util.List<net.minecraft.sounds.SoundEvent> sounds = new java.util.ArrayList<>();
      java.util.function.Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener = event -> {
         if (event.getLevel() == helper.getLevel() && event.getPosition().distanceToSqr(player.position()) < 0.01) {
            sounds.add(event.getSound().value());
         }
      };
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
      try {
         for (Technique technique : Technique.values()) {
            resetCardResources(player);
            player.getPersistentData().remove("ServantCardPlayerVoiceLastTick");
            sounds.clear();
            helper.assertTrue(ServantCardTransformManager.triggerAction(player, technique.slot()),
               "Voiced skill failed: " + technique);
            var expected = technique == Technique.TSUBAME_GAESHI
               ? net.xxxjk.TYPE_MOON_WORLD.init.ModSounds.SASAKI_KOJIRO_VOICE_TSURIGAMESHI.get()
               : net.xxxjk.TYPE_MOON_WORLD.init.ModSounds.SASAKI_KOJIRO_VOICE_ATTACK.get();
            helper.assertTrue(sounds.stream().filter(sound -> sound == expected).count() == 1L,
               "Skill must broadcast its voice exactly once: " + technique);
         }
      } finally {
         net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
      }
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardCounterReachesFourBlocksWhileNpcKeepsItsRange(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      LivingEntity attacker = target(helper, new BlockPos(4, 2, 8));
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 8), "Card counter stance failed");
      attacker.setPos(player.position().add(0.0, 0.0, 4.05));
      SasakiKojiroCombatService.onSuccessfulGuard(player, attacker);
      helper.assertTrue(attacker.getHealth() == 1000.0F, "Card counter reached beyond four blocks");
      attacker.setPos(player.position().add(0.0, 0.0, 4.0));
      SasakiKojiroCombatService.onSuccessfulGuard(player, attacker);
      helper.assertTrue(Math.abs(attacker.getHealth() - 976.0F) < 0.02F, "Card counter failed at four blocks");
      SasakiKojiroEntity npc = sasaki(helper, new BlockPos(4, 2, 4));
      SasakiKojiroCombatService.startCounter(npc);
      attacker.invulnerableTime = 0;
      SasakiKojiroCombatService.onSuccessfulGuard(npc, attacker);
      helper.assertTrue(Math.abs(attacker.getHealth() - 976.0F) < 0.02F, "NPC unexpectedly received the card range override");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void cardTsubameStillRequiresAnAimedTarget(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 9), "Tsubame succeeded without a target");
      LivingEntity defender = target(helper, new BlockPos(4, 2, 8));
      player.setYRot(-45.0F);
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 9), "Tsubame hit an unaimed target");
      helper.assertTrue(defender.getHealth() == 1000.0F && vars.servantCardState().mana() == 200.0
         && ServantCardTransformManager.getSkillCooldown(vars, 9) == 0, "Rejected Tsubame changed health or resources");
      player.setYRot(0.0F);
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Aimed Tsubame failed");
      helper.assertTrue(defender.getHealth() == 1000.0F, "Tsubame damaged its target before the stance finished");
      helper.runAfterDelay(11, () -> {
         helper.assertTrue(Math.abs(defender.getHealth() - 550.0F) < 0.02F, "Aimed Tsubame damage changed");
         helper.succeed();
      });
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

   @GameTest(template = "ancient_temple", timeoutTicks = 25)
   public static void tsubameRechecksReachAfterTheWindup(GameTestHelper helper) {
      SasakiKojiroEntity attacker = sasaki(helper, new BlockPos(4, 2, 4));
      LivingEntity defender = target(helper, new BlockPos(4, 2, 6));
      helper.assertTrue(SasakiKojiroCombatService.performTsubameGaeshi(attacker, defender), "Tsubame did not start");
      helper.assertTrue(!SasakiKojiroCombatService.performTsubameGaeshi(attacker, defender), "Tsubame stacked two pending casts");
      defender.setPos(defender.position().add(0.0, 0.0, 12.0));
      helper.runAfterDelay(11, () -> {
         helper.assertTrue(defender.getHealth() == 1000.0F, "Pending Tsubame damaged a target that left its reach");
         helper.assertTrue(!SasakiKojiroCombatService.isPreparingTsubame(attacker), "Tsubame windup state did not clear");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 25)
   public static void releasingTheCardCancelsPendingTsubameDamage(GameTestHelper helper) {
      ServerPlayer player = card(helper);
      LivingEntity defender = target(helper, new BlockPos(4, 2, 6));
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Card Tsubame did not start");
      ServantCardTransformManager.release(player, false);
      helper.runAfterDelay(11, () -> {
         helper.assertTrue(defender.getHealth() == 1000.0F, "Released Sasaki card left delayed Tsubame damage behind");
         helper.succeed();
      });
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
      helper.assertTrue(defender.getHealth() == 1000.0F && SasakiKojiroCombatService.isPreparingTsubame(player),
         "Card Tsubame skipped its ten-tick windup");
      helper.assertTrue(Math.abs(state.mana() - 180.0) < 0.001, "Card Tsubame did not cost 20 MP");
      helper.assertTrue(state.npCooldown() == 0, "Tsubame wrote noble phantasm cooldown");
      helper.assertTrue(ServantCardTransformManager.getSkillCooldown(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), 9) == 600,
         "Tsubame did not populate the cooldown slot used by its HUD");
      helper.assertTrue(!ServantCardTransformManager.triggerAction(player, 9), "Tsubame ignored skill cooldown");
      helper.assertTrue(Math.abs(state.mana() - 180.0) < 0.001, "Cooldown rejection spent mana");
      helper.runAfterDelay(8, () -> helper.assertTrue(defender.getHealth() == 1000.0F,
         "Card Tsubame damaged its target during the stance"));
      helper.runAfterDelay(11, () -> {
         helper.assertTrue(Math.abs(defender.getHealth() - 550.0F) < 0.02F, "Card Tsubame damage differs from NPC");
         helper.succeed();
      });
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
      helper.assertTrue(attacker.getSasakiAction() == net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction.TSUBAME_WINDUP,
         "NPC Tsubame did not start with the imported Ganryu stance");
      helper.runAfterDelay(11, () -> {
         helper.assertTrue(defender.getHealth() == health, "Tsubame bypassed automatic dodge");
         helper.assertTrue(defender.getPersistentData().getInt(ServantCombatDefenseService.AUTO_CHARGES) == 0,
            "Tsubame did not use the shared dodge charge");
         helper.assertTrue(attacker.getSasakiAction() == net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction.TSUBAME_GAESHI,
            "NPC Tsubame never transitioned to the three-blade release");
         helper.succeed();
      });
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

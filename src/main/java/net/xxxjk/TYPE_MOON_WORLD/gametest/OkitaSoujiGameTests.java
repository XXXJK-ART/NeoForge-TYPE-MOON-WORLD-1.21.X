package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberTechniqueService;

@GameTestHolder("typemoonworld_okita")
@PrefixGameTestTemplate(false)
public final class OkitaSoujiGameTests {
   private OkitaSoujiGameTests() { }

   private static void arena(GameTestHelper h) {
      for (int x = 1; x <= 10; x++) for (int z = 1; z <= 10; z++) {
         h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
         for (int y = 2; y <= 6; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
      }
   }

   private static ServerPlayer card(GameTestHelper h) {
      arena(h);
      var player = h.makeMockServerPlayerInLevel();
      Vec3 position = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 2, 4)));
      player.teleportTo(position.x, position.y, position.z);
      player.setYRot(0); player.setXRot(0);
      h.assertTrue(ServantCardTransformManager.transform(player, "okita_souji_saber"), "Okita transform failed");
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().setMana(200);
      return player;
   }

   private static LivingEntity target(GameTestHelper h, Vec3 position) {
      var target = EntityType.HUSK.create(h.getLevel());
      target.setPos(position); target.setNoAi(true); target.setNoGravity(true);
      target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
      target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
      target.setHealth(1000); h.getLevel().addFreshEntity(target);
      return target;
   }

   private static void health(GameTestHelper h, LivingEntity target, float expected) {
      h.assertTrue(Math.abs(target.getHealth() - expected) < 0.02F,
         "Expected " + expected + " health, got " + target.getHealth());
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void cardThreeHitsFollowWindupAndSpendResourcesOnce(GameTestHelper h) {
      var player = card(h);
      var target = target(h, player.position().add(0, 0, 2));
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      var state = vars.servantCardState();
      h.assertTrue(ServantCardTransformManager.triggerAction(player, 8), "Aimed NP failed");
      h.assertTrue(state.mana() == 180 && ServantCardTransformManager.getSkillCooldown(vars, 8) == 3600,
         "NP cost/cooldown changed: MP=" + state.mana() + " cooldown=" + ServantCardTransformManager.getSkillCooldown(vars, 8));
      h.assertTrue(!ServantCardTransformManager.triggerAction(player, 4), "Another skill overwrote NP");
      health(h, target, 1000);
      h.runAfterDelay(14, () -> health(h, target, 1000));
      h.runAfterDelay(16, () -> health(h, target, 850));
      h.runAfterDelay(18, () -> health(h, target, 700));
      h.runAfterDelay(21, () -> { health(h, target, 550); h.succeed(); });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void npcUsesTheSameThreeHitTimelineAndKeepsItsActionLock(GameTestHelper h) {
      arena(h);
      var npc = ModEntities.OKITA_SOUJI_SABER.get().create(h.getLevel());
      npc.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 2, 4))));
      npc.setNoAi(true); npc.setNoGravity(true); h.getLevel().addFreshEntity(npc);
      npc.ensureDefaultNpcLoadout(false);
      var target = target(h, npc.position().add(0, 0, 2));
      h.assertTrue(OkitaSoujiSaberTechniqueService.beginMumyoudan(npc, target, () -> { }), "NPC NP failed");
      h.assertTrue(npc.isPerformingAction(), "Windup did not lock NPC actions");
      h.runAfterDelay(14, () -> { health(h, target, 1000); h.assertTrue(npc.isPerformingAction(), "NPC lost clip lock"); });
      h.runAfterDelay(16, () -> health(h, target, 850));
      h.runAfterDelay(18, () -> health(h, target, 700));
      h.runAfterDelay(26, () -> { health(h, target, 550); h.assertTrue(!npc.isPerformingAction(), "NPC lock persisted"); h.succeed(); });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void releasingTheCardCancelsEveryPendingHit(GameTestHelper h) {
      var player = card(h);
      var target = target(h, player.position().add(0, 0, 2));
      h.assertTrue(ServantCardTransformManager.triggerAction(player, 8), "NP failed");
      ServantCardTransformManager.release(player, false);
      h.runAfterDelay(26, () -> { health(h, target, 1000); h.succeed(); });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void eachHitRechecksRangeAndLineOfSight(GameTestHelper h) {
      var player = card(h);
      var target = target(h, player.position().add(0, 0, 2));
      Vec3 start = target.position();
      h.assertTrue(ServantCardTransformManager.triggerAction(player, 8), "NP failed");
      h.runAfterDelay(16, () -> { health(h, target, 850); target.setPos(start.add(0, 0, 5)); });
      h.runAfterDelay(18, () -> {
         health(h, target, 850); target.setPos(start);
         h.setBlock(new BlockPos(4, 2, 5), Blocks.STONE);
         h.setBlock(new BlockPos(4, 3, 5), Blocks.STONE);
      });
      h.runAfterDelay(22, () -> { health(h, target, 850); h.succeed(); });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void fixedHitsBypassArmorResistanceAbsorptionAndNpcGuard(GameTestHelper h) {
      var player = card(h);
      var defender = ModEntities.SASAKI_KOJIRO.get().create(h.getLevel());
      defender.setPos(player.position().add(0, 0, 2)); defender.setNoAi(true); defender.setNoGravity(true);
      h.getLevel().addFreshEntity(defender); defender.ensureDefaultNpcLoadout(false);
      defender.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); defender.setHealth(1000);
      defender.getAttribute(Attributes.ARMOR).setBaseValue(30);
      defender.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4));
      defender.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 4));
      defender.setYRot(180); defender.yHeadRot = 180; defender.yBodyRot = 180;
      ServantCombatDefenseService.initialize(defender);
      ServantCombatDefenseService.setGuarding(defender, true);
      for (int i = 0; i < 3; i++) h.assertTrue(
         OkitaSoujiSaberTechniqueService.strikeMumyoudanPoint(player, defender, h.getLevel(), i), "Fixed hit was defended");
      health(h, defender, 550); h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void ordinarySkillsCanBeCastWithoutAnyAimedEnemy(GameTestHelper h) {
      var player = card(h);
      var state = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      for (int slot : new int[]{1, 2, 3, 4, 5, 6}) {
         state.setMana(200); state.setSkillCooldowns("");
         h.assertTrue(ServantCardTransformManager.triggerAction(player, slot), "No-target skill failed: " + slot);
      }
      state.setMana(200); state.setNpCooldown(0);
      h.assertTrue(!ServantCardTransformManager.triggerAction(player, 8), "NP cast without an aimed enemy");
      h.assertTrue(state.mana() == 200 && state.npCooldown() == 0, "Rejected NP spent resources");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void flagUsesItsOwnVoiceAndAttackVictoryFailureVoicesAreReachable(GameTestHelper h) {
      var player = card(h);
      List<SoundEvent> sounds = new ArrayList<>();
      Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
         if (event.getLevel() == h.getLevel() && event.getPosition().distanceToSqr(player.position()) < 0.01)
            sounds.add(event.getSound().value());
      };
      NeoForge.EVENT_BUS.addListener(listener);
      try {
         h.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Flag failed");
         h.assertTrue(sounds.contains(ModSounds.OKITA_SOUJI_SABER_VOICE_FLAG.get()), "Flag voice missing");
         h.assertTrue(!sounds.contains(ModSounds.OKITA_SOUJI_SABER_VOICE_NP.get()), "Flag played NP voice");
         player.getPersistentData().remove("ServantCardPlayerVoiceLastTick");
         ServantCardVoiceHelper.tryPlayVictory(player);
         player.getPersistentData().remove("ServantCardPlayerVoiceLastTick");
         ServantCardVoiceHelper.tryPlayFail(player);
         h.assertTrue(sounds.contains(ModSounds.OKITA_SOUJI_SABER_VOICE_VICTORY.get()), "Victory voice missing");
         h.assertTrue(sounds.contains(ModSounds.OKITA_SOUJI_SABER_VOICE_FAIL.get()), "Failure voice missing");
         player.getPersistentData().remove("ServantCardPlayerVoiceLastTick");
         ServantCardVoiceHelper.tryPlaySkill(player, "okita_downward_slash");
         h.assertTrue(sounds.contains(ModSounds.OKITA_SOUJI_SABER_VOICE_BATTLE_START.get()), "Battle-start voice missing");
         for (int i = 0; i < 60; i++) {
            player.getPersistentData().remove("ServantCardPlayerVoiceLastTick");
            ServantCardVoiceHelper.tryPlayAttack(player);
         }
         h.assertTrue(sounds.contains(ModSounds.OKITA_SOUJI_SABER_VOICE_ATTACK.get()), "Attack voice unreachable");
      } finally { NeoForge.EVENT_BUS.unregister(listener); }
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void impactSoundsFollowConfirmedHitsAndTheFinisherIsHeavier(GameTestHelper h) {
      var player = card(h);
      var target = target(h, player.position().add(0, 0, 2));
      List<Float> volumes = new ArrayList<>();
      List<Float> pitches = new ArrayList<>();
      Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
         if (event.getLevel() == h.getLevel() && event.getSound().value() == SoundEvents.TRIDENT_HIT
            && event.getPosition().distanceToSqr(target.position().add(0, target.getBbHeight() * 0.55, 0)) < 0.01) {
            volumes.add(event.getNewVolume()); pitches.add(event.getNewPitch());
         }
      };
      NeoForge.EVENT_BUS.addListener(listener);
      try {
         for (int strike = 0; strike < 3; strike++) h.assertTrue(
            OkitaSoujiSaberTechniqueService.strikeMumyoudanPoint(player, target, h.getLevel(), strike), "Impact test hit failed");
         health(h, target, 550);
         h.assertTrue(volumes.size() == 3, "Each successful thrust needs exactly one piercing sound");
         h.assertTrue(volumes.get(2) > volumes.get(0) && pitches.get(2) < pitches.get(0), "Finisher lacks its heavier sound");
         target.setPos(target.position().add(0, 0, 8));
         h.assertTrue(!OkitaSoujiSaberTechniqueService.strikeMumyoudanPoint(player, target, h.getLevel(), 2), "Out-of-range thrust hit");
         h.assertTrue(volumes.size() == 3, "Miss played a contact sound");
      } finally { NeoForge.EVENT_BUS.unregister(listener); }
      h.succeed();
   }
}

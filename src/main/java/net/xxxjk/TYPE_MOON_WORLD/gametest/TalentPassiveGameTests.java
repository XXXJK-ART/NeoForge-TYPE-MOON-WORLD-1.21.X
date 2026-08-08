package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;

@GameTestHolder("typemoonworld")
@PrefixGameTestTemplate(false)
public final class TalentPassiveGameTests {
   private TalentPassiveGameTests() {
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void monstrousStrengthStacksWithVanillaStrength(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.talent_proficiencies.put(TalentService.MONSTROUS_STRENGTH, 40.0);
      double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
      double vanillaOnly = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      helper.assertTrue(TalentService.cast(player, vars, TalentService.MONSTROUS_STRENGTH), "talent cast failed");
      double stacked = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      helper.assertTrue(vanillaOnly > base, "vanilla Strength did not modify attack damage");
      helper.assertTrue(stacked >= vanillaOnly + 8.99, "Monstrous Strength III did not independently add 9 attack damage");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void monstrousStrengthResistsCuresAndExplicitClears(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.talent_proficiencies.put(TalentService.MONSTROUS_STRENGTH, 0.0);
      TalentService.cast(player, vars, TalentService.MONSTROUS_STRENGTH);
      player.removeEffectsCuredBy(EffectCures.MILK);
      helper.assertTrue(player.hasEffect(ModMobEffects.MONSTROUS_STRENGTH), "milk removed Monstrous Strength");
      player.removeAllEffects();
      helper.assertTrue(player.hasEffect(ModMobEffects.MONSTROUS_STRENGTH), "removeAllEffects removed Monstrous Strength");
      player.removeEffect(ModMobEffects.MONSTROUS_STRENGTH);
      helper.assertTrue(player.hasEffect(ModMobEffects.MONSTROUS_STRENGTH), "explicit effect removal removed Monstrous Strength");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void monstrousStrengthExpiresNaturally(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      TalentService.clearMonstrousStrength(player);
      player.getPersistentData().putLong(TalentService.STRENGTH_UNTIL_TAG, player.level().getGameTime() + 2L);
      player.getPersistentData().putInt(TalentService.STRENGTH_AMPLIFIER_TAG, 0);
      player.addEffect(new MobEffectInstance(ModMobEffects.MONSTROUS_STRENGTH, 2, 0));
      helper.runAfterDelay(6, () -> {
         helper.assertTrue(!player.hasEffect(ModMobEffects.MONSTROUS_STRENGTH), "effect remained after natural expiration");
         helper.assertTrue(!player.getPersistentData().contains(TalentService.STRENGTH_UNTIL_TAG), "expiration timestamp was not cleared");
         helper.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void divinityAndDodgeApplyToPlayers(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double baseHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
      double baseAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      vars.passive_ranks.put(PassiveService.DIVINITY, PassiveRank.E);
      vars.passive_ranks.put(PassiveService.MIND_EYE_TRUE, PassiveRank.A);
      vars.passive_ranks.put(PassiveService.INSTINCT, PassiveRank.A);
      PassiveService.reconcileAttributes(player, vars);
      helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == baseHealth + 10.0, "Divinity E max health was incorrect");
      helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack + 2.0, "Divinity E attack was incorrect");
      helper.assertTrue(ServantIdentityHelper.hasTrait(player, ServantTraitTag.DIVINE), "Divinity passive did not expose Divine identity");
      helper.assertTrue(!PassiveService.tryDodge(player, player.damageSources().fall()), "environmental fall damage was dodgeable");

      var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
      boolean dodged = false;
      for (int i = 0; i < 20 && !dodged; i++) {
         dodged = PassiveService.tryDodge(player, player.damageSources().mobAttack(attacker));
      }
      helper.assertTrue(dodged, "ordinary melee damage never entered the 80% passive dodge path");
      helper.succeed();
   }
}

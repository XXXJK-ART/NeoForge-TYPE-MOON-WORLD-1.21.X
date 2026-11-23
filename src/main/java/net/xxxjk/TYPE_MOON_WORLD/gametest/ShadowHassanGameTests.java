package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanRules;
import net.minecraft.core.BlockPos;

@GameTestHolder("typemoonworld_hassan")
@PrefixGameTestTemplate(false)
public final class ShadowHassanGameTests {
   private ShadowHassanGameTests() {
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void possessionGateRejectsDarknessAndInvalidHosts(GameTestHelper helper) {
      helper.assertTrue(ShadowHassanRules.canBeginPossession(false, true, true, true, true),
         "valid possession gate was rejected");
      helper.assertTrue(!ShadowHassanRules.canBeginPossession(true, true, true, true, true),
         "complete darkness allowed possession");
      helper.assertTrue(!ShadowHassanRules.canBeginPossession(false, false, true, true, true),
         "host without a shadow anchor was accepted");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void possessionManaMatchesShadowLanternNetChange(GameTestHelper helper) {
      helper.assertTrue(ShadowHassanRules.possessionManaAfterInterval(1.0, 400.0, true) == 5.0,
         "shadow-zone possession did not net four MP per second");
      helper.assertTrue(ShadowHassanRules.possessionManaAfterInterval(1.0, 400.0, false) == 0.0,
         "possession did not pay its one MP maintenance cost");
      helper.assertTrue(ShadowHassanRules.possessionManaAfterInterval(0.0, 400.0, true) == 0.0,
         "zero MP was allowed to recover during possession");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void npcAssassinIgnoresFarmAnimalsButTargetsHostiles(GameTestHelper helper) {
      var hassan = helper.spawn(ModEntities.SHADOW_HASSAN.get(), new BlockPos(3, 8, 3));
      hassan.setNoAi(true);
      var pig = helper.spawn(EntityType.PIG, new BlockPos(5, 8, 3));
      pig.setNoAi(true);
      var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 8, 3));
      zombie.setNoAi(true);
      helper.assertTrue(!ShadowHassanRules.isNpcAssassinationTarget(hassan, pig),
         "NPC Hassan selected a passive farm animal");
      helper.assertTrue(ShadowHassanRules.isNpcAssassinationTarget(hassan, zombie),
         "NPC Hassan rejected a hostile target");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 20)
   public static void assassinDamageHasNoAttackerAndUsesMundaneDeathText(GameTestHelper helper) {
      var hassan = helper.spawn(ModEntities.SHADOW_HASSAN.get(), new BlockPos(3, 8, 3));
      hassan.setNoAi(true);
      var victim = helper.spawn(EntityType.WOLF, new BlockPos(5, 8, 3));
      victim.setNoAi(true);
      var source = ShadowHassanDamageSource.forTarget(hassan.damageSources().mobAttack(hassan), victim);
      helper.assertTrue(source.getEntity() == null && source.getDirectEntity() == null,
         "assassin damage retained attacker attribution");
      helper.assertTrue(source.resolveAttacker(helper.getLevel()) == hassan,
         "hidden attacker UUID was not preserved for servant mechanics");
      String message = source.getLocalizedDeathMessage(victim).getString();
      helper.assertTrue(!message.contains(hassan.getName().getString()),
         "death text named Hassan as the killer");
      helper.succeed();
   }
}

package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemNecklaceItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.CombatTerrainProtection;

@GameTestHolder("typemoonworld_combat")
@PrefixGameTestTemplate(false)
public final class CombatRevisionGameTests {
   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void necklaceConservesBothManaPoolsAndPersists(GameTestHelper h) {
      var player = h.makeMockServerPlayerInLevel();
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ItemStack necklace = new ItemStack(ModItems.GEM_NECKLACE.get());
      player.setItemInHand(InteractionHand.OFF_HAND, necklace);
      vars.magicState().setMaxMana(1000); vars.magicState().setMana(37.75);
      h.assertTrue(GemNecklaceItem.transfer(player, necklace, true), "offhand deposit failed");
      h.assertTrue(GemNecklaceItem.stored(necklace) == 37 && Math.abs(vars.magicState().mana() - 0.75) < 1e-6, "partial integer deposit lost mana");
      ItemStack copy = necklace.copy();
      player.setItemInHand(InteractionHand.OFF_HAND, copy);
      h.assertTrue(GemNecklaceItem.stored(copy) == 37, "item component did not survive copying");
      h.assertTrue(GemNecklaceItem.transfer(player, copy, false), "withdraw failed");
      h.assertTrue(Math.abs(vars.magicState().mana() - 37.75) < 1e-6 && GemNecklaceItem.stored(copy) == 0, "round-trip did not conserve mana");
      vars.servantCardState().setTransformed(true); vars.servantCardState().setMaxMana(1000); vars.servantCardState().setMana(150);
      h.assertTrue(GemNecklaceItem.transfer(player, copy, true), "card deposit failed");
      h.assertTrue(vars.servantCardState().mana() == 50 && vars.magicState().mana() == 37.75, "wrong mana pool debited");
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 120)
   public static void guardResourcesAndQuietRecovery(GameTestHelper h) {
      var level = h.getLevel();
      var defender = BuiltinServantEntityFactory.create(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", "medea"));
      defender.moveTo(h.absolutePos(new BlockPos(3, 3, 3)).getCenter()); defender.setNoAi(true); level.addFreshEntity(defender);
      var attacker = h.spawn(EntityType.ZOMBIE, new BlockPos(6, 3, 3)); attacker.setNoAi(true);
      ServantCombatDefenseService.initialize(defender);
      double full = defender.getPersistentData().getDouble(ServantCombatDefenseService.TAG_STAMINA);
      h.assertTrue(ServantCombatDefenseService.setGuarding(defender, true), "guard request rejected");
      h.assertTrue(ServantCombatDefenseService.applyGuardHit(defender, attacker.damageSources().mobAttack(attacker), 20) == 0, "perfect guard did not block");
      h.assertTrue(defender.getPersistentData().getDouble(ServantCombatDefenseService.TAG_STAMINA) == full, "perfect guard spent defense");
      ServantCombatDefenseService.setGuarding(defender, false);
      h.assertTrue(ServantCombatDefenseService.applyGuardHit(defender, attacker.damageSources().mobAttack(attacker), 20) == 10, "automatic guard reduction wrong");
      defender.getPersistentData().putDouble(ServantCombatDefenseService.TAG_STAMINA, 0);
      defender.getPersistentData().putDouble(ServantCombatDefenseService.TAG_POISE, 5);
      ServantCombatDefenseService.losePoise(defender, 10);
      h.assertTrue(ServantCombatDefenseService.broken(defender), "empty poise did not break guard");
      h.assertTrue(!ServantCombatDefenseService.setGuarding(defender, true), "broken guard can still block");
      defender.getPersistentData().putLong(ServantCombatDefenseService.LAST_HIT, level.getGameTime());
      h.runAfterDelay(40, () -> {
         ServantCombatDefenseService.tick(defender);
         h.assertTrue(defender.getPersistentData().getDouble(ServantCombatDefenseService.TAG_POISE) == 0, "poise regenerated before 3 quiet seconds");
      });
      h.runAfterDelay(65, () -> {
         ServantCombatDefenseService.tick(defender);
         h.assertTrue(defender.getPersistentData().getDouble(ServantCombatDefenseService.TAG_POISE) > 0, "poise did not recover after quiet time");
         h.assertTrue(ServantCombatDefenseService.broken(defender), "guard unlocked before full poise");
         defender.discard(); h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 180)
   public static void interruptedNpCancelsEffectAndRefundsOnlyItsDebit(GameTestHelper h) {
      var level = h.getLevel();
      var caster = BuiltinServantEntityFactory.create(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", "medea"));
      caster.moveTo(h.absolutePos(new BlockPos(3, 3, 3)).getCenter()); caster.setNoAi(true); level.addFreshEntity(caster);
      caster.setCurrentMp(100);
      NoblePhantasmCastService.noteNpcPayment(caster, 100);
      int[] cooldown = {0}; boolean[] fired = {false};
      NoblePhantasmCastService.begin(caster, "paracelsus_elemental_sword", 100, ticks -> cooldown[0] = ticks, () -> { });
      NoblePhantasmCastService.deferEffect(caster, () -> fired[0] = true);
      caster.setCurrentMp(110); // unrelated recovery must survive the refund
      h.runAfterDelay(122, () -> {
         h.assertTrue(NoblePhantasmCastService.inWindow(caster), "four-to-three-second interrupt window missing");
         double manaBeforeCancel = caster.getCurrentMp();
         NoblePhantasmCastService.cancel(caster, true);
         h.assertTrue(Math.abs(caster.getCurrentMp() - (manaBeforeCancel + 50.0)) < 1.0E-3,
            "mana settlement did not add half the paid amount: before=" + manaBeforeCancel + " after=" + caster.getCurrentMp());
         h.assertTrue(cooldown[0] == 50, "interrupted cooldown expected 50 but was " + cooldown[0]);
         h.runAfterDelay(30, () -> { h.assertTrue(!fired[0], "interrupted effect still ran"); caster.discard(); h.succeed(); });
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void instantNpsReleaseInTheSameTickWithoutChanting(GameTestHelper h) {
      var level = h.getLevel();
      for (String policy : new String[]{"okita_mumyoudan_zuki", "cursed_arm_hassan_zabaniya", "li_shuwen_wu_er_da", "lancelot_arondight"}) {
         var caster = BuiltinServantEntityFactory.create(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", "medea"));
         caster.moveTo(h.absolutePos(new BlockPos(3, 3, 3)).getCenter());
         caster.setNoAi(true);
         level.addFreshEntity(caster);
         int[] fired = {0};
         int[] interrupted = {0};
         NoblePhantasmCastService.begin(caster, policy, 100, ticks -> interrupted[0]++, () -> interrupted[0]++);
         h.assertTrue(NoblePhantasmCastService.releaseAt(caster) == level.getGameTime(), policy + " still waits for its voice");
         h.assertTrue(NoblePhantasmCastService.deferEffect(caster, () -> fired[0]++), "effect ownership was not transferred");
         h.assertTrue(fired[0] == 1 && !NoblePhantasmCastService.chanting(caster), policy + " did not release immediately");
         h.assertTrue(!NoblePhantasmCastService.inWindow(caster), policy + " can still be interrupted");
         NoblePhantasmCastService.cancel(caster, true);
         h.assertTrue(interrupted[0] == 0, policy + " received interrupted settlement after release");
         h.assertTrue(!NoblePhantasmCastService.suppressLegacyVoice(caster,
            net.xxxjk.TYPE_MOON_WORLD.init.ModSounds.OKITA_SOUJI_SABER_VOICE_NP.get()), "instant release voice was suppressed");
         caster.discard();
      }
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void instantCardsStartTheirEffectsWithoutAChant(GameTestHelper h) {
      var player = h.makeMockServerPlayerInLevel();
      player.moveTo(h.absolutePos(new BlockPos(2, 3, 2)).getCenter(), 0, 0);
      player.setNoGravity(true);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servantCardState().setTransformed(true);
      vars.servant_card_id = "cursed_arm_hassan";
      vars.servantCardState().setMaxMana(1000);
      vars.servantCardState().setMana(500);
      var target = h.spawn(EntityType.IRON_GOLEM, new BlockPos(2, 3, 5));
      target.setNoAi(true);
      h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.triggerAction(player, 9), "Zabaniya activation failed");
      h.assertTrue(vars.servant_card_hassan_zabaniya_animation_until == player.tickCount + 32, "Zabaniya animation still waits for the voice");
      h.assertTrue(!NoblePhantasmCastService.chanting(player), "instant card entered chanting state");
      h.assertTrue(Math.abs(vars.servantCardState().mana() - 420) < 1.0E-3, "instant card was not charged exactly once");

      vars.servant_card_id = "lancelot_berserker";
      vars.servantCardState().setNpCooldown(0);
      vars.servantCardState().setNpCooldownEnd(0);
      h.assertTrue(net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager.triggerAction(player, 9), "Aroundight activation failed");
      h.assertTrue(player.getMainHandItem().is(ModItems.AROUNDIGHT.get()), "Aroundight was not equipped immediately");
      h.assertTrue(!NoblePhantasmCastService.chanting(player), "Aroundight entered chanting state");
      target.discard();
      h.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 180)
   public static void longNpKeepsItsReleaseWindowAndDistinctShortVoice(GameTestHelper h) {
      var level = h.getLevel();
      var caster = BuiltinServantEntityFactory.create(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", "medea"));
      caster.moveTo(h.absolutePos(new BlockPos(3, 3, 3)).getCenter());
      caster.setNoAi(true);
      level.addFreshEntity(caster);
      int[] fired = {0};
      long start = level.getGameTime();
      long voiceEnd = NoblePhantasmCastService.begin(caster, "paracelsus_elemental_sword", 100, ticks -> { }, () -> { });
      h.assertTrue(NoblePhantasmCastService.releaseAt(caster) == voiceEnd - 60, "long NP release moved from the final three seconds");
      NoblePhantasmCastService.deferEffect(caster, () -> fired[0]++);
      h.assertTrue(NoblePhantasmCastService.suppressLegacyVoice(caster,
         net.xxxjk.TYPE_MOON_WORLD.init.ModSounds.PARACELSUS_VOICE_NP.get()), "long NP would replay its chant");
      h.assertTrue(!NoblePhantasmCastService.suppressLegacyVoice(caster,
         net.xxxjk.TYPE_MOON_WORLD.init.ModSounds.ARTORIA_VOICE_EXCALIBUR_SHORT.get()), "a distinct short voice was suppressed");
      h.runAfterDelay(121, () -> h.assertTrue(fired[0] == 0 && !NoblePhantasmCastService.inWindow(caster), "long NP released or became interruptible early"));
      h.runAfterDelay(123, () -> h.assertTrue(fired[0] == 0 && NoblePhantasmCastService.inWindow(caster), "long NP lost its four-to-three-second interrupt window"));
      h.runAfterDelay(143, () -> h.assertTrue(fired[0] == 1 && !NoblePhantasmCastService.inWindow(caster), "long NP did not release once at the final three seconds"));
      h.runAfterDelay(163, () -> {
         h.assertTrue(level.getGameTime() >= start + 163 && !NoblePhantasmCastService.inWindow(caster), "last two seconds became interruptible");
         h.assertTrue(fired[0] == 1, "long NP effect repeated");
         caster.discard();
         h.succeed();
      });
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 40)
   public static void repeatedClearingCannotDigBelowOriginalSurface(GameTestHelper h) {
      var level = h.getLevel(); BlockPos surface = h.absolutePos(new BlockPos(2, 3, 2));
      level.setBlockAndUpdate(surface, Blocks.STONE.defaultBlockState());
      level.setBlockAndUpdate(surface.above(), Blocks.AIR.defaultBlockState());
      h.assertTrue(!CombatTerrainProtection.removeBlock(level, surface, false), "ordinary impact removed the surface");
      h.assertTrue(!CombatTerrainProtection.removeBlock(level, surface.below(), false), "ordinary impact removed a support layer");
      h.succeed();
   }
}

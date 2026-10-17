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

   @GameTest(template = "ancient_temple", timeoutTicks = 80)
   public static void interruptedNpCancelsEffectAndRefundsOnlyItsDebit(GameTestHelper h) {
      var level = h.getLevel();
      var caster = BuiltinServantEntityFactory.create(level, ResourceLocation.fromNamespaceAndPath("typemoonworld", "medea"));
      caster.moveTo(h.absolutePos(new BlockPos(3, 3, 3)).getCenter()); caster.setNoAi(true); level.addFreshEntity(caster);
      caster.setCurrentMp(100);
      NoblePhantasmCastService.noteNpcPayment(caster, 100);
      int[] cooldown = {0}; boolean[] fired = {false};
      NoblePhantasmCastService.begin(caster, "zhao_yun_changbanpo", 100, ticks -> cooldown[0] = ticks, () -> { });
      NoblePhantasmCastService.deferEffect(caster, () -> fired[0] = true);
      caster.setCurrentMp(110); // unrelated recovery must survive the refund
      h.assertTrue(NoblePhantasmCastService.inWindow(caster), "short recorded voice window missing");
      NoblePhantasmCastService.cancel(caster, true);
      h.assertTrue(caster.getCurrentMp() == 160 && cooldown[0] == 50, "half-cost settlement rewound unrelated mana");
      h.runAfterDelay(30, () -> { h.assertTrue(!fired[0], "interrupted effect still ran"); caster.discard(); h.succeed(); });
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

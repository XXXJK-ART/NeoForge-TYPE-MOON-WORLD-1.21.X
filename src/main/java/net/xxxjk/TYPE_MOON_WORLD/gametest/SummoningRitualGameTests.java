package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SummoningCircleBlock;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemNecklaceItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.summon.SummoningRitualService;

@GameTestHolder("typemoonworld_summoning")
@PrefixGameTestTemplate(false)
public final class SummoningRitualGameTests {
   private static final BlockPos CIRCLE = new BlockPos(3, 8, 3);

   private SummoningRitualGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 240)
   public static void necklaceRitualCompletesWithoutTransferringMana(GameTestHelper helper) {
      ServerPlayer player = master(helper, new ItemStack(ModItems.GEM_NECKLACE.get()));
      player.setShiftKeyDown(true);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.player_max_mana = 1000;
      vars.player_mana = 500;
      useInAir(player);
      helper.assertTrue(player.isUsingItem(), "zero-duration catalyst did not enter ritual use");
      helper.assertTrue(GemNecklaceItem.stored(player.getMainHandItem()) == 0, "ritual deposited mana into necklace");
      helper.runAfterDelay(5, () -> {
         helper.assertTrue(player.isUsingItem(), "functional catalyst stopped using before the chant completed");
         helper.assertTrue(helper.getBlockState(CIRCLE).getValue(SummoningCircleBlock.LIT), "circle went dark");
      });
      helper.runAfterDelay(210, () -> {
         helper.assertTrue(!helper.getBlockState(CIRCLE).is(ModBlocks.SUMMONING_CIRCLE.get()), "completed ritual did not consume circle");
         helper.assertTrue(!vars.master_servant_uuid.isBlank(), "completed ritual did not bind a servant");
         helper.assertTrue(!player.isUsingItem(), "completed ritual left the catalyst in use");
         helper.assertTrue(GemNecklaceItem.stored(player.getMainHandItem()) == 0, "ritual transferred necklace mana");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void excaliburChantDoesNotChargeOrReleaseNoblePhantasm(GameTestHelper helper) {
      ServerPlayer player = master(helper, new ItemStack(ModItems.EXCALIBUR.get()));
      useOnCircle(helper, player);
      helper.runAfterDelay(5, () -> {
         helper.assertTrue(player.isUsingItem(), "Excalibur interrupted the ritual");
         helper.assertTrue(!player.getPersistentData().contains("TypeMoonExcaliburCharge"), "ritual charged Excalibur");
         player.releaseUsingItem();
         helper.assertTrue(!SummoningRitualService.isRitualUsing(player, player.getMainHandItem()), "release did not cancel ritual");
         helper.assertTrue(!helper.getBlockState(CIRCLE).getValue(SummoningCircleBlock.LIT), "cancelled circle stayed lit");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void ordinaryWeaponCatalystKeepsChanting(GameTestHelper helper) {
      ServerPlayer player = master(helper, new ItemStack(ModItems.AROUNDIGHT.get()));
      useOnCircle(helper, player);
      helper.runAfterDelay(5, () -> {
         helper.assertTrue(player.isUsingItem(), "weapon with no normal use duration interrupted chant");
         player.releaseUsingItem();
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 20)
   public static void failedRitualStillSuppressesCatalystAbility(GameTestHelper helper) {
      ServerPlayer player = master(helper, new ItemStack(ModItems.GEM_NECKLACE.get()));
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_active = false;
      player.setShiftKeyDown(true);
      useInAir(player);
      helper.assertTrue(!player.isUsingItem(), "invalid master began a ritual");
      helper.assertTrue(GemNecklaceItem.stored(player.getMainHandItem()) == 0, "failed ritual fell through to item use");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 20)
   public static void necklaceWorksNormallyOutsideCircle(GameTestHelper helper) {
      ServerPlayer player = master(helper, new ItemStack(ModItems.GEM_NECKLACE.get()));
      helper.setBlock(CIRCLE, Blocks.AIR);
      player.setShiftKeyDown(true);
      useInAir(player);
      helper.assertTrue(GemNecklaceItem.stored(player.getMainHandItem()) == 100, "ordinary necklace use was suppressed outside circle");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 20)
   public static void blockCatalystDoesNotPlaceOrRestartChant(GameTestHelper helper) {
      ServerPlayer player = master(helper, new ItemStack(ModBlocks.ANCIENT_TEMPLE_STONE.get()));
      useOnCircle(helper, player);
      long started = player.getPersistentData().getLong("TmwSummoningRitualStart");
      helper.runAfterDelay(3, () -> {
         useOnCircle(helper, player);
         helper.assertTrue(helper.getBlockState(CIRCLE.above()).isAir(), "block catalyst was placed");
         helper.assertTrue(player.getPersistentData().getLong("TmwSummoningRitualStart") == started, "repeated right-click restarted chant");
         helper.succeed();
      });
   }

   private static ServerPlayer master(GameTestHelper helper, ItemStack catalyst) {
      helper.setBlock(CIRCLE, ModBlocks.SUMMONING_CIRCLE.get());
      helper.setBlock(CIRCLE.above(), Blocks.AIR);
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      player.setNoGravity(true);
      player.setPos(helper.absolutePos(CIRCLE).getBottomCenter());
      player.setItemInHand(InteractionHand.MAIN_HAND, catalyst);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.APPLE));
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.is_magus = true;
      vars.master_active = true;
      vars.player_max_mana = 1000;
      vars.player_mana = 500;
      // Embedded mock connections do not advance the normal player simulation.
      helper.onEachTick(player::doTick);
      return player;
   }

   private static void useInAir(ServerPlayer player) {
      player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
   }

   private static void useOnCircle(GameTestHelper helper, ServerPlayer player) {
      BlockPos circle = helper.absolutePos(CIRCLE);
      InteractionResult result = player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(),
         InteractionHand.MAIN_HAND, new BlockHitResult(circle.getCenter(), Direction.UP, circle, false));
      helper.assertTrue(result == InteractionResult.SUCCESS, "circle did not consume catalyst interaction");
   }
}

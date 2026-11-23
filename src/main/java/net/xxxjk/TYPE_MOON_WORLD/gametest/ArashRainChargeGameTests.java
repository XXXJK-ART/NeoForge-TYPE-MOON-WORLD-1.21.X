package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ArashBowItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardArashSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;

@GameTestHolder("typemoonworld_arash")
@PrefixGameTestTemplate(false)
public final class ArashRainChargeGameTests {
   private ArashRainChargeGameTests() { }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void vanillaReleaseDuringRainNeverFiresBasicArrow(GameTestHelper helper) {
      ServerPlayer player = arash(helper);
      int stock = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().arashArrowStock();
      ServantCardArashSkills.beginArrowRainCharge(player);
      player.releaseUsingItem();
      helper.assertTrue(arrows(player, ArashParticleArrowEntity.NORMAL) == 0,
         "Vanilla use-key release fired a basic arrow during rain charge");
      helper.assertTrue(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().arashArrowStock() == stock,
         "Vanilla release consumed an extra arrow during rain charge");
      ServantCardArashSkills.clear(player);
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void rainReleaseConsumesOnlyRainAndClearsCharge(GameTestHelper helper) {
      ServerPlayer player = arash(helper);
      var state = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      int stock = state.arashArrowStock();
      ServantCardArashSkills.beginArrowRainCharge(player);
      player.getPersistentData().putLong(ServantCardArashSkills.ARROW_RAIN_CHARGE_START,
         player.level().getGameTime() - ArashCombatRules.ARROW_MAX_CHARGE_TICKS);
      ServantCardArashSkills.releaseArrowRainCharge(player);
      helper.assertTrue(arrows(player, ArashParticleArrowEntity.RAIN) == ArashCombatRules.RAIN_ARROW_COUNT,
         "Release did not fire the rain volley");
      helper.assertTrue(arrows(player, ArashParticleArrowEntity.NORMAL) == 0, "Rain release fired a basic arrow");
      helper.assertTrue(state.arashArrowStock() == stock - ArashCombatRules.ARROW_RAIN_COST,
         "Rain charged an additional basic-arrow cost");
      helper.assertTrue(!player.isUsingItem()
         && !player.getPersistentData().contains(ServantCardArashSkills.ARROW_RAIN_CHARGE_START)
         && !player.getPersistentData().contains(ServantCardArashSkills.ARROW_RAIN_CHARGE_RELEASE)
         && !player.getPersistentData().contains(ArashBowItem.INTERRUPTED_TAG), "Rain left stale charge state");
      ServantCardArashSkills.releaseArrowRainCharge(player);
      helper.assertTrue(state.arashArrowStock() == stock - ArashCombatRules.ARROW_RAIN_COST,
         "Duplicate release consumed more arrows");
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void rainEquipsExistingBowAndSwapCancelsCharge(GameTestHelper helper) {
      ServerPlayer player = arash(helper);
      int bowSlot = player.getInventory().selected;
      player.getInventory().selected = (bowSlot + 1) % 9;
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
      ServantCardArashSkills.beginArrowRainCharge(player);
      helper.assertTrue(player.getMainHandItem().is(ModItems.ARASH_BOW.get()) && player.isUsingItem(),
         "Rain did not select the existing special bow for its charge");
      player.getInventory().selected = (bowSlot + 1) % 9;
      ServantCardArashSkills.tick(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      player.getInventory().selected = bowSlot;
      ServantCardArashSkills.releaseArrowRainCharge(player);
      helper.assertTrue(arrows(player, ArashParticleArrowEntity.RAIN) == 0, "Swapping back revived a canceled rain charge");
      helper.assertTrue(!player.isUsingItem(), "Swapping away left the bow drawn");
      ArashBowItem bow = (ArashBowItem)player.getMainHandItem().getItem();
      bow.use(player.level(), player, InteractionHand.MAIN_HAND);
      player.releaseUsingItem();
      helper.assertTrue(arrows(player, ArashParticleArrowEntity.NORMAL) == 1, "Canceled rain suppressed the next normal shot");
      helper.succeed();
   }

   private static ServerPlayer arash(GameTestHelper helper) {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(player.connection.getConnection());
      return transform(helper, player);
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void damagedRainCancelsWithoutCostAndAllowsNextNormalShot(GameTestHelper helper) {
      var level = helper.getLevel();
      var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
         new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "arash-rain-damage"), false);
      var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
      var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
      new io.netty.channel.embedded.EmbeddedChannel(connection);
      net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
      level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
      player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
      transform(helper, player);
      var state = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      int stock = state.arashArrowStock();
      ServantCardArashSkills.beginArrowRainCharge(player);
      player.getAbilities().invulnerable = false;
      player.invulnerableTime = 0;
      player.hurt(level.damageSources().genericKill(), 1.0F);
      helper.assertTrue(!player.isUsingItem(), "Damage did not cancel the rain draw");
      helper.assertTrue(!player.getPersistentData().contains(ServantCardArashSkills.ARROW_RAIN_CHARGE_START),
         "Damage left an active rain charge");
      ServantCardArashSkills.releaseArrowRainCharge(player);
      helper.assertTrue(arrows(player, ArashParticleArrowEntity.NORMAL) == 0
         && arrows(player, ArashParticleArrowEntity.RAIN) == 0 && state.arashArrowStock() == stock,
         "Releasing interrupted rain fired or consumed arrows");
      ArashBowItem bow = (ArashBowItem)player.getMainHandItem().getItem();
      bow.use(level, player, InteractionHand.MAIN_HAND);
      player.releaseUsingItem();
      helper.assertTrue(arrows(player, ArashParticleArrowEntity.NORMAL) == 1
         && state.arashArrowStock() == stock - ArashCombatRules.NORMAL_ARROW_COST,
         "Interrupted rain suppressed or duplicated the next normal shot");
      player.discard();
      helper.succeed();
   }

   private static ServerPlayer transform(GameTestHelper helper, ServerPlayer player) {
      BlockPos start = helper.absolutePos(new BlockPos(2, 3, 2));
      player.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, -90.0F, 0.0F);
      helper.assertTrue(ServantCardTransformManager.transform(player, "arash"), "Arash transformation failed");
      player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_mana = 1000.0;
      return player;
   }

   private static int arrows(ServerPlayer player, int variant) {
      return player.serverLevel().getEntitiesOfClass(ArashParticleArrowEntity.class,
         player.getBoundingBox().inflate(8.0), arrow -> arrow.getOwner() == player && arrow.getVariant() == variant).size();
   }
}

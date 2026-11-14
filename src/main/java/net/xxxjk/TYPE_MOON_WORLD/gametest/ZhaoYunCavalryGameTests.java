package net.xxxjk.TYPE_MOON_WORLD.gametest;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardZhaoYunSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;

@GameTestHolder("typemoonworld_zhaoyun")
@PrefixGameTestTemplate(false)
public final class ZhaoYunCavalryGameTests {
   private ZhaoYunCavalryGameTests() { }

   private static BlockPos arena(GameTestHelper h) {
      BlockPos ground = h.absolutePos(new BlockPos(3, 30, 3));
      for (BlockPos pos : BlockPos.betweenClosed(ground.offset(-30, 0, -30), ground.offset(30, 0, 30))) {
         h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
      }
      return ground.above();
   }

   private static ServerPlayer player(GameTestHelper h) {
      var cookie = CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "zhaoyun-test"), false);
      var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
      var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
      new io.netty.channel.embedded.EmbeddedChannel(connection);
      h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
      player.setGameMode(GameType.SURVIVAL);
      player.setPos(arena(h).getBottomCenter());
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true;
      vars.servant_card_id = "zhao_yun_rider";
      vars.servantCardState().setMaxMana(1000);
      vars.servantCardState().setMana(1000);
      // Embedded connections send no player input packets, so explicitly drive
      // the card tick just as the real player's tick handler does.
      h.onEachTick(() -> {
         if (!player.isRemoved()) ServantCardZhaoYunSkills.tick(player, vars);
      });
      return player;
   }

   @GameTest(template = "empty", timeoutTicks = 290, batch = "zhaoyun_player")
   public static void mountedPlayerKeepsSameHorseAndReleasesOnSharedVoiceTimeline(GameTestHelper h) {
      var player = player(h);
      h.assertTrue(ServantCardZhaoYunSkills.summonSkillHakuryu(player), "Skill horse summon failed");
      var horse = player.getVehicle();
      // Old/reloaded NP state can still refer to this same persistent horse.
      player.getPersistentData().putUUID(ServantCardZhaoYunSkills.TAG_NP_MOUNT_UUID, horse.getUUID());
      h.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Mounted NP rejected");
      long now = h.getLevel().getGameTime();
      long release = NoblePhantasmCastService.releaseAt(player);
      h.assertTrue(NoblePhantasmCastService.voiceEnd(player) - now >= 300, "Sixteen-second clip registered as a one-second chant");
      h.runAfterDelay(5, () -> h.assertTrue(player.getVehicle() == horse && horse.isAlive(), "Chant forcibly dismounted player"));
      h.runAfterDelay(release - now - 2, () -> {
         h.assertTrue(player.getVehicle() == horse, "Horse replaced during chant");
         h.assertTrue(!ServantCardZhaoYunSkills.isNpActive(player), "NP released before shared timeline");
      });
      h.runAfterDelay(release - now + 2, () -> {
         h.assertTrue(ServantCardZhaoYunSkills.isNpActive(player), "Extra local chant delayed NP release at "
            + h.getLevel().getGameTime() + " release=" + release + " state=" + player.getPersistentData());
         h.assertTrue(player.getVehicle() == horse && horse.isAlive(), "Re-mounting same horse discarded it");
         h.assertTrue(player.getPersistentData().getInt("TypeMoonNpCastCount") == 1, "NP started more than once");
         ServantCardZhaoYunSkills.clear(player);
         player.discard();
         h.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 290, batch = "zhaoyun_cancel")
   public static void cancelledChantKeepsSkillHorseAndNeverReleases(GameTestHelper h) {
      var player = player(h);
      h.assertTrue(ServantCardZhaoYunSkills.summonSkillHakuryu(player), "Skill horse summon failed");
      var horse = player.getVehicle();
      h.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "NP rejected");
      h.runAfterDelay(10, () -> NoblePhantasmCastService.cancel(player, true));
      h.runAfterDelay(270, () -> {
         h.assertTrue(!ServantCardZhaoYunSkills.isNpActive(player), "Cancelled NP released later");
         h.assertTrue(player.getVehicle() == horse && horse.isAlive(), "Cancellation destroyed ordinary horse");
         ServantCardZhaoYunSkills.clear(player);
         player.discard();
         h.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 190, batch = "zhaoyun_cavalry")
   public static void mountedNpcRunsThroughInsteadOfStoppingBesideEnemy(GameTestHelper h) {
      var pos = arena(h).getBottomCenter();
      var rider = ModEntities.ZHAO_YUN_RIDER.get().create(h.getLevel());
      rider.setNoAi(true);
      rider.setPos(pos);
      rider.getPersistentData().putBoolean(ZhaoYunRiderEntity.TAG_MOUNT_UNLOCKED, true);
      h.getLevel().addFreshEntity(rider);
      var target = EntityType.IRON_GOLEM.create(h.getLevel());
      target.setNoAi(true);
      target.setInvulnerable(true);
      target.setPos(pos.add(0, 0, 2.5));
      h.getLevel().addFreshEntity(target);
      rider.setTarget(target);
      int[] crossings = {0};
      double[] previous = {-2.5};
      h.onEachTick(() -> {
         var horse = rider.getHakuryu();
         if (horse == null || !target.isAlive()) return;
         double offset = horse.getZ() - target.getZ();
         if (Math.abs(offset) > 1.0 && Math.signum(offset) != Math.signum(previous[0])) {
            crossings[0]++;
            previous[0] = offset;
         }
      });
      h.runAfterDelay(20, () -> {
         var horse = rider.getHakuryu();
         h.assertTrue(horse != null && rider.getVehicle() == horse, "NPC did not mount");
         h.assertTrue(horse.position().subtract(target.position()).horizontalDistance() > 7,
            "Cavalry stayed beside enemy instead of continuing its pass");
      });
      h.runAfterDelay(160, () -> {
         h.assertTrue(crossings[0] >= 2, "Cavalry did not turn and make another pass");
         h.assertTrue(rider.getPersistentData().getLong("ZhaoYunNormalThrustUntil") > 0,
            "NPC never used its ordinary mounted right-click thrust");
         target.discard();
      });
      h.runAfterDelay(170, () -> {
         var horse = rider.getHakuryu();
         var before = horse.position();
         h.runAfterDelay(3, () -> {
            h.assertTrue(horse.position().subtract(before).horizontalDistance() < 0.1,
               "Horse kept charging after combat ended");
            rider.discard();
            horse.discard();
            h.succeed();
         });
      });
   }

   @GameTest(template = "empty", timeoutTicks = 290, batch = "zhaoyun_npc_np")
   public static void npcHasOnePendingCastAndReleasesWithoutASecondChant(GameTestHelper h) {
      var rider = ModEntities.ZHAO_YUN_RIDER.get().create(h.getLevel());
      rider.setNoAi(true);
      rider.setPos(arena(h).getBottomCenter());
      h.getLevel().addFreshEntity(rider);
      rider.setCurrentMp(rider.getMaxMp());
      h.assertTrue(rider.startChangbanpo(), "NPC NP rejected");
      double mana = rider.getCurrentMp();
      h.assertTrue(rider.isChangbanpoCharging() && !rider.startChangbanpo(), "NPC did not reserve its pending cast");
      h.assertTrue(rider.getCurrentMp() == mana, "NPC paid for the pending NP twice");
      long release = NoblePhantasmCastService.releaseAt(rider);
      h.runAfterDelay(release - h.getLevel().getGameTime() + 2, () -> {
         h.assertTrue(rider.isChangbanpoActive() && rider.getVehicle() == rider.getHakuryu(), "NPC release added another chant or failed to mount");
         h.assertTrue(rider.getPersistentData().getInt("TypeMoonNpCastCount") == 1, "NPC restarted its pending cast");
         var horse = rider.getHakuryu();
         rider.discard();
         if (horse != null) horse.discard();
         h.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 30, batch = "zhaoyun_qinggang")
   public static void mountedNormalThrustKeepsQinggangDelayedFollowUp(GameTestHelper h) {
      var pos = arena(h).getBottomCenter();
      var rider = ModEntities.ZHAO_YUN_RIDER.get().create(h.getLevel());
      rider.setNoAi(true);
      rider.setPos(pos);
      rider.getPersistentData().putBoolean(ZhaoYunRiderEntity.TAG_MOUNT_UNLOCKED, true);
      h.getLevel().addFreshEntity(rider);
      rider.setCurrentMp(rider.getMaxMp());
      var target = EntityType.IRON_GOLEM.create(h.getLevel());
      target.setNoAi(true);
      target.setPos(pos.add(0, 0, 2.5));
      h.getLevel().addFreshEntity(target);
      h.runAfterDelay(2, () -> {
         var horse = rider.getHakuryu();
         horse.setYRot(0);
         h.assertTrue(rider.startQinggangSword(), "Qinggang rejected");
         rider.tryMountedNormalThrust(horse, target);
         float afterThrust = target.getHealth();
         h.assertTrue(afterThrust < target.getMaxHealth(), "Normal thrust missed");
         h.runAfterDelay(12, () -> {
            h.assertTrue(target.getHealth() < afterThrust, "Mounted normal thrust lost Qinggang's delayed second strike");
            rider.discard();
            horse.discard();
            target.discard();
            h.succeed();
         });
      });
   }
}

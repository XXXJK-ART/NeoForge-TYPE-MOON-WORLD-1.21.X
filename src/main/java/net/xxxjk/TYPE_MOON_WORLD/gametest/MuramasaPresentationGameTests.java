package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSenkoMuramasaSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MuramasaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SenkoMuramasaEntity;

@GameTestHolder("typemoonworld_muramasa")
@PrefixGameTestTemplate(false)
public final class MuramasaPresentationGameTests {
   private MuramasaPresentationGameTests() { }

   private static ServerPlayer player(GameTestHelper helper) {
      // Vanilla's mock overrides isCreative() to true even after changing game mode.
      // Use a connected survival player so damage traverses the normal combat events.
      var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
         new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "muramasa-test"), false);
      var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
      var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
      new io.netty.channel.embedded.EmbeddedChannel(connection);
      helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
      player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
      player.setInvulnerable(false);
      // Keep each 25-block sword field clear of neighbouring test structures.
      BlockPos center = helper.absolutePos(new BlockPos(3, 50, 3));
      player.setPos(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
      player.setNoGravity(true);
      helper.getLevel().setBlock(center, Blocks.STONE.defaultBlockState(), 3);
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_transformed = true;
      vars.servant_card_id = "senko_muramasa";
      vars.servantCardState().setMaxMana(1000);
      vars.servantCardState().setMana(1000);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
      return player;
   }

   @GameTest(template = "empty", timeoutTicks = 20, batch = "muramasa_orphan")
   public static void unloadedNpcWithoutFieldDoesNotKeepAnOrphanedPendingLock(GameTestHelper helper) {
      var entity = ModEntities.SENKO_MURAMASA.get().create(helper.getLevel());
      entity.setNoAi(true);
      entity.setPos(helper.absolutePos(new BlockPos(3, 50, 3)).getCenter());
      entity.getPersistentData().putBoolean("MuramasaNoblePhantasmPending", true);
      entity.getPersistentData().putLong("MuramasaActionLockUntil", Long.MAX_VALUE);
      helper.getLevel().addFreshEntity(entity);
      helper.runAfterDelay(3, () -> {
         helper.assertTrue(!MuramasaCombatHelper.isSummonPending(entity), "NPC without terrain retained an orphaned pending lock");
         helper.assertTrue(!entity.getPersistentData().contains("MuramasaActionLockUntil"), "NPC retained its abandoned action lock");
         entity.discard();
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 20, batch = "muramasa_recovery")
   public static void persistedFieldWithoutLiveCastRestoresInsteadOfGrantingSword(GameTestHelper helper) {
      var entity = ModEntities.SENKO_MURAMASA.get().create(helper.getLevel());
      entity.setNoAi(true);
      entity.setNoGravity(true);
      var ground = helper.absolutePos(new BlockPos(3, 50, 3));
      entity.setPos(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5);
      helper.getLevel().setBlock(ground, Blocks.RED_SANDSTONE.defaultBlockState(), 3);
      var terrain = new net.minecraft.nbt.ListTag();
      var entry = new net.minecraft.nbt.CompoundTag();
      entry.putLong("pos", ground.asLong());
      entry.putInt("original", net.minecraft.world.level.block.Block.getId(Blocks.STONE.defaultBlockState()));
      entry.putInt("replacement", net.minecraft.world.level.block.Block.getId(Blocks.RED_SANDSTONE.defaultBlockState()));
      terrain.add(entry);
      var data = entity.getPersistentData();
      data.putBoolean("MuramasaFieldActive", true);
      data.put("MuramasaFieldTerrain", terrain);
      data.putString("MuramasaFieldDimension", helper.getLevel().dimension().location().toString());
      data.putBoolean("MuramasaNoblePhantasmPending", true);
      helper.getLevel().addFreshEntity(entity);
      helper.runAfterDelay(3, () -> {
         helper.assertTrue(helper.getLevel().getBlockState(ground).is(Blocks.STONE), "Persisted terrain was stranded after losing the cast token");
         helper.assertTrue(!data.getBoolean("MuramasaNoblePhantasmPending"), "Persisted pending flag locked NPC forever");
         helper.assertTrue(!entity.getMainHandItem().is(ModItems.TSUMUKARI_MURAMASA.get()), "Orphaned field granted a free NP sword");
         entity.discard();
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 330, batch = "muramasa_player")
   public static void playerExpandsDuringVoiceAndReceivesSwordOnlyAtVoiceEnd(GameTestHelper helper) {
      var player = player(helper);
      var level = helper.getLevel();
      var ground = player.blockPosition().below();
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Player NP rejected");
      long voiceEnd = NoblePhantasmCastService.voiceEnd(player);
      int duration = (int)(voiceEnd - level.getGameTime());
      helper.assertTrue(duration == 302 && NoblePhantasmCastService.releaseAt(player) == voiceEnd,
         "NP release does not match the 15.08-second voice");
      helper.assertTrue(player.getPersistentData().getBoolean("MuramasaFieldActive"), "Presentation did not begin with the voice");
      helper.runAfterDelay(5, () -> helper.assertTrue(level.getBlockState(ground).is(Blocks.RED_SANDSTONE), "NPC terrain expansion missing"));
      helper.runAfterDelay(duration - 2, () -> {
         helper.assertTrue(!player.getMainHandItem().is(ModItems.TSUMUKARI_MURAMASA.get()), "Usable NP sword granted during chanting");
         helper.assertTrue(!ServantCardSenkoMuramasaSkills.isNpUsed(player), "Presentation counted as the NP taking effect");
         helper.assertTrue(player.getPersistentData().getBoolean("MuramasaFieldCollapsing"), "Sword field did not enter NPC collapse phase");
      });
      helper.runAfterDelay(duration + 2, () -> {
         helper.assertTrue(player.getMainHandItem().is(ModItems.TSUMUKARI_MURAMASA.get()), "No NP sword at voice end");
         helper.assertTrue(ServantCardSenkoMuramasaSkills.isNpUsed(player), "Completed NP not recorded");
         helper.assertTrue(level.getBlockState(ground).is(Blocks.STONE), "Temporary terrain was not restored");
         helper.assertTrue(!player.getPersistentData().getBoolean(ServantCardSenkoMuramasaSkills.NP_PENDING), "Casting lock not cleared");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 330, batch = "muramasa_cancel")
   public static void interruptedPlayerRestoresTerrainAndNeverReceivesDelayedSword(GameTestHelper helper) {
      var player = player(helper);
      var ground = player.blockPosition().below();
      helper.assertTrue(ServantCardTransformManager.triggerAction(player, 9), "Player NP rejected");
      helper.runAfterDelay(230, () -> {
         helper.assertTrue(NoblePhantasmCastService.inWindow(player), "Expected damage interruption window");
         player.invulnerableTime = 0;
         helper.assertTrue(player.hurt(player.damageSources().magic(), 1.0F), "Interruption fixture did not receive damage");
         helper.assertTrue(NoblePhantasmCastService.cancelled(player), "Damage did not cancel the chant");
         helper.assertTrue(helper.getLevel().getBlockState(ground).is(Blocks.STONE), "Interrupted field left terrain behind");
         helper.assertTrue(!player.getPersistentData().getBoolean(ServantCardSenkoMuramasaSkills.NP_PENDING), "Interrupted field retained casting lock");
      });
      helper.runAfterDelay(310, () -> {
         helper.assertTrue(!player.getMainHandItem().is(ModItems.TSUMUKARI_MURAMASA.get()), "Cancelled cast granted a delayed sword");
         helper.assertTrue(!ServantCardSenkoMuramasaSkills.isNpUsed(player), "Cancelled forging consumed the one-time NP");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 330, batch = "muramasa_npc")
   public static void npcUsesTheSameVoiceBoundPresentation(GameTestHelper helper) throws Exception {
      var entity = ModEntities.SENKO_MURAMASA.get().create(helper.getLevel());
      entity.setNoAi(true);
      entity.setNoGravity(true);
      var center = helper.absolutePos(new BlockPos(3, 50, 3));
      entity.setPos(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
      entity.setCurrentMp(1000);
      helper.getLevel().addFreshEntity(entity);
      var start = MuramasaCombatHelper.class.getDeclaredMethod("summonTsumukari", SenkoMuramasaEntity.class, net.minecraft.server.level.ServerLevel.class);
      start.setAccessible(true);
      start.invoke(null, entity, helper.getLevel());
      helper.assertTrue(entity.getPersistentData().getBoolean("MuramasaFieldActive"), "NPC presentation waited until after its voice");
      int duration = (int)(NoblePhantasmCastService.voiceEnd(entity) - helper.getLevel().getGameTime());
      helper.runAfterDelay(duration - 2, () -> helper.assertTrue(!entity.getMainHandItem().is(ModItems.TSUMUKARI_MURAMASA.get()), "NPC sword granted before voice end"));
      helper.runAfterDelay(duration + 2, () -> {
         helper.assertTrue(entity.getMainHandItem().is(ModItems.TSUMUKARI_MURAMASA.get()), "NPC forging did not finish");
         helper.assertTrue(!entity.getPersistentData().getBoolean("MuramasaFieldActive"), "NPC field was not restored");
         entity.discard();
         helper.succeed();
      });
   }
}

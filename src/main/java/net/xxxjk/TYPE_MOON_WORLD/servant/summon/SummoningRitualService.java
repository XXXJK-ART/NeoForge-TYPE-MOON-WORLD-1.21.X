package net.xxxjk.TYPE_MOON_WORLD.servant.summon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

@EventBusSubscriber(modid = "typemoonworld")
public final class SummoningRitualService {
   private static final String ACTIVE = "TmwSummoningRitualActive";
   private static final String X = "TmwSummoningRitualX";
   private static final String Y = "TmwSummoningRitualY";
   private static final String Z = "TmwSummoningRitualZ";
   private static final String START = "TmwSummoningRitualStart";
   private static final String INDEX = "TmwSummoningRitualIndex";
   private static final int TICKS_PER_LINE = 20;

   private static final List<String> STANDARD_LINES = List.of(
      "message.typemoonworld.summon.chant.body",
      "message.typemoonworld.summon.chant.fate",
      "message.typemoonworld.summon.chant.response",
      "message.typemoonworld.summon.chant.obey",
      "message.typemoonworld.summon.chant.oath",
      "message.typemoonworld.summon.chant.good",
      "message.typemoonworld.summon.chant.evil",
      "message.typemoonworld.summon.chant.three_words",
      "message.typemoonworld.summon.chant.wheel",
      "message.typemoonworld.summon.chant.guardian"
   );
   private static final List<String> BERSERKER_LINES = List.of(
      "message.typemoonworld.summon.chant.berserker_eyes",
      "message.typemoonworld.summon.chant.berserker_prisoner",
      "message.typemoonworld.summon.chant.berserker_master"
   );

   private SummoningRitualService() {
   }

   public static boolean begin(ServerPlayer player, BlockPos circle, ItemStack catalyst) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.master_active || !isBlank(vars.master_servant_uuid)
         || SummoningRelicRegistry.candidates(catalyst).isEmpty()
         || !player.getMainHandItem().is(catalyst.getItem())) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.summon.invalid"), true);
         return false;
      }
      player.getPersistentData().putBoolean(ACTIVE, true);
      player.getPersistentData().putInt(X, circle.getX());
      player.getPersistentData().putInt(Y, circle.getY());
      player.getPersistentData().putInt(Z, circle.getZ());
      player.getPersistentData().putLong(START, player.level().getGameTime());
      player.getPersistentData().putInt(INDEX, -1);
      sendLine(player, 0);
      return true;
   }

   /** Handles block catalysts such as ancient temple stone, whose item class is BlockItem. */
   @SubscribeEvent
   public static void onRightClickCircle(RightClickBlock event) {
      if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
         || !(event.getEntity() instanceof ServerPlayer player)
         || !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.SUMMONING_CIRCLE.get())
         || SummoningRelicRegistry.candidates(event.getItemStack()).isEmpty()) {
         return;
      }
      if (begin(player, event.getPos(), event.getItemStack())) {
         player.startUsingItem(event.getHand());
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      if (!player.getPersistentData().getBoolean(ACTIVE)) return;
      ItemStack held = player.getMainHandItem();
      if (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND
         || held.isEmpty() || SummoningRelicRegistry.candidates(held).isEmpty()
         || !held.is(player.getUseItem().getItem())) {
         cancel(player);
         return;
      }
      BlockPos circle = ritualPos(player);
      if (!(player.level() instanceof ServerLevel level)
         || !level.getBlockState(circle).is(ModBlocks.SUMMONING_CIRCLE.get())
         || player.distanceToSqr(circle.getCenter()) > 25.0) {
         cancel(player);
         return;
      }
      long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(START);
      int lineIndex = (int)(elapsed / TICKS_PER_LINE);
      List<String> lines = linesFor(player.getUseItem());
      if (lineIndex > player.getPersistentData().getInt(INDEX) && lineIndex < lines.size()) {
         sendLine(player, lineIndex);
      }
      if (lineIndex >= lines.size()) {
         complete(player, level, circle);
      }
   }

   private static List<String> linesFor(ItemStack stack) {
      List<String> lines = new ArrayList<>(STANDARD_LINES);
      List<String> candidates = SummoningRelicRegistry.candidates(stack);
      if (candidates.stream().anyMatch(SummoningRitualService::isBerserker)) {
         lines.addAll(7, BERSERKER_LINES);
      }
      return lines;
   }

   private static boolean isBerserker(String id) {
      return "heracles".equals(id) || "nightingale".equals(id);
   }

   private static void sendLine(ServerPlayer player, int index) {
      List<String> lines = linesFor(player.getUseItem());
      if (index < 0 || index >= lines.size()) return;
      player.displayClientMessage(Component.translatable(lines.get(index)), false);
      player.getPersistentData().putInt(INDEX, index);
   }

   private static void complete(ServerPlayer master, ServerLevel level, BlockPos circle) {
      List<String> candidates = SummoningRelicRegistry.candidates(master.getMainHandItem());
      ServerPlayer playerServant = findPlayerServant(master, candidates);
      boolean success = playerServant != null
         ? summonPlayer(master, playerServant, level, circle)
         : summonNpc(master, candidates, level, circle);
      if (success) {
         level.removeBlock(circle, false);
         master.displayClientMessage(Component.translatable("message.typemoonworld.summon.success"), false);
      } else {
         master.displayClientMessage(Component.translatable("message.typemoonworld.summon.failed"), false);
      }
      clear(master);
   }

   private static ServerPlayer findPlayerServant(ServerPlayer master, List<String> candidates) {
      ServerPlayer result = null;
      double best = Double.MAX_VALUE;
      for (ServerPlayer player : master.getServer().getPlayerList().getPlayers()) {
         if (player == master || !player.isAlive()) continue;
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!candidates.contains(SummoningRelicRegistry.normalizeServantId(vars.servant_card_id))
            || !SummoningRelicRegistry.isMatchingPlayer(player, vars.servant_card_id)
            || !isBlank(vars.servant_card_master_uuid)) continue;
         double distance = player.level() == master.level()
            ? player.distanceToSqr(master) : Double.MAX_VALUE - player.getUUID().getLeastSignificantBits() * 0.000001;
         if (result == null || distance < best) {
            result = player;
            best = distance;
         }
      }
      return result;
   }

   private static boolean summonPlayer(ServerPlayer master, ServerPlayer servant, ServerLevel level, BlockPos circle) {
      if (!MasterStateManager.bindForSummoning(master, servant)) return false;
      servant.teleportTo(level, circle.getX() + 0.5, circle.getY() + 0.1, circle.getZ() + 0.5,
         Set.of(), servant.getYRot(), servant.getXRot());
      return true;
   }

   private static boolean summonNpc(ServerPlayer master, List<String> candidates, ServerLevel level, BlockPos circle) {
      if (candidates.isEmpty()) return false;
      String servantId = candidates.get(master.getRandom().nextInt(candidates.size()));
      ServantEntity entity = BuiltinServantEntityFactory.create(level,
         ResourceLocation.fromNamespaceAndPath("typemoonworld", servantId));
      if (entity == null) return false;
      entity.moveTo(circle.getX() + 0.5, circle.getY() + 0.1, circle.getZ() + 0.5, master.getYRot(), 0.0F);
      entity.finalizeSpawn(level, level.getCurrentDifficultyAt(circle), MobSpawnType.MOB_SUMMONED, null);
      if (!level.addFreshEntity(entity)) return false;
      if (!MasterStateManager.bindEntityServant(master, entity)) {
         entity.discard();
         return false;
      }
      return true;
   }

   private static BlockPos ritualPos(ServerPlayer player) {
      var data = player.getPersistentData();
      return new BlockPos(data.getInt(X), data.getInt(Y), data.getInt(Z));
   }

   private static void cancel(ServerPlayer player) {
      player.displayClientMessage(Component.translatable("message.typemoonworld.summon.cancelled"), true);
      clear(player);
   }

   private static void clear(ServerPlayer player) {
      player.getPersistentData().remove(ACTIVE);
      player.getPersistentData().remove(X);
      player.getPersistentData().remove(Y);
      player.getPersistentData().remove(Z);
      player.getPersistentData().remove(START);
      player.getPersistentData().remove(INDEX);
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }
}

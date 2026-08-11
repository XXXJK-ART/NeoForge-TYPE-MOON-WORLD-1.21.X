package net.xxxjk.TYPE_MOON_WORLD.servant.summon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SummoningCircleBlock;
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
   private static final String CATALYST = "TmwSummoningRitualCatalyst";
   private static final int TICKS_PER_LINE = 20;
   private static final DustParticleOptions RITUAL_RED = new DustParticleOptions(new Vector3f(1.0F, 0.025F, 0.02F), 1.35F);
   private static final DustParticleOptions RITUAL_DARK_RED = new DustParticleOptions(new Vector3f(0.48F, 0.0F, 0.015F), 1.8F);

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
      player.getPersistentData().putString(CATALYST, itemId(catalyst));
      setCircleLit(player.level(), circle, true);
      sendLine(player, 0);
      return true;
   }

   /** Handles block catalysts such as ancient temple stone, whose item class is BlockItem. */
   @SubscribeEvent
   public static void onRightClickCircle(RightClickBlock event) {
      if (event.getHand() != InteractionHand.MAIN_HAND
         || !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.SUMMONING_CIRCLE.get())
         || SummoningRelicRegistry.candidates(event.getItemStack()).isEmpty()) {
         return;
      }
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.SUCCESS);
      if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) return;
      if (begin(player, event.getPos(), event.getItemStack())) {
         if (!(event.getItemStack().getItem() instanceof BlockItem)) {
            player.startUsingItem(event.getHand());
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      if (!player.getPersistentData().getBoolean(ACTIVE)) return;
      ItemStack held = player.getMainHandItem();
      boolean blockCatalyst = held.getItem() instanceof BlockItem;
      boolean activelyUsing = blockCatalyst || player.isUsingItem()
         && player.getUsedItemHand() == InteractionHand.MAIN_HAND
         && held.is(player.getUseItem().getItem());
      if (held.isEmpty() || SummoningRelicRegistry.candidates(held).isEmpty()
         || !itemId(held).equals(player.getPersistentData().getString(CATALYST)) || !activelyUsing) {
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
      spawnRitualGlow(level, circle, elapsed);
      int lineIndex = (int)(elapsed / TICKS_PER_LINE);
      List<String> lines = linesFor(held);
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
      List<String> lines = linesFor(player.getMainHandItem());
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
         spawnSummoningCompletionGlow(level, circle);
         level.removeBlock(circle, false);
         master.displayClientMessage(Component.translatable("message.typemoonworld.summon.success"), false);
      } else {
         setCircleLit(level, circle, false);
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
      entity.ensureDefaultNpcLoadout(false);
      if (!level.addFreshEntity(entity)) return false;
      if (!MasterStateManager.bindEntityServant(master, entity)) {
         entity.discard();
         return false;
      }
      entity.ensureDefaultNpcLoadout(true);
      TYPE_MOON_WORLD.queueServerWork(1, () -> {
         if (entity.isAlive() && entity.level() == level) {
            entity.ensureDefaultNpcLoadout(true);
         }
      });
      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         if (entity.isAlive() && entity.level() == level) {
            entity.ensureDefaultNpcLoadout(true);
         }
      });
      return true;
   }

   private static BlockPos ritualPos(ServerPlayer player) {
      var data = player.getPersistentData();
      return new BlockPos(data.getInt(X), data.getInt(Y), data.getInt(Z));
   }

   private static void cancel(ServerPlayer player) {
      setCircleLit(player.level(), ritualPos(player), false);
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
      player.getPersistentData().remove(CATALYST);
   }

   private static String itemId(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return "";
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id == null ? "" : id.toString();
   }

   private static void setCircleLit(Level level, BlockPos circle, boolean lit) {
      if (level == null || circle == null) return;
      BlockState state = level.getBlockState(circle);
      if (state.is(ModBlocks.SUMMONING_CIRCLE.get()) && state.getValue(SummoningCircleBlock.LIT) != lit) {
         level.setBlock(circle, state.setValue(SummoningCircleBlock.LIT, lit), net.minecraft.world.level.block.Block.UPDATE_ALL);
      }
   }

   private static void spawnRitualGlow(ServerLevel level, BlockPos circle, long elapsed) {
      if ((elapsed & 1L) != 0L) return;
      double centerX = circle.getX() + 0.5;
      double centerY = circle.getY() + 0.12;
      double centerZ = circle.getZ() + 0.5;
      double radius = 1.12 + Math.sin(elapsed * 0.16) * 0.24;
      double rotation = elapsed * 0.11;
      for (int i = 0; i < 12; i++) {
         double angle = rotation + Math.PI * 2.0 * i / 12.0;
         level.sendParticles(RITUAL_RED, centerX + Math.cos(angle) * radius, centerY,
            centerZ + Math.sin(angle) * radius, 1, 0.015, 0.01, 0.015, 0.0);
      }
      level.sendParticles(RITUAL_DARK_RED, centerX, centerY + 0.03, centerZ, 8, 1.15, 0.025, 1.15, 0.0);
   }

   private static void spawnSummoningCompletionGlow(ServerLevel level, BlockPos circle) {
      double centerX = circle.getX() + 0.5;
      double centerY = circle.getY() + 0.3;
      double centerZ = circle.getZ() + 0.5;
      level.sendParticles(RITUAL_RED, centerX, centerY + 0.8, centerZ, 180, 1.25, 1.15, 1.25, 0.08);
      level.sendParticles(RITUAL_DARK_RED, centerX, centerY + 0.45, centerZ, 90, 1.5, 0.7, 1.5, 0.04);
      level.sendParticles(ParticleTypes.END_ROD, centerX, centerY + 0.85, centerZ, 36, 0.9, 1.0, 0.9, 0.035);
      level.sendParticles(ParticleTypes.FLASH, centerX, centerY + 1.0, centerZ, 2, 0.1, 0.15, 0.1, 0.0);
      for (int delay = 4; delay <= 60; delay += 4) {
         int scheduledDelay = delay;
         TYPE_MOON_WORLD.queueServerWork(scheduledDelay, () -> {
            level.sendParticles(RITUAL_RED, centerX, centerY + 0.8, centerZ, 22, 0.9, 1.0, 0.9, 0.025);
            level.sendParticles(ParticleTypes.END_ROD, centerX, centerY + 0.7, centerZ, 4, 0.65, 0.8, 0.65, 0.015);
         });
      }
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }
}

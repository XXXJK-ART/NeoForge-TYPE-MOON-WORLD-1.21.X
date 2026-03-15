package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class StructureProjectionBuildHandler {
   private static final int SET_BLOCK_FLAGS = 2;
   private static final int MAX_BLOCK_PLACEMENTS_PER_TICK = 128;
   private static final long PROJECTED_NO_DROP_TTL_TICKS = 18000L;
   private static final int NO_DROP_CLEANUP_INTERVAL_TICKS = 200;
   private static final Map<UUID, StructureProjectionBuildHandler.ActiveBuild> ACTIVE_BUILDS = new ConcurrentHashMap<>();
   private static final Set<GlobalPos> NO_DROP_PROJECTED_BLOCKS = ConcurrentHashMap.newKeySet();
   private static final Map<GlobalPos, Long> NO_DROP_EXPIRY_TICKS = new ConcurrentHashMap<>();
   private static volatile long lastNoDropCleanupTick = 0L;

   private StructureProjectionBuildHandler() {
   }

   public static boolean isPlayerBuildActive(UUID playerId) {
      return playerId == null ? false : ACTIVE_BUILDS.containsKey(playerId);
   }

   public static boolean startProjection(Player player, String structureId, BlockPos anchorPos, int rotationIndex) {
      return startProjectionInternal(player, structureId, anchorPos, rotationIndex, false, false, false);
   }

   public static boolean startProjectionFromGem(ServerPlayer player, String structureId, BlockPos anchorPos, int rotationIndex) {
      return startProjectionInternal(player, structureId, anchorPos, rotationIndex, true, true, true);
   }

   private static boolean startProjectionInternal(
      Player player, String structureId, BlockPos anchorPos, int rotationIndex, boolean bypassSelectionCheck, boolean freeManaCost, boolean silentTips
   ) {
      if (player instanceof ServerPlayer serverPlayer) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)serverPlayer.getData(
            TypeMoonWorldModVariables.PLAYER_VARIABLES
         );
         if (!bypassSelectionCheck && !isProjectionSelected(vars)) {
            return false;
         } else {
            String targetId = structureId != null && !structureId.isEmpty() ? structureId : vars.projection_selected_structure_id;
            TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = vars.getStructureById(targetId);
            if (structure == null) {
               return false;
            } else if (!TypeMoonWorldModVariables.PlayerVariables.isTrustedStructure(structure)) {
               if (!silentTips) {
                  serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.start_failed"), true);
               }

               return false;
            } else {
               Rotation rotation = rotationFromIndex(rotationIndex);
               double costMultiplier = freeManaCost ? 0.0 : 1.0;
               boolean swordAttributeActive = vars.player_magic_attributes_sword && !vars.isCurrentSelectionFromCrest("projection");
               List<StructureProjectionBuildHandler.LayerPlacement> layers = buildLayers(structure, anchorPos, rotation, swordAttributeActive, costMultiplier);
               if (layers.isEmpty()) {
                  return false;
               } else {
                  int[] rotatedSize = rotatedFootprint(structure.sizeX, structure.sizeZ, rotation);
                  BlockPos maxPos = anchorPos.offset(rotatedSize[0] - 1, structure.sizeY - 1, rotatedSize[1] - 1);
                  StructureProjectionBuildHandler.ActiveBuild build = new StructureProjectionBuildHandler.ActiveBuild(
                     serverPlayer.getUUID(),
                     serverPlayer.serverLevel().dimension(),
                     structure.name,
                     anchorPos,
                     maxPos,
                     layers,
                     !swordAttributeActive,
                     silentTips
                  );
                  ACTIVE_BUILDS.put(serverPlayer.getUUID(), build);
                  if (!silentTips) {
                     serverPlayer.displayClientMessage(
                        Component.translatable("message.typemoonworld.projection.structure.started", structure.name), true
                     );
                  }

                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      long currentTick = event.getServer().getTickCount();
      if (currentTick - lastNoDropCleanupTick >= 200L) {
         cleanupExpiredNoDropMarkers(currentTick);
         lastNoDropCleanupTick = currentTick;
      }

      if (!ACTIVE_BUILDS.isEmpty()) {
         Iterator<Entry<UUID, StructureProjectionBuildHandler.ActiveBuild>> iterator = ACTIVE_BUILDS.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<UUID, StructureProjectionBuildHandler.ActiveBuild> entry = iterator.next();
            StructureProjectionBuildHandler.ActiveBuild build = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(build.playerId);
            if (player != null && player.isAlive()) {
               if (!player.serverLevel().dimension().equals(build.dimension)) {
                  iterator.remove();
               } else {
                  tickBuild(player, build, iterator);
               }
            } else {
               iterator.remove();
            }
         }
      }
   }

   @SubscribeEvent
   public static void onBlockBreak(BreakEvent event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getLevel() instanceof ServerLevel serverLevel) {
            GlobalPos var3 = GlobalPos.of(serverLevel.dimension(), event.getPos());
            if (NO_DROP_PROJECTED_BLOCKS.remove(var3)) {
               NO_DROP_EXPIRY_TICKS.remove(var3);
               event.setCanceled(true);
               serverLevel.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 2);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      ACTIVE_BUILDS.remove(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getLevel() instanceof Level level) {
            NO_DROP_PROJECTED_BLOCKS.removeIf(pos -> pos.dimension().equals(level.dimension()));
            NO_DROP_EXPIRY_TICKS.entrySet().removeIf(entry -> entry.getKey().dimension().equals(level.dimension()));
            ACTIVE_BUILDS.entrySet().removeIf(entry -> entry.getValue().dimension.equals(level.dimension()));
         }
      }
   }

   private static void tickBuild(
      ServerPlayer player, StructureProjectionBuildHandler.ActiveBuild build, Iterator<Entry<UUID, StructureProjectionBuildHandler.ActiveBuild>> iterator
   ) {
      ServerLevel level = player.serverLevel();
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!build.areaCleared) {
         clearArea(level, build.minPos, build.maxPos);
         build.areaCleared = true;
         if (!build.silentTips) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.cleared"), true);
         }
      } else if (build.layerIndex >= build.layers.size()) {
         iterator.remove();
         if (!build.silentTips) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.completed"), true);
         }
      } else {
         StructureProjectionBuildHandler.LayerPlacement layer = build.layers.get(build.layerIndex);
         boolean changedMana = false;
         boolean placedAnyThisTick = false;
         int attempts = layer.blocks.size();
         int placementsBudget = 128;
         long currentTick = level.getServer() != null ? level.getServer().getTickCount() : level.getGameTime();

         while (attempts > 0 && !layer.blocks.isEmpty() && placementsBudget > 0) {
            attempts--;
            StructureProjectionBuildHandler.BlockPlacement placement = layer.blocks.pollFirst();
            if (placement == null) {
               break;
            }

            if (vars.player_mana < placement.cost) {
               layer.blocks.addFirst(placement);
               if (vars.player_mana > 0.0) {
                  vars.player_mana = 0.0;
                  changedMana = true;
               }

               if (changedMana) {
                  vars.syncMana(player);
               }

               if (!build.silentTips && level.getGameTime() - build.lastPauseTipTick >= 20L) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.paused_mana"), true);
                  build.lastPauseTipTick = level.getGameTime();
               }

               return;
            }

            if (!placement.state.canSurvive(level, placement.worldPos)) {
               layer.blocks.addLast(placement);
            } else {
               vars.player_mana = vars.player_mana - placement.cost;
               changedMana = true;
               placedAnyThisTick = true;
               placementsBudget--;
               level.setBlock(placement.worldPos, placement.state, 2);
               clearContainerContents(level, placement.worldPos);
               GlobalPos key = GlobalPos.of(level.dimension(), placement.worldPos);
               if (build.noDropsWhenBroken) {
                  NO_DROP_PROJECTED_BLOCKS.add(key);
                  NO_DROP_EXPIRY_TICKS.put(key, currentTick + 18000L);
               } else {
                  NO_DROP_PROJECTED_BLOCKS.remove(key);
                  NO_DROP_EXPIRY_TICKS.remove(key);
               }
            }
         }

         if (!layer.blocks.isEmpty() && !placedAnyThisTick) {
            StructureProjectionBuildHandler.BlockPlacement forced = layer.blocks.pollFirst();
            if (forced == null) {
               return;
            }

            if (vars.player_mana < forced.cost) {
               layer.blocks.addFirst(forced);
               if (vars.player_mana > 0.0) {
                  vars.player_mana = 0.0;
                  changedMana = true;
               }

               if (changedMana) {
                  vars.syncMana(player);
               }

               if (!build.silentTips && level.getGameTime() - build.lastPauseTipTick >= 20L) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.paused_mana"), true);
                  build.lastPauseTipTick = level.getGameTime();
               }

               return;
            }

            vars.player_mana = vars.player_mana - forced.cost;
            changedMana = true;
            level.setBlock(forced.worldPos, forced.state, 2);
            clearContainerContents(level, forced.worldPos);
            GlobalPos key = GlobalPos.of(level.dimension(), forced.worldPos);
            if (build.noDropsWhenBroken) {
               NO_DROP_PROJECTED_BLOCKS.add(key);
               NO_DROP_EXPIRY_TICKS.put(key, currentTick + 18000L);
            } else {
               NO_DROP_PROJECTED_BLOCKS.remove(key);
               NO_DROP_EXPIRY_TICKS.remove(key);
            }
         }

         if (changedMana) {
            vars.syncMana(player);
         }

         if (layer.blocks.isEmpty()) {
            build.layerIndex++;
         }
      }
   }

   private static void clearArea(ServerLevel level, BlockPos minPos, BlockPos maxPos) {
      for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
         for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
            for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
               BlockPos pos = new BlockPos(x, y, z);
               level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
               GlobalPos key = GlobalPos.of(level.dimension(), pos);
               NO_DROP_PROJECTED_BLOCKS.remove(key);
               NO_DROP_EXPIRY_TICKS.remove(key);
            }
         }
      }
   }

   private static Rotation rotationFromIndex(int index) {
      int normalized = (index % 4 + 4) % 4;

      return switch (normalized) {
         case 1 -> Rotation.CLOCKWISE_90;
         case 2 -> Rotation.CLOCKWISE_180;
         case 3 -> Rotation.COUNTERCLOCKWISE_90;
         default -> Rotation.NONE;
      };
   }

   private static int[] rotatedFootprint(int sizeX, int sizeZ, Rotation rotation) {
      return rotation != Rotation.CLOCKWISE_90 && rotation != Rotation.COUNTERCLOCKWISE_90 ? new int[]{sizeX, sizeZ} : new int[]{sizeZ, sizeX};
   }

   private static int[] rotateXZ(int x, int z, int sizeX, int sizeZ, Rotation rotation) {
      return switch (rotation) {
         case CLOCKWISE_90 -> new int[]{z, sizeX - 1 - x};
         case CLOCKWISE_180 -> new int[]{sizeX - 1 - x, sizeZ - 1 - z};
         case COUNTERCLOCKWISE_90 -> new int[]{sizeZ - 1 - z, x};
         default -> new int[]{x, z};
      };
   }

   private static List<StructureProjectionBuildHandler.LayerPlacement> buildLayers(
      TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure,
      BlockPos anchorPos,
      Rotation rotation,
      boolean hasSwordAttribute,
      double costMultiplier
   ) {
      Map<Integer, Deque<StructureProjectionBuildHandler.BlockPlacement>> byLayer = new ConcurrentHashMap<>();

      for (TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock savedBlock : structure.blocks) {
         ResourceLocation blockId = ResourceLocation.tryParse(savedBlock.blockId);
         if (blockId != null) {
            Block block = (Block)BuiltInRegistries.BLOCK.get(blockId);
            if (block != Blocks.AIR) {
               int[] rotated = rotateXZ(savedBlock.x, savedBlock.z, structure.sizeX, structure.sizeZ, rotation);
               BlockPos worldPos = anchorPos.offset(rotated[0], savedBlock.y, rotated[1]);
               BlockState state = block.defaultBlockState();
               state = applySavedStateProperties(state, savedBlock.blockStateProps);

               try {
                  state = state.rotate(rotation);
               } catch (Exception var18) {
               }

               Item item = block.asItem();
               if (item != Items.AIR) {
                  ItemStack costStack = item.getDefaultInstance();
                  costStack.setCount(1);
                  double cost = MagicStructuralAnalysis.calculateStructureCost(costStack, hasSwordAttribute) * Math.max(0.0, costMultiplier);
                  byLayer.computeIfAbsent(savedBlock.y, k -> new ArrayDeque<>())
                     .addLast(new StructureProjectionBuildHandler.BlockPlacement(worldPos, state, cost));
               }
            }
         }
      }

      List<Integer> sortedY = new ArrayList<>(byLayer.keySet());
      sortedY.sort(Integer::compareTo);
      List<StructureProjectionBuildHandler.LayerPlacement> layers = new ArrayList<>();

      for (Integer y : sortedY) {
         Deque<StructureProjectionBuildHandler.BlockPlacement> placements = byLayer.get(y);
         if (placements != null && !placements.isEmpty()) {
            layers.add(new StructureProjectionBuildHandler.LayerPlacement(y, placements));
         }
      }

      return layers;
   }

   private static boolean isProjectionSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.is_magus || !vars.is_magic_circuit_open) {
         return false;
      } else if (vars.selected_magics.isEmpty()) {
         return false;
      } else {
         int index = vars.current_magic_index;
         return index >= 0 && index < vars.selected_magics.size() ? "projection".equals(vars.selected_magics.get(index)) : false;
      }
   }

   private static void clearContainerContents(ServerLevel level, BlockPos pos) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof Container container) {
         for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, ItemStack.EMPTY);
         }

         blockEntity.setChanged();
      }
   }

   private static BlockState applySavedStateProperties(BlockState baseState, String rawProperties) {
      if (baseState != null && rawProperties != null && !rawProperties.isEmpty()) {
         BlockState result = baseState;
         String[] entries = rawProperties.split(",");

         for (String entry : entries) {
            if (entry != null && !entry.isEmpty()) {
               int eq = entry.indexOf(61);
               if (eq > 0 && eq < entry.length() - 1) {
                  String propName = entry.substring(0, eq).trim();
                  String valueName = entry.substring(eq + 1).trim();
                  if (!propName.isEmpty() && !valueName.isEmpty()) {
                     Property property = result.getBlock().getStateDefinition().getProperty(propName);
                     if (property != null) {
                        Optional value = property.getValue(valueName);
                        if (!value.isEmpty()) {
                           try {
                              result = (BlockState)result.setValue(property, (Comparable)value.get());
                           } catch (Exception var14) {
                           }
                        }
                     }
                  }
               }
            }
         }

         return result;
      } else {
         return baseState;
      }
   }

   private static void cleanupExpiredNoDropMarkers(long currentTick) {
      if (!NO_DROP_EXPIRY_TICKS.isEmpty()) {
         NO_DROP_EXPIRY_TICKS.entrySet().removeIf(entry -> {
            Long expiresAt = entry.getValue();
            if (expiresAt != null && expiresAt <= currentTick) {
               NO_DROP_PROJECTED_BLOCKS.remove(entry.getKey());
               return true;
            } else {
               return false;
            }
         });
      }
   }

   private static class ActiveBuild {
      private final UUID playerId;
      private final ResourceKey<Level> dimension;
      private final String structureName;
      private final BlockPos minPos;
      private final BlockPos maxPos;
      private final List<StructureProjectionBuildHandler.LayerPlacement> layers;
      private final boolean noDropsWhenBroken;
      private final boolean silentTips;
      private boolean areaCleared = false;
      private int layerIndex = 0;
      private long lastPauseTipTick = 0L;

      private ActiveBuild(
         UUID playerId,
         ResourceKey<Level> dimension,
         String structureName,
         BlockPos minPos,
         BlockPos maxPos,
         List<StructureProjectionBuildHandler.LayerPlacement> layers,
         boolean noDropsWhenBroken,
         boolean silentTips
      ) {
         this.playerId = playerId;
         this.dimension = dimension;
         this.structureName = structureName;
         this.minPos = minPos;
         this.maxPos = maxPos;
         this.layers = layers;
         this.noDropsWhenBroken = noDropsWhenBroken;
         this.silentTips = silentTips;
      }
   }

   private record BlockPlacement(BlockPos worldPos, BlockState state, double cost) {
   }

   private record LayerPlacement(int layerY, Deque<StructureProjectionBuildHandler.BlockPlacement> blocks) {
   }
}

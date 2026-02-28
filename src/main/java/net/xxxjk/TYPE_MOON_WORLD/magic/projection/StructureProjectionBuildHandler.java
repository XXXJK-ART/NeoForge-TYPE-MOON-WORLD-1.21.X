package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class StructureProjectionBuildHandler {
    private static final int SET_BLOCK_FLAGS = 2;
    private static final int MAX_BLOCK_PLACEMENTS_PER_TICK = 128;
    private static final Map<UUID, ActiveBuild> ACTIVE_BUILDS = new ConcurrentHashMap<>();
    private static final Set<GlobalPos> NO_DROP_PROJECTED_BLOCKS = ConcurrentHashMap.newKeySet();

    private StructureProjectionBuildHandler() {
    }

    public static boolean isPlayerBuildActive(UUID playerId) {
        if (playerId == null) return false;
        return ACTIVE_BUILDS.containsKey(playerId);
    }

    public static boolean startProjection(Player player, String structureId, BlockPos anchorPos, int rotationIndex) {
        return startProjectionInternal(player, structureId, anchorPos, rotationIndex, false, false, false);
    }

    public static boolean startProjectionFromGem(ServerPlayer player, String structureId, BlockPos anchorPos, int rotationIndex) {
        return startProjectionInternal(player, structureId, anchorPos, rotationIndex, true, true, true);
    }

    private static boolean startProjectionInternal(Player player, String structureId, BlockPos anchorPos, int rotationIndex, boolean bypassSelectionCheck, boolean freeManaCost, boolean silentTips) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!bypassSelectionCheck && !isProjectionSelected(vars)) {
            return false;
        }

        String targetId = structureId == null || structureId.isEmpty() ? vars.projection_selected_structure_id : structureId;
        TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = vars.getStructureById(targetId);
        if (structure == null) {
            return false;
        }
        if (!TypeMoonWorldModVariables.PlayerVariables.isTrustedStructure(structure)) {
            if (!silentTips) {
                serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.start_failed"), true);
            }
            return false;
        }

        Rotation rotation = rotationFromIndex(rotationIndex);
        double costMultiplier = freeManaCost ? 0.0 : 1.0;
        List<LayerPlacement> layers = buildLayers(structure, anchorPos, rotation, vars.player_magic_attributes_sword, costMultiplier);
        if (layers.isEmpty()) {
            return false;
        }

        int[] rotatedSize = rotatedFootprint(structure.sizeX, structure.sizeZ, rotation);
        BlockPos minPos = anchorPos;
        BlockPos maxPos = anchorPos.offset(rotatedSize[0] - 1, structure.sizeY - 1, rotatedSize[1] - 1);

        ActiveBuild build = new ActiveBuild(
                serverPlayer.getUUID(),
                serverPlayer.serverLevel().dimension(),
                structure.name,
                minPos,
                maxPos,
                layers,
                !vars.player_magic_attributes_sword,
                silentTips
        );
        ACTIVE_BUILDS.put(serverPlayer.getUUID(), build);

        if (!silentTips) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.started", structure.name), true);
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_BUILDS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, ActiveBuild>> iterator = ACTIVE_BUILDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveBuild> entry = iterator.next();
            ActiveBuild build = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(build.playerId);
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            if (!player.serverLevel().dimension().equals(build.dimension)) {
                iterator.remove();
                continue;
            }

            tickBuild(player, build, iterator);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        GlobalPos key = GlobalPos.of(serverLevel.dimension(), event.getPos());
        if (!NO_DROP_PROJECTED_BLOCKS.remove(key)) {
            return;
        }

        event.setCanceled(true);
        serverLevel.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), SET_BLOCK_FLAGS);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_BUILDS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof Level level)) return;

        NO_DROP_PROJECTED_BLOCKS.removeIf(pos -> pos.dimension().equals(level.dimension()));
        ACTIVE_BUILDS.entrySet().removeIf(entry -> entry.getValue().dimension.equals(level.dimension()));
    }

    private static void tickBuild(ServerPlayer player, ActiveBuild build, Iterator<Map.Entry<UUID, ActiveBuild>> iterator) {
        ServerLevel level = player.serverLevel();
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        if (!build.areaCleared) {
            clearArea(level, build.minPos, build.maxPos);
            build.areaCleared = true;
            if (!build.silentTips) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.cleared"), true);
            }
            return;
        }

        if (build.layerIndex >= build.layers.size()) {
            iterator.remove();
            if (!build.silentTips) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.completed"), true);
            }
            return;
        }

        LayerPlacement layer = build.layers.get(build.layerIndex);
        boolean changedMana = false;
        boolean placedAnyThisTick = false;
        int attempts = layer.blocks.size();
        int placementsBudget = MAX_BLOCK_PLACEMENTS_PER_TICK;

        while (attempts > 0 && !layer.blocks.isEmpty() && placementsBudget > 0) {
            attempts--;
            BlockPlacement placement = layer.blocks.remove(0);
            if (vars.player_mana < placement.cost) {
                layer.blocks.add(0, placement);
                if (vars.player_mana > 0) {
                    vars.player_mana = 0;
                    changedMana = true;
                }
                if (changedMana) {
                    vars.syncMana(player);
                }
                if (!build.silentTips && level.getGameTime() - build.lastPauseTipTick >= 20) {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.paused_mana"), true);
                    build.lastPauseTipTick = level.getGameTime();
                }
                return;
            }

            if (!placement.state.canSurvive(level, placement.worldPos)) {
                // Some blocks (e.g., redstone torch) depend on neighbors.
                // Defer them to the end and try again after supports are placed.
                layer.blocks.add(placement);
                continue;
            }

            vars.player_mana -= placement.cost;
            changedMana = true;
            placedAnyThisTick = true;
            placementsBudget--;
            level.setBlock(placement.worldPos, placement.state, SET_BLOCK_FLAGS);
            clearContainerContents(level, placement.worldPos);

            GlobalPos key = GlobalPos.of(level.dimension(), placement.worldPos);
            if (build.noDropsWhenBroken) {
                NO_DROP_PROJECTED_BLOCKS.add(key);
            } else {
                NO_DROP_PROJECTED_BLOCKS.remove(key);
            }
        }

        if (!layer.blocks.isEmpty() && !placedAnyThisTick) {
            // Prevent deadlock when all remaining blocks report canSurvive=false.
            BlockPlacement forced = layer.blocks.remove(0);
            if (vars.player_mana < forced.cost) {
                layer.blocks.add(0, forced);
                if (vars.player_mana > 0) {
                    vars.player_mana = 0;
                    changedMana = true;
                }
                if (changedMana) {
                    vars.syncMana(player);
                }
                if (!build.silentTips && level.getGameTime() - build.lastPauseTipTick >= 20) {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.paused_mana"), true);
                    build.lastPauseTipTick = level.getGameTime();
                }
                return;
            }

            vars.player_mana -= forced.cost;
            changedMana = true;
            level.setBlock(forced.worldPos, forced.state, SET_BLOCK_FLAGS);
            clearContainerContents(level, forced.worldPos);
            GlobalPos key = GlobalPos.of(level.dimension(), forced.worldPos);
            if (build.noDropsWhenBroken) {
                NO_DROP_PROJECTED_BLOCKS.add(key);
            } else {
                NO_DROP_PROJECTED_BLOCKS.remove(key);
            }
        }

        if (changedMana) {
            vars.syncMana(player);
        }

        if (layer.blocks.isEmpty()) {
            build.layerIndex++;
        }
    }

    private static void clearArea(ServerLevel level, BlockPos minPos, BlockPos maxPos) {
        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_BLOCK_FLAGS);
                    NO_DROP_PROJECTED_BLOCKS.remove(GlobalPos.of(level.dimension(), pos));
                }
            }
        }
    }

    private static Rotation rotationFromIndex(int index) {
        int normalized = ((index % 4) + 4) % 4;
        return switch (normalized) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static int[] rotatedFootprint(int sizeX, int sizeZ, Rotation rotation) {
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            return new int[]{sizeZ, sizeX};
        }
        return new int[]{sizeX, sizeZ};
    }

    private static int[] rotateXZ(int x, int z, int sizeX, int sizeZ, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new int[]{z, sizeX - 1 - x};
            case CLOCKWISE_180 -> new int[]{sizeX - 1 - x, sizeZ - 1 - z};
            case COUNTERCLOCKWISE_90 -> new int[]{sizeZ - 1 - z, x};
            default -> new int[]{x, z};
        };
    }

    private static List<LayerPlacement> buildLayers(TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure, BlockPos anchorPos, Rotation rotation, boolean hasSwordAttribute, double costMultiplier) {
        Map<Integer, List<BlockPlacement>> byLayer = new ConcurrentHashMap<>();

        for (TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock savedBlock : structure.blocks) {
            ResourceLocation blockId = ResourceLocation.tryParse(savedBlock.blockId);
            if (blockId == null) continue;

            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (block == Blocks.AIR) continue;

            int[] rotated = rotateXZ(savedBlock.x, savedBlock.z, structure.sizeX, structure.sizeZ, rotation);
            BlockPos worldPos = anchorPos.offset(rotated[0], savedBlock.y, rotated[1]);

            BlockState state = block.defaultBlockState();
            state = applySavedStateProperties(state, savedBlock.blockStateProps);
            try {
                state = state.rotate(rotation);
            } catch (Exception ignored) {
            }

            Item item = block.asItem();
            if (item == Items.AIR) continue;
            ItemStack costStack = item.getDefaultInstance();
            costStack.setCount(1);
            double cost = MagicStructuralAnalysis.calculateStructureCost(costStack, hasSwordAttribute) * Math.max(0.0, costMultiplier);

            byLayer.computeIfAbsent(savedBlock.y, k -> new ArrayList<>()).add(new BlockPlacement(worldPos, state, cost));
        }

        List<Integer> sortedY = new ArrayList<>(byLayer.keySet());
        sortedY.sort(Integer::compareTo);

        List<LayerPlacement> layers = new ArrayList<>();
        for (Integer y : sortedY) {
            List<BlockPlacement> placements = byLayer.get(y);
            if (placements == null || placements.isEmpty()) continue;
            layers.add(new LayerPlacement(y, placements));
        }
        return layers;
    }

    private static boolean isProjectionSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (!vars.is_magus || !vars.is_magic_circuit_open) return false;
        if (vars.selected_magics.isEmpty()) return false;
        int index = vars.current_magic_index;
        if (index < 0 || index >= vars.selected_magics.size()) return false;
        return "projection".equals(vars.selected_magics.get(index));
    }

    private static void clearContainerContents(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            return;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, ItemStack.EMPTY);
        }
        blockEntity.setChanged();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applySavedStateProperties(BlockState baseState, String rawProperties) {
        if (baseState == null || rawProperties == null || rawProperties.isEmpty()) {
            return baseState;
        }

        BlockState result = baseState;
        String[] entries = rawProperties.split(",");
        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) continue;
            int eq = entry.indexOf('=');
            if (eq <= 0 || eq >= entry.length() - 1) continue;

            String propName = entry.substring(0, eq).trim();
            String valueName = entry.substring(eq + 1).trim();
            if (propName.isEmpty() || valueName.isEmpty()) continue;

            Property property = result.getBlock().getStateDefinition().getProperty(propName);
            if (property == null) continue;
            java.util.Optional value = property.getValue(valueName);
            if (value.isEmpty()) continue;
            try {
                result = result.setValue(property, (Comparable) value.get());
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private record BlockPlacement(BlockPos worldPos, BlockState state, double cost) {
    }

    private record LayerPlacement(int layerY, List<BlockPlacement> blocks) {
    }

    private static class ActiveBuild {
        private final UUID playerId;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final String structureName;
        private final BlockPos minPos;
        private final BlockPos maxPos;
        private final List<LayerPlacement> layers;
        private final boolean noDropsWhenBroken;
        private final boolean silentTips;

        private boolean areaCleared = false;
        private int layerIndex = 0;
        private long lastPauseTipTick = 0;

        private ActiveBuild(UUID playerId, net.minecraft.resources.ResourceKey<Level> dimension, String structureName, BlockPos minPos, BlockPos maxPos, List<LayerPlacement> layers, boolean noDropsWhenBroken, boolean silentTips) {
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
}

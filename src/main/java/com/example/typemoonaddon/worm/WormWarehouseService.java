package com.example.typemoonaddon.worm;

import com.example.typemoonaddon.block.entity.WormWarehouseBlockEntity;
import com.example.typemoonaddon.registry.AddonBlocks;
import com.example.typemoonaddon.registry.AddonItems;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;

public final class WormWarehouseService {
    public static final int MIN_INTERIOR_SIDE = 3;
    public static final int MAX_INTERIOR_SIDE = 20;
    public static final int MIN_FRAME_SIDE = MIN_INTERIOR_SIDE + 2;
    public static final int MAX_FRAME_SIDE = MAX_INTERIOR_SIDE + 2;
    public static final int MAX_SLOTS = MAX_INTERIOR_SIDE * MAX_INTERIOR_SIDE;
    public static final int PAGE_SIZE = 54;
    private static final int FEED_INTERVAL_TICKS = 20 * 1000;
    private static final int MAX_CONNECTED_BLOCKS = MAX_SLOTS;
    private static final Set<GlobalPos> DESTROYING = new HashSet<>();

    private WormWarehouseService() {
    }

    public static boolean activateFromFrame(Level level, BlockPos clickedPos, @Nullable ServerPlayer player, ItemStack wormStack) {
        if (!(level instanceof ServerLevel serverLevel) || player == null || !wormStack.is(AddonItems.WORM.get())) {
            return false;
        }
        Bounds bounds = scanFrameBounds(serverLevel, clickedPos);
        if (bounds == null || bounds.width() < MIN_FRAME_SIDE || bounds.height() < MIN_FRAME_SIDE) {
            return false;
        }
        BlockPos controllerPos = bounds.controller();
        if (serverLevel.getBlockEntity(controllerPos) instanceof WormWarehouseBlockEntity) {
            return false;
        }
        placeWarehouse(serverLevel, bounds);
        return true;
    }

    @Nullable
    public static BlockPos resolveController(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        Bounds bounds = findBounds(serverLevel, pos);
        if (bounds == null) {
            return null;
        }
        return bounds.controller();
    }

    public static void destroyWarehouse(ServerLevel level, BlockPos anyPos) {
        Bounds bounds = findBounds(level, anyPos);
        if (bounds == null) {
            return;
        }
        destroyWarehouse(level, bounds);
    }

    public static void destroyWarehouse(ServerLevel level, Bounds bounds) {
        GlobalPos key = GlobalPos.of(level.dimension(), bounds.controller());
        if (!DESTROYING.add(key)) {
            return;
        }
        try {
            if (level.getBlockEntity(bounds.controller()) instanceof WormWarehouseBlockEntity controller) {
                controller.dropContents(level);
            }
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                if (x == bounds.minX() || x == bounds.maxX() || z == bounds.minZ() || z == bounds.maxZ()) {
                    continue;
                }
                BlockPos current = new BlockPos(x, bounds.y(), z);
                if (level.getBlockState(current).is(AddonBlocks.WORM_WAREHOUSE.get())) {
                    level.removeBlock(current, false);
                }
            }
        }
        } finally {
            DESTROYING.remove(key);
        }
    }

    public static void tick(ServerLevel level, BlockPos controllerPos, WormWarehouseBlockEntity blockEntity) {
        Bounds bounds = blockEntity.getBounds();
        if (bounds == null || !isIntact(level, bounds)) {
            if (bounds != null) {
                destroyWarehouse(level, bounds);
            } else {
                blockEntity.dropContents(level);
                level.removeBlock(controllerPos, false);
            }
            return;
        }
        blockEntity.setCapacity(bounds.capacity());
        if (level.getGameTime() % Math.max(20L, FEED_INTERVAL_TICKS / Math.max(1, blockEntity.getCapacity())) != 0) {
            return;
        }
        blockEntity.consumeGrowth(level, controllerPos, bounds.capacity());
    }

    public static Optional<WormWarehouseBlockEntity> controllerEntity(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return findController(serverLevel, pos);
    }

    private static void placeWarehouse(ServerLevel level, Bounds bounds) {
        BlockPos controller = bounds.controller();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                if (x == bounds.minX() || x == bounds.maxX() || z == bounds.minZ() || z == bounds.maxZ()) {
                    continue;
                }
                BlockPos current = new BlockPos(x, bounds.y(), z);
                boolean controllerHere = current.equals(controller);
                level.setBlock(current, AddonBlocks.WORM_WAREHOUSE.get().defaultBlockState()
                        .setValue(com.example.typemoonaddon.block.WormWarehouseBlock.CONTROLLER, controllerHere),
                        3);
            }
        }
        if (level.getBlockEntity(controller) instanceof WormWarehouseBlockEntity be) {
            be.setBounds(bounds.minX(), bounds.maxX(), bounds.minZ(), bounds.maxZ());
            be.setCapacity(bounds.capacity());
            be.setChanged();
        }
    }

    @Nullable
    private static Bounds findBounds(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(AddonBlocks.WORM_WAREHOUSE.get())) {
            return findController(level, pos)
                    .map(WormWarehouseBlockEntity::getBounds)
                    .filter(bounds -> bounds.contains(pos))
                    .orElse(null);
        }
        if (!level.getBlockState(pos).is(Blocks.DEEPSLATE_BRICKS)) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                Optional<WormWarehouseBlockEntity> controller = findController(level, pos.relative(direction));
                if (controller.isPresent()) {
                    return controller.get().getBounds();
                }
            }
            return null;
        }
        return scanFrameBounds(level, pos);
    }

    @Nullable
    private static Bounds scanFrameBounds(ServerLevel level, BlockPos pos) {
        int y = pos.getY();
        int minX = pos.getX();
        while (level.getBlockState(new BlockPos(minX - 1, y, pos.getZ())).is(Blocks.DEEPSLATE_BRICKS)) {
            minX--;
            if (pos.getX() - minX > MAX_FRAME_SIDE) {
                return null;
            }
        }
        int maxX = pos.getX();
        while (level.getBlockState(new BlockPos(maxX + 1, y, pos.getZ())).is(Blocks.DEEPSLATE_BRICKS)) {
            maxX++;
            if (maxX - pos.getX() > MAX_FRAME_SIDE) {
                return null;
            }
        }
        int minZ = pos.getZ();
        while (level.getBlockState(new BlockPos(pos.getX(), y, minZ - 1)).is(Blocks.DEEPSLATE_BRICKS)) {
            minZ--;
            if (pos.getZ() - minZ > MAX_FRAME_SIDE) {
                return null;
            }
        }
        int maxZ = pos.getZ();
        while (level.getBlockState(new BlockPos(pos.getX(), y, maxZ + 1)).is(Blocks.DEEPSLATE_BRICKS)) {
            maxZ++;
            if (maxZ - pos.getZ() > MAX_FRAME_SIDE) {
                return null;
            }
        }
        if (maxX - minX + 1 < MIN_FRAME_SIDE || maxX - minX + 1 > MAX_FRAME_SIDE
                || maxZ - minZ + 1 < MIN_FRAME_SIDE || maxZ - minZ + 1 > MAX_FRAME_SIDE) {
            return null;
        }
        for (int x = minX; x <= maxX; x++) {
            if (!level.getBlockState(new BlockPos(x, y, minZ)).is(Blocks.DEEPSLATE_BRICKS)
                    || !level.getBlockState(new BlockPos(x, y, maxZ)).is(Blocks.DEEPSLATE_BRICKS)) {
                return null;
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            if (!level.getBlockState(new BlockPos(minX, y, z)).is(Blocks.DEEPSLATE_BRICKS)
                    || !level.getBlockState(new BlockPos(maxX, y, z)).is(Blocks.DEEPSLATE_BRICKS)) {
                return null;
            }
        }
        int interiorWidth = maxX - minX - 1;
        int interiorHeight = maxZ - minZ - 1;
        if (interiorWidth < MIN_INTERIOR_SIDE || interiorWidth > MAX_INTERIOR_SIDE
                || interiorHeight < MIN_INTERIOR_SIDE || interiorHeight > MAX_INTERIOR_SIDE) {
            return null;
        }
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                BlockPos interior = new BlockPos(x, y, z);
                BlockPos below = new BlockPos(x, y - 1, z);
                if (!level.getBlockState(interior).isAir()
                        || !level.getBlockState(below).is(Blocks.DEEPSLATE_BRICKS)) {
                    return null;
                }
            }
        }
        return new Bounds(y, minX, maxX, minZ, maxZ);
    }

    private static Optional<WormWarehouseBlockEntity> findController(ServerLevel level, BlockPos origin) {
        if (!level.getBlockState(origin).is(AddonBlocks.WORM_WAREHOUSE.get())) {
            return Optional.empty();
        }
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(origin);
        while (!pending.isEmpty() && visited.size() < MAX_CONNECTED_BLOCKS) {
            BlockPos current = pending.removeFirst();
            if (!visited.add(current) || !level.getBlockState(current).is(AddonBlocks.WORM_WAREHOUSE.get())) {
                continue;
            }
            if (level.getBlockEntity(current) instanceof WormWarehouseBlockEntity controller) {
                return Optional.of(controller);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                pending.addLast(current.relative(direction));
            }
        }
        return Optional.empty();
    }

    private static boolean isIntact(ServerLevel level, Bounds bounds) {
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            if (!level.getBlockState(new BlockPos(x, bounds.y(), bounds.minZ())).is(Blocks.DEEPSLATE_BRICKS)
                    || !level.getBlockState(new BlockPos(x, bounds.y(), bounds.maxZ())).is(Blocks.DEEPSLATE_BRICKS)) {
                return false;
            }
        }
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            if (!level.getBlockState(new BlockPos(bounds.minX(), bounds.y(), z)).is(Blocks.DEEPSLATE_BRICKS)
                    || !level.getBlockState(new BlockPos(bounds.maxX(), bounds.y(), z)).is(Blocks.DEEPSLATE_BRICKS)) {
                return false;
            }
        }
        for (int x = bounds.minX() + 1; x < bounds.maxX(); x++) {
            for (int z = bounds.minZ() + 1; z < bounds.maxZ(); z++) {
                BlockPos interior = new BlockPos(x, bounds.y(), z);
                if (!level.getBlockState(interior).is(AddonBlocks.WORM_WAREHOUSE.get())
                        || !level.getBlockState(interior.below()).is(Blocks.DEEPSLATE_BRICKS)) {
                    return false;
                }
            }
        }
        return level.getBlockState(bounds.controller()).getValue(com.example.typemoonaddon.block.WormWarehouseBlock.CONTROLLER);
    }

    public record Bounds(int y, int minX, int maxX, int minZ, int maxZ) {
        public BlockPos controller() {
            return new BlockPos(minX + (maxX - minX) / 2, y, minZ + (maxZ - minZ) / 2);
        }

        public int width() {
            return maxX - minX + 1;
        }

        public int height() {
            return maxZ - minZ + 1;
        }

        public int capacity() {
            return Math.min(MAX_SLOTS, Math.max(1, (width() - 2) * (height() - 2)));
        }

        public boolean contains(BlockPos pos) {
            return pos.getY() == y
                    && pos.getX() >= minX + 1
                    && pos.getX() < maxX
                    && pos.getZ() >= minZ + 1
                    && pos.getZ() < maxZ;
        }
    }
}

package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagicEmeraldWinterRiver {
    public static void execute(Entity entity) {
        if (entity == null)
            return;

        if (entity instanceof Player player) {
            ItemStack requiredItem = new ItemStack(ModItems.CARVED_EMERALD_FULL.get());
            int requiredCount = 2;
            int count = 0;

            // Count items
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (stack.getItem() == requiredItem.getItem()) {
                    count += stack.getCount();
                }
            }
            if (player.getOffhandItem().getItem() == requiredItem.getItem()) {
                count += player.getOffhandItem().getCount();
            }

            if (count >= requiredCount) {
                // Consume items
                int toRemove = requiredCount;
                for (int i = 0; i < player.getInventory().items.size(); i++) {
                    if (toRemove <= 0) break;
                    ItemStack stack = player.getInventory().items.get(i);
                    if (stack.getItem() == requiredItem.getItem()) {
                        int remove = Math.min(toRemove, stack.getCount());
                        stack.shrink(remove);
                        toRemove -= remove;
                        if (stack.isEmpty()) {
                            player.getInventory().removeItem(stack);
                        }
                    }
                }
                if (toRemove > 0) {
                    ItemStack stack = player.getOffhandItem();
                    if (stack.getItem() == requiredItem.getItem()) {
                        int remove = Math.min(toRemove, stack.getCount());
                        stack.shrink(remove);
                    }
                }

                // Execute Winter River (Emerald)
                Level level = player.level();
                if (!level.isClientSide) {
                    BlockPos center = player.blockPosition().above(); // Center roughly on player body
                    
                    // Buckets for scheduling restoration: Map<Delay, List<BlockPos>>
                    Map<Integer, List<BlockPos>> restoreBuckets = new HashMap<>();
                    RandomSource random = level.getRandom();
                    
                    // Particles
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX(), center.getY(), center.getZ(), 50, 2, 2, 2, 0.1);
                    }

                    // 5x5x5 Hollow Sphere
                    // Diameter 5 -> Radius 2 (with center 0,0,0: -2, -1, 0, 1, 2)
                    int radius = 2;
                    
                    for (int x = -radius; x <= radius; x++) {
                        for (int y = -radius; y <= radius; y++) {
                            for (int z = -radius; z <= radius; z++) {
                                // Sphere check: x^2 + y^2 + z^2 <= r^2
                                // To make it "center hollow", we can exclude the very center or a small radius
                                // "中心镂空" usually means removing the core.
                                // Let's exclude radius < 1? i.e. center block (0,0,0) and maybe adjacents if they are too close.
                                // But 5x5x5 is small. 
                                // Distance check:
                                double distSq = x * x + y * y + z * z;
                                
                                if (distSq <= radius * radius) {
                                    // Hollow check: exclude if distSq is small (e.g. center block)
                                    // distance 0 is center. distance 1 is adjacent.
                                    // Let's exclude distSq < 1.0 (only center block)
                                    // Or distSq < 2.0 (center + 6 neighbors)?
                                    // "Hollow Sphere" usually implies a shell.
                                    // If we want a shell of radius 2, we keep distSq > (r-1)^2 ?
                                    // If r=2, (r-1)=1. So distSq > 1.
                                    // This would keep only the outer layer.
                                    // Let's try to keep blocks where distSq > 1.5?
                                    // Center block is 0. Adjacents are 1. Corners of 3x3 are 2 or 3.
                                    // Let's just exclude the exact center block (0,0,0) as a safe "hollow".
                                    // But the user said "Center Hollow" (中心镂空).
                                    // Let's exclude distSq < 2. This excludes the cross at the center.
                                    
                                    if (distSq > 1.5) { 
                                        BlockPos pos = center.offset(x, y, z);
                                        // Force replace any breakable block
                                        if (level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                                            if (level.setBlock(pos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get().defaultBlockState(), 2)) {
                                                // Add to restore bucket
                                                int delay = 160 + random.nextInt(41); // 160-200 ticks
                                                restoreBuckets.computeIfAbsent(delay, k -> new ArrayList<>()).add(pos);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Schedule Revert Tasks
                    for (Map.Entry<Integer, List<BlockPos>> entry : restoreBuckets.entrySet()) {
                        int delay = entry.getKey();
                        List<BlockPos> posList = entry.getValue();
                        
                        TYPE_MOON_WORLD.queueServerWork(delay, () -> {
                            for (BlockPos pos : posList) {
                                BlockState state = level.getBlockState(pos);
                                if (state.is(ModBlocks.GREEN_TRANSPARENT_BLOCK.get())) {
                                    level.levelEvent(2001, pos, Block.getId(state));
                                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                                }
                            }
                        });
                    }
                }
                
            } else {
                player.displayClientMessage(Component.literal("需要2个绿宝石"), true);
            }
        }
    }
}

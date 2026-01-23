package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

import java.util.ArrayList;
import java.util.List;

public class MagicEmeraldUse {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        if (entity instanceof Player player) {
            ItemStack requiredItem = new ItemStack(ModItems.CARVED_EMERALD_FULL.get());
            boolean hasItem = false;
            
            // Check main inventory
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (stack.getItem() == requiredItem.getItem()) {
                    hasItem = true;
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                         player.getInventory().removeItem(stack);
                    }
                    break;
                }
            }
            
            if (!hasItem) {
                // Check offhand
                 ItemStack stack = player.getOffhandItem();
                 if (stack.getItem() == requiredItem.getItem()) {
                     hasItem = true;
                     stack.shrink(1);
                 }
            }

            if (hasItem) {
                Level level = player.level();
                if (!level.isClientSide) {
                    // Use 8-direction logic for more precise placement
                    float yaw = player.getYRot();
                    // Normalize yaw to 0-360
                    float normalizedYaw = (yaw % 360 + 360) % 360;
                    // 0=South, 90=West, 180=North, 270=East
                    // Octant 0: South (337.5 - 22.5)
                    // Octant 1: South-West (22.5 - 67.5)
                    // Octant 2: West (67.5 - 112.5)
                    // ...
                    int octant = Math.round(normalizedYaw / 45f) % 8;

                    int dx = 0;
                    int dz = 0;
                    int rx = 0; // Right vector X
                    int rz = 0; // Right vector Z
                    
                    // Distance scaling for diagonals (optional, but 4 blocks diagonal is further than 4 blocks cardinal)
                    // Let's use roughly same distance count
                    int dist = MagicConstants.EMERALD_WALL_DISTANCE;
                    if (octant % 2 != 0) {
                        dist = (int) Math.round(dist * 0.75); // Slightly reduce step count for diagonals to match Euclidian distance roughly
                        if (dist < 1) dist = 1;
                    }

                    switch (octant) {
                        case 0: // South (+Z)
                            dx = 0; dz = 1;
                            rx = -1; rz = 0; // Right is West (-X)
                            break;
                        case 1: // South-West (-X, +Z)
                            dx = -1; dz = 1;
                            rx = -1; rz = -1; // Right is North-West (-X, -Z)
                            break;
                        case 2: // West (-X)
                            dx = -1; dz = 0;
                            rx = 0; rz = -1; // Right is North (-Z)
                            break;
                        case 3: // North-West (-X, -Z)
                            dx = -1; dz = -1;
                            rx = 1; rz = -1; // Right is North-East (+X, -Z)
                            break;
                        case 4: // North (-Z)
                            dx = 0; dz = -1;
                            rx = 1; rz = 0; // Right is East (+X)
                            break;
                        case 5: // North-East (+X, -Z)
                            dx = 1; dz = -1;
                            rx = 1; rz = 1; // Right is South-East (+X, +Z)
                            break;
                        case 6: // East (+X)
                            dx = 1; dz = 0;
                            rx = 0; rz = 1; // Right is South (+Z)
                            break;
                        case 7: // South-East (+X, +Z)
                            dx = 1; dz = 1;
                            rx = -1; rz = 1; // Right is South-West (-X, +Z)
                            break;
                    }

                    BlockPos playerPos = player.blockPosition();
                    // Center of the wall
                    BlockPos startPos = playerPos.offset(dx * dist, 0, dz * dist);
                    
                    Direction up = Direction.UP;
                    
                    List<BlockPos> placedBlocks = new ArrayList<>();
                    RandomSource random = level.getRandom();
                    
                    for (int w = -1; w <= 1; w++) {
                        for (int h = 0; h < 3; h++) {
                            // Calculate position based on "Right" vector
                            BlockPos targetPos = startPos.offset(rx * w, 0, rz * w).relative(up, h);
                            
                            if (level.getBlockState(targetPos).canBeReplaced()) {
                                if (level.setBlock(targetPos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get().defaultBlockState(), 3)) {
                                    placedBlocks.add(targetPos);
                                    
                                    // Particles
                                    if (level instanceof ServerLevel serverLevel) {
                                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 5, 0.5, 0.5, 0.5, 0.1);
                                    }
                                }
                            }
                        }
                    }

                    // Schedule Cleanup
                    if (!placedBlocks.isEmpty()) {
                        // Randomize duration slightly? User said 10-8s.
                        int duration = MagicConstants.EMERALD_WALL_DURATION;
                        
                        // We can schedule individual block removal or batch. 
                        // To support "each block 10-8s after break", we should probably iterate and schedule per block or just one task that handles all with slight variations?
                        // queueServerWork is per tick.
                        
                        for (BlockPos pos : placedBlocks) {
                            int delay = duration + random.nextInt(40); // 8s + 0-2s = 8-10s
                            TYPE_MOON_WORLD.queueServerWork(delay, () -> {
                                if (level.getBlockState(pos).is(ModBlocks.GREEN_TRANSPARENT_BLOCK.get())) {
                                    level.destroyBlock(pos, false);
                                }
                            });
                        }
                    }
                }
            } else {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_EMERALD_USE_NEED_GEM), true);
            }
        }
    }
}

package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
                    Direction facing = player.getDirection();
                    BlockPos playerPos = player.blockPosition();
                    BlockPos startPos = playerPos.relative(facing, MagicConstants.EMERALD_WALL_DISTANCE);
                    
                    Direction right = facing.getClockWise();
                    Direction up = Direction.UP;
                    
                    List<BlockPos> placedBlocks = new ArrayList<>();
                    RandomSource random = level.getRandom();
                    
                    // Create Wall (centered on startPos)
                    // Width 3 (-1 to 1), Height 3 (0 to 2)
                    for (int w = -1; w <= 1; w++) {
                        for (int h = 0; h < 3; h++) {
                            BlockPos targetPos = startPos.relative(right, w).relative(up, h);
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

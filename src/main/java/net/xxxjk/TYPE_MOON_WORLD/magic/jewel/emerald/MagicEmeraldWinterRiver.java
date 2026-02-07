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
                    RandomSource random = level.getRandom();
                    
                    // Particles
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX(), center.getY(), center.getZ(), 50, 2, 2, 2, 0.1);
                    }

                    // 5x5x5 Hollow Sphere
                    int radius = 2;
                    
                    for (int x = -radius; x <= radius; x++) {
                        for (int y = -radius; y <= radius; y++) {
                            for (int z = -radius; z <= radius; z++) {
                                double distSq = x * x + y * y + z * z;
                                
                                if (distSq <= radius * radius) {
                                    // Hollow check: exclude distSq < 2 (Center)
                                    if (distSq > 1.5) { 
                                        BlockPos pos = center.offset(x, y, z);
                                        BlockState currentState = level.getBlockState(pos);
                                        
                                        // Place block if replaceable or refresh if it's already our block
                                        if (currentState.getDestroySpeed(level, pos) >= 0 || currentState.is(ModBlocks.GREEN_TRANSPARENT_BLOCK.get())) {
                                            if (level.setBlock(pos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get().defaultBlockState(), 3)) {
                                                // onPlace will handle scheduling
                                            } else if (currentState.is(ModBlocks.GREEN_TRANSPARENT_BLOCK.get())) {
                                                // Force refresh timer if block was already there and didn't change
                                                int delay = 160 + random.nextInt(41);
                                                level.scheduleTick(pos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get(), delay);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
            } else {
                player.displayClientMessage(Component.literal("需要2个绿宝石"), true);
            }
        }
    }
}

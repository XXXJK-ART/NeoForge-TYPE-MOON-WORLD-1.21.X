package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagicSapphireWinterFrost {
    public static void execute(Entity entity) {
        if (entity == null)
            return;

        if (entity instanceof Player player) {
            ItemStack requiredItem = new ItemStack(ModItems.CARVED_SAPPHIRE_FULL.get());
            int requiredCount = 3;
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

                // Execute Winter Frost
                Level level = player.level();
                if (!level.isClientSide) {
                    BlockPos center = player.blockPosition();
                    int radius = 10;
                    
                    // Buckets for scheduling restoration: Map<Delay, List<BlockInfo>>
                    Map<Integer, List<RestoreData>> restoreBuckets = new HashMap<>();
                    RandomSource random = level.getRandom();

                    // Particles
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.getX(), center.getY() + 1, center.getZ(), 100, 5, 5, 5, 0.1);
                    }

                    // 1. Replace Blocks (Ground/Environment)
                    for (int x = -radius; x <= radius; x++) {
                        for (int y = -radius; y <= radius; y++) {
                            for (int z = -radius; z <= radius; z++) {
                                if (x * x + y * y + z * z <= radius * radius) {
                                    BlockPos pos = center.offset(x, y, z);
                                    BlockState state = level.getBlockState(pos);
                                    
                                    // Constraint: Not Air, Not Tile Entity, Destructible
                                    if (!state.isAir() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0) {
                                        if (pos.equals(center) || pos.equals(center.above())) {
                                            continue; // Don't freeze player's immediate space
                                        }

                                        BlockState iceState;
                                        int roll = random.nextInt(100);
                                        if (roll < 60) iceState = Blocks.ICE.defaultBlockState();
                                        else if (roll < 90) iceState = Blocks.PACKED_ICE.defaultBlockState();
                                        else iceState = Blocks.BLUE_ICE.defaultBlockState();
                                        
                                        level.setBlock(pos, iceState, 2);
                                        
                                        // Add to restore bucket
                                        int delay = 160 + random.nextInt(41); // 160 to 200 ticks (8-10s)
                                        restoreBuckets.computeIfAbsent(delay, k -> new ArrayList<>())
                                                      .add(new RestoreData(pos, state, iceState));
                                    }
                                }
                            }
                        }
                    }

                    // 2. Entities Logic (Ice Prison)
                    AABB aabb = new AABB(center).inflate(radius);
                    List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
                    for (LivingEntity target : entities) {
                        if (target != player) {
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4)); // Slowness V
                            
                            // Ice Prison Generation
                            AABB targetBox = target.getBoundingBox();
                            // Inflate by ~1 block to create a cage around the hitbox
                            AABB cageBox = targetBox.inflate(1.0);
                            
                            // Iterate through the cage bounds
                            int minX = (int) Math.floor(cageBox.minX);
                            int maxX = (int) Math.ceil(cageBox.maxX);
                            int minY = (int) Math.floor(cageBox.minY);
                            int maxY = (int) Math.ceil(cageBox.maxY);
                            int minZ = (int) Math.floor(cageBox.minZ);
                            int maxZ = (int) Math.ceil(cageBox.maxZ);

                            for (int x = minX; x < maxX; x++) {
                                for (int y = minY; y < maxY; y++) {
                                    for (int z = minZ; z < maxZ; z++) {
                                        BlockPos pos = new BlockPos(x, y, z);
                                        // Only place if it does NOT intersect the entity's actual hitbox (keep hollow)
                                        AABB blockBox = new AABB(pos);
                                        if (!blockBox.intersects(targetBox)) {
                                            // Only place in Air (don't replace existing blocks, only cage them)
                                            BlockState currentState = level.getBlockState(pos);
                                            if (currentState.isAir()) {
                                                BlockState iceState;
                                                int roll = random.nextInt(100);
                                                if (roll < 60) iceState = Blocks.ICE.defaultBlockState();
                                                else if (roll < 90) iceState = Blocks.PACKED_ICE.defaultBlockState();
                                                else iceState = Blocks.BLUE_ICE.defaultBlockState();
                                                
                                                level.setBlock(pos, iceState, 2);
                                                
                                                // Add to restore bucket (restore to AIR)
                                                int delay = 160 + random.nextInt(41); // 160 to 200 ticks (8-10s)
                                                restoreBuckets.computeIfAbsent(delay, k -> new ArrayList<>())
                                                              .add(new RestoreData(pos, Blocks.AIR.defaultBlockState(), iceState));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Schedule Revert Tasks
                    for (Map.Entry<Integer, List<RestoreData>> entry : restoreBuckets.entrySet()) {
                        int delay = entry.getKey();
                        List<RestoreData> dataList = entry.getValue();
                        
                        TYPE_MOON_WORLD.queueServerWork(delay, () -> {
                            for (RestoreData data : dataList) {
                                BlockState currentState = level.getBlockState(data.pos);
                                // Only revert if the current block matches the ice we placed.
                                // If it's different (air, or other block), it means it was broken/changed.
                                if (currentState.getBlock() == data.placedState.getBlock()) {
                                    level.levelEvent(2001, data.pos, Block.getId(currentState));
                                    level.setBlock(data.pos, data.originalState, 2);
                                }
                            }
                        });
                    }
                }

            } else {
                player.displayClientMessage(Component.literal("需要3个蓝宝石"), true);
            }
        }
    }
    
    // Helper record class
    private static class RestoreData {
        final BlockPos pos;
        final BlockState originalState;
        final BlockState placedState;

        RestoreData(BlockPos pos, BlockState originalState, BlockState placedState) {
            this.pos = pos;
            this.originalState = originalState;
            this.placedState = placedState;
        }
    }
}

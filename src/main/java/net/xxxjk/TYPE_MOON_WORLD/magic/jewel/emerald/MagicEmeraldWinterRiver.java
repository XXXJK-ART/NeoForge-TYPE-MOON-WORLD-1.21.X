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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

import java.util.List;

public class MagicEmeraldWinterRiver {
    public static void execute(Entity entity) {
        if (entity == null)
            return;

        if (entity instanceof Player player) {
            int count = 0;
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (stack.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.EMERALD) {
                    count += stack.getCount();
                }
            }
            ItemStack offhand = player.getOffhandItem();
            if (offhand.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.EMERALD) {
                count += offhand.getCount();
            }

            if (count >= 2) {
                // Consume 2 gems
                ItemStack gem1 = GemUtils.consumeGem(player, GemType.EMERALD);
                ItemStack gem2 = GemUtils.consumeGem(player, GemType.EMERALD);
                
                float m1 = 1.0f;
                if (gem1.getItem() instanceof FullManaCarvedGemItem g) m1 = g.getQuality().getEffectMultiplier();
                float m2 = 1.0f;
                if (gem2.getItem() instanceof FullManaCarvedGemItem g) m2 = g.getQuality().getEffectMultiplier();
                
                float multiplier = (m1 + m2) / 2.0f;

                // Execute Winter River (Emerald)
                Level level = player.level();
                if (!level.isClientSide) {
                    // Cube
                    // Scale: Poor(0.5)->1, Normal(1.0)->2, High(1.5)->3
                    int radius = Math.max(1, Math.round(2 * multiplier));
                    int height = 2 * radius; // Total height from 0 to 2*radius

                    BlockPos center = player.blockPosition();
                    
                    // Adjust center if in fluid to surround player
                    if (player.isInWater() || player.isInLava()) {
                        center = center.below(radius);
                    }
                    
                    RandomSource random = level.getRandom();
                    
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX(), center.getY() + 1, center.getZ(), 50, 2, 2, 2, 0.1);
                    }
                    
                    // 1. Repel/Teleport Entities inside the area
                    BlockPos geometricCenter = center.offset(0, radius, 0);
                    BlockPos minPos = center.offset(-radius, 0, -radius);
                    BlockPos maxPos = center.offset(radius, height, radius);
                    AABB box = new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1, maxPos.getY() + 1, maxPos.getZ() + 1).inflate(1);
                    List<Entity> entities = level.getEntities(player, box, e -> e != player); // Exclude caster
                    
                    for (Entity e : entities) {
                        Vec3 pPos = e.position();
                        Vec3 cPos = geometricCenter.getCenter();
                        Vec3 dir = pPos.subtract(cPos);
                        
                        if (dir.lengthSqr() < 0.01) {
                            dir = new Vec3(1, 0, 0);
                        }
                        dir = dir.normalize();
                        
                        // Teleport outside the box
                        Vec3 target = cPos.add(dir.scale(radius * 1.5 + 2));
                        e.teleportTo(target.x, target.y, target.z);
                    }

                    // 2. Build Cube
                    for (int x = -radius; x <= radius; x++) {
                        for (int y = 0; y <= height; y++) {
                            for (int z = -radius; z <= radius; z++) {
                                // Hollow Cube Check
                                boolean isBorder = Math.abs(x) == radius || Math.abs(z) == radius || y == 0 || y == height;
                                BlockPos pos = center.offset(x, y, z);
                                
                                if (isBorder) {
                                    BlockState currentState = level.getBlockState(pos);
                                    
                                    if (currentState.is(ModBlocks.GREEN_TRANSPARENT_BLOCK.get())) {
                                        int delay = 160 + random.nextInt(41);
                                        level.scheduleTick(pos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get(), delay);
                                    } else if (currentState.getDestroySpeed(level, pos) >= 0 && !currentState.is(Blocks.BEDROCK)) {
                                        boolean canPlace = currentState.isAir() || !currentState.getFluidState().isEmpty();
                                        if (!canPlace) {
                                            // Destroy block without dropping items
                                            canPlace = level.destroyBlock(pos, false);
                                        }
                                        
                                        // Force set if destroyBlock failed but it's valid (e.g. some fluids/states)
                                        // or just proceed if canPlace is true
                                        if (canPlace || currentState.getDestroySpeed(level, pos) >= 0) {
                                            level.setBlock(pos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get().defaultBlockState(), 3);
                                        }
                                    }
                                } else {
                                    // Interior: Clear Space
                                    BlockState innerState = level.getBlockState(pos);
                                    if (!innerState.isAir()) {
                                        level.destroyBlock(pos, false);
                                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                    }
                                }
                            }
                        }
                    }
                }
                
            } else {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_EMERALD_WINTER_RIVER_NEED_GEM), true);
            }
        }
    }
}

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
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;

import java.util.List;

public class MagicEmeraldWinterRiver {
    public static void execute(Entity entity) {
        if (!(entity instanceof Player player))
            return;

        int count = countFullGems(player);
        if (count < 2) {
            player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_EMERALD_WINTER_RIVER_NEED_GEM), true);
            return;
        }

        ItemStack gem1 = GemUtils.consumeGem(player, GemType.EMERALD);
        ItemStack gem2 = GemUtils.consumeGem(player, GemType.EMERALD);

        float m1 = 1.0f;
        if (gem1.getItem() instanceof FullManaCarvedGemItem g) m1 = g.getQuality().getEffectMultiplier();
        float m2 = 1.0f;
        if (gem2.getItem() instanceof FullManaCarvedGemItem g) m2 = g.getQuality().getEffectMultiplier();

        activateWinterRiver(player, (m1 + m2) / 2.0f);
    }

    public static void executeFromCombo(Player player, float multiplier) {
        if (player == null) {
            return;
        }
        activateWinterRiver(player, Math.max(0.4f, multiplier));
    }

    private static int countFullGems(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.EMERALD) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.EMERALD) {
            count += offhand.getCount();
        }
        return count;
    }

    private static void activateWinterRiver(Player player, float multiplier) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        int radius = Math.max(1, Math.round(2 * multiplier));
        int height = 2 * radius;

        BlockPos center = player.blockPosition();
        if (player.isInWater() || player.isInLava()) {
            center = center.below(radius);
        }

        RandomSource random = level.getRandom();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX(), center.getY() + 1, center.getZ(), 50, 2, 2, 2, 0.1);
        }

        BlockPos geometricCenter = center.offset(0, radius, 0);
        BlockPos minPos = center.offset(-radius, 0, -radius);
        BlockPos maxPos = center.offset(radius, height, radius);
        AABB box = new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1, maxPos.getY() + 1, maxPos.getZ() + 1).inflate(1);
        List<Entity> entities = level.getEntities(player, box, e -> e != player);

        for (Entity e : entities) {
            Vec3 pPos = e.position();
            Vec3 cPos = geometricCenter.getCenter();
            Vec3 dir = pPos.subtract(cPos);

            if (dir.lengthSqr() < 0.01) {
                dir = new Vec3(1, 0, 0);
            }
            dir = dir.normalize();
            Vec3 target = cPos.add(dir.scale(radius * 1.5 + 2));
            e.teleportTo(target.x, target.y, target.z);
        }

        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = -radius; z <= radius; z++) {
                    boolean isBorder = Math.abs(x) == radius || Math.abs(z) == radius || y == 0 || y == height;
                    BlockPos pos = center.offset(x, y, z);

                    if (isBorder) {
                        BlockState currentState = level.getBlockState(pos);

                        if (currentState.is(ModBlocks.GREEN_TRANSPARENT_BLOCK.get())) {
                            int delay = 160 + random.nextInt(41);
                            level.scheduleTick(pos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get(), delay);
                        } else if (currentState.getDestroySpeed(level, pos) >= 0 && !currentState.is(Blocks.BEDROCK)) {
                            replaceBlockWithoutDrops(level, pos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get().defaultBlockState());
                        }
                    } else {
                        BlockState innerState = level.getBlockState(pos);
                        if (!innerState.isAir()) {
                            replaceBlockWithoutDrops(level, pos, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void replaceBlockWithoutDrops(Level level, BlockPos pos, BlockState targetState) {
        if (level.getBlockEntity(pos) != null) {
            level.removeBlockEntity(pos);
        }
        level.setBlock(pos, targetState, 3);
    }
}

package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;

public class MagicEmeraldUse {
   public static void execute(Entity entity) {
      if (entity != null) {
         if (entity instanceof Player player) {
            ItemStack gemStack = GemUtils.consumeGem(player, GemType.EMERALD);
            if (!gemStack.isEmpty()) {
               Level level = player.level();
               if (!level.isClientSide) {
                  float multiplier = 1.0F;
                  if (gemStack.getItem() instanceof FullManaCarvedGemItem gemItem) {
                     multiplier = gemItem.getQuality().getEffectMultiplier();
                  }

                  float yaw = player.getYRot();
                  float normalizedYaw = (yaw % 360.0F + 360.0F) % 360.0F;
                  int octant = Math.round(normalizedYaw / 45.0F) % 8;
                  int dx = 0;
                  int dz = 0;
                  int rx = 0;
                  int rz = 0;
                  int dist = 4;
                  if (octant % 2 != 0) {
                     dist = (int)Math.round(dist * 0.75);
                     if (dist < 1) {
                        dist = 1;
                     }
                  }

                  switch (octant) {
                     case 0:
                        dx = 0;
                        dz = 1;
                        rx = -1;
                        rz = 0;
                        break;
                     case 1:
                        dx = -1;
                        dz = 1;
                        rx = -1;
                        rz = -1;
                        break;
                     case 2:
                        dx = -1;
                        dz = 0;
                        rx = 0;
                        rz = -1;
                        break;
                     case 3:
                        dx = -1;
                        dz = -1;
                        rx = 1;
                        rz = -1;
                        break;
                     case 4:
                        dx = 0;
                        dz = -1;
                        rx = 1;
                        rz = 0;
                        break;
                     case 5:
                        dx = 1;
                        dz = -1;
                        rx = 1;
                        rz = 1;
                        break;
                     case 6:
                        dx = 1;
                        dz = 0;
                        rx = 0;
                        rz = 1;
                        break;
                     case 7:
                        dx = 1;
                        dz = 1;
                        rx = -1;
                        rz = 1;
                  }

                  BlockPos playerPos = player.blockPosition();
                  BlockPos startPos = playerPos.offset(dx * dist, 0, dz * dist);
                  Direction up = Direction.UP;
                  List<BlockPos> placedBlocks = new ArrayList<>();
                  RandomSource random = level.getRandom();
                  int widthRadius = Math.max(1, Math.round(1.0F * multiplier));
                  int height = Math.max(2, Math.round(3.0F * multiplier));
                  int thickness = multiplier >= 1.2 ? 2 : 1;

                  for (int d = 0; d < thickness; d++) {
                     BlockPos layerStartPos = startPos.offset(dx * d, 0, dz * d);

                     for (int w = -widthRadius; w <= widthRadius; w++) {
                        for (int h = 0; h < height; h++) {
                           BlockPos targetPos = layerStartPos.offset(rx * w, 0, rz * w).relative(up, h);
                           if (level.getBlockState(targetPos).getDestroySpeed(level, targetPos) >= 0.0F
                              && replaceBlockWithoutDrops(level, targetPos, ((Block)ModBlocks.GREEN_TRANSPARENT_BLOCK.get()).defaultBlockState())) {
                              placedBlocks.add(targetPos);
                              if (level instanceof ServerLevel serverLevel) {
                                 serverLevel.sendParticles(
                                    ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 5, 0.5, 0.5, 0.5, 0.1
                                 );
                              }
                           }
                        }
                     }
                  }

                  if (!placedBlocks.isEmpty()) {
                     int baseDuration = 160;

                     for (BlockPos pos : placedBlocks) {
                        int delay = baseDuration + random.nextInt(40);
                        TYPE_MOON_WORLD.queueServerWork(delay, () -> {
                           if (level.getBlockState(pos).is((Block)ModBlocks.GREEN_TRANSPARENT_BLOCK.get())) {
                              level.destroyBlock(pos, false);
                           }
                        });
                     }
                  }
               }
            } else {
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.emerald_use.need_gem"), true);
            }
         }
      }
   }

   private static boolean replaceBlockWithoutDrops(Level level, BlockPos pos, BlockState targetState) {
      if (level.getBlockEntity(pos) != null) {
         level.removeBlockEntity(pos);
      }

      return level.setBlock(pos, targetState, 3);
   }
}

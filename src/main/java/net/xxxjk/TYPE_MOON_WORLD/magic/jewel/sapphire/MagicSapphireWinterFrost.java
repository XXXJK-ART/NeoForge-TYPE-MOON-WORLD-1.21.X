package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.GemUtils;

public class MagicSapphireWinterFrost {
   public static void execute(Entity entity) {
      if (entity instanceof Player player) {
         int count = countFullGems(player);
         if (count < 3) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.sapphire_winter_frost.need_gem"), true);
         } else {
            ItemStack gem1 = GemUtils.consumeGem(player, GemType.SAPPHIRE);
            ItemStack gem2 = GemUtils.consumeGem(player, GemType.SAPPHIRE);
            ItemStack gem3 = GemUtils.consumeGem(player, GemType.SAPPHIRE);
            float m1 = 1.0F;
            if (gem1.getItem() instanceof FullManaCarvedGemItem g) {
               m1 = g.getQuality().getEffectMultiplier();
            }

            float m2 = 1.0F;
            if (gem2.getItem() instanceof FullManaCarvedGemItem g) {
               m2 = g.getQuality().getEffectMultiplier();
            }

            float m3 = 1.0F;
            if (gem3.getItem() instanceof FullManaCarvedGemItem g) {
               m3 = g.getQuality().getEffectMultiplier();
            }

            applyWinterFrost(player, (m1 + m2 + m3) / 3.0F);
         }
      }
   }

   public static void executeFromCombo(Player player, float multiplier) {
      if (player != null) {
         applyWinterFrost(player, Math.max(0.4F, multiplier));
      }
   }

   private static int countFullGems(Player player) {
      int count = 0;

      for (ItemStack stack : player.getInventory().items) {
         if (stack.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.SAPPHIRE) {
            count += stack.getCount();
         }
      }

      ItemStack offhand = player.getOffhandItem();
      if (offhand.getItem() instanceof FullManaCarvedGemItem gemItem && gemItem.getType() == GemType.SAPPHIRE) {
         count += offhand.getCount();
      }

      return count;
   }

   private static void applyWinterFrost(Player player, float multiplier) {
      Level level = player.level();
      if (!level.isClientSide) {
         BlockPos center = player.blockPosition();
         int radius = Math.max(5, Math.round(10.0F * multiplier));
         Map<Integer, List<MagicSapphireWinterFrost.RestoreData>> restoreBuckets = new HashMap<>();
         RandomSource random = level.getRandom();
         if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.getX(), center.getY() + 1, center.getZ(), 100, 5.0, 5.0, 5.0, 0.1);
         }

         for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
               for (int z = -radius; z <= radius; z++) {
                  if (x * x + y * y + z * z <= radius * radius) {
                     BlockPos pos = center.offset(x, y, z);
                     BlockState state = level.getBlockState(pos);
                     if (!state.isAir()
                        && !state.hasBlockEntity()
                        && !(state.getDestroySpeed(level, pos) < 0.0F)
                        && !pos.equals(center)
                        && !pos.equals(center.above())) {
                        BlockState iceState = randomIceState(random);
                        level.setBlock(pos, iceState, 2);
                        int baseDelay = 160 + random.nextInt(41);
                        int delay = Math.round(baseDelay * multiplier);
                        restoreBuckets.computeIfAbsent(delay, k -> new ArrayList<>()).add(new MagicSapphireWinterFrost.RestoreData(pos, state, iceState));
                     }
                  }
               }
            }
         }

         AABB aabb = new AABB(center).inflate(radius);

         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (target != player && !EntityUtils.isImmunePlayerTarget(target)) {
               int duration = Math.round(200.0F * multiplier);
               target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4));
               EntityUtils.triggerSwarmAnger(level, player, target);
               if (target instanceof Mob mob) {
                  mob.setTarget(player);
               }

               AABB targetBox = target.getBoundingBox();
               AABB cageBox = targetBox.inflate(1.0);
               int minX = (int)Math.floor(cageBox.minX);
               int maxX = (int)Math.ceil(cageBox.maxX);
               int minY = (int)Math.floor(cageBox.minY);
               int maxY = (int)Math.ceil(cageBox.maxY);
               int minZ = (int)Math.floor(cageBox.minZ);
               int maxZ = (int)Math.ceil(cageBox.maxZ);

               for (int x = minX; x < maxX; x++) {
                  for (int y = minY; y < maxY; y++) {
                     for (int zx = minZ; zx < maxZ; zx++) {
                        BlockPos pos = new BlockPos(x, y, zx);
                        AABB blockBox = new AABB(pos);
                        if (!blockBox.intersects(targetBox)) {
                           BlockState currentState = level.getBlockState(pos);
                           if (currentState.isAir()) {
                              BlockState iceState = randomIceState(random);
                              level.setBlock(pos, iceState, 2);
                              int baseDelay = 160 + random.nextInt(41);
                              int delay = Math.round(baseDelay * multiplier);
                              restoreBuckets.computeIfAbsent(delay, k -> new ArrayList<>())
                                 .add(new MagicSapphireWinterFrost.RestoreData(pos, Blocks.AIR.defaultBlockState(), iceState));
                           }
                        }
                     }
                  }
               }
            }
         }

         for (Entry<Integer, List<MagicSapphireWinterFrost.RestoreData>> entry : restoreBuckets.entrySet()) {
            int delay = entry.getKey();
            List<MagicSapphireWinterFrost.RestoreData> dataList = entry.getValue();
            TYPE_MOON_WORLD.queueServerWork(delay, () -> {
               for (MagicSapphireWinterFrost.RestoreData data : dataList) {
                  BlockState currentStatex = level.getBlockState(data.pos);
                  if (currentStatex.getBlock() == data.placedState.getBlock()) {
                     boolean exposed = false;

                     for (Direction dir : Direction.values()) {
                        if (level.getBlockState(data.pos.relative(dir)).isAir()) {
                           exposed = true;
                           break;
                        }
                     }

                     if (exposed) {
                        level.levelEvent(2001, data.pos, Block.getId(currentStatex));
                     }

                     level.setBlock(data.pos, data.originalState, 2);
                  }
               }
            });
         }
      }
   }

   private static BlockState randomIceState(RandomSource random) {
      int roll = random.nextInt(100);
      if (roll < 60) {
         return Blocks.ICE.defaultBlockState();
      } else {
         return roll < 90 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
      }
   }

   private record RestoreData(BlockPos pos, BlockState originalState, BlockState placedState) {
   }
}

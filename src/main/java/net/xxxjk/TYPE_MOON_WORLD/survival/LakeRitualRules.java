package net.xxxjk.TYPE_MOON_WORLD.survival;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DivineSteelItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.ProjectionDataHelper;

public final class LakeRitualRules {
   public static final int WATER_REQUIRED = 6, DURATION = 40;
   private LakeRitualRules() {}
   public static boolean connectedWater(Level level, BlockPos start) {
      return connectedWater(start, p -> level.hasChunkAt(p) && level.getFluidState(p).is(FluidTags.WATER)
         && level.getFluidState(p).isSource());
   }
   public static boolean connectedWater(BlockPos start, Predicate<BlockPos> source) {
      if (!source.test(start)) return false;
      Set<BlockPos> found = new HashSet<>(); ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      found.add(start); queue.add(start);
      while (!queue.isEmpty() && found.size() < WATER_REQUIRED) {
         BlockPos p = queue.remove();
         for (Direction d : Direction.values()) {
            BlockPos n = p.relative(d);
            if (!found.contains(n) && source.test(n)) { found.add(n); queue.add(n); }
         }
      }
      return found.size() >= WATER_REQUIRED;
   }
   public static boolean isLake(Level level, BlockPos center) {
      if (connectedWater(level, center)) return true;
      for (Direction d : Direction.values()) if (connectedWater(level, center.relative(d))) return true;
      return false;
   }
   public static ItemStack result(ItemStack sword, ItemStack offering) {
      if (!sword.is(ModItems.BROKEN_EXCALIBUR.get()) || ProjectionDataHelper.isProjected(sword)
         || ProjectionDataHelper.isProjected(offering)) return ItemStack.EMPTY;
      if (offering.is(ModItems.AVALON.get())) return new ItemStack(ModItems.EXCALIBUR.get());
      if (offering.is(Items.SUNFLOWER)) return new ItemStack(ModItems.EXCALIBUR_GALLATIN.get());
      if (DivineSteelItem.isComplete(offering)) return new ItemStack(ModItems.AROUNDIGHT.get());
      return ItemStack.EMPTY;
   }
}

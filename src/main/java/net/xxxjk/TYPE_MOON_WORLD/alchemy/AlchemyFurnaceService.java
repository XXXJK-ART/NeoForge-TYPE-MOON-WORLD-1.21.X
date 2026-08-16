package net.xxxjk.TYPE_MOON_WORLD.alchemy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public final class AlchemyFurnaceService {
   public static final int ALCHEMY_BURN_TIME = 200;

   private AlchemyFurnaceService() {
   }

   public static boolean isAlchemyContainer(ItemStack stack) {
      return !stack.isEmpty() && (stack.is(Items.GLASS_BOTTLE) || stack.getItem() instanceof FullManaCarvedGemItem);
   }

   public static boolean isAlchemyBlast(AbstractFurnaceBlockEntity furnace) {
      return furnace != null && furnace.getType() == BlockEntityType.BLAST_FURNACE && furnace.getItem(0).is(ModItems.CINNABAR.get());
   }

   public static boolean shouldKeepAlchemyContainer(AbstractFurnaceBlockEntity furnace, ItemStack fuel) {
      return isAlchemyBlast(furnace) && isAlchemyContainer(fuel);
   }

   public static ItemStack alchemyFuelResult(ItemStack fuel) {
      if (fuel.is(Items.GLASS_BOTTLE)) {
         return new ItemStack(ModItems.MERCURY_BOTTLE.get());
      }

      if (fuel.getItem() instanceof FullManaCarvedGemItem gem) {
         Item molten = moltenBottleFor(gem.getType());
         return molten == Items.AIR ? ItemStack.EMPTY : new ItemStack(molten);
      }

      return ItemStack.EMPTY;
   }

   public static Item moltenBottleFor(GemType type) {
      return switch (type) {
         case RUBY -> ModItems.MOLTEN_RUBY_BOTTLE.get();
         case SAPPHIRE -> ModItems.MOLTEN_SAPPHIRE_BOTTLE.get();
         case EMERALD -> ModItems.MOLTEN_EMERALD_BOTTLE.get();
         case TOPAZ -> ModItems.MOLTEN_TOPAZ_BOTTLE.get();
         case WHITE_GEMSTONE -> ModItems.MOLTEN_WHITE_GEMSTONE_BOTTLE.get();
         case CYAN -> ModItems.MOLTEN_CYAN_GEMSTONE_BOTTLE.get();
         case BLACK_SHARD -> ModItems.MOLTEN_BLACK_SHARD_BOTTLE.get();
      };
   }
}

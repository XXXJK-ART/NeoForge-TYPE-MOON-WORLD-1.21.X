package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MoltenGemBottleItem extends Item {
   private final GemType type;

   public MoltenGemBottleItem(Properties properties, GemType type) {
      super(properties);
      this.type = type;
   }

   public GemType getType() {
      return this.type;
   }

   @Override
   public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
      return true;
   }

   @Override
   public @NotNull ItemStack getCraftingRemainingItem(ItemStack itemstack) {
      return new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
   }
}

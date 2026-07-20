package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

public class MysticMercuryItem extends Item {
   public MysticMercuryItem(Properties properties) {
      super(properties.stacksTo(1));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      int current = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0)).value();
      stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(current == 1 ? 0 : 1));
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }

   public static ItemStack emptyVariant(Item item) {
      ItemStack stack = new ItemStack(item);
      stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
      return stack;
   }
}

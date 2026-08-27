package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneLearningService;

/** One item type can teach any rune through its rune_id component. */
public class RuneLearningItem extends Item {
   public RuneLearningItem(Properties properties) { super(properties.stacksTo(1)); }
   @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide()) {
         CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
         if (RuneLearningService.learn(player, tag.getString("rune_id"))) stack.shrink(1);
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }
}

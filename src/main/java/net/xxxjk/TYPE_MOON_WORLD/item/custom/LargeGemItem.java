package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class LargeGemItem extends Item {
   private final GemType type;

   public LargeGemItem(Properties properties, GemType type) {
      super(properties);
      this.type = type;
   }

   public GemType getType() {
      return this.type;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable(this.getDescriptionId(stack) + ".desc").withStyle(ChatFormatting.AQUA));
   }
}

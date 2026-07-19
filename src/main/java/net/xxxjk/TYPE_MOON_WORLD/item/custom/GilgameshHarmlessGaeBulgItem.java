package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

/** A non-instant-death projection of Gae Bolg for Gilgamesh's treasury wheel. */
public final class GilgameshHarmlessGaeBulgItem extends SwordItem {
   public GilgameshHarmlessGaeBulgItem(Properties properties) {
      super(Tiers.NETHERITE, properties);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.gilgamesh_gae_bulg.desc").withStyle(ChatFormatting.RED));
   }
}

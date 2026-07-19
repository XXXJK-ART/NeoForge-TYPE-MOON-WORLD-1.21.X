package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A non-instant-death projection of Gae Bolg for Gilgamesh's treasury wheel. */
public final class GilgameshHarmlessGaeBulgItem extends GaeBulgItem {
   public GilgameshHarmlessGaeBulgItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return InteractionResultHolder.pass(player.getItemInHand(hand));
   }
}

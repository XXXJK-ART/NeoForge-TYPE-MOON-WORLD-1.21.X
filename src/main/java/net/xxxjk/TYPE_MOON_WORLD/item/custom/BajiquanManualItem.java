package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanCombatService;

public class BajiquanManualItem extends Item {
   public BajiquanManualItem(Properties properties) {
      super(properties.stacksTo(1));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && BajiquanCombatService.learn(serverPlayer)) {
         if (!player.getAbilities().instabuild) stack.shrink(1);
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }
}

package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;

public class ServantCardItem extends Item {
   private final String servantId;

   public ServantCardItem(Properties properties, String servantId) {
      super(properties);
      this.servantId = servantId;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
         ServantCardTransformManager.transform(serverPlayer, this.servantId);
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }

   public String servantId() {
      return this.servantId;
   }
}

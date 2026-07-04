package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;

public class ServantMasterContractItem extends Item {
   public ServantMasterContractItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (!player.level().isClientSide && player instanceof ServerPlayer servant && target instanceof ServerPlayer master) {
         return ServantCardTransformManager.bindMaster(servant, master) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
      }
      return InteractionResult.sidedSuccess(player.level().isClientSide);
   }
}

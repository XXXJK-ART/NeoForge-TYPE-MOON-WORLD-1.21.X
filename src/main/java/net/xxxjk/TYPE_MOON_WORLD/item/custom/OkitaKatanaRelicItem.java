package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.servant.summon.SummoningRitualService;

public final class OkitaKatanaRelicItem extends JapaneseSwordItem {
   public OkitaKatanaRelicItem(Properties properties) {
      super("katana", properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      if (context.getHand() != InteractionHand.MAIN_HAND
         || !context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.SUMMONING_CIRCLE.get())) {
         return InteractionResult.PASS;
      }
      if (context.getPlayer() instanceof ServerPlayer player
         && !SummoningRitualService.begin(player, context.getClickedPos(), context.getItemInHand())) {
         return InteractionResult.FAIL;
      }
      if (context.getPlayer() != null) {
         context.getPlayer().startUsingItem(context.getHand());
      }
      return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
   }

   @Override
   public ItemStack finishUsingItem(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity) {
      return stack;
   }
}

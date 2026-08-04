package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.servant.summon.SummoningRitualService;

/** Catalyst item that can only be used while standing on a summoning circle. */
public class SummoningRelicItem extends Item {
   public SummoningRelicItem(Properties properties) {
      super(properties);
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
   public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
      return 72000;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   @Override
   public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
      return stack;
   }
}

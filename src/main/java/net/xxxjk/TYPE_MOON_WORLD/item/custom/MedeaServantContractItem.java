package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MedeaSpecialContractService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public class MedeaServantContractItem extends Item {
   public MedeaServantContractItem(Properties properties) {
      super(properties);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.medea_servant_contract.desc").withStyle(ChatFormatting.DARK_PURPLE));
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.level().isClientSide) {
         return InteractionResult.SUCCESS;
      }
      if (!(player instanceof ServerPlayer actor) || !(target instanceof ServantEntity servant)) {
         return InteractionResult.FAIL;
      }
      boolean success = actor.isShiftKeyDown() && MedeaSpecialContractService.isLinked(actor, servant)
         ? MedeaSpecialContractService.clear(actor, true, false)
         : MedeaSpecialContractService.bind(actor, servant);
      if (success && !actor.getAbilities().instabuild && !actor.isShiftKeyDown()) {
         stack.shrink(1);
      }
      return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
   }
}

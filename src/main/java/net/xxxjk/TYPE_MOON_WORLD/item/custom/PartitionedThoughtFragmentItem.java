package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveRank;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;

public class PartitionedThoughtFragmentItem extends Item {
   public PartitionedThoughtFragmentItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide()) return InteractionResultHolder.consume(stack);
      if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
      TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (MagicProficiencyService.getRaw(vars, "magic_analysis") < 50.0) {
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.partitioned_thought_fragment.need_analysis"), true);
         return InteractionResultHolder.fail(stack);
      }
      PassiveRank current = PassiveService.rank(vars, PassiveService.PARTITIONED_THOUGHT);
      if (current == PassiveRank.A) {
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.partitioned_thought_fragment.max"), true);
         return InteractionResultHolder.fail(stack);
      }
      vars.passive_ranks.put(PassiveService.PARTITIONED_THOUGHT, current == null ? PassiveRank.E : current.next());
      if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
      vars.syncPlayerVariables(serverPlayer);
      serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.partitioned_thought_fragment.learned"), true);
      return InteractionResultHolder.sidedSuccess(stack, false);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.typemoonworld.partitioned_thought_fragment.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
   }
}

package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

public class CommandSpellItem extends Item {
   public CommandSpellItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
         boolean success;
         if (stack.is(ModItems.SUPERVISOR_COMMAND_SPELL.get())) {
            success = MasterStateManager.replaceCommandSpells(serverPlayer, 11, "supervisor");
         } else if (stack.is(ModItems.ELSA_COMMAND_SPELL.get())) {
            success = MasterStateManager.replaceCommandSpells(serverPlayer, 3, "elsa_saijo");
         } else if (stack.is(ModItems.SINGLE_COMMAND_SPELL.get())) {
            success = MasterStateManager.addSingleCommandSpell(serverPlayer);
         } else {
            success = MasterStateManager.activate(serverPlayer);
         }
         if (success && !serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
         }
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }
}

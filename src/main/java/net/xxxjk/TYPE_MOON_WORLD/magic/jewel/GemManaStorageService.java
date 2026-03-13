package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class GemManaStorageService {
   private GemManaStorageService() {
   }

   public static ItemStack storeIntoGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack, Item fullGemItem, GemType type, GemQuality quality) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int manaAmount = getStorageManaAmount(heldStack, type, quality);
      if (vars.player_mana < manaAmount) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         return heldStack;
      } else {
         vars.player_mana -= manaAmount;
         vars.syncMana(player);
         Item targetFullGem = fullGemItem;
         if (GemEngravingService.getEngravedMagicId(heldStack) != null) {
            targetFullGem = ModItems.getNormalizedFullCarvedGem(type);
         }

         ItemStack filled = new ItemStack(targetFullGem);
         GemEngravingService.copyEngravingData(heldStack, filled);
         ItemStack result = replaceHeldSingle(player, hand, heldStack, filled);
         player.displayClientMessage(Component.translatable("message.typemoonworld.gem.storage.stored", new Object[]{manaAmount}), true);
         return result;
      }
   }

   public static ItemStack withdrawFromGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack, Item emptyGemItem, GemType type, GemQuality quality) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int manaAmount = getStorageManaAmount(heldStack, type, quality);
      vars.player_mana += manaAmount;
      vars.syncMana(player);
      Item targetEmptyGem = emptyGemItem;
      if (GemEngravingService.getEngravedMagicId(heldStack) != null) {
         targetEmptyGem = ModItems.getNormalizedCarvedGem(type);
      }

      ItemStack emptied = new ItemStack(targetEmptyGem);
      GemEngravingService.copyEngravingData(heldStack, emptied);
      ItemStack result = replaceHeldSingle(player, hand, heldStack, emptied);
      if (vars.player_mana > vars.player_max_mana) {
         ChatFormatting color = ChatFormatting.YELLOW;
         if (vars.player_mana > vars.player_max_mana * 1.25) {
            color = ChatFormatting.DARK_RED;
         } else if (vars.player_mana > vars.player_max_mana * 1.2) {
            color = ChatFormatting.RED;
         }

         player.displayClientMessage(
            Component.translatable("message.typemoonworld.mana.overload.warning", new Object[]{(int)vars.player_mana, (int)vars.player_max_mana})
               .withStyle(color),
            true
         );
      }

      player.displayClientMessage(Component.translatable("message.typemoonworld.gem.storage.withdrawn", new Object[]{manaAmount}), true);
      return result;
   }

   private static ItemStack replaceHeldSingle(ServerPlayer player, InteractionHand hand, ItemStack heldStack, ItemStack replacement) {
      if (heldStack.getCount() <= 1) {
         player.setItemInHand(hand, replacement);
         return replacement;
      } else {
         heldStack.shrink(1);
         if (!player.addItem(replacement.copy())) {
            player.drop(replacement.copy(), false);
         }

         return player.getItemInHand(hand);
      }
   }

   private static int getStorageManaAmount(ItemStack stack, GemType type, GemQuality quality) {
      String engravedMagicId = GemEngravingService.getEngravedMagicId(stack);
      if (engravedMagicId == null) {
         return quality.getCapacity(type);
      } else {
         double engravedCost = GemEngravingService.getEngravedManaCost(stack);
         if (engravedCost <= 0.0) {
            return switch (engravedMagicId) {
               case "gravity_magic" -> 20;
               case "gander" -> 25;
               case "reinforcement" -> (int)Math.ceil(GemEngravingService.calculateReinforcementManaCost(GemEngravingService.getReinforcementLevel(stack, 1)));
               default -> 100;
            };
         } else {
            return Math.max(1, (int)Math.ceil(engravedCost));
         }
      }
   }
}

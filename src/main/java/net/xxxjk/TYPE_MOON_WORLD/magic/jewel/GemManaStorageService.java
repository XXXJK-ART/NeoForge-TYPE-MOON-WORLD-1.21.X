package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class GemManaStorageService {
    private static final String BASIC_JEWEL_MAGIC_ID = "jewel_magic_shoot";

    private GemManaStorageService() {
    }

    public static boolean hasStorageUnlock(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        return vars.learned_magics.contains(BASIC_JEWEL_MAGIC_ID);
    }

    public static ItemStack storeIntoGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack, Item fullGemItem, GemType type, GemQuality quality) {
        if (!hasStorageUnlock(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.storage.locked"), true);
            return heldStack;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int manaAmount = getStorageManaAmount(heldStack, type, quality);
        if (vars.player_mana < manaAmount) {
            player.displayClientMessage(Component.translatable(MagicConstants.MSG_NOT_ENOUGH_MANA), true);
            return heldStack;
        }

        vars.player_mana -= manaAmount;
        vars.syncMana(player);

        Item targetFullGem = fullGemItem;
        if (GemEngravingService.getEngravedMagicId(heldStack) != null) {
            targetFullGem = ModItems.getNormalizedFullCarvedGem(type);
        }
        ItemStack filled = new ItemStack(targetFullGem);
        GemEngravingService.copyEngravingData(heldStack, filled);
        ItemStack result = replaceHeldSingle(player, hand, heldStack, filled);
        player.displayClientMessage(Component.translatable("message.typemoonworld.gem.storage.stored", manaAmount), true);
        return result;
    }

    public static ItemStack withdrawFromGem(ServerPlayer player, InteractionHand hand, ItemStack heldStack, Item emptyGemItem, GemType type, GemQuality quality) {
        if (!hasStorageUnlock(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.gem.storage.locked"), true);
            return heldStack;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
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
                    Component.translatable("message.typemoonworld.mana.overload.warning", (int) vars.player_mana, (int) vars.player_max_mana).withStyle(color),
                    true
            );
        }

        player.displayClientMessage(Component.translatable("message.typemoonworld.gem.storage.withdrawn", manaAmount), true);
        return result;
    }

    private static ItemStack replaceHeldSingle(ServerPlayer player, InteractionHand hand, ItemStack heldStack, ItemStack replacement) {
        if (heldStack.getCount() <= 1) {
            player.setItemInHand(hand, replacement);
            return replacement;
        }

        heldStack.shrink(1);
        if (!player.addItem(replacement.copy())) {
            player.drop(replacement.copy(), false);
        }
        return player.getItemInHand(hand);
    }

    private static int getStorageManaAmount(ItemStack stack, GemType type, GemQuality quality) {
        String engravedMagicId = GemEngravingService.getEngravedMagicId(stack);
        if (engravedMagicId == null) {
            int baseCapacity = quality.getCapacity(type);
            return baseCapacity;
        }

        double engravedCost = GemEngravingService.getEngravedManaCost(stack);
        if (engravedCost <= 0.0D) {
            return switch (engravedMagicId) {
                case "gravity_magic" -> 20;
                case "reinforcement" -> (int) Math.ceil(
                        GemEngravingService.calculateReinforcementManaCost(
                                GemEngravingService.getReinforcementLevel(stack, 1)
                        )
                );
                default -> 100;
            };
        }

        return Math.max(1, (int) Math.ceil(engravedCost));
    }
}

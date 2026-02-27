package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Using_mana {
    public static void execute(Entity entity) {
        if (entity == null) {
            return;
        }

        // Allow overload: just add mana.
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.player_mana += 10;
        vars.syncMana(entity);

        if (entity instanceof Player player) {
            ItemStack toRemove = new ItemStack(ModItems.MAGIC_FRAGMENTS.get());
            player.getInventory().clearOrCountMatchingItems(
                    p -> toRemove.getItem() == p.getItem(),
                    1,
                    player.inventoryMenu.getCraftSlots()
            );

            double current = vars.player_mana;
            double max = vars.player_max_mana;
            if (current > max && !player.level().isClientSide()) {
                ChatFormatting color = ChatFormatting.YELLOW;
                if (current > max * 1.25) {
                    color = ChatFormatting.DARK_RED;
                } else if (current > max * 1.2) {
                    color = ChatFormatting.RED;
                }
                player.displayClientMessage(
                        Component.translatable("message.typemoonworld.mana.overload.warning", (int) current, (int) max)
                                .withStyle(color),
                        true
                );
            }
        }
    }
}

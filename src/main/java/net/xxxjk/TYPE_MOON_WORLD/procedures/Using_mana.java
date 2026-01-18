package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Using_mana {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        if (entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana < entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana) {
            {
                TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                _vars.player_mana = Math.min(entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana
                        + 10, entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana);
                _vars.syncPlayerVariables(entity);
            }
            if (entity instanceof Player _player) {
                ItemStack _stktoremove = new ItemStack(ModItems.MAGIC_FRAGMENTS.get());
                _player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem()
                        == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
            }
        } else {
            if (entity instanceof Player _player && !_player.level().isClientSide())
                _player.displayClientMessage(Component.literal("魔力已满"), true);
        }
    }
}

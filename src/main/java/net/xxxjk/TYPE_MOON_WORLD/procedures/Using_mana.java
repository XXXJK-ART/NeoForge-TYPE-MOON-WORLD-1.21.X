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
        
        // Removed the upper limit check to allow overload
        {
            TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            // Add mana without capping at max_mana
            _vars.player_mana = _vars.player_mana + 10;
            _vars.syncMana(entity);
        }
        
        if (entity instanceof Player _player) {
            ItemStack _stktoremove = new ItemStack(ModItems.MAGIC_FRAGMENTS.get());
            _player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem()
                    == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
            
            // Add feedback for overload
            double current = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana;
            double max = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana;
            if (current > max && !_player.level().isClientSide()) {
                 String color = "\u00A7e"; // Yellow
                 if (current > max * 1.25) {
                      color = "\u00A74"; // Dark Red
                 } else if (current > max * 1.2) {
                      color = "\u00A7c"; // Red
                 }
                 _player.displayClientMessage(Component.literal(color + "警告：魔力过载！ (" + (int)current + "/" + (int)max + ")"), true);
            }
        }
    }
}

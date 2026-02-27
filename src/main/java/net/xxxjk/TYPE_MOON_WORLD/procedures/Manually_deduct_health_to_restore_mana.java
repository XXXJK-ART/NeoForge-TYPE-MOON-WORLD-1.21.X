package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Manually_deduct_health_to_restore_mana {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        if (entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana != entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana) {
            if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 1) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    // Allow Overload: Just add mana, don't cap at Max if we are already overloading or want to overload.
                    // Previous logic capped at Max, causing Overload (>Max) to drop to Max.
                    _vars.player_mana = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana + 10;
                    _vars.syncMana(entity);
                }
                LivingEntity _entity = (LivingEntity) entity;
                LivingEntity _livEnt = (LivingEntity) entity;
                _entity.setHealth((float) (_livEnt.getHealth() - 1));
            } else {
                if (entity instanceof Player _player && !_player.level().isClientSide())
                    _player.displayClientMessage(Component.translatable("message.typemoonworld.health.depleted"), true);
            }
        } else {
            if (entity instanceof Player _player && !_player.level().isClientSide())
                _player.displayClientMessage(Component.translatable("message.typemoonworld.mana.already_full"), true);
        }
    }

}

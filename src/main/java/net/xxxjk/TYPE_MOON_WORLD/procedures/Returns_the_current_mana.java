package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Returns_the_current_mana {
    public static String execute(Entity entity) {
        if (entity == null)
            return "";
        return entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana + "/" + entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana;
    }
}


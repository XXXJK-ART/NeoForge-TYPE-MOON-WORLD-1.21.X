package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Back_max_mana {
    public static String execute(Entity entity) {
        if (entity == null)
            return "";
        return "魔力值上限：%s".formatted(entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana);
    }
}

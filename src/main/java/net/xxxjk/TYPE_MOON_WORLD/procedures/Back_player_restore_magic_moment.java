package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Back_player_restore_magic_moment {
    public static String execute(Entity entity) {
        if (entity == null)
            return "";
        return "每%s秒回单位魔力".formatted(entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_restore_magic_moment / 20);
    }
}

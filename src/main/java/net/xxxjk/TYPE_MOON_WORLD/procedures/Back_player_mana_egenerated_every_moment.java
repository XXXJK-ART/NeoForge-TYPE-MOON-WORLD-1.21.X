package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Back_player_mana_egenerated_every_moment {
    public static String execute(Entity entity) {
        if (entity == null) {
            return "";
        }
        return "%.1f".formatted(entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana_egenerated_every_moment);
    }
}

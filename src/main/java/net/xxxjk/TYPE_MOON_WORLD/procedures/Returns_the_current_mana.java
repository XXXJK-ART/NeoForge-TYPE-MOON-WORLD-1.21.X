package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class Returns_the_current_mana {
   public static String execute(Entity entity) {
      return entity == null
         ? ""
         : (int)((TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).player_mana
            + "/"
            + (int)((TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).player_max_mana;
   }
}

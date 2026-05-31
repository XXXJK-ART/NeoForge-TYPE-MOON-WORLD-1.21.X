package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class ToggleMagicCircuit {
   public static void execute(Entity entity) {
      if (entity != null) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean newState = !vars.is_magic_circuit_open;
         vars.is_magic_circuit_open = newState;
         if (newState) {
            vars.magic_circuit_open_timer = 0.0;
         } else {
            vars.magic_circuit_open_timer = 0.0;
         }

         vars.syncMana(entity);
      }
   }
}

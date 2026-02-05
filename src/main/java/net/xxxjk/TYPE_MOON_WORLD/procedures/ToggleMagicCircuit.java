package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class ToggleMagicCircuit {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean newState = !vars.is_magic_circuit_open;
        
        vars.is_magic_circuit_open = newState;
        if (newState) {
            vars.magic_circuit_open_timer = 0;
            // The prompt "Current Magic: " will be handled by the overlay
        } else {
            vars.magic_circuit_open_timer = 0;
        }
        vars.syncMana(entity);
    }
}

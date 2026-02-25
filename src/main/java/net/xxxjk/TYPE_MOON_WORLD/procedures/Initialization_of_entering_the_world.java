package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
@SuppressWarnings("null")
public class Initialization_of_entering_the_world {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        execute(event, event.getEntity());
    }

    public static void execute(Entity entity) {
        execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        if (entity == null)
            return;
        if (entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana == 0) {
            {
                TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                _vars.player_max_mana = 0; // Initialize to 0 until awakened
                _vars.player_mana = 0;
                _vars.syncPlayerVariables(entity);
            }
        }
    }
}


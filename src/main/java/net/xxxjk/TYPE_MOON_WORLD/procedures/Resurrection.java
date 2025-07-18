package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class Resurrection {
    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        execute(event, event.getEntity().level(), event.getEntity());
    }

    public static void execute(LevelAccessor world, Entity entity) {
        execute(null, world, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
        if (entity == null)
            return;
        {
            TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            _vars.player_mana = 0;
            _vars.syncPlayerVariables(entity);
        }
        Restore_mana.execute(world, entity);
    }
}
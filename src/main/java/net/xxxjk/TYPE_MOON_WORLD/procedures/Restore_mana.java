package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

import javax.annotation.Nullable;

@EventBusSubscriber
public class Restore_mana {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        execute(event, event.getEntity().level(), event.getEntity());
    }

    public static void execute(LevelAccessor world, Entity entity) {
        execute(null, world, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
        if (entity == null)
            return;
        if (entity.isAlive()) {
            {
                TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                _vars.player_mana = Math.min(entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana + entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana_egenerated_every_moment,
                        entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana);
                _vars.syncPlayerVariables(entity);
            }
            TYPE_MOON_WORLD.queueServerWork((int) entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_restore_magic_moment, () -> {
                if (entity instanceof Player _player && !_player.level().isClientSide())
                    _player.displayClientMessage(Component.literal(("魔力存量" + entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana + "\n" + "魔力上限"
                            + entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_max_mana + "\n" + "每单位回魔" + entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana_egenerated_every_moment + "\n"
                            + "每" + entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_restore_magic_moment / 20 + "s" + "回一单位魔力")), false);
                Restore_mana.execute(world, entity);
            });
        }
    }
}

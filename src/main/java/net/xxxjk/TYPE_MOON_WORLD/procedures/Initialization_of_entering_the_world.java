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
                _vars.player_max_mana = Mth.nextInt(RandomSource.create(), 100, 1000);
                _vars.syncPlayerVariables(entity);
            }
            {
                TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                _vars.player_mana_egenerated_every_moment = Mth.nextInt(RandomSource.create(), 1, 10);
                _vars.syncPlayerVariables(entity);
            }
            {
                TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                _vars.player_restore_magic_moment = Mth.nextInt(RandomSource.create(), 20, 100);
                _vars.syncPlayerVariables(entity);
            }
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 50) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_earth = true;
                    _vars.syncPlayerVariables(entity);
                }
            }
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 50) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_water = true;
                    _vars.syncPlayerVariables(entity);
                }
            }
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 50) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_fire = true;
                    _vars.syncPlayerVariables(entity);
                }
            }
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 50) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_wind = true;
                    _vars.syncPlayerVariables(entity);
                }
            }
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 50) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_ether = true;
                    _vars.syncPlayerVariables(entity);
                }
            }
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 80) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_none = true;
                    _vars.syncPlayerVariables(entity);
                }
            }
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 80) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_imaginary_number = true;
                    _vars.syncPlayerVariables(entity);
                }
            }
        }
    }
}


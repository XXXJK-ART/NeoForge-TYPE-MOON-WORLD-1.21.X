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
            boolean hasSpecialAttribute = false;
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 80) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_none = true;
                    _vars.syncPlayerVariables(entity);
                    hasSpecialAttribute = true;
                }
            }
            if (Mth.nextInt(RandomSource.create(), 1, 100) >= 80) {
                {
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    _vars.player_magic_attributes_imaginary_number = true;
                    _vars.syncPlayerVariables(entity);
                    hasSpecialAttribute = true;
                }
            }
            if (!hasSpecialAttribute) {
                // 优化基础属性生成逻辑：减少多属性共存的概率
                // 单一属性概率较高，多属性概率较低
                int attributeCount = 1;
                int roll = Mth.nextInt(RandomSource.create(), 1, 100);
                if (roll > 95) {
                    attributeCount = 2; // 双重属性
                }
                // 极低概率获得三重或更多属性，暂不实现，保持大多数为单一属性

                // 随机选择属性
                // 0: Earth, 1: Water, 2: Fire, 3: Wind, 4: Ether
                java.util.List<Integer> availableAttributes = new java.util.ArrayList<>(java.util.Arrays.asList(0, 1, 2, 3, 4));
                java.util.Collections.shuffle(availableAttributes);

                for (int i = 0; i < attributeCount; i++) {
                    int attr = availableAttributes.get(i);
                    TypeMoonWorldModVariables.PlayerVariables _vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    switch (attr) {
                        case 0:
                            _vars.player_magic_attributes_earth = true;
                            break;
                        case 1:
                            _vars.player_magic_attributes_water = true;
                            break;
                        case 2:
                            _vars.player_magic_attributes_fire = true;
                            break;
                        case 3:
                            _vars.player_magic_attributes_wind = true;
                            break;
                        case 4:
                            _vars.player_magic_attributes_ether = true;
                            break;
                    }
                    _vars.syncPlayerVariables(entity);
                }
            }
        }
    }
}


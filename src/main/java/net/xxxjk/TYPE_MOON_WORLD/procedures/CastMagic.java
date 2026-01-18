package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@SuppressWarnings("null")
public class CastMagic {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
        
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.is_magic_circuit_open) {
            if (entity instanceof Player player && !player.level().isClientSide()) {
                 player.displayClientMessage(Component.literal("魔术回路未开启"), true);
            }
            return;
        }

        if (vars.selected_magics.isEmpty()) {
             if (entity instanceof Player player && !player.level().isClientSide()) {
                 player.displayClientMessage(Component.literal("未选择魔术"), true);
            }
            return;
        }

        int index = vars.current_magic_index;
        if (index >= 0 && index < vars.selected_magics.size()) {
            String magicId = vars.selected_magics.get(index);
            if ("ruby_throw".equals(magicId)) {
                MagicRubyThrow.execute(entity);
            } else if ("sapphire_throw".equals(magicId)) {
                MagicSapphireThrow.execute(entity);
            } else if ("emerald_use".equals(magicId)) {
                MagicEmeraldUse.execute(entity);
            } else if ("topaz_throw".equals(magicId)) {
                MagicTopazThrow.execute(entity);
            }
        }
    }
}

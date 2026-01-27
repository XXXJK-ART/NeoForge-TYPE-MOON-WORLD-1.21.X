package net.xxxjk.TYPE_MOON_WORLD.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyFlameSword;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireWinterFrost;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldUse;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldWinterRiver;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazThrow;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazReinforcement;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.MagicProjection;

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

        if (vars.magic_cooldown > 0) {
            // Cooldown active, do nothing
            return;
        }

        int index = vars.current_magic_index;
        if (index >= 0 && index < vars.selected_magics.size()) {
            String magicId = vars.selected_magics.get(index);
            boolean castSuccess = false;
            
            if ("ruby_throw".equals(magicId)) {
                MagicRubyThrow.execute(entity);
                castSuccess = true;
            } else if ("sapphire_throw".equals(magicId)) {
                MagicSapphireThrow.execute(entity);
                castSuccess = true;
            } else if ("emerald_use".equals(magicId)) {
                MagicEmeraldUse.execute(entity);
                castSuccess = true;
            } else if ("topaz_throw".equals(magicId)) {
                MagicTopazThrow.execute(entity);
                castSuccess = true;
            } else if ("ruby_flame_sword".equals(magicId)) {
                MagicRubyFlameSword.execute(entity);
                castSuccess = true;
            } else if ("sapphire_winter_frost".equals(magicId)) {
                MagicSapphireWinterFrost.execute(entity);
                castSuccess = true;
            } else if ("emerald_winter_river".equals(magicId)) {
                MagicEmeraldWinterRiver.execute(entity);
                castSuccess = true;
            } else if ("topaz_reinforcement".equals(magicId)) {
                MagicTopazReinforcement.execute(entity);
                castSuccess = true;
            } else if ("projection".equals(magicId)) {
                MagicProjection.execute(entity);
                castSuccess = true;
            }
            
            if (castSuccess) {
                vars.magic_cooldown = 20; // 1 second cooldown (20 ticks)
                vars.syncPlayerVariables(entity);
            }
        }
    }
}

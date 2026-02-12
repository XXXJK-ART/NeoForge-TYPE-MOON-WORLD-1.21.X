package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.ruby.MagicRubyFlameSword;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.sapphire.MagicSapphireWinterFrost;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.emerald.MagicEmeraldWinterRiver;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.topaz.MagicTopazReinforcement;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.cyan.MagicCyanWind;

public class MagicJewelRelease {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int mode = vars.jewel_magic_mode;
        
        switch (mode) {
            case 0: // Ruby
                MagicRubyFlameSword.execute(player);
                break;
            case 1: // Sapphire
                MagicSapphireWinterFrost.execute(player);
                break;
            case 2: // Emerald
                MagicEmeraldWinterRiver.execute(player);
                break;
            case 3: // Topaz
                MagicTopazReinforcement.execute(player);
                break;
            case 4: // Cyan
                MagicCyanWind.execute(player);
                break;
            default:
                MagicRubyFlameSword.execute(player);
                break;
        }
    }
}

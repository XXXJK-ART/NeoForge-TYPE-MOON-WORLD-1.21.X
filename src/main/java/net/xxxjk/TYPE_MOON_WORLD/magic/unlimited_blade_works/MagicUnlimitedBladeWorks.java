package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicUnlimitedBladeWorks {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        // Check if unlocked
        if (!vars.has_unlimited_blade_works) {
             // Should not happen if keybind is restricted, but just in case
             return;
        }

        // Toggle UBW or Cancel Chant
        if (vars.is_chanting_ubw) {
            // Cancel Chant
            vars.is_chanting_ubw = false;
            vars.ubw_chant_progress = 0;
            vars.ubw_chant_timer = 0;
            vars.syncPlayerVariables(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.cancelled"), true);
            return;
        }

        // Check if player is actually in UBW dimension
        boolean isInUBWDimension = player.level().dimension().location().equals(net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions.UBW_KEY.location());

        if (vars.is_in_ubw || isInUBWDimension) {
            // Exit UBW - Use ChantHandler logic to ensure entities are returned and cleanup is done
            ChantHandler.returnFromUBW(player, vars);
            player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.exit"), true);
        } else {
            // Start Chant for UBW
            double initialCost = 50.0;
            if (ManaHelper.consumeManaOrHealth(player, initialCost)) {
                vars.is_chanting_ubw = true;
                vars.ubw_chant_progress = 1;
                vars.ubw_chant_timer = 0;
                
                // Save current location now, just in case (though it should be saved at activation really, but saving here is safer if they move?)
                // Actually, let's save at activation to be precise. But we can save here too.
                // Let's stick to saving at activation in ChantHandler to ensure we return to where they were when they ENTERED, not when they started chanting.
                
                vars.syncPlayerVariables(player);
                player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.chant"), true);
            }
        }
        
        vars.syncPlayerVariables(player);
    }
    
    public static void checkUnlock(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        // Auto unlock if player has Sword Attribute and hasn't unlocked it yet
        if (vars.player_magic_attributes_sword && !vars.has_unlimited_blade_works) {
            vars.has_unlimited_blade_works = true;
            vars.syncPlayerVariables(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.unlocked"), false);
        }
    }
}

package net.xxxjk.TYPE_MOON_WORLD.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class ManaHelper {

    /**
     * Attempts to consume mana. If mana is insufficient, consumes health instead.
     * 
     * @param player The player attempting to use magic.
     * @param amount The amount of mana required.
     * @return true if the cost was paid (either by mana or health), false if the player died or couldn't pay.
     */
    public static boolean consumeManaOrHealth(ServerPlayer player, double amount) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        if (vars.player_mana >= amount) {
            vars.player_mana -= amount;
            vars.syncPlayerVariables(player);
            return true;
        } else {
            // Not enough mana, check health
            double missingMana = amount - vars.player_mana;
            // Conversion rate: 1 Health (0.5 Hearts) = 10 Mana? Or 1:1?
            // Usually magic mods do something like 1 Health = X Mana.
            // Let's assume a "Life Force" conversion.
            // For now, let's say 1 Health Point = 20 Mana. (So half a heart = 20 mana)
            // Or maybe simpler: 1 Health = 10 Mana.
            // Let's stick to a configurable-like constant: 1 HP = 10 Mana.
            double healthCost = missingMana / 10.0;
            
            // If cost is less than 1 HP (e.g. 5 mana missing = 0.5 HP), it still hurts.
            if (healthCost < 0.5) healthCost = 0.5; 
            
            if (player.getHealth() > healthCost) {
                // Consume all remaining mana first
                vars.player_mana = 0;
                vars.syncPlayerVariables(player);
                
                // Damage player
                // We need a damage source. MAGIC is appropriate.
                DamageSource source = player.damageSources().magic();
                player.hurt(source, (float)healthCost);
                
                player.displayClientMessage(Component.translatable("message.typemoonworld.mana.health_conversion", String.format("%.1f", healthCost), (int)amount), true);
                return true;
            } else {
                // Not enough health either (would die)
                // Do we allow suicide? Usually no for casting unless specified.
                player.displayClientMessage(Component.translatable("message.typemoonworld.mana.not_enough_health"), true);
                return false;
            }
        }
    }
}

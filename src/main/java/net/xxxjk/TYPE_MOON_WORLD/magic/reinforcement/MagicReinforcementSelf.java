package net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

import java.util.ArrayList;

public class MagicReinforcementSelf {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        // Calculate Max Level based on Proficiency (1 level per 20 proficiency, max 5)
        int maxLevel = Math.min(5, 1 + (int)(vars.proficiency_reinforcement / 20));
        int requestLevel = vars.reinforcement_level == 0 ? 1 : vars.reinforcement_level;
        int level = Math.min(requestLevel, maxLevel);
        
        // Mana Cost
        double cost = 20.0 * level;
        
        // Duration: Base 30s + (Proficiency * 0.5s) * Level
        int duration = (600 + (int)(vars.proficiency_reinforcement * 10)) * level;
        int amplifier = level - 1;

        MobEffectInstance effect = null;
        MobEffectInstance extraEffect = null;

        switch (vars.reinforcement_mode) {
            case 0: // Body
                effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_DEFENSE, duration, amplifier, false, false, true);
                break;
            case 1: // Hand
                effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_STRENGTH, duration, amplifier, false, false, true);
                break;
            case 2: // Leg
                effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_AGILITY, duration, amplifier, false, false, true);
                break;
            case 3: // Eye
                effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_SIGHT, duration, amplifier, false, false, true);
                extraEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false); // Night vision doesn't need amplifier, hide icon
                break;
        }

        // Check if effect is already present with higher or equal amplifier
        if (effect != null) {
            MobEffectInstance existing = player.getEffect(effect.getEffect());
            if (existing != null && existing.getAmplifier() >= effect.getAmplifier() && existing.getDuration() >= duration) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.already_better", existing.getAmplifier() + 1), true);
                return; // No mana cost if no improvement
            }
        }

        if (!ManaHelper.consumeManaOrHealth(player, cost)) {
            return;
        }

        if (effect != null) {
            player.addEffect(effect);
        }
        if (extraEffect != null) {
            player.addEffect(extraEffect);
        }
        
        // Track caster for cancellation logic
        TypeMoonWorldModVariables.ReinforcementData data = player.getData(TypeMoonWorldModVariables.REINFORCEMENT_DATA);
        data.casterUUID = player.getUUID();

        // Display success message after consumption
        switch (vars.reinforcement_mode) {
            case 0: player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.body"), true); break;
            case 1: player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.arm"), true); break;
            case 2: player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.leg"), true); break;
            case 3: player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.eye"), true); break;
        }
        
        // Increase Proficiency
        vars.proficiency_reinforcement = Math.min(100.0, vars.proficiency_reinforcement + 0.5);
        vars.syncPlayerVariables(player);
    }
}

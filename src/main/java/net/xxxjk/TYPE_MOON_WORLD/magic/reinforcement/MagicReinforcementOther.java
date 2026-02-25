package net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicReinforcementOther {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        // Raytrace to find target
        Entity targetEntity = null;
        HitResult hitResult = net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.getRayTraceTarget(player, 10.0D);
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            targetEntity = ((EntityHitResult) hitResult).getEntity();
        }

        if (!(targetEntity instanceof LivingEntity livingTarget)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.no_target"), true);
            return;
        }

        // Calculate Max Level based on Proficiency
        int maxLevel = Math.min(5, 1 + (int)(vars.proficiency_reinforcement / 20));
        int requestLevel = vars.reinforcement_level == 0 ? 1 : vars.reinforcement_level;
        int level = Math.min(requestLevel, maxLevel);

        // Mana Cost (Higher for Other)
        double cost = 30.0 * level;

        // Duration
        int duration = (600 + (int)(vars.proficiency_reinforcement * 10)) * level; 

        // Apply Effect based on Body Part
        String partKey = "";
        int amplifier = level - 1;
        
        MobEffectInstance effect = null;
        MobEffectInstance extraEffect = null;

        switch (vars.reinforcement_mode) {
            case 0: // Body -> Defense
                effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE, duration, amplifier, false, false, true);
                partKey = "body";
                break;
            case 1: // Hand -> Strength
                effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH, duration, amplifier, false, false, true);
                partKey = "arm";
                break;
            case 2: // Leg -> Speed
                effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_OTHER_AGILITY, duration, amplifier, false, false, true);
                partKey = "leg";
                break;
            case 3: // Eye -> Sight
                effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_OTHER_SIGHT, duration, amplifier, false, false, true);
                // Hide vanilla Night Vision icon
                extraEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false); 
                partKey = "eye";
                break;
        }

        // Check if effect is already present with higher or equal amplifier on target
        if (effect != null) {
            MobEffectInstance existing = livingTarget.getEffect(effect.getEffect());
            if (existing != null && existing.getAmplifier() >= effect.getAmplifier() && existing.getDuration() >= duration) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.already_better", existing.getAmplifier() + 1), true);
                return; // No mana cost if no improvement
            }
        }

        if (!ManaHelper.consumeManaOrHealth(player, cost)) {
            return;
        }
        
        if (effect != null) livingTarget.addEffect(effect);
        if (extraEffect != null) livingTarget.addEffect(extraEffect);

        // Track caster for cancellation logic
        TypeMoonWorldModVariables.ReinforcementData targetData = livingTarget.getData(TypeMoonWorldModVariables.REINFORCEMENT_DATA);
        targetData.casterUUID = player.getUUID();

        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.success", livingTarget.getDisplayName()), true);
        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement." + partKey), true);

        // Increase Proficiency
        vars.proficiency_reinforcement = Math.min(100.0, vars.proficiency_reinforcement + 0.5);
        vars.syncPlayerVariables(player);
    }
}

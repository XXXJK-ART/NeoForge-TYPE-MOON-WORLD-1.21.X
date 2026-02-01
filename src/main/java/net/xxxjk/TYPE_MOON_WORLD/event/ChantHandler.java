package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class ChantHandler {

    // Chant intervals (in ticks). Assuming 20 ticks = 1 second.
    // Total lines: 8 lines + 1 activation
    // Line 1: I am the bone of my sword. (Triggered on start)
    // Line 2: Steel is my body, and fire is my blood.
    // Line 3: I have created over a thousand blades.
    // Line 4: Unknown to Death.
    // Line 5: Nor known to Life.
    // Line 6: Have withstood pain to create many weapons.
    // Line 7: Yet, those hands will never hold anything.
    // Line 8: So as I pray, Unlimited Blade Works.
    private static final int CHANT_INTERVAL = 40; // 2 seconds between lines

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            if (vars.is_chanting_ubw) {
                // Apply Slowness during chant
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false));
                
                vars.ubw_chant_timer++;
                
                if (vars.ubw_chant_timer >= CHANT_INTERVAL) {
                    vars.ubw_chant_timer = 0;
                    vars.ubw_chant_progress++;
                    
                    // Process Chant Steps
                    processChantStep(player, vars);
                }
                
                // Sync variable occasionally or when state changes (handled in processChantStep)
            }
        }
    }
    
    private static void processChantStep(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        int progress = vars.ubw_chant_progress;
        double cost = 15.0;
        String chantText = "";
        
        // Line 1: 10 mana (handled in MagicUnlimitedBladeWorks)
        // Lines 2-7: 15 mana each
        // Line 8: 400 mana
        // Total steps: 1 (start), 2, 3, 4, 5, 6, 7, 8 (final chant), 9 (activation)
        
        if (progress == 2) {
            chantText = "§bSteel is my body, and fire is my blood.";
        } else if (progress == 3) {
            chantText = "§bI have created over a thousand blades.";
        } else if (progress == 4) {
            chantText = "§bUnknown to Death.";
        } else if (progress == 5) {
            chantText = "§bNor known to Life.";
        } else if (progress == 6) {
            chantText = "§bHave withstood pain to create many weapons.";
        } else if (progress == 7) {
            chantText = "§bYet, those hands will never hold anything.";
        } else if (progress == 8) {
            chantText = "§cSo as I pray——";
            cost = 400.0;
        } else if (progress > 8) {
            // Activation Phase
            activateUBW(player, vars);
            return;
        }
        
        // Consume Cost
        if (ManaHelper.consumeManaOrHealth(player, cost)) {
            player.displayClientMessage(Component.literal(chantText), true);
            vars.syncPlayerVariables(player);
        } else {
            // Not enough mana, interrupt
            interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.mana_depleted");
        }
    }
    
    private static void activateUBW(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        vars.is_chanting_ubw = false;
        vars.ubw_chant_progress = 0;
        vars.ubw_chant_timer = 0;
        vars.syncPlayerVariables(player);
        
        player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.activated"), true);
        
        // Actual Activation Logic (Dimension TP or Structure Gen) will go here later
        // For now, maybe a sound or particle effect?
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
    }
    
    private static void interruptChant(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String reasonKey) {
        vars.is_chanting_ubw = false;
        vars.ubw_chant_progress = 0;
        vars.ubw_chant_timer = 0;
        vars.syncPlayerVariables(player);
        
        player.displayClientMessage(Component.translatable(reasonKey), true);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            if (vars.is_chanting_ubw) {
                float damage = event.getNewDamage();
                // Threshold for interruption: 4.0 damage (2 hearts)
                if (damage >= 4.0f) {
                    interruptChant(player, vars, "message.typemoonworld.unlimited_blade_works.interrupted");
                }
            }
        }
    }
}

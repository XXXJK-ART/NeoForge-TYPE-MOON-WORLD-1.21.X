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
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicProjection;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicStructuralAnalysis;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.MagicBrokenPhantasm;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.MagicUnlimitedBladeWorks;

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
            
            // Validate if magic is learned
            if (!vars.learned_magics.contains(magicId)) {
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_learned"), true);
                }
                return;
            }
            
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
            } else if ("structural_analysis".equals(magicId)) {
                MagicStructuralAnalysis.execute(entity);
                castSuccess = true;
            } else if ("broken_phantasm".equals(magicId)) {
                MagicBrokenPhantasm.execute(entity);
                castSuccess = true;
            } else if ("unlimited_blade_works".equals(magicId)) {
                MagicUnlimitedBladeWorks.execute(entity);
                castSuccess = true;
            }
            
            if (castSuccess) {
                // Default Cooldown 10 ticks (0.5s)
                double cooldown = 10;
                
                // Jewel Magic Cooldown Logic
                if (vars.selected_magics.get(index).startsWith("ruby") || 
                    vars.selected_magics.get(index).startsWith("sapphire") || 
                    vars.selected_magics.get(index).startsWith("emerald") || 
                    vars.selected_magics.get(index).startsWith("topaz")) {
                    
                    // Base 20 ticks (1s)
                    cooldown = 20;
                    
                    // Reduce by proficiency
                    // 0 proficiency = 20 ticks
                    // 100 proficiency = 0 ticks (instant) -> Clamp to min 1 tick
                    // Reduction: proficiency * 0.2 ticks per level? 
                    // 100 * 0.2 = 20 ticks reduction. Perfect.
                    cooldown = Math.max(1, cooldown - (vars.proficiency_jewel_magic * 0.2));
                    
                    // Increase proficiency slightly on use
                    vars.proficiency_jewel_magic = Math.min(100, vars.proficiency_jewel_magic + 0.1);
                }
                
                vars.magic_cooldown = cooldown;
                vars.syncPlayerVariables(entity);
            }
        }
    }
}

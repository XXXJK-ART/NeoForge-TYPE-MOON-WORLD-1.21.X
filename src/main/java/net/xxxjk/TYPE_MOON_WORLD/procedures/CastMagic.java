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
import net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementSelf;
import net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementOther;
import net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicProjection;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicStructuralAnalysis;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.MagicBrokenPhantasm;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.MagicUnlimitedBladeWorks;

@SuppressWarnings("null")
public class CastMagic {
    public static void execute(Entity entity) {
        if (entity == null)
            return;
            
        // Magic casting should only happen on server side
        if (entity.level().isClientSide())
            return;
        
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.is_magic_circuit_open) {
            if (entity instanceof Player player && !player.level().isClientSide()) {
                 player.displayClientMessage(Component.translatable("message.typemoonworld.magic.circuit_not_open"), true);
            }
            return;
        }

        if (vars.selected_magics.isEmpty()) {
             if (entity instanceof Player player && !player.level().isClientSide()) {
                 player.displayClientMessage(Component.translatable("message.typemoonworld.magic.no_magic_selected"), true);
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
            } else if ("reinforcement".equals(magicId)) {
                switch (vars.reinforcement_target) {
                    case 0: // Self
                        MagicReinforcementSelf.execute(entity);
                        break;
                    case 1: // Other
                        MagicReinforcementOther.execute(entity);
                        break;
                    case 2: // Item
                        MagicReinforcementItem.execute(entity);
                        break;
                    case 3: // Cancel
                        if (entity instanceof Player player) {
                            // Sub-selection for cancel
                            int cancelType = vars.reinforcement_mode; // Reuse reinforcement_mode for cancel type? Or add new var?
                            // Let's use mode to decide what to cancel
                            if (cancelType == 0) {
                                // Cancel Self
                                TypeMoonWorldModVariables.ReinforcementData data = player.getData(TypeMoonWorldModVariables.REINFORCEMENT_DATA);
                                if (player.getUUID().equals(data.casterUUID)) {
                                    player.removeEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.REINFORCEMENT_SELF_STRENGTH);
                                    player.removeEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.REINFORCEMENT_SELF_DEFENSE);
                                    player.removeEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.REINFORCEMENT_SELF_AGILITY);
                                    player.removeEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.REINFORCEMENT_SELF_SIGHT);
                                    player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                                    data.casterUUID = null;
                                    player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.self.success"), true);
                                } else {
                                    player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.other.not_yours"), true);
                                }
                            } else if (cancelType == 1) {
                                // Cancel Other
                                net.minecraft.world.phys.HitResult hitResult = net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.getRayTraceTarget((net.minecraft.server.level.ServerPlayer) player, 10.0D);
                                if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                                    net.minecraft.world.entity.Entity target = ((net.minecraft.world.phys.EntityHitResult) hitResult).getEntity();
                                    if (target instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
                                        TypeMoonWorldModVariables.ReinforcementData data = livingTarget.getData(TypeMoonWorldModVariables.REINFORCEMENT_DATA);
                                        if (player.getUUID().equals(data.casterUUID)) {
                                            livingTarget.removeEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
                                            livingTarget.removeEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
                                            livingTarget.removeEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
                                            livingTarget.removeEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
                                            livingTarget.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                                            data.casterUUID = null;
                                            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.other.success", livingTarget.getDisplayName()), true);
                                        } else {
                                            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.other.not_yours"), true);
                                        }
                                    }
                                } else {
                                    player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.no_target"), true);
                                }
                            } else if (cancelType == 2) {
                                // Cancel Item
                                net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
                                if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                                    net.minecraft.nbt.CompoundTag tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
                                    if (tag.getBoolean("Reinforced")) {
                                        if (tag.hasUUID("CasterUUID") && player.getUUID().equals(tag.getUUID("CasterUUID"))) {
                                            net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement.MagicReinforcementEventHandler.removeReinforcement(player, stack, tag);
                                            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.item.success"), true);
                                        } else {
                                            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.other.not_yours"), true);
                                        }
                                    } else {
                                        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.item.not_reinforced"), true);
                                    }
                                } else if (!stack.isEmpty()) {
                                    player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.cancel.item.not_reinforced"), true);
                                }
                            }
                        }
                        break;
                }
                castSuccess = true;
            } else if ("reinforcement_self".equals(magicId)) {
                MagicReinforcementSelf.execute(entity);
                castSuccess = true;
            } else if ("reinforcement_other".equals(magicId)) {
                MagicReinforcementOther.execute(entity);
                castSuccess = true;
            } else if ("reinforcement_item".equals(magicId)) {
                MagicReinforcementItem.execute(entity);
                castSuccess = true;
            } else if ("jewel_magic_shoot".equals(magicId)) {
                net.xxxjk.TYPE_MOON_WORLD.magic.jewel.MagicJewelShoot.execute(entity);
                castSuccess = true;
            } else if ("jewel_magic_release".equals(magicId)) {
                net.xxxjk.TYPE_MOON_WORLD.magic.jewel.MagicJewelRelease.execute(entity);
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
            } else if ("sword_barrel_full_open".equals(magicId)) {
                net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.MagicSwordBarrelFullOpen.execute(entity);
                castSuccess = true;
            }
            
            if (castSuccess) {
                // Default Cooldown 10 ticks (0.5s)
                double cooldown = 10;
                
                // Jewel Magic Cooldown Logic (Proficiency dependent)
                if (vars.selected_magics.get(index).startsWith("jewel_magic_shoot")) {
                    // Base 20 ticks (1s)
                    double baseCooldown = 20;
                    cooldown = Math.max(1, baseCooldown - (vars.proficiency_jewel_magic_shoot * 0.2));
                    
                    // Increase proficiency slightly on use
                    vars.proficiency_jewel_magic_shoot = Math.min(100, vars.proficiency_jewel_magic_shoot + 0.1);
                    vars.syncProficiency(entity);
                } else if (vars.selected_magics.get(index).startsWith("jewel_magic_release")) {
                     // Base 20 ticks (1s)
                    double baseCooldown = 20;
                    cooldown = Math.max(1, baseCooldown - (vars.proficiency_jewel_magic_release * 0.2));
                    
                    // Increase proficiency slightly on use
                    vars.proficiency_jewel_magic_release = Math.min(100, vars.proficiency_jewel_magic_release + 0.1);
                    vars.syncProficiency(entity);
                }
                else if (vars.selected_magics.get(index).startsWith("ruby") || 
                    vars.selected_magics.get(index).startsWith("sapphire") || 
                    vars.selected_magics.get(index).startsWith("emerald") || 
                    vars.selected_magics.get(index).startsWith("topaz")) {
                    
                    // Legacy Support
                    double baseCooldown = 20;
                    cooldown = Math.max(1, baseCooldown - (vars.proficiency_jewel_magic_shoot * 0.2));
                } 
                else {
                    // For all other skills, set strict 0.5s cooldown (10 ticks)
                    cooldown = 10;
                }
                
                vars.magic_cooldown = cooldown;
                vars.syncMana(entity);
            }
        }
    }
}

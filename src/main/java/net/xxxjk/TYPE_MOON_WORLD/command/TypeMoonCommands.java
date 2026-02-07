package net.xxxjk.TYPE_MOON_WORLD.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import java.util.List;

@SuppressWarnings({"null", "unchecked"})
public class TypeMoonCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("typemoon")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            
            // Basic Stats
            .then(Commands.literal("stats")
                .then(Commands.literal("mana")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> setMana(ctx, DoubleArgumentType.getDouble(ctx, "value")))))
                .then(Commands.literal("max_mana")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> setMaxMana(ctx, DoubleArgumentType.getDouble(ctx, "value")))))
                .then(Commands.literal("regen")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> setRegen(ctx, DoubleArgumentType.getDouble(ctx, "value")))))
                .then(Commands.literal("restore")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> setRestore(ctx, DoubleArgumentType.getDouble(ctx, "value")))))
            )
            
            // Attributes
            .then(Commands.literal("attr")
                // 5 Basic Elements
                .then(Commands.literal("earth")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAttribute(ctx, "earth", BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("water")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAttribute(ctx, "water", BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("fire")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAttribute(ctx, "fire", BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("wind")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAttribute(ctx, "wind", BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("ether")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAttribute(ctx, "ether", BoolArgumentType.getBool(ctx, "enabled")))))
                
                // 3 Special Elements
                .then(Commands.literal("none")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAttribute(ctx, "none", BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("imaginary_number")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAttribute(ctx, "imaginary_number", BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("sword")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setAttribute(ctx, "sword", BoolArgumentType.getBool(ctx, "enabled")))))
            )

            // Proficiency
            .then(Commands.literal("proficiency")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"structural_analysis", "projection", "jewel_magic", "unlimited_blade_works"}, builder))
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> setProficiency(ctx, StringArgumentType.getString(ctx, "type"), DoubleArgumentType.getDouble(ctx, "value"))))))
            
            // Magic Learning
            .then(Commands.literal("magic")
                .then(Commands.literal("learn")
                    .then(Commands.argument("magic_id", StringArgumentType.string())
                        .executes(ctx -> learnMagic(ctx, StringArgumentType.getString(ctx, "magic_id")))))
                .then(Commands.literal("forget")
                    .then(Commands.argument("magic_id", StringArgumentType.string())
                        .executes(ctx -> forgetMagic(ctx, StringArgumentType.getString(ctx, "magic_id")))))
                .then(Commands.literal("learn_all")
                    .executes(TypeMoonCommands::learnAllMagics))
                .then(Commands.literal("forget_all")
                    .executes(TypeMoonCommands::forgetAllMagics))
            )

            // Clear Shiki Entity
            .then(Commands.literal("shiki")
                .then(Commands.literal("clear")
                    .executes(TypeMoonCommands::clearShiki))
                .then(Commands.literal("health")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0))
                        .executes(ctx -> setShikiHealth(ctx, DoubleArgumentType.getDouble(ctx, "value")))))
            )
        );
    }

    private static int setShikiHealth(CommandContext<CommandSourceStack> ctx, double health) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            int count = 0;
            
            // Iterate over all entities in the level
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof RyougiShikiEntity shiki) {
                    shiki.setHealth((float) health);
                    count++;
                }
            }
            
            final int finalCount = count;
            final double finalHealth = health;
            ctx.getSource().sendSuccess(() -> Component.literal("Set health of " + finalCount + " Ryougi Shiki entities to " + finalHealth), true);
            return count;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error setting health: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearShiki(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            int count = 0;
            
            // Iterate over all entities in the level
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof RyougiShikiEntity shiki) {
                    shiki.discard(); // Force remove, bypassing immunity
                    count++;
                }
            }
            
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + finalCount + " Ryougi Shiki entities."), true);
            return count;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error clearing entities: " + e.getMessage()));
            return 0;
        }
    }

    private static int learnMagic(CommandContext<CommandSourceStack> ctx, String magicId) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            if (!vars.learned_magics.contains(magicId)) {
                vars.learned_magics.add(magicId);
                vars.syncPlayerVariables(player);
                ctx.getSource().sendSuccess(() -> Component.literal("Learned magic: " + magicId), true);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("Magic already learned: " + magicId), false);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int forgetMagic(CommandContext<CommandSourceStack> ctx, String magicId) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            if (vars.learned_magics.contains(magicId)) {
                vars.learned_magics.remove(magicId);
                vars.syncPlayerVariables(player);
                ctx.getSource().sendSuccess(() -> Component.literal("Forgot magic: " + magicId), true);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("Magic not found: " + magicId), false);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static int learnAllMagics(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Add all known magics
            String[] allMagics = {
                "ruby_throw", "sapphire_throw", "emerald_use", "topaz_throw",
                "ruby_flame_sword", "sapphire_winter_frost", "emerald_winter_river", "topaz_reinforcement",
                "projection", "structural_analysis", "broken_phantasm",
                "unlimited_blade_works", "sword_barrel_full_open"
            };
            
            for (String m : allMagics) {
                if (!vars.learned_magics.contains(m)) {
                    vars.learned_magics.add(m);
                }
            }
            
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Learned all magics"), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private static int forgetAllMagics(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            vars.learned_magics.clear();
            
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Forgot all magics"), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setProficiency(CommandContext<CommandSourceStack> ctx, String type, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Limit Proficiency to 0-100
            value = Math.max(0.0, Math.min(100.0, value));
            
            switch (type) {
                case "structural_analysis": vars.proficiency_structural_analysis = value; break;
                case "projection": vars.proficiency_projection = value; break;
                case "jewel_magic": vars.proficiency_jewel_magic = value; break;
                case "unlimited_blade_works": vars.proficiency_unlimited_blade_works = value; break;
            }
            
            vars.syncPlayerVariables(player);
            // Must use final variable for lambda
            final double finalValue = value;
            ctx.getSource().sendSuccess(() -> Component.literal("Set Proficiency " + type + " to " + finalValue), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setMana(CommandContext<CommandSourceStack> ctx, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Limit Mana to 0-100000
            value = Math.max(0.0, Math.min(100000.0, value));
            
            vars.player_mana = value;
            vars.syncPlayerVariables(player);
            final double finalValue = value;
            ctx.getSource().sendSuccess(() -> Component.literal("Set Mana to " + finalValue), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setMaxMana(CommandContext<CommandSourceStack> ctx, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Limit Max Mana to 0-100000
            value = Math.max(0.0, Math.min(100000.0, value));
            
            vars.player_max_mana = value;
            vars.syncPlayerVariables(player);
            final double finalValue = value;
            ctx.getSource().sendSuccess(() -> Component.literal("Set Max Mana to " + finalValue), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setRegen(CommandContext<CommandSourceStack> ctx, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Limit Regen Speed (Interval) to 0s - 60s
            // Assuming the input is in seconds or ticks? Usually commands use human readable units if not specified.
            // But checking the logic, it's likely used as a timer threshold.
            value = Math.max(0.0, Math.min(60.0, value));
            
            vars.player_mana_egenerated_every_moment = value;
            vars.syncPlayerVariables(player);
            final double finalValue = value;
            ctx.getSource().sendSuccess(() -> Component.literal("Set Mana Regen Interval to " + finalValue + "s"), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setRestore(CommandContext<CommandSourceStack> ctx, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Limit Restore Amount to 0 - 10000
            value = Math.max(0.0, Math.min(10000.0, value));
            
            vars.player_restore_magic_moment = value;
            vars.syncPlayerVariables(player);
            final double finalValue = value;
            ctx.getSource().sendSuccess(() -> Component.literal("Set Mana Restore Amount to " + finalValue), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setAttribute(CommandContext<CommandSourceStack> ctx, String attr, boolean enabled) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            switch (attr) {
                case "earth": vars.player_magic_attributes_earth = enabled; break;
                case "water": vars.player_magic_attributes_water = enabled; break;
                case "fire": vars.player_magic_attributes_fire = enabled; break;
                case "wind": vars.player_magic_attributes_wind = enabled; break;
                case "ether": vars.player_magic_attributes_ether = enabled; break;
                case "none": vars.player_magic_attributes_none = enabled; break;
                case "imaginary_number": vars.player_magic_attributes_imaginary_number = enabled; break;
                case "sword": vars.player_magic_attributes_sword = enabled; break;
            }
            
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Set Attribute " + attr + " to " + enabled), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}

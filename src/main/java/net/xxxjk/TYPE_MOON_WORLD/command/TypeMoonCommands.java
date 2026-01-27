package net.xxxjk.TYPE_MOON_WORLD.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

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
        );
    }

    private static int setMana(CommandContext<CommandSourceStack> ctx, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.player_mana = value;
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Set Mana to " + value), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setMaxMana(CommandContext<CommandSourceStack> ctx, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.player_max_mana = value;
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Set Max Mana to " + value), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setRegen(CommandContext<CommandSourceStack> ctx, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.player_mana_egenerated_every_moment = value;
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Set Mana Regen to " + value), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setRestore(CommandContext<CommandSourceStack> ctx, double value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.player_restore_magic_moment = value;
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Set Mana Restore to " + value), true);
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

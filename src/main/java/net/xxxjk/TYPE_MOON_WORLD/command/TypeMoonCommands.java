
package net.xxxjk.TYPE_MOON_WORLD.command;

import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineNoise;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;

@SuppressWarnings({"null", "unchecked"})
public class TypeMoonCommands {
    private static final int DEFAULT_DISTRIBUTION_SAMPLES = 200_000;
    private static final int SAMPLE_COORD_RANGE = 2_000_000;
    private static final double ACCEPT_MEAN_MIN = 9.0;
    private static final double ACCEPT_MEAN_MAX = 11.0;
    private static final double ACCEPT_P80_MAX = 0.10;
    private static final double ACCEPT_P50_79_MAX = 0.20;
    private static final double ACCEPT_P_LT_30_MIN = 0.80;
    private static final double ACCEPT_P_LT_50_MIN = 0.90;

    private static final String[] ALL_MAGICS = {
        "ruby_throw", "sapphire_throw", "emerald_use", "topaz_throw", "cyan_throw",
        "ruby_flame_sword", "sapphire_winter_frost", "emerald_winter_river", "topaz_reinforcement", "cyan_wind",
        "jewel_magic_shoot", "jewel_magic_release",
        "projection", "structural_analysis", "broken_phantasm",
        "unlimited_blade_works", "sword_barrel_full_open",
        "reinforcement_self", "reinforcement_other", "reinforcement_item",
        "gravity_magic"
    };

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
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"structural_analysis", "projection", "jewel_magic", "unlimited_blade_works", "reinforcement", "gravity_magic"}, builder))
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> setProficiency(ctx, StringArgumentType.getString(ctx, "type"), DoubleArgumentType.getDouble(ctx, "value"))))))
            
            // Magic Learning
            .then(Commands.literal("magic")
                .then(Commands.literal("learn")
                    .then(Commands.argument("magic_id", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ALL_MAGICS, builder))
                        .executes(ctx -> learnMagic(ctx, StringArgumentType.getString(ctx, "magic_id")))))
                .then(Commands.literal("forget")
                    .then(Commands.argument("magic_id", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ALL_MAGICS, builder))
                        .executes(ctx -> forgetMagic(ctx, StringArgumentType.getString(ctx, "magic_id")))))
                .then(Commands.literal("learn_all")
                    .executes(TypeMoonCommands::learnAllMagics))
                .then(Commands.literal("forget_all")
                    .executes(TypeMoonCommands::forgetAllMagics))
            )

            // Reset Command
            .then(Commands.literal("reset")
                .executes(TypeMoonCommands::resetPlayer))
            
            // Max Level & All Skills Command
            .then(Commands.literal("max")
                .executes(TypeMoonCommands::setMaxLevel))
            
            // No Cooldown Command (Toggle)
            .then(Commands.literal("cooldown")
                .then(Commands.literal("toggle")
                    .executes(TypeMoonCommands::toggleCooldown)))

            // Clear Shiki Entity
            .then(Commands.literal("shiki")
                .then(Commands.literal("clear")
                    .executes(TypeMoonCommands::clearShiki))
                .then(Commands.literal("health")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0))
                        .executes(ctx -> setShikiHealth(ctx, DoubleArgumentType.getDouble(ctx, "value")))))
            )
            // King Qualification (Advancement)
            .then(Commands.literal("king")
                .then(Commands.literal("grant")
                    .executes(TypeMoonCommands::grantKing))
                .then(Commands.literal("revoke")
                    .executes(TypeMoonCommands::revokeKing))
            )
            .then(Commands.literal("favor")
                .then(Commands.literal("merlin")
                    .then(Commands.argument("value", IntegerArgumentType.integer(-5, 5))
                        .executes(ctx -> setMerlinFavor(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
                .then(Commands.literal("shiki")
                    .then(Commands.argument("value", IntegerArgumentType.integer(-5, 5))
                        .executes(ctx -> setShikiFavor(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
            )
            .then(Commands.literal("leyline")
                .then(Commands.literal("here")
                    .executes(TypeMoonCommands::showLeylineHere))
                .then(Commands.literal("chunk")
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                            .executes(ctx -> showLeylineChunk(
                                    ctx,
                                    IntegerArgumentType.getInteger(ctx, "x"),
                                    IntegerArgumentType.getInteger(ctx, "z")
                            )))))
                .then(Commands.literal("verify_distribution")
                    .executes(ctx -> verifyLeylineDistribution(ctx, DEFAULT_DISTRIBUTION_SAMPLES))
                    .then(Commands.argument("samples", IntegerArgumentType.integer(10_000, 1_000_000))
                        .executes(ctx -> verifyLeylineDistribution(ctx, IntegerArgumentType.getInteger(ctx, "samples")))))
            )
        );
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Reset Stats
            vars.player_mana = 0;
            vars.player_max_mana = 100.0;
            vars.player_mana_egenerated_every_moment = 5.0; // Default regen interval
            vars.player_restore_magic_moment = 1.0; // Default restore amount
            
            // Reset Attributes
            vars.player_magic_attributes_earth = false;
            vars.player_magic_attributes_water = false;
            vars.player_magic_attributes_fire = false;
            vars.player_magic_attributes_wind = false;
            vars.player_magic_attributes_ether = false;
            vars.player_magic_attributes_none = false;
            vars.player_magic_attributes_imaginary_number = false;
            vars.player_magic_attributes_sword = false;
            
            // Reset Proficiency
            vars.proficiency_structural_analysis = 0.0;
            vars.proficiency_projection = 0.0;
            vars.proficiency_jewel_magic_shoot = 0.0;
            vars.proficiency_jewel_magic_release = 0.0;
            vars.proficiency_unlimited_blade_works = 0.0;
            vars.proficiency_gravity_magic = 0.0;
            vars.proficiency_reinforcement = 0.0;
            
            // Reset Magics
            vars.learned_magics.clear();
            vars.has_unlimited_blade_works = false;
            
            // Reset Cooldown
            player.getPersistentData().putBoolean("TypeMoonNoCooldown", false);
            
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Player stats and skills have been reset."), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int grantKing(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, "king_qualification");
            net.minecraft.advancements.AdvancementHolder holder = player.server.getAdvancements().get(id);
            if (holder == null) {
                ctx.getSource().sendFailure(Component.literal("Advancement not found: " + id));
                return 0;
            }
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(holder, criterion);
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("command.typemoonworld.king.granted"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int revokeKing(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, "king_qualification");
            net.minecraft.advancements.AdvancementHolder holder = player.server.getAdvancements().get(id);
            if (holder == null) {
                ctx.getSource().sendFailure(Component.literal("Advancement not found: " + id));
                return 0;
            }
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
            for (String criterion : progress.getCompletedCriteria()) {
                player.getAdvancements().revoke(holder, criterion);
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("command.typemoonworld.king.revoked"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setMaxLevel(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Max Stats
            vars.player_max_mana = 100000.0;
            vars.player_mana = vars.player_max_mana;
            vars.player_mana_egenerated_every_moment = 100.0; // High regen amount
            vars.player_restore_magic_moment = 1.0; // Fast regen interval (1 tick)
            
            // All Attributes
            vars.player_magic_attributes_earth = true;
            vars.player_magic_attributes_water = true;
            vars.player_magic_attributes_fire = true;
            vars.player_magic_attributes_wind = true;
            vars.player_magic_attributes_ether = true;
            vars.player_magic_attributes_none = true;
            vars.player_magic_attributes_imaginary_number = true;
            vars.player_magic_attributes_sword = true;
            
            // Max Proficiency
            vars.proficiency_structural_analysis = 100.0;
            vars.proficiency_projection = 100.0;
            vars.proficiency_jewel_magic_shoot = 100.0;
            vars.proficiency_jewel_magic_release = 100.0;
            vars.proficiency_unlimited_blade_works = 100.0;
            vars.proficiency_sword_barrel_full_open = 100.0;
            vars.proficiency_gravity_magic = 100.0;
            vars.proficiency_reinforcement = 100.0;
            
            // Learn All Magics
            for (String m : ALL_MAGICS) {
                if (!vars.learned_magics.contains(m)) {
                    vars.learned_magics.add(m);
                }
            }
            if (!vars.learned_magics.contains("reinforcement")) {
                vars.learned_magics.add("reinforcement");
            }
            vars.has_unlimited_blade_works = true;
            
            vars.syncPlayerVariables(player);
            ctx.getSource().sendSuccess(() -> Component.literal("Player set to MAX LEVEL with ALL SKILLS."), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int toggleCooldown(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            boolean current = player.getPersistentData().getBoolean("TypeMoonNoCooldown");
            boolean newState = !current;
            player.getPersistentData().putBoolean("TypeMoonNoCooldown", newState);
            
            if (newState) {
                ctx.getSource().sendSuccess(() -> Component.literal("No Cooldown Mode: ON"), true);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("No Cooldown Mode: OFF"), true);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
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
            for (String m : ALL_MAGICS) {
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

    private static int showLeylineHere(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            ChunkPos chunkPos = new ChunkPos(player.blockPosition());
            return printLeylineProfile(ctx, level, chunkPos.x, chunkPos.z);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Unable to resolve player position for leyline query."));
            return 0;
        }
    }

    private static int showLeylineChunk(CommandContext<CommandSourceStack> ctx, int chunkX, int chunkZ) {
        return printLeylineProfile(ctx, ctx.getSource().getLevel(), chunkX, chunkZ);
    }

    private static int printLeylineProfile(CommandContext<CommandSourceStack> ctx, ServerLevel level, int chunkX, int chunkZ) {
        var profile = LeylineService.getProfile(level, chunkX, chunkZ);
        String message = String.format(
                Locale.ROOT,
                "[Leyline] dim=%s chunk=(%d,%d) concentration=%d capacity=%d regenMultiplier=%.3f gemBonus=%s",
                level.dimension().location(),
                chunkX,
                chunkZ,
                profile.concentration(),
                profile.manaCapacity(),
                profile.regenMultiplier(),
                profile.gemTerrainBonusApplied() ? "yes" : "no"
        );
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int verifyLeylineDistribution(CommandContext<CommandSourceStack> ctx, int samples) {
        boolean allPass = true;

        for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
            DistributionStats stats = sampleDistribution(level, samples);
            boolean pass = stats.mean >= ACCEPT_MEAN_MIN
                    && stats.mean <= ACCEPT_MEAN_MAX
                    && stats.pGte80 <= ACCEPT_P80_MAX
                    && stats.p50To79 <= ACCEPT_P50_79_MAX
                    && stats.pLt30 >= ACCEPT_P_LT_30_MIN
                    && stats.pLt50 >= ACCEPT_P_LT_50_MIN;

            if (!pass) {
                allPass = false;
            }

            String detail = String.format(
                    Locale.ROOT,
                    "[Leyline Verify] dim=%s samples=%d mean=%.3f P(>=80)=%.4f P(50~79)=%.4f P(<30)=%.4f P(<50)=%.4f corr_adj=%.4f result=%s",
                    level.dimension().location(),
                    samples,
                    stats.mean,
                    stats.pGte80,
                    stats.p50To79,
                    stats.pLt30,
                    stats.pLt50,
                    stats.adjacentCorrelation,
                    pass ? "PASS" : "FAIL"
            );
            ctx.getSource().sendSuccess(() -> Component.literal(detail), false);
        }

        if (allPass) {
            ctx.getSource().sendSuccess(() -> Component.literal("Leyline distribution verification passed on all loaded dimensions."), true);
            return 1;
        }

        ctx.getSource().sendFailure(Component.literal("Leyline distribution verification failed in one or more loaded dimensions."));
        return 0;
    }

    private static DistributionStats sampleDistribution(ServerLevel level, int samples) {
        String dimId = level.dimension().location().toString();
        long seed = LeylineNoise.dimensionSeed(level.getSeed(), dimId) ^ 0x4F1BBCDC3A6D8E71L;
        SplittableRandom random = new SplittableRandom(seed);

        long sum = 0;
        int gte80 = 0;
        int range50To79 = 0;
        int lt30 = 0;
        int lt50 = 0;

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXX = 0.0;
        double sumYY = 0.0;
        double sumXY = 0.0;
        int pairs = 0;

        for (int i = 0; i < samples; i++) {
            int chunkX = random.nextInt(-SAMPLE_COORD_RANGE, SAMPLE_COORD_RANGE + 1);
            int chunkZ = random.nextInt(-SAMPLE_COORD_RANGE, SAMPLE_COORD_RANGE + 1);

            int concentration = LeylineService.getProfile(level, chunkX, chunkZ).concentration();
            sum += concentration;
            if (concentration >= 80) gte80++;
            if (concentration >= 50 && concentration <= 79) range50To79++;
            if (concentration < 30) lt30++;
            if (concentration < 50) lt50++;

            int eastConcentration = LeylineService.getProfile(level, chunkX + 1, chunkZ).concentration();
            double x = concentration;
            double y = eastConcentration;
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumYY += y * y;
            sumXY += x * y;
            pairs++;
        }

        double total = samples;
        double mean = sum / total;
        double pGte80 = gte80 / total;
        double p50To79 = range50To79 / total;
        double pLt30 = lt30 / total;
        double pLt50 = lt50 / total;
        double corr = pearson(sumX, sumY, sumXX, sumYY, sumXY, pairs);
        return new DistributionStats(mean, pGte80, p50To79, pLt30, pLt50, corr);
    }

    private static double pearson(double sumX, double sumY, double sumXX, double sumYY, double sumXY, int n) {
        if (n <= 1) {
            return 0.0;
        }
        double numerator = (n * sumXY) - (sumX * sumY);
        double left = (n * sumXX) - (sumX * sumX);
        double right = (n * sumYY) - (sumY * sumY);
        double denominator = Math.sqrt(Math.max(0.0, left * right));
        if (denominator <= 1.0E-9) {
            return 0.0;
        }
        return numerator / denominator;
    }

    private record DistributionStats(
            double mean,
            double pGte80,
            double p50To79,
            double pLt30,
            double pLt50,
            double adjacentCorrelation
    ) {
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
                case "jewel_magic": 
                    vars.proficiency_jewel_magic_shoot = value; 
                    vars.proficiency_jewel_magic_release = value;
                    break;
                case "unlimited_blade_works": vars.proficiency_unlimited_blade_works = value; break;
                case "gravity_magic": vars.proficiency_gravity_magic = value; break;
                case "reinforcement": vars.proficiency_reinforcement = value; break;
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

    private static int setShikiFavor(CommandContext<CommandSourceStack> ctx, int value) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            int count = 0;
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof RyougiShikiEntity shiki) {
                    if (value < -5) value = -5;
                    if (value > 5) value = 5;
                    int current = shiki.getFriendshipLevel();
                    shiki.setFriendshipLevel(value);
                    count++;
                }
            }
            final int finalValue = value;
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> Component.translatable("command.typemoonworld.favor.shiki.set", finalCount, finalValue), true);
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setMerlinFavor(CommandContext<CommandSourceStack> ctx, int value) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (value < -5) value = -5;
            if (value > 5) value = 5;
            vars.merlin_favor = value;
            vars.syncPlayerVariables(player);
            final int finalValue = value;
            ctx.getSource().sendSuccess(() -> Component.translatable("command.typemoonworld.favor.merlin.set", finalValue), true);
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

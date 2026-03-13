package net.xxxjk.TYPE_MOON_WORLD.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Locale;
import java.util.SplittableRandom;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineChunkProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineNoise;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;

public class TypeMoonCommands {
   private static final String BASIC_JEWEL_MAGIC_ID = "jewel_magic_shoot";
   private static final String ADVANCED_JEWEL_MAGIC_ID = "jewel_magic_release";
   private static final String RANDOM_JEWEL_MAGIC_ID = "jewel_random_shoot";
   private static final String MACHINE_GUN_MAGIC_ID = "jewel_machine_gun";
   private static final String GANDER_MAGIC_ID = "gander";
   private static final String GANDR_MACHINE_GUN_MAGIC_ID = "gandr_machine_gun";
   private static final int DEFAULT_DISTRIBUTION_SAMPLES = 200000;
   private static final int SAMPLE_COORD_RANGE = 2000000;
   private static final double ACCEPT_MEAN_MIN = 9.0;
   private static final double ACCEPT_MEAN_MAX = 11.0;
   private static final double ACCEPT_P80_MAX = 0.1;
   private static final double ACCEPT_P50_79_MAX = 0.2;
   private static final double ACCEPT_P_LT_30_MIN = 0.8;
   private static final double ACCEPT_P_LT_50_MIN = 0.9;
   private static final String[] PROFICIENCY_TYPES = new String[]{
      "structural_analysis",
      "projection",
      "jewel_magic",
      "jewel_magic_shoot",
      "jewel_magic_release",
      "unlimited_blade_works",
      "sword_barrel_full_open",
      "reinforcement",
      "gravity_magic",
      "gander"
   };
   private static final String[] ALL_MAGICS = new String[]{
      BASIC_JEWEL_MAGIC_ID,
      ADVANCED_JEWEL_MAGIC_ID,
      RANDOM_JEWEL_MAGIC_ID,
      MACHINE_GUN_MAGIC_ID,
      GANDR_MACHINE_GUN_MAGIC_ID,
      "projection",
      "structural_analysis",
      "broken_phantasm",
      "unlimited_blade_works",
      "sword_barrel_full_open",
      "reinforcement",
      "reinforcement_self",
      "reinforcement_other",
      "reinforcement_item",
      "gravity_magic",
      GANDER_MAGIC_ID
   };

   @SuppressWarnings({"unchecked", "rawtypes"})
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "typemoon"
                              )
                              .requires(source -> source.hasPermission(2)))
                           .then(Commands.literal("help").executes(TypeMoonCommands::showHelp)))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                "player"
                                             )
                                             .then(
                                                ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("stats")
                                                            .then(
                                                               Commands.literal("mana")
                                                                  .then(
                                                                     Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                                                        .executes(ctx -> setMana(ctx, DoubleArgumentType.getDouble(ctx, "value")))
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("max_mana")
                                                               .then(
                                                                  Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                                                     .executes(ctx -> setMaxMana(ctx, DoubleArgumentType.getDouble(ctx, "value")))
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("regen")
                                                            .then(
                                                               Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                                                  .executes(ctx -> setRegen(ctx, DoubleArgumentType.getDouble(ctx, "value")))
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal("restore")
                                                         .then(
                                                            Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                                               .executes(ctx -> setRestore(ctx, DoubleArgumentType.getDouble(ctx, "value")))
                                                         )
                                                   )
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                                        "attr"
                                                                     )
                                                                     .then(
                                                                        Commands.literal("earth")
                                                                           .then(
                                                                              Commands.argument("enabled", BoolArgumentType.bool())
                                                                                 .executes(
                                                                                    ctx -> setAttribute(ctx, "earth", BoolArgumentType.getBool(ctx, "enabled"))
                                                                                 )
                                                                           )
                                                                     ))
                                                                  .then(
                                                                     Commands.literal("water")
                                                                        .then(
                                                                           Commands.argument("enabled", BoolArgumentType.bool())
                                                                              .executes(
                                                                                 ctx -> setAttribute(ctx, "water", BoolArgumentType.getBool(ctx, "enabled"))
                                                                              )
                                                                        )
                                                                  ))
                                                               .then(
                                                                  Commands.literal("fire")
                                                                     .then(
                                                                        Commands.argument("enabled", BoolArgumentType.bool())
                                                                           .executes(ctx -> setAttribute(ctx, "fire", BoolArgumentType.getBool(ctx, "enabled")))
                                                                     )
                                                               ))
                                                            .then(
                                                               Commands.literal("wind")
                                                                  .then(
                                                                     Commands.argument("enabled", BoolArgumentType.bool())
                                                                        .executes(ctx -> setAttribute(ctx, "wind", BoolArgumentType.getBool(ctx, "enabled")))
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("ether")
                                                               .then(
                                                                  Commands.argument("enabled", BoolArgumentType.bool())
                                                                     .executes(ctx -> setAttribute(ctx, "ether", BoolArgumentType.getBool(ctx, "enabled")))
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("none")
                                                            .then(
                                                               Commands.argument("enabled", BoolArgumentType.bool())
                                                                  .executes(ctx -> setAttribute(ctx, "none", BoolArgumentType.getBool(ctx, "enabled")))
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal("imaginary_number")
                                                         .then(
                                                            Commands.argument("enabled", BoolArgumentType.bool())
                                                               .executes(ctx -> setAttribute(ctx, "imaginary_number", BoolArgumentType.getBool(ctx, "enabled")))
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal("sword")
                                                      .then(
                                                         Commands.argument("enabled", BoolArgumentType.bool())
                                                            .executes(ctx -> setAttribute(ctx, "sword", BoolArgumentType.getBool(ctx, "enabled")))
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("proficiency")
                                             .then(
                                                Commands.argument("type", StringArgumentType.word())
                                                   .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PROFICIENCY_TYPES, builder))
                                                   .then(
                                                      Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                                         .executes(
                                                            ctx -> setProficiency(
                                                               ctx, StringArgumentType.getString(ctx, "type"), DoubleArgumentType.getDouble(ctx, "value")
                                                            )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(Commands.literal("reset").executes(TypeMoonCommands::resetPlayer)))
                                 .then(Commands.literal("max").executes(TypeMoonCommands::setMaxLevel)))
                              .then(Commands.literal("cooldown").then(Commands.literal("toggle").executes(TypeMoonCommands::toggleCooldown)))
                        ))
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("magic")
                                    .then(
                                       Commands.literal("learn")
                                          .then(
                                             Commands.argument("magic_id", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ALL_MAGICS, builder))
                                                .executes(ctx -> learnMagic(ctx, StringArgumentType.getString(ctx, "magic_id")))
                                          )
                                    ))
                                 .then(
                                    Commands.literal("forget")
                                       .then(
                                          Commands.argument("magic_id", StringArgumentType.word())
                                             .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ALL_MAGICS, builder))
                                             .executes(ctx -> forgetMagic(ctx, StringArgumentType.getString(ctx, "magic_id")))
                                       )
                                 ))
                              .then(Commands.literal("learn_all").executes(TypeMoonCommands::learnAllMagics)))
                           .then(Commands.literal("forget_all").executes(TypeMoonCommands::forgetAllMagics))
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("npc")
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("shiki").then(Commands.literal("clear").executes(TypeMoonCommands::clearShiki)))
                                 .then(
                                    Commands.literal("health")
                                       .then(
                                          Commands.argument("value", DoubleArgumentType.doubleArg(1.0))
                                             .executes(ctx -> setShikiHealth(ctx, DoubleArgumentType.getDouble(ctx, "value")))
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("favor")
                                 .then(
                                    Commands.literal("merlin")
                                       .then(
                                          Commands.argument("value", IntegerArgumentType.integer(-5, 5))
                                             .executes(ctx -> setMerlinFavor(ctx, IntegerArgumentType.getInteger(ctx, "value")))
                                       )
                                 ))
                              .then(
                                 Commands.literal("shiki")
                                    .then(
                                       Commands.argument("value", IntegerArgumentType.integer(-5, 5))
                                          .executes(ctx -> setShikiFavor(ctx, IntegerArgumentType.getInteger(ctx, "value")))
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("progress")
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("king").then(Commands.literal("grant").executes(TypeMoonCommands::grantKing)))
                           .then(Commands.literal("revoke").executes(TypeMoonCommands::revokeKing))
                     )
               ))
            .then(
               Commands.literal("world")
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("leyline")
                              .then(Commands.literal("here").executes(TypeMoonCommands::showLeylineHere)))
                           .then(
                              Commands.literal("chunk")
                                 .then(
                                    Commands.argument("x", IntegerArgumentType.integer())
                                       .then(
                                          Commands.argument("z", IntegerArgumentType.integer())
                                             .executes(
                                                ctx -> showLeylineChunk(ctx, IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "z"))
                                             )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("verify_distribution").executes(ctx -> verifyLeylineDistribution(ctx, 200000)))
                              .then(
                                 Commands.argument("samples", IntegerArgumentType.integer(10000, 1000000))
                                    .executes(ctx -> verifyLeylineDistribution(ctx, IntegerArgumentType.getInteger(ctx, "samples")))
                              )
                        )
                  )
            )
      );
   }

   private static int showHelp(CommandContext<CommandSourceStack> ctx) {
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("TYPE-MOON-WORLD commands:"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon help"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon player stats mana|max_mana|regen|restore <value>"), false);
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.literal("/typemoon player attr earth|water|fire|wind|ether|none|imaginary_number|sword <true|false>"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon player proficiency <type> <0-100>"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon player reset | max | cooldown toggle"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon magic learn|forget <magic_id>"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon magic learn_all | forget_all"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon npc shiki clear | health <value>"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon npc favor merlin|shiki <-5..5>"), false);
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("/typemoon progress king grant | revoke"), false);
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.literal("/typemoon world leyline here | chunk <x> <z> | verify_distribution [samples]"), false);
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(() -> Component.literal("Note: /typemoon magic learn gandr_machine_gun requires gander learned + gander proficiency >= 50."), false);
      return 1;
   }

   private static int resetPlayer(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.player_mana = 0.0;
         vars.player_max_mana = 100.0;
         vars.player_mana_egenerated_every_moment = 5.0;
         vars.player_restore_magic_moment = 1.0;
         vars.player_magic_attributes_earth = false;
         vars.player_magic_attributes_water = false;
         vars.player_magic_attributes_fire = false;
         vars.player_magic_attributes_wind = false;
         vars.player_magic_attributes_ether = false;
         vars.player_magic_attributes_none = false;
         vars.player_magic_attributes_imaginary_number = false;
         vars.player_magic_attributes_sword = false;
         vars.proficiency_structural_analysis = 0.0;
         vars.proficiency_projection = 0.0;
         vars.proficiency_jewel_magic_shoot = 0.0;
         vars.proficiency_jewel_magic_release = 0.0;
         vars.proficiency_unlimited_blade_works = 0.0;
         vars.proficiency_gravity_magic = 0.0;
         vars.proficiency_reinforcement = 0.0;
         vars.proficiency_gander = 0.0;
         vars.learned_magics.clear();
         vars.has_unlimited_blade_works = false;
         player.getPersistentData().putBoolean("TypeMoonNoCooldown", false);
         vars.syncPlayerVariables(player);
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Player stats and skills have been reset."), true);
         return 1;
      } catch (Exception var3) {
         return 0;
      }
   }

   private static int grantKing(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "king_qualification");
         AdvancementHolder holder = player.server.getAdvancements().get(id);
         if (holder == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Advancement not found: " + id));
            return 0;
         } else {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);

            for (String criterion : progress.getRemainingCriteria()) {
               player.getAdvancements().award(holder, criterion);
            }

            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("command.typemoonworld.king.granted"), true);
            return 1;
         }
      } catch (Exception var7) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Error: " + var7.getMessage()));
         return 0;
      }
   }

   private static int revokeKing(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath("typemoonworld", "king_qualification");
         AdvancementHolder holder = player.server.getAdvancements().get(id);
         if (holder == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Advancement not found: " + id));
            return 0;
         } else {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);

            for (String criterion : progress.getCompletedCriteria()) {
               player.getAdvancements().revoke(holder, criterion);
            }

            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable("command.typemoonworld.king.revoked"), true);
            return 1;
         }
      } catch (Exception var7) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Error: " + var7.getMessage()));
         return 0;
      }
   }

   private static int setMaxLevel(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.player_max_mana = 100000.0;
         vars.player_mana = vars.player_max_mana;
         vars.player_mana_egenerated_every_moment = 100.0;
         vars.player_restore_magic_moment = 1.0;
         vars.player_magic_attributes_earth = true;
         vars.player_magic_attributes_water = true;
         vars.player_magic_attributes_fire = true;
         vars.player_magic_attributes_wind = true;
         vars.player_magic_attributes_ether = true;
         vars.player_magic_attributes_none = true;
         vars.player_magic_attributes_imaginary_number = true;
         vars.player_magic_attributes_sword = true;
         vars.proficiency_structural_analysis = 100.0;
         vars.proficiency_projection = 100.0;
         vars.proficiency_jewel_magic_shoot = 100.0;
         vars.proficiency_jewel_magic_release = 100.0;
         vars.proficiency_unlimited_blade_works = 100.0;
         vars.proficiency_sword_barrel_full_open = 100.0;
         vars.proficiency_gravity_magic = 100.0;
         vars.proficiency_reinforcement = 100.0;
         vars.proficiency_gander = 100.0;

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
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Player set to MAX LEVEL with ALL SKILLS."), true);
         return 1;
      } catch (Exception var7) {
         return 0;
      }
   }

   private static int toggleCooldown(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         boolean current = player.getPersistentData().getBoolean("TypeMoonNoCooldown");
         boolean newState = !current;
         player.getPersistentData().putBoolean("TypeMoonNoCooldown", newState);
         if (newState) {
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("No Cooldown Mode: ON"), true);
         } else {
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("No Cooldown Mode: OFF"), true);
         }

         return 1;
      } catch (Exception var4) {
         return 0;
      }
   }

   private static int setShikiHealth(CommandContext<CommandSourceStack> ctx, double health) {
      try {
         ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
         int count = 0;

         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof RyougiShikiEntity shiki) {
               shiki.setHealth((float)health);
               count++;
            }
         }

         int finalCount = count;
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.literal("Set health of " + finalCount + " Ryougi Shiki entities to " + health), true);
         return count;
      } catch (Exception var8) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Error setting health: " + var8.getMessage()));
         return 0;
      }
   }

   private static int clearShiki(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
         int count = 0;

         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof RyougiShikiEntity shiki) {
               shiki.discard();
               count++;
            }
         }

         int finalCount = count;
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Cleared " + finalCount + " Ryougi Shiki entities."), true);
         return count;
      } catch (Exception var6) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Error clearing entities: " + var6.getMessage()));
         return 0;
      }
   }

   private static int learnMagic(CommandContext<CommandSourceStack> ctx, String magicId) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (("jewel_magic_release".equals(magicId) || "jewel_machine_gun".equals(magicId)) && !vars.learned_magics.contains("jewel_magic_shoot")) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Learn basic jewel magic first: jewel_magic_shoot"));
            return 0;
         } else if ("jewel_machine_gun".equals(magicId) && !vars.learned_magics.contains("gander")) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Learn gander first: gander"));
            return 0;
         } else if ("gandr_machine_gun".equals(magicId) && !vars.learned_magics.contains("gander")) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Learn gander first: gander"));
            return 0;
         } else if ("gandr_machine_gun".equals(magicId) && vars.proficiency_gander < 50.0) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Gandr machine gun requires gander proficiency >= 50."));
            return 0;
         } else {
            if (isBasicOrRandomJewelMagic(magicId)) {
               boolean changed = false;
               if (!vars.learned_magics.contains("jewel_magic_shoot")) {
                  vars.learned_magics.add("jewel_magic_shoot");
                  changed = true;
               }

               if (!vars.learned_magics.contains("jewel_random_shoot")) {
                  vars.learned_magics.add("jewel_random_shoot");
                  changed = true;
               }

               vars.syncPlayerVariables(player);
               if (changed) {
                  ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Learned magics: jewel_magic_shoot, jewel_random_shoot"), true);
               } else {
                  ((CommandSourceStack)ctx.getSource())
                     .sendSuccess(() -> Component.literal("Magics already learned: jewel_magic_shoot, jewel_random_shoot"), false);
               }
            } else if (!vars.learned_magics.contains(magicId)) {
               vars.learned_magics.add(magicId);
               vars.syncPlayerVariables(player);
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Learned magic: " + magicId), true);
            } else {
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Magic already learned: " + magicId), false);
            }

            return 1;
         }
      } catch (Exception var5) {
         return 0;
      }
   }

   private static int forgetMagic(CommandContext<CommandSourceStack> ctx, String magicId) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (isBasicOrRandomJewelMagic(magicId)) {
            boolean removed = vars.learned_magics.remove("jewel_magic_shoot") | vars.learned_magics.remove("jewel_random_shoot");
            vars.syncPlayerVariables(player);
            if (removed) {
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Forgot magics: jewel_magic_shoot, jewel_random_shoot"), true);
            } else {
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Magics not found: jewel_magic_shoot, jewel_random_shoot"), false);
            }
         } else if (vars.learned_magics.contains(magicId)) {
            vars.learned_magics.remove(magicId);
            vars.syncPlayerVariables(player);
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Forgot magic: " + magicId), true);
         } else {
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Magic not found: " + magicId), false);
         }

         return 1;
      } catch (Exception var5) {
         return 0;
      }
   }

   private static boolean isBasicOrRandomJewelMagic(String magicId) {
      return "jewel_magic_shoot".equals(magicId) || "jewel_random_shoot".equals(magicId);
   }

   private static int learnAllMagics(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

         for (String m : ALL_MAGICS) {
            if (!vars.learned_magics.contains(m)) {
               vars.learned_magics.add(m);
            }
         }

         vars.syncPlayerVariables(player);
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Learned all magics"), true);
         return 1;
      } catch (Exception var7) {
         return 0;
      }
   }

   private static int forgetAllMagics(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.learned_magics.clear();
         vars.syncPlayerVariables(player);
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Forgot all magics"), true);
         return 1;
      } catch (Exception var3) {
         return 0;
      }
   }

   private static int showLeylineHere(CommandContext<CommandSourceStack> ctx) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         ServerLevel level = player.serverLevel();
         ChunkPos chunkPos = new ChunkPos(player.blockPosition());
         return printLeylineProfile(ctx, level, chunkPos.x, chunkPos.z);
      } catch (Exception var4) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Unable to resolve player position for leyline query."));
         return 0;
      }
   }

   private static int showLeylineChunk(CommandContext<CommandSourceStack> ctx, int chunkX, int chunkZ) {
      return printLeylineProfile(ctx, ((CommandSourceStack)ctx.getSource()).getLevel(), chunkX, chunkZ);
   }

   private static int printLeylineProfile(CommandContext<CommandSourceStack> ctx, ServerLevel level, int chunkX, int chunkZ) {
      LeylineChunkProfile profile = LeylineService.getProfile(level, chunkX, chunkZ);
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
      ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal(message), false);
      return 1;
   }

   private static int verifyLeylineDistribution(CommandContext<CommandSourceStack> ctx, int samples) {
      boolean allPass = true;

      for (ServerLevel level : ((CommandSourceStack)ctx.getSource()).getServer().getAllLevels()) {
         TypeMoonCommands.DistributionStats stats = sampleDistribution(level, samples);
         boolean pass = stats.mean >= 9.0 && stats.mean <= 11.0 && stats.pGte80 <= 0.1 && stats.p50To79 <= 0.2 && stats.pLt30 >= 0.8 && stats.pLt50 >= 0.9;
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
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal(detail), false);
      }

      if (allPass) {
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Leyline distribution verification passed on all loaded dimensions."), true);
         return 1;
      } else {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Leyline distribution verification failed in one or more loaded dimensions."));
         return 0;
      }
   }

   private static TypeMoonCommands.DistributionStats sampleDistribution(ServerLevel level, int samples) {
      String dimId = level.dimension().location().toString();
      long seed = LeylineNoise.dimensionSeed(level.getSeed(), dimId) ^ 5700357407426580081L;
      SplittableRandom random = new SplittableRandom(seed);
      long sum = 0L;
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
         int chunkX = random.nextInt(-2000000, 2000001);
         int chunkZ = random.nextInt(-2000000, 2000001);
         int concentration = LeylineService.getProfile(level, chunkX, chunkZ).concentration();
         sum += concentration;
         if (concentration >= 80) {
            gte80++;
         }

         if (concentration >= 50 && concentration <= 79) {
            range50To79++;
         }

         if (concentration < 30) {
            lt30++;
         }

         if (concentration < 50) {
            lt50++;
         }

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
      return new TypeMoonCommands.DistributionStats(mean, pGte80, p50To79, pLt30, pLt50, corr);
   }

   private static double pearson(double sumX, double sumY, double sumXX, double sumYY, double sumXY, int n) {
      if (n <= 1) {
         return 0.0;
      } else {
         double numerator = n * sumXY - sumX * sumY;
         double left = n * sumXX - sumX * sumX;
         double right = n * sumYY - sumY * sumY;
         double denominator = Math.sqrt(Math.max(0.0, left * right));
         return denominator <= 1.0E-9 ? 0.0 : numerator / denominator;
      }
   }

   private static int setProficiency(CommandContext<CommandSourceStack> ctx, String type, double value) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         value = Math.max(0.0, Math.min(100.0, value));
         boolean validType = true;
         switch (type) {
            case "structural_analysis":
               vars.proficiency_structural_analysis = value;
               break;
            case "projection":
               vars.proficiency_projection = value;
               break;
            case "jewel_magic":
               vars.proficiency_jewel_magic_shoot = value;
               vars.proficiency_jewel_magic_release = value;
               break;
            case "jewel_magic_shoot":
               vars.proficiency_jewel_magic_shoot = value;
               break;
            case "jewel_magic_release":
               vars.proficiency_jewel_magic_release = value;
               break;
            case "unlimited_blade_works":
               vars.proficiency_unlimited_blade_works = value;
               break;
            case "sword_barrel_full_open":
               vars.proficiency_sword_barrel_full_open = value;
               break;
            case "gravity_magic":
               vars.proficiency_gravity_magic = value;
               break;
            case "reinforcement":
               vars.proficiency_reinforcement = value;
               break;
            case "gander":
               vars.proficiency_gander = value;
               break;
            default:
               validType = false;
         }

         if (!validType) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Unknown proficiency type: " + type));
            return 0;
         } else {
            vars.syncPlayerVariables(player);
            double finalValue = value;
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Set Proficiency " + type + " to " + finalValue), true);
            return 1;
         }
      } catch (Exception var9) {
         return 0;
      }
   }

   private static int setMana(CommandContext<CommandSourceStack> ctx, double value) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         value = Math.max(0.0, Math.min(100000.0, value));
         vars.player_mana = value;
         vars.syncPlayerVariables(player);
         double finalValue = value;
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Set Mana to " + finalValue), true);
         return 1;
      } catch (Exception var7) {
         return 0;
      }
   }

   private static int setMaxMana(CommandContext<CommandSourceStack> ctx, double value) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         value = Math.max(0.0, Math.min(100000.0, value));
         vars.player_max_mana = value;
         vars.syncPlayerVariables(player);
         double finalValue = value;
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Set Max Mana to " + finalValue), true);
         return 1;
      } catch (Exception var7) {
         return 0;
      }
   }

   private static int setShikiFavor(CommandContext<CommandSourceStack> ctx, int value) {
      try {
         ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
         int count = 0;

         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof RyougiShikiEntity shiki) {
               if (value < -5) {
                  value = -5;
               }

               if (value > 5) {
                  value = 5;
               }

               shiki.setFriendshipLevel(value);
               count++;
            }
         }

         int finalValue = value;
         int finalCount = count;
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("command.typemoonworld.favor.shiki.set", new Object[]{finalCount, finalValue}), true);
         return count;
      } catch (Exception var7) {
         return 0;
      }
   }

   private static int setMerlinFavor(CommandContext<CommandSourceStack> ctx, int value) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (value < -5) {
            value = -5;
         }

         if (value > 5) {
            value = 5;
         }

         vars.merlin_favor = value;
         vars.syncPlayerVariables(player);
         int finalValue = value;
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("command.typemoonworld.favor.merlin.set", new Object[]{finalValue}), true);
         return 1;
      } catch (Exception var5) {
         return 0;
      }
   }

   private static int setRegen(CommandContext<CommandSourceStack> ctx, double value) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         value = Math.max(0.0, Math.min(60.0, value));
         vars.player_mana_egenerated_every_moment = value;
         vars.syncPlayerVariables(player);
         double finalValue = value;
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Set Mana Regen Interval to " + finalValue + "s"), true);
         return 1;
      } catch (Exception var7) {
         return 0;
      }
   }

   private static int setRestore(CommandContext<CommandSourceStack> ctx, double value) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         value = Math.max(0.0, Math.min(10000.0, value));
         vars.player_restore_magic_moment = value;
         vars.syncPlayerVariables(player);
         double finalValue = value;
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Set Mana Restore Amount to " + finalValue), true);
         return 1;
      } catch (Exception var7) {
         return 0;
      }
   }

   private static int setAttribute(CommandContext<CommandSourceStack> ctx, String attr, boolean enabled) {
      try {
         ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         switch (attr) {
            case "earth":
               vars.player_magic_attributes_earth = enabled;
               break;
            case "water":
               vars.player_magic_attributes_water = enabled;
               break;
            case "fire":
               vars.player_magic_attributes_fire = enabled;
               break;
            case "wind":
               vars.player_magic_attributes_wind = enabled;
               break;
            case "ether":
               vars.player_magic_attributes_ether = enabled;
               break;
            case "none":
               vars.player_magic_attributes_none = enabled;
               break;
            case "imaginary_number":
               vars.player_magic_attributes_imaginary_number = enabled;
               break;
            case "sword":
               vars.player_magic_attributes_sword = enabled;
         }

         vars.syncPlayerVariables(player);
         ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Set Attribute " + attr + " to " + enabled), true);
         return 1;
      } catch (Exception var7) {
         return 0;
      }
   }

   private record DistributionStats(double mean, double pGte80, double p50To79, double pLt30, double pLt50, double adjacentCorrelation) {
   }
}

package net.xxxjk.TYPE_MOON_WORLD.vfx.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXPerformanceBudget;
import net.xxxjk.TYPE_MOON_WORLD.vfx.data.EffectLibrary;

public final class VFXClientCommands {
   private VFXClientCommands() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         Commands.literal("vfx")
            .then(
               Commands.literal("reload")
                  .executes(ctx -> {
                     boolean ok = EffectLibrary.INSTANCE.reloadNow();
                     ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Reloaded VFX effects." : "Unable to reload VFX effects."), false);
                     return ok ? 1 : 0;
                  })
            )
            .then(
               Commands.literal("client")
                  .then(Commands.literal("quality")
                     .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal("VFX quality=" + Config.vfxQuality), false);
                        return 1;
                     })
                     .then(Commands.argument("value", StringArgumentType.word())
                        .executes(ctx -> {
                           String value = StringArgumentType.getString(ctx, "value").toUpperCase(java.util.Locale.ROOT);
                           VFXPerformanceBudget.Quality quality = VFXPerformanceBudget.qualityByName(value);
                           if (!value.equals(quality.name())) {
                              ctx.getSource().sendFailure(Component.literal("Quality must be AUTO, LOW, MEDIUM or HIGH."));
                              return 0;
                           }
                           Config.vfxQuality = quality.name();
                           ctx.getSource().sendSuccess(() -> Component.literal("VFX quality set to " + quality.name()), false);
                           return 1;
                        }))
                  )
            )
            .then(
               Commands.literal("debug")
                  .then(Commands.literal("stats").executes(ctx -> {
                     ctx.getSource().sendSuccess(() -> Component.literal(VFXRenderManager.debugStats()), false);
                     return 1;
                  }))
                  .then(Commands.literal("profile").executes(ctx -> {
                     ctx.getSource().sendSuccess(() -> Component.literal(VFXRenderManager.debugStats()), false);
                     return 1;
                  }))
            )
            .then(
               Commands.literal("loop")
                  .then(
                     Commands.literal("stop")
                        .executes(ctx -> {
                           boolean stopped = VFXClientLoop.stop();
                           ctx.getSource().sendSuccess(() -> Component.literal(stopped ? "Stopped VFX loop." : "No VFX loop is running."), false);
                           return stopped ? 1 : 0;
                        })
                  )
                  .then(
                     Commands.argument("effect_name", StringArgumentType.word())
                        .executes(ctx -> {
                           String effectId = StringArgumentType.getString(ctx, "effect_name");
                           if (!VFXClientLoop.start(effectId)) {
                              ctx.getSource().sendFailure(Component.literal("Unknown VFX effect: " + effectId));
                              return 0;
                           }
                           ctx.getSource().sendSuccess(() -> Component.literal("Looping VFX effect: " + effectId), false);
                           return 1;
                        })
                        .then(
                           Commands.argument("x", DoubleArgumentType.doubleArg())
                              .then(
                                 Commands.argument("y", DoubleArgumentType.doubleArg())
                                    .then(
                                       Commands.argument("z", DoubleArgumentType.doubleArg())
                                          .executes(ctx -> {
                                             String effectId = StringArgumentType.getString(ctx, "effect_name");
                                             double x = DoubleArgumentType.getDouble(ctx, "x");
                                             double y = DoubleArgumentType.getDouble(ctx, "y");
                                             double z = DoubleArgumentType.getDouble(ctx, "z");
                                             if (!VFXClientLoop.start(effectId, x, y, z)) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown VFX effect: " + effectId));
                                                return 0;
                                             }
                                             ctx.getSource()
                                                .sendSuccess(() -> Component.literal("Looping VFX effect: " + effectId + " at " + x + " " + y + " " + z), false);
                                             return 1;
                                          })
                                    )
                              )
                        )
                  )
            )
      );
   }
}

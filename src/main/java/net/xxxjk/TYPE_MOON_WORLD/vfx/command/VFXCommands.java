package net.xxxjk.TYPE_MOON_WORLD.vfx.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.vfx.network.VFXSpawnEffectMessage;

public final class VFXCommands {
   private static final double DEFAULT_RADIUS = 96.0;

   private VFXCommands() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         Commands.literal("vfx")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("test").executes(ctx -> spawnTest(ctx.getSource())))
            .then(
               Commands.literal("spawn")
                  .then(
                     Commands.argument("effect_name", StringArgumentType.word())
                        .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "effect_name"), null, null))
                        .then(
                           Commands.argument("x", DoubleArgumentType.doubleArg())
                              .then(
                                 Commands.argument("y", DoubleArgumentType.doubleArg())
                                    .then(
                                       Commands.argument("z", DoubleArgumentType.doubleArg())
                                          .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "effect_name"), pos(ctx), null))
                                          .then(
                                             Commands.argument("target_entity", EntityArgument.entity())
                                                .executes(
                                                   ctx -> spawn(
                                                      ctx.getSource(),
                                                      StringArgumentType.getString(ctx, "effect_name"),
                                                      pos(ctx),
                                                      EntityArgument.getEntity(ctx, "target_entity")
                                                   )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static double[] pos(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
      return new double[]{DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "y"), DoubleArgumentType.getDouble(ctx, "z")};
   }

   private static int spawnTest(CommandSourceStack source) {
      double[] origin = new double[]{source.getPosition().x, source.getPosition().y, source.getPosition().z};
      send(source, "test", origin, null);
      source.sendSuccess(() -> Component.literal("Spawned VFX test particles."), false);
      return 1;
   }

   private static int spawn(CommandSourceStack source, String effectId, double[] origin, Entity target) {
      double[] resolved = origin != null ? origin : new double[]{source.getPosition().x, source.getPosition().y, source.getPosition().z};
      send(source, effectId, resolved, target);
      source.sendSuccess(() -> Component.literal("Spawned VFX effect: " + effectId), false);
      return 1;
   }

   private static void send(CommandSourceStack source, String effectId, double[] origin, Entity target) {
      if (!(source.getLevel() instanceof ServerLevel level)) {
         return;
      }
      Optional<UUID> targetUuid = target == null ? Optional.empty() : Optional.of(target.getUUID());
      VFXSpawnEffectMessage message = new VFXSpawnEffectMessage(
         effectId,
         origin[0],
         origin[1],
         origin[2],
         targetUuid,
         level.dimension().location().toString(),
         level.getRandom().nextLong()
      );
      PacketDistributor.sendToPlayersNear(level, null, origin[0], origin[1], origin[2], DEFAULT_RADIUS, message, new CustomPacketPayload[0]);
   }
}

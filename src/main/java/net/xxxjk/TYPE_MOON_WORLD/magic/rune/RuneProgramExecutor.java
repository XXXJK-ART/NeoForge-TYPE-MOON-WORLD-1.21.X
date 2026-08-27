package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Server-only baseline executor. Definitions remain data-driven and safe to extend with richer effects. */
public final class RuneProgramExecutor {
   private RuneProgramExecutor() { }

   public static MagicExecutionResult execute(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, RuneProgram program) {
      if (player == null || vars == null || program == null) return MagicExecutionResult.FAILED;
      RuneProgramValidationResult validation = RuneProgramService.validate(vars, program);
      if (!validation.valid()) { player.displayClientMessage(Component.translatable("message.typemoonworld.rune.cast_failed"), true); return MagicExecutionResult.FAILED; }
      double cost = RuneProgramCostService.calculate(program);
      if (vars.player_mana < cost) { player.displayClientMessage(Component.translatable("message.typemoonworld.magic.insufficient_mana"), true); return MagicExecutionResult.FAILED; }
      vars.player_mana = Math.max(0.0D, vars.player_mana - cost);
      int triggerCount = 0;
      for (String id : program.slots(RunePosition.TRIGGER)) if (!id.isEmpty()) triggerCount++;
      for (String id : program.slots(RunePosition.EFFECT)) {
         if ("fire".equals(RuneRegistry.get(id) == null ? "" : RuneRegistry.get(id).semantic(RunePosition.EFFECT))) {
            LivingEntity target = player; target.setRemainingFireTicks(40);
         }
      }
      vars.magic_cooldown = Math.max(vars.magic_cooldown, 10.0D + triggerCount * 2.0D);
      vars.syncMana(player);
      return new MagicExecutionResult(true, true, cost, (int)vars.magic_cooldown);
   }
}

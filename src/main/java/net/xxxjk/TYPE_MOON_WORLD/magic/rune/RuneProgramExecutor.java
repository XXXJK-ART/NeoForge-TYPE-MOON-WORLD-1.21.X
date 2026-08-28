package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Server-only baseline executor. Definitions remain data-driven and safe to extend with richer effects. */
public final class RuneProgramExecutor {
   private RuneProgramExecutor() { }

   public static MagicExecutionResult execute(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, RuneProgram program) {
      return execute(player, vars, program, null);
   }

   public static MagicExecutionResult execute(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, RuneProgram program, LivingEntity explicitTarget) {
      if (player == null || vars == null || program == null) return new MagicExecutionResult(true, false, 0.0D, -1, "missing_program");
      RuneProgramValidationResult validation = RuneProgramService.validate(vars, program);
      if (!validation.valid()) { failMessage(player, validation.errors().isEmpty() ? "invalid_program" : validation.errors().get(0)); return new MagicExecutionResult(true, false, 0.0D, -1, validation.errors().isEmpty() ? "invalid_program" : validation.errors().get(0)); }
      if (!vars.is_magic_circuit_open) { failMessage(player, "circuit_closed"); return new MagicExecutionResult(true, false, 0.0D, -1, "circuit_closed"); }
      if (vars.magic_cooldown > 0.0D) { failMessage(player, "cooldown"); return new MagicExecutionResult(true, false, 0.0D, -1, "cooldown"); }
      double cost = RuneProgramCostService.calculate(program);
      if (vars.player_mana < cost) { player.displayClientMessage(Component.translatable("message.typemoonworld.magic.insufficient_mana"), true); return new MagicExecutionResult(true, false, 0.0D, -1, "insufficient_mana"); }
      int triggerCount = 0;
      for (String id : program.slots(RunePosition.TRIGGER)) if (!id.isEmpty()) triggerCount++;
      AABB area = player.getBoundingBox().inflate(program.releaseConfig().getDouble("radius") > 0 ? Math.min(16.0, program.releaseConfig().getDouble("radius")) : 4.0);
      LivingEntity target = explicitTarget != null && explicitTarget.isAlive() ? explicitTarget : rayTarget(player);
      if (target == null && program.releaseMode() != RuneReleaseMode.BODY) {
         target = player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
            e -> e != player && e.isAlive()).stream().findFirst().orElse(null);
      }
      if (target == null && (program.releaseMode() == RuneReleaseMode.BODY || !requiresTarget(program))) target = player;
      if (target == null) {
         vars.player_mana = Math.max(0.0D, vars.player_mana - cost);
         failMessage(player, "target_missing");
         applyFailureEasterEgg(player, program);
         vars.syncMana(player);
         return new MagicExecutionResult(true, false, cost, -1, "target_missing");
      }
      try {
         RuneExecutionContext execution = RuneReleaseService.executeEffects(player, target, program);
         if (execution == null || execution.failed()) {
            vars.player_mana = Math.max(0.0D, vars.player_mana - cost);
            String reason = execution == null ? "runtime_error" : execution.failureReason();
            failMessage(player, reason);
            applyFailureEasterEgg(player, program);
            vars.syncMana(player);
            return new MagicExecutionResult(true, false, cost, -1, reason);
         }
         vars.player_mana = Math.max(0.0D, vars.player_mana - cost);
         vars.magic_cooldown = Math.max(vars.magic_cooldown, 10.0D + triggerCount * 2.0D);
         vars.syncMana(player);
         return new MagicExecutionResult(true, true, cost, (int)vars.magic_cooldown, "");
      } catch (RuntimeException exception) {
         vars.player_mana = Math.max(0.0D, vars.player_mana - cost);
         failMessage(player, "runtime_error");
         applyFailureEasterEgg(player, program);
         vars.syncMana(player);
         return new MagicExecutionResult(true, false, cost, -1, "runtime_error");
      }
   }

   private static void failMessage(ServerPlayer player, String reason) {
      player.displayClientMessage(Component.translatable("message.typemoonworld.rune.cast_failed", reason), true);
   }

   private static boolean requiresTarget(RuneProgram program) {
      for (RunePosition position : RunePosition.values()) {
         for (String id : program.slots(position)) {
            RuneDefinition definition = RuneRegistry.get(id);
            if (definition == null) continue;
            String semantic = definition.semantic(position);
            if (switch (semantic) {
               case "wealth", "break", "storm", "command", "ignite", "bind", "need", "freeze", "shine", "judge", "chance" -> true;
               case "fire", "ice", "drain", "light", "shadow", "pierce", "water", "earth", "mind", "impact" -> true;
               case "strike", "discharge", "detonate", "execute", "burn", "purify", "disperse", "banish" -> true;
               default -> false;
            }) return true;
         }
      }
      return false;
   }

   private static LivingEntity rayTarget(ServerPlayer player) {
      if (EntityUtils.getRayTraceTarget(player, 32.0D) instanceof EntityHitResult hit
         && hit.getEntity() instanceof LivingEntity living && living.isAlive() && living != player) {
         return living;
      }
      return null;
   }

   private static void applyFailureEasterEgg(ServerPlayer player, RuneProgram program) {
      boolean lightning = program.slots().stream().map(RuneRegistry::get).filter(java.util.Objects::nonNull)
         .anyMatch(definition -> "storm".equals(definition.semantic(RunePosition.TRIGGER))
            || "break".equals(definition.semantic(RunePosition.TRIGGER)));
      long now = player.level().getGameTime();
      long last = player.getPersistentData().getLong("tmwRuneFailureLightning");
      if (lightning && now - last >= 200L) {
         player.getPersistentData().putLong("tmwRuneFailureLightning", now);
         player.hurt(player.damageSources().magic(), Math.min(4.0F, player.getMaxHealth() * 0.1F));
         player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY(0.6), player.getZ(), 16, .35, .7, .35, .05);
      }
   }
}

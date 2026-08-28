package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.SwordItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Server-side release hooks for rune programs and their non-projectile media. */
public final class RuneReleaseService {
   private static final String LAST_FAIL = "tmwRuneFailTick";
   private RuneReleaseService() { }

   public static boolean canUse(RuneProgram program, RuneReleaseMode mode, ItemStack stack) {
      if (program == null || mode == null) return false;
      if (program.releaseMode() != mode) return false;
      if (!RuneProgramService.matchesReleaseMode(program, mode)) return false;
      if ((mode == RuneReleaseMode.RUNE_STONE || mode == RuneReleaseMode.WEAPON || mode == RuneReleaseMode.ARMOR || mode == RuneReleaseMode.TOOL) && !mediumMatches(stack, mode)) return false;
      return mode != RuneReleaseMode.BODY || !program.hasRunes(RunePosition.TRIGGER);
   }

   public static boolean trigger(ServerPlayer player, RuneReleaseMode mode, ItemStack stack, LivingEntity target) {
      if (player == null || player.level().isClientSide()) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      RuneProgram program = current(vars);
      if (program == null) return false;
      if (target == null) target = player;
      return release(player, program, mode, stack, target).success();
   }

   public static boolean triggerProgram(ServerPlayer player, RuneProgram program, RuneReleaseMode mode, ItemStack stack, LivingEntity target) {
      if (player == null || program == null) return false;
      return release(player, program, mode, stack, target == null ? player : target).success();
   }

   /** Unified server release result used by UI, logging and tests. */
   public static MagicExecutionResult release(ServerPlayer player, RuneProgram program, RuneReleaseMode mode, ItemStack stack, LivingEntity target) {
      if (player == null || program == null) return new MagicExecutionResult(true, false, 0.0D, -1, "missing_program");
      if (!canUse(program, mode, stack)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.rune.cast_failed", "medium_incompatible"), true);
         return new MagicExecutionResult(true, false, 0.0D, -1, "medium_incompatible");
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return mode == RuneReleaseMode.DIRECT_AIR
         ? RuneProgramExecutor.execute(player, vars, program, target)
         : RuneProgramExecutor.executeExternal(player, vars, program, target, mode);
   }

   private static boolean mediumMatches(ItemStack stack, RuneReleaseMode mode) {
      if (stack == null || stack.isEmpty()) return mode == RuneReleaseMode.BODY;
      return switch (mode) {
         case RUNE_STONE -> !(stack.getItem() instanceof ArmorItem) && !(stack.getItem() instanceof TieredItem) && !(stack.getItem() instanceof SwordItem);
         case WEAPON -> stack.getAttributeModifiers().modifiers().stream().anyMatch(e -> e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
         case ARMOR -> stack.getItem() instanceof ArmorItem;
         case TOOL -> {
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            yield path.endsWith("pickaxe") || path.endsWith("axe") || path.endsWith("shovel") || path.endsWith("hoe") || path.endsWith("shears");
         }
         default -> true;
      };
   }

   public static RuneProgram current(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) return null;
      String id = net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService.getCurrentMagicId(vars);
      return RuneProgramService.find(vars, id);
   }

   public static void applyEffects(ServerPlayer caster, LivingEntity target, RuneProgram program) {
      if (caster == null || target == null || program == null) return;
      RuneEffectDispatcher.execute(new RuneExecutionContext(caster, target, program));
   }

   public static RuneExecutionContext executeEffects(ServerPlayer caster, LivingEntity target, RuneProgram program) {
      if (caster == null || target == null || program == null) return null;
      return RuneEffectDispatcher.execute(new RuneExecutionContext(caster, target, program));
   }

   public static void onDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      RuneProgram program = current(vars);
      if (program != null && program.releaseMode() == RuneReleaseMode.ARMOR && program.kind() == RuneProgramKind.REINFORCEMENT) {
         RuneEffectDispatcher.applyReinforcement(player, program);
      }
   }
}

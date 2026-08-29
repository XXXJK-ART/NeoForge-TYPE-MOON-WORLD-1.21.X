package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Server-owned CRUD facade for dynamic rune programs. */
public final class RuneProgramService {
   public static final int MAX_PROGRAMS = 20;
   private RuneProgramService() { }

   public static String dynamicId(UUID uuid) { return uuid == null ? "" : "rune_program/" + uuid; }
   public static boolean isDynamicId(String id) { if (id == null || !id.startsWith("rune_program/")) return false; try { UUID.fromString(id.substring(13)); return true; } catch (Exception ignored) { return false; } }
   public static UUID uuidFromDynamicId(String id) { return isDynamicId(id) ? UUID.fromString(id.substring(13)) : null; }
   public static RuneProgram find(TypeMoonWorldModVariables.PlayerVariables vars, UUID id) { if (vars == null || id == null) return null; for (RuneProgram p : vars.rune_programs) if (p != null && id.equals(p.uuid())) return p; return null; }
   public static RuneProgram find(TypeMoonWorldModVariables.PlayerVariables vars, String dynamicId) { return find(vars, uuidFromDynamicId(dynamicId)); }

   public static RuneProgramValidationResult validate(TypeMoonWorldModVariables.PlayerVariables vars, RuneProgram program) {
      if (vars == null || program == null) return RuneProgramValidationResult.failure("missing_program");
      RuneProgramValidationResult result = program.validate();
      List<String> errors = new ArrayList<>(result.errors());
      for (String id : program.slots()) if (!id.isEmpty() && !RuneLearningService.hasRune(vars, id)) errors.add("rune_not_learned:" + id);
      if (!RuneLearningService.hasOrigin(vars)) errors.add("origin_not_unlocked");
      return new RuneProgramValidationResult(errors.isEmpty(), errors);
   }

   /** Shared availability predicate used by wheel switching and cast execution. */
   public static boolean isRuneProgramCastable(TypeMoonWorldModVariables.PlayerVariables vars, RuneProgram program) {
      if (vars == null || program == null || !RuneLearningService.hasOrigin(vars)) return false;
      if (!validate(vars, program).valid()) return false;
      return switch (program.releaseMode()) {
         case DIRECT_AIR -> program.kind() == RuneProgramKind.FULL_RELEASE || program.kind() == RuneProgramKind.ENCHANTMENT;
         case BLOCK_TRAP -> program.kind() == RuneProgramKind.FULL_RELEASE;
         case WEAPON, TOOL -> program.kind() == RuneProgramKind.ENCHANTMENT;
         case ARMOR -> program.kind() == RuneProgramKind.REINFORCEMENT;
         case RUNE_STONE -> true;
         case BODY -> !program.hasRunes(RunePosition.TRIGGER);
      };
   }

   public static boolean matchesReleaseMode(RuneProgram program, RuneReleaseMode mode) {
      if (program == null || mode == null || program.kind() == RuneProgramKind.INVALID) return false;
      return switch (mode) {
         case RUNE_STONE -> true;
         case DIRECT_AIR -> program.kind() == RuneProgramKind.FULL_RELEASE || program.kind() == RuneProgramKind.ENCHANTMENT;
         case BLOCK_TRAP -> program.kind() == RuneProgramKind.FULL_RELEASE;
         case WEAPON, TOOL -> program.kind() == RuneProgramKind.ENCHANTMENT;
         case ARMOR -> program.kind() == RuneProgramKind.REINFORCEMENT;
         case BODY -> !program.hasRunes(RunePosition.TRIGGER);
      };
   }

   public static RuneProgram upsert(Player player, RuneProgram requested) {
      return upsert(player, requested == null ? null : requested.uuid(), requested);
   }

   /** Uses the pre-edit UUID as the update anchor so a stale client payload cannot create a duplicate program. */
   public static RuneProgram upsert(Player player, UUID persistedId, RuneProgram requested) {
      if (player == null || requested == null || player.level().isClientSide()) return null;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      RuneProgram normalized = RuneProgram.fromNBT(requested.serializeNBT());
      RuneProgram existing = find(vars, persistedId == null ? normalized.uuid() : persistedId);
      if (existing == null) {
         normalized = RuneProgram.ordered(UUID.randomUUID(), normalized.displayName(), normalized.sequence(), normalized.sequencePositions(),
            normalized.releaseMode(), normalizeReleaseConfig(normalized.releaseConfig()), System.currentTimeMillis(), System.currentTimeMillis());
      } else {
         normalized = RuneProgram.ordered(existing.uuid(), normalized.displayName(), normalized.sequence(), normalized.sequencePositions(),
            normalized.releaseMode(), normalizeReleaseConfig(normalized.releaseConfig()), existing.createdAt(), System.currentTimeMillis());
      }
      if (!matchesReleaseMode(normalized, normalized.releaseMode())) return null;
      if (!validate(vars, normalized).valid()) return null;
      if (existing == null && vars.rune_programs.size() >= MAX_PROGRAMS) return null;
      if (existing == null) vars.rune_programs.add(normalized); else vars.rune_programs.set(vars.rune_programs.indexOf(existing), normalized);
      vars.forceSyncPlayerVariables(player);
      return normalized;
   }

   public static boolean remove(Player player, UUID id) {
      if (player == null || id == null || player.level().isClientSide()) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean removed = vars.rune_programs.removeIf(p -> p != null && id.equals(p.uuid()));
      if (removed) { vars.removeWheelReferences(dynamicId(id)); vars.forceSyncPlayerVariables(player); }
      return removed;
   }

   public static RuneProgram copy(Player player, UUID id) {
      TypeMoonWorldModVariables.PlayerVariables vars = player == null ? null : player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      RuneProgram source = find(vars, id); if (source == null || vars.rune_programs.size() >= MAX_PROGRAMS) return null;
      RuneProgram copy = RuneProgram.fromNBT(source.serializeNBT());
      copy = RuneProgram.ordered(UUID.randomUUID(), source.displayName() + " copy", source.sequence(), source.sequencePositions(), source.releaseMode(), source.releaseConfig(), System.currentTimeMillis(), System.currentTimeMillis());
      return upsert(player, copy);
   }

   public static CompoundTag snapshot(TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag result = new CompoundTag(); if (vars == null) return result;
      net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag(); for (RuneProgram p : vars.rune_programs) if (p != null) list.add(p.serializeNBT()); result.put("programs", list); return result;
   }

   public static CompoundTag normalizeReleaseConfig(CompoundTag input) {
      CompoundTag result = new CompoundTag();
      if (input == null) return result;
      if (input.contains("radius")) result.putDouble("radius", Math.max(1.0D, Math.min(16.0D, input.getDouble("radius"))));
      if (input.contains("delay")) result.putInt("delay", Math.max(0, Math.min(1200, input.getInt("delay"))));
      if (input.contains("condition")) result.putString("condition", input.getString("condition").substring(0, Math.min(32, input.getString("condition").length())));
      return result;
   }
}

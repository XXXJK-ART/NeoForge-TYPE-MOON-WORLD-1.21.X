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

   public static RuneProgram upsert(Player player, RuneProgram requested) {
      if (player == null || requested == null || player.level().isClientSide()) return null;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      RuneProgram normalized = RuneProgram.fromNBT(requested.serializeNBT());
      if (!validate(vars, normalized).valid()) return null;
      RuneProgram existing = find(vars, normalized.uuid());
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
      copy = new RuneProgram(UUID.randomUUID(), source.displayName() + " copy", source.slots(RunePosition.TRIGGER), source.slots(RunePosition.EFFECT), source.slots(RunePosition.MODIFIER), source.slots(RunePosition.TERMINAL), source.releaseMode(), source.releaseConfig(), System.currentTimeMillis(), System.currentTimeMillis());
      return upsert(player, copy);
   }

   public static CompoundTag snapshot(TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag result = new CompoundTag(); if (vars == null) return result;
      net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag(); for (RuneProgram p : vars.rune_programs) if (p != null) list.add(p.serializeNBT()); result.put("programs", list); return result;
   }
}

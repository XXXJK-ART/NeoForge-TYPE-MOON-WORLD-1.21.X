package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/** A normalized player-owned 20-slot rune program. */
public final class RuneProgram {
   public static final int BAND_SIZE = 5;
   public static final int SLOT_COUNT = 20;
   public static final int MAX_NAME_LENGTH = 32;
   public static final int BYTECODE_VERSION = 1;
   private final UUID uuid;
   private String displayName;
   private final List<String> slots;
   private RuneReleaseMode releaseMode;
   private CompoundTag releaseConfig;
   private long createdAt;
   private long updatedAt;
   /** Ordered source and compiled form. Empty entries are omitted from the source. */
   private final List<String> sequence;
   private final List<RunePosition> sequencePositions;
   private int bytecodeVersion;
   private List<RuneInstruction> bytecode;

   public RuneProgram() { this(UUID.randomUUID(), "未命名卢恩程序", List.of(), List.of(), List.of(), List.of(), RuneReleaseMode.DIRECT_AIR, new CompoundTag(), System.currentTimeMillis(), System.currentTimeMillis()); }

   public RuneProgram(UUID uuid, String displayName, List<String> triggerSlots, List<String> effectSlots,
      List<String> modifierSlots, List<String> terminalSlots, RuneReleaseMode releaseMode,
      CompoundTag releaseConfig, long createdAt, long updatedAt) {
      this.uuid = uuid == null ? UUID.randomUUID() : uuid;
      this.displayName = normalizeName(displayName);
      this.slots = new ArrayList<>(SLOT_COUNT);
      addBand(triggerSlots); addBand(effectSlots); addBand(modifierSlots); addBand(terminalSlots);
      while (this.slots.size() < SLOT_COUNT) this.slots.add("");
      this.releaseMode = releaseMode == null ? RuneReleaseMode.DIRECT_AIR : releaseMode;
      this.releaseConfig = RuneProgramService.normalizeReleaseConfig(releaseConfig);
      this.createdAt = createdAt <= 0L ? System.currentTimeMillis() : createdAt;
      this.updatedAt = updatedAt <= 0L ? this.createdAt : updatedAt;
      this.sequence = new ArrayList<>();
      this.sequencePositions = new ArrayList<>();
      for (RunePosition position : RunePosition.values()) for (String id : slots(position)) if (id != null && !id.isBlank()) { this.sequence.add(id); this.sequencePositions.add(position); }
      this.bytecodeVersion = 0;
      this.bytecode = List.of();
      rebuildBytecode();
   }

   private RuneProgram(UUID uuid, String displayName, List<String> ordered, List<RunePosition> positions, RuneReleaseMode releaseMode,
      CompoundTag releaseConfig, long createdAt, long updatedAt, boolean orderedForm) {
      this.uuid = uuid == null ? UUID.randomUUID() : uuid;
      this.displayName = normalizeName(displayName);
      this.slots = new ArrayList<>(SLOT_COUNT);
      this.sequence = new ArrayList<>();
      this.sequencePositions = new ArrayList<>();
      if (ordered != null) for (String id : ordered) if (id != null && !id.isBlank()) this.sequence.add(id);
      if (positions != null) this.sequencePositions.addAll(positions);
      while (this.sequencePositions.size() < this.sequence.size()) this.sequencePositions.add(RunePosition.EFFECT);
      if (this.sequencePositions.size() > this.sequence.size()) this.sequencePositions.subList(this.sequence.size(), this.sequencePositions.size()).clear();
      while (this.slots.size() < SLOT_COUNT) this.slots.add("");
      for (int i = 0; i < Math.min(SLOT_COUNT, this.sequence.size()); i++) {
         RunePosition role = this.sequencePositions.get(i);
         int slot = role.ordinal() * BAND_SIZE;
         while (slot < role.ordinal() * BAND_SIZE + BAND_SIZE && !this.slots.get(slot).isEmpty()) slot++;
         if (slot < role.ordinal() * BAND_SIZE + BAND_SIZE) this.slots.set(slot, this.sequence.get(i));
      }
      this.releaseMode = releaseMode == null ? RuneReleaseMode.DIRECT_AIR : releaseMode;
      this.releaseConfig = RuneProgramService.normalizeReleaseConfig(releaseConfig);
      this.createdAt = createdAt <= 0L ? System.currentTimeMillis() : createdAt;
      this.updatedAt = updatedAt <= 0L ? this.createdAt : updatedAt;
      this.bytecodeVersion = 0;
      this.bytecode = List.of();
      rebuildBytecode();
   }

   public static RuneProgram ordered(UUID uuid, String displayName, List<String> ordered,
      RuneReleaseMode mode, CompoundTag config, long createdAt, long updatedAt) {
      return new RuneProgram(uuid, displayName, ordered, null, mode, config, createdAt, updatedAt, true);
   }
   public static RuneProgram ordered(UUID uuid, String displayName, List<String> ordered, List<RunePosition> positions,
      RuneReleaseMode mode, CompoundTag config, long createdAt, long updatedAt) {
      return new RuneProgram(uuid, displayName, ordered, positions, mode, config, createdAt, updatedAt, true);
   }

   private static String normalizeName(String name) {
      if (name == null || name.isBlank()) return "未命名卢恩程序";
      return name.substring(0, Math.min(MAX_NAME_LENGTH, name.length())).trim();
   }
   private void addBand(List<String> band) {
      if (band != null) for (int i = 0; i < BAND_SIZE; i++) slots.add(i < band.size() && band.get(i) != null ? band.get(i) : "");
      else for (int i = 0; i < BAND_SIZE; i++) slots.add("");
   }
   public UUID uuid() { return uuid; }
   public String displayName() { return displayName; }
   public void setDisplayName(String name) { displayName = normalizeName(name); touch(); }
   public RuneReleaseMode releaseMode() { return releaseMode; }
   public void setReleaseMode(RuneReleaseMode mode) { releaseMode = mode == null ? RuneReleaseMode.DIRECT_AIR : mode; touch(); }
   public CompoundTag releaseConfig() { return releaseConfig.copy(); }
   public void setReleaseConfig(CompoundTag config) { releaseConfig = RuneProgramService.normalizeReleaseConfig(config); touch(); }
   public long createdAt() { return createdAt; }
   public long updatedAt() { return updatedAt; }
   public String getSlot(RunePosition position, int index) { return position == null || index < 0 || index >= BAND_SIZE ? "" : slots.get(position.ordinal() * BAND_SIZE + index); }
   public void setSlot(RunePosition position, int index, String runeId) { if (position != null && index >= 0 && index < BAND_SIZE) { slots.set(position.ordinal() * BAND_SIZE + index, runeId == null ? "" : runeId); rebuildSequenceFromLegacySlots(); touch(); } }
   public List<String> slots() { return List.copyOf(slots); }
   public List<String> slots(RunePosition position) { List<String> result = new ArrayList<>(BAND_SIZE); for (int i = 0; i < BAND_SIZE; i++) result.add(getSlot(position, i)); return List.copyOf(result); }
   public List<String> sequence() { return Collections.unmodifiableList(sequence); }
   public List<String> orderedSlots() { return sequence(); }
   /** Returns trigger-delimited cast segments for Noita-style execution. */
   public List<List<String>> segments() {
      List<List<String>> result = new ArrayList<>();
      List<String> current = null;
      for (int i = 0; i < sequence.size(); i++) {
         if (i < sequencePositions.size() && sequencePositions.get(i) == RunePosition.TRIGGER) {
            current = new ArrayList<>();
            result.add(current);
         }
         if (current == null) {
            current = new ArrayList<>();
            result.add(current);
         }
         current.add(sequence.get(i));
      }
      return result.stream().map(List::copyOf).toList();
   }
   public List<RunePosition> sequencePositions() { return Collections.unmodifiableList(sequencePositions); }
   public boolean hasRunes(RunePosition position) {
      return position != null && sequencePositions.stream().anyMatch(position::equals);
   }
   public boolean isEmptyProgram() { return sequence.isEmpty(); }
   public RuneProgramKind kind() {
      if (hasRunes(RunePosition.TRIGGER)) return RuneProgramKind.FULL_RELEASE;
      boolean effect = hasRunes(RunePosition.EFFECT);
      boolean modifier = hasRunes(RunePosition.MODIFIER);
      boolean terminal = hasRunes(RunePosition.TERMINAL);
      if (effect && !terminal) return RuneProgramKind.ENCHANTMENT;
      if (!effect && (modifier || terminal)) return RuneProgramKind.REINFORCEMENT;
      if (!effect && !modifier && !terminal) return RuneProgramKind.INSCRIPTION;
      return RuneProgramKind.INVALID;
   }
   public int bytecodeVersion() { return bytecodeVersion; }
   public List<RuneInstruction> bytecode() { if (bytecodeVersion != BYTECODE_VERSION) rebuildBytecode(); return Collections.unmodifiableList(bytecode); }
   public int compiledBytecodeVersion() { return bytecodeVersion; }
   public void invalidateBytecode() { bytecodeVersion = 0; bytecode = List.of(); }
   public RuneProgram copy() { return RuneProgram.ordered(uuid, displayName, sequence, sequencePositions, releaseMode, releaseConfig, createdAt, updatedAt); }
   private void touch() { updatedAt = Math.max(System.currentTimeMillis(), updatedAt + 1L); }

   private void rebuildSequenceFromLegacySlots() {
      sequence.clear();
      sequencePositions.clear();
      for (RunePosition position : RunePosition.values()) for (String id : slots(position)) if (id != null && !id.isBlank()) { sequence.add(id); sequencePositions.add(position); }
      rebuildBytecode();
   }

   public void rebuildBytecode() {
      List<RuneInstruction> parsed = new ArrayList<>();
      for (String id : sequence) {
         RuneDefinition definition = RuneRegistry.get(id);
         if (definition != null) parsed.add(new RuneInstruction(id, definition.semantic(RunePosition.TRIGGER), definition.semantic(RunePosition.EFFECT), definition.semantic(RunePosition.MODIFIER), definition.semantic(RunePosition.TERMINAL)));
      }
      bytecode = List.copyOf(parsed);
      bytecodeVersion = BYTECODE_VERSION;
   }

   public RuneProgramValidationResult validate() {
      List<String> errors = new ArrayList<>();
      for (String id : slots) if (!id.isEmpty() && !RuneRegistry.isKnown(id)) errors.add("unknown_rune:" + id);
      for (String id : sequence) if (!id.isEmpty() && !RuneRegistry.isKnown(id)) errors.add("unknown_rune:" + id);
      // Effect and terminal bands without a trigger or a reinforcement-only
      // shape cannot be released by any medium. Surface this in the editor
      // instead of letting the server reject the save with no useful state.
      if (kind() == RuneProgramKind.INVALID) errors.add("invalid_structure");
      if (releaseMode == RuneReleaseMode.BODY && slots(RunePosition.TRIGGER).stream().anyMatch(id -> !id.isEmpty())) errors.add("release_mode_conflict");
      return new RuneProgramValidationResult(errors.isEmpty(), errors);
   }

   private static boolean isProjectileTrigger(RuneDefinition definition) {
      String id = definition.idPath();
      return id.equals("fehu") || id.equals("thurisaz") || id.equals("ansuz")
         || id.equals("hagalaz") || id.equals("kenaz") || id.equals("sowilo")
         || id.equals("laguz");
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.putString("uuid", uuid.toString()); tag.putString("display_name", displayName);
      tag.putString("release_mode", releaseMode.name()); tag.put("release_config", releaseConfig.copy());
      tag.putLong("created_at", createdAt); tag.putLong("updated_at", updatedAt);
      ListTag list = new ListTag(); for (String id : slots) list.add(StringTag.valueOf(id == null ? "" : id)); tag.put("slots", list);
      ListTag ordered = new ListTag(); for (String id : sequence) ordered.add(StringTag.valueOf(id)); tag.put("sequence", ordered);
      ListTag roles = new ListTag(); for (RunePosition position : sequencePositions) roles.add(StringTag.valueOf(position.name())); tag.put("sequence_roles", roles);
      tag.putInt("bytecode_version", bytecodeVersion); ListTag code = new ListTag(); for (RuneInstruction instruction : bytecode()) code.add(instruction.toNBT()); tag.put("bytecode", code);
      return tag;
   }
   public static RuneProgram fromNBT(CompoundTag tag) {
      if (tag == null) return new RuneProgram();
      UUID id; try { id = UUID.fromString(tag.getString("uuid")); } catch (Exception ignored) { id = UUID.randomUUID(); }
      List<String> slots = new ArrayList<>(); if (tag.contains("slots", 9)) { ListTag list = tag.getList("slots", 8); for (int i = 0; i < Math.min(SLOT_COUNT, list.size()); i++) slots.add(list.getString(i)); }
      while (slots.size() < SLOT_COUNT) slots.add("");
      List<String> ordered = new ArrayList<>();
      List<RunePosition> roles = new ArrayList<>();
      if (tag.contains("sequence", 9)) { ListTag list = tag.getList("sequence", 8); for (int i = 0; i < list.size(); i++) if (!list.getString(i).isBlank()) ordered.add(list.getString(i)); }
      if (tag.contains("sequence_roles", 9)) { ListTag list = tag.getList("sequence_roles", 8); for (int i = 0; i < list.size(); i++) try { roles.add(RunePosition.valueOf(list.getString(i))); } catch (Exception ignored) { roles.add(RunePosition.EFFECT); } }
      if (ordered.isEmpty()) for (RunePosition position : RunePosition.values()) for (int i = 0; i < BAND_SIZE; i++) { String value = slots.get(position.ordinal() * BAND_SIZE + i); if (!value.isBlank()) { ordered.add(value); roles.add(position); } }
      RuneProgram result = RuneProgram.ordered(id, tag.getString("display_name"), ordered, roles, RuneReleaseMode.byName(tag.getString("release_mode")), tag.contains("release_config", 10) ? tag.getCompound("release_config") : new CompoundTag(), tag.getLong("created_at"), tag.getLong("updated_at"));
      if (tag.getInt("bytecode_version") != BYTECODE_VERSION) result.rebuildBytecode();
      return result;
   }

   public record RuneInstruction(String runeId, String trigger, String effect, String modifier, String terminal) {
      CompoundTag toNBT() { CompoundTag tag = new CompoundTag(); tag.putString("id", runeId); tag.putString("trigger", trigger); tag.putString("effect", effect); tag.putString("modifier", modifier); tag.putString("terminal", terminal); return tag; }
   }
}

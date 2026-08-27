package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/** A normalized player-owned 20-slot rune program. */
public final class RuneProgram {
   public static final int BAND_SIZE = 5;
   public static final int SLOT_COUNT = 20;
   public static final int MAX_NAME_LENGTH = 32;
   private final UUID uuid;
   private String displayName;
   private final List<String> slots;
   private RuneReleaseMode releaseMode;
   private CompoundTag releaseConfig;
   private long createdAt;
   private long updatedAt;

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
      this.releaseConfig = releaseConfig == null ? new CompoundTag() : releaseConfig.copy();
      this.createdAt = createdAt <= 0L ? System.currentTimeMillis() : createdAt;
      this.updatedAt = updatedAt <= 0L ? this.createdAt : updatedAt;
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
   public void setReleaseConfig(CompoundTag config) { releaseConfig = config == null ? new CompoundTag() : config.copy(); touch(); }
   public long createdAt() { return createdAt; }
   public long updatedAt() { return updatedAt; }
   public String getSlot(RunePosition position, int index) { return position == null || index < 0 || index >= BAND_SIZE ? "" : slots.get(position.ordinal() * BAND_SIZE + index); }
   public void setSlot(RunePosition position, int index, String runeId) { if (position != null && index >= 0 && index < BAND_SIZE) { slots.set(position.ordinal() * BAND_SIZE + index, runeId == null ? "" : runeId); touch(); } }
   public List<String> slots() { return List.copyOf(slots); }
   public List<String> slots(RunePosition position) { List<String> result = new ArrayList<>(BAND_SIZE); for (int i = 0; i < BAND_SIZE; i++) result.add(getSlot(position, i)); return List.copyOf(result); }
   public RuneProgram copy() { return new RuneProgram(uuid, displayName, slots(RunePosition.TRIGGER), slots(RunePosition.EFFECT), slots(RunePosition.MODIFIER), slots(RunePosition.TERMINAL), releaseMode, releaseConfig, createdAt, updatedAt); }
   private void touch() { updatedAt = Math.max(System.currentTimeMillis(), updatedAt + 1L); }

   public RuneProgramValidationResult validate() {
      List<String> errors = new ArrayList<>();
      for (String id : slots) if (!id.isEmpty() && !RuneRegistry.isKnown(id)) errors.add("unknown_rune:" + id);
      boolean trigger = !slots(RunePosition.TRIGGER).stream().allMatch(String::isEmpty);
      if (!trigger) errors.add("missing_trigger");
      if (releaseMode != RuneReleaseMode.DIRECT_AIR && slots(RunePosition.TRIGGER).stream().anyMatch(id -> !id.isEmpty() && (releaseMode == RuneReleaseMode.WEAPON || releaseMode == RuneReleaseMode.ARMOR || releaseMode == RuneReleaseMode.TOOL || releaseMode == RuneReleaseMode.BODY))) errors.add("release_mode_conflict");
      return new RuneProgramValidationResult(errors.isEmpty(), errors);
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      tag.putString("uuid", uuid.toString()); tag.putString("display_name", displayName);
      tag.putString("release_mode", releaseMode.name()); tag.put("release_config", releaseConfig.copy());
      tag.putLong("created_at", createdAt); tag.putLong("updated_at", updatedAt);
      ListTag list = new ListTag(); for (String id : slots) list.add(StringTag.valueOf(id == null ? "" : id)); tag.put("slots", list);
      return tag;
   }
   public static RuneProgram fromNBT(CompoundTag tag) {
      UUID id; try { id = UUID.fromString(tag.getString("uuid")); } catch (Exception ignored) { id = UUID.randomUUID(); }
      List<String> slots = new ArrayList<>(); if (tag.contains("slots", 9)) { ListTag list = tag.getList("slots", 8); for (int i = 0; i < Math.min(SLOT_COUNT, list.size()); i++) slots.add(list.getString(i)); }
      while (slots.size() < SLOT_COUNT) slots.add("");
      return new RuneProgram(id, tag.getString("display_name"), slots.subList(0, 5), slots.subList(5, 10), slots.subList(10, 15), slots.subList(15, 20), RuneReleaseMode.byName(tag.getString("release_mode")), tag.contains("release_config", 10) ? tag.getCompound("release_config") : new CompoundTag(), tag.getLong("created_at"), tag.getLong("updated_at"));
   }
}

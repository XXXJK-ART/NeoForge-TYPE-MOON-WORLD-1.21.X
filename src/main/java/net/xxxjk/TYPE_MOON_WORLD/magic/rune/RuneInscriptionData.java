package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

/** Stable payload schema stored in the dedicated rune_inscription data component. */
public record RuneInscriptionData(String programId, CompoundTag snapshot, int remainingUses, RuneReleaseMode medium, String state) {
   public static final int BYTECODE_VERSION = RuneProgram.BYTECODE_VERSION;
   public RuneInscriptionData {
      programId = programId == null ? "" : programId;
      snapshot = snapshot == null ? new CompoundTag() : snapshot.copy();
      remainingUses = Math.max(0, Math.min(20, remainingUses));
      medium = medium == null ? RuneReleaseMode.RUNE_STONE : medium;
      state = state == null ? "ready" : state;
   }
   public CustomData toComponent() {
      CompoundTag tag = new CompoundTag(); tag.putString("program_id", programId); tag.put("snapshot", snapshot.copy()); tag.putInt("remaining_uses", remainingUses); tag.putString("medium", medium.name()); tag.putString("state", state); tag.putInt("bytecode_version", BYTECODE_VERSION); return CustomData.of(tag);
   }
   public static RuneInscriptionData fromComponent(CustomData data) {
      CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
      CompoundTag snapshot = tag.contains("snapshot", 10) ? tag.getCompound("snapshot") : tag;
      String programId = tag.contains("program_id") ? tag.getString("program_id") : tag.getString("rune_program");
      RuneReleaseMode medium = RuneReleaseMode.byName(tag.getString("medium"));
      if (medium == RuneReleaseMode.DIRECT_AIR && tag.getBoolean("rune_stone")) medium = RuneReleaseMode.RUNE_STONE;
      int uses = tag.contains("remaining_uses") ? tag.getInt("remaining_uses") : tag.getInt("uses");
      return new RuneInscriptionData(programId, snapshot, uses, medium, tag.getString("state").isBlank() ? "ready" : tag.getString("state"));
   }
}

package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

/** Stable payload schema stored in the dedicated rune_inscription data component. */
public record RuneInscriptionData(String programId, CompoundTag snapshot, int remainingUses, RuneReleaseMode medium, String state) {
   public RuneInscriptionData {
      programId = programId == null ? "" : programId;
      snapshot = snapshot == null ? new CompoundTag() : snapshot.copy();
      remainingUses = Math.max(0, Math.min(20, remainingUses));
      medium = medium == null ? RuneReleaseMode.RUNE_STONE : medium;
      state = state == null ? "ready" : state;
   }
   public CustomData toComponent() {
      CompoundTag tag = new CompoundTag(); tag.putString("program_id", programId); tag.put("snapshot", snapshot.copy()); tag.putInt("remaining_uses", remainingUses); tag.putString("medium", medium.name()); tag.putString("state", state); return CustomData.of(tag);
   }
   public static RuneInscriptionData fromComponent(CustomData data) {
      CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
      return new RuneInscriptionData(tag.getString("program_id"), tag.getCompound("snapshot"), tag.getInt("remaining_uses"), RuneReleaseMode.byName(tag.getString("medium")), tag.getString("state"));
   }
}

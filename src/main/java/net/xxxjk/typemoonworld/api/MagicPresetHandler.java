package net.xxxjk.typemoonworld.api;

import net.minecraft.nbt.CompoundTag;

public interface MagicPresetHandler {
   default CompoundTag normalize(CompoundTag payload) {
      return payload == null ? new CompoundTag() : payload.copy();
   }

   default boolean isValidForNpc(CompoundTag payload) {
      return true;
   }
}

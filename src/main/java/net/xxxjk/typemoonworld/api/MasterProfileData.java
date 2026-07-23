package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

public record MasterProfileData(
   ResourceLocation id,
   String nameKey,
   String commandSpellStyle,
   double maximumMana,
   double regenerationAmount,
   int regenerationIntervalTicks
) {
   public MasterProfileData {
      if (id == null) throw new IllegalArgumentException("id");
      nameKey = nameKey == null || nameKey.isBlank() ? "master." + id.getNamespace() + "." + id.getPath() : nameKey;
      commandSpellStyle = commandSpellStyle == null || commandSpellStyle.isBlank() ? "default" : commandSpellStyle;
      maximumMana = Math.max(1.0, maximumMana);
      regenerationAmount = Math.max(0.0, regenerationAmount);
      regenerationIntervalTicks = Math.max(1, regenerationIntervalTicks);
   }
}
